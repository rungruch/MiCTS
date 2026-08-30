package com.parallelc.micts.capture

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.parallelc.micts.R
import com.parallelc.micts.data.CaptureFiles
import com.parallelc.micts.data.ProjectionConsentStore
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CaptureResult
import com.parallelc.micts.ui.activity.FallbackActivity
import com.parallelc.micts.ui.activity.MainActivity
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {
    companion object {
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"
        private const val EXTRA_FROM_STORED_CONSENT = "from_stored_consent"
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 2301
        private const val CAPTURE_TIMEOUT_MS = 7_000L

        fun createIntent(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            fromStoredConsent: Boolean = false,
        ): Intent =
            Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_FROM_STORED_CONSENT, fromStoredConsent)
            }
    }

    private val completed = AtomicBoolean(false)
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var fromStoredConsent = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            if (completed.get()) return
            if (fromStoredConsent) {
                // An in-memory consent token that stops right away is dead:
                // drop it so the next trigger asks for approval once more.
                ProjectionConsentStore.clear()
                completeFailure(CaptureFailureReason.CONSENT_EXPIRED)
            } else {
                completeFailure(CaptureFailureReason.PROJECTION_STOPPED)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        workerThread = HandlerThread("MiCTSCapture").apply { start() }
        workerHandler = Handler(workerThread.looper)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            completeFailure(CaptureFailureReason.INVALID_PERMISSION_RESULT)
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        @Suppress("DEPRECATION")
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            completeFailure(CaptureFailureReason.INVALID_PERMISSION_RESULT)
            return START_NOT_STICKY
        }
        fromStoredConsent = intent.getBooleanExtra(EXTRA_FROM_STORED_CONSENT, false)

        startCaptureForeground()
        CaptureFiles.prepareForCapture(this)
        workerHandler.post { beginProjection(resultCode, resultData) }
        workerHandler.postDelayed(
            { completeFailure(CaptureFailureReason.TIMED_OUT) },
            CAPTURE_TIMEOUT_MS,
        )
        return START_NOT_STICKY
    }

    private fun beginProjection(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        val projection = runCatching {
            projectionManager.getMediaProjection(resultCode, resultData)
        }.getOrElse {
            onConsentRejected()
            return
        } ?: run {
            onConsentRejected()
            return
        }

        runCatching {
            mediaProjection = projection
            projection.registerCallback(projectionCallback, workerHandler)

            val displayInfo = getDisplayInfo()
            val reader = ImageReader.newInstance(
                displayInfo.width,
                displayInfo.height,
                PixelFormat.RGBA_8888,
                2,
            )
            imageReader = reader
            reader.setOnImageAvailableListener({ availableReader ->
                captureImage(availableReader, displayInfo.width, displayInfo.height)
            }, workerHandler)

            virtualDisplay = projection.createVirtualDisplay(
                "MiCTS one-shot capture",
                displayInfo.width,
                displayInfo.height,
                displayInfo.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                workerHandler,
            )
        }.onFailure {
            completeFailure(CaptureFailureReason.SERVICE_START_FAILED)
        }
    }

    private fun onConsentRejected() {
        if (fromStoredConsent) {
            // Android invalidated the in-memory approval (OEM policy): clear
            // it so the next trigger asks exactly once more.
            ProjectionConsentStore.clear()
            completeFailure(CaptureFailureReason.CONSENT_EXPIRED)
        } else {
            completeFailure(CaptureFailureReason.SERVICE_START_FAILED)
        }
    }

    private fun captureImage(reader: ImageReader, width: Int, height: Int) {
        if (completed.get()) return
        val image = reader.acquireLatestImage() ?: return
        runCatching {
            val plane = image.planes.firstOrNull()
                ?: error("Captured image has no pixel plane")
            val bitmap = PixelBufferExtractor.extract(
                buffer = plane.buffer,
                width = width,
                height = height,
                pixelStride = plane.pixelStride,
                rowStride = plane.rowStride,
            )
            val result = try {
                BitmapCaptureWriter.write(this, bitmap)
            } finally {
                bitmap.recycle()
            }
            when (result) {
                is CaptureResult.Success -> completeSuccess(result.probablyProtected)
                CaptureResult.PermissionDenied ->
                    completeFailure(CaptureFailureReason.INVALID_PERMISSION_RESULT)
                is CaptureResult.Failure -> completeFailure(result.reason)
            }
        }.onFailure {
            completeFailure(CaptureFailureReason.WRITE_FAILED)
        }.also {
            image.close()
        }
    }

    private fun completeSuccess(probablyProtected: Boolean) {
        if (!completed.compareAndSet(false, true)) return
        routeCapture(probablyProtected)
        releaseResources()
        stopSelf()
    }

    private fun completeFailure(reason: CaptureFailureReason) {
        if (!completed.compareAndSet(false, true)) return
        workerHandler.removeCallbacksAndMessages(null)
        launchFallbackActivity(false, reason)
        releaseResources()
        stopSelf()
    }

    private fun routeCapture(probablyProtected: Boolean) {
        launchFallbackActivity(probablyProtected, null)
    }

    private fun launchFallbackActivity(
        probablyProtected: Boolean,
        failureReason: CaptureFailureReason?,
        autoLens: Boolean = false,
    ) {
        val intent = FallbackActivity.createIntent(this, probablyProtected, failureReason, autoLens).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun releaseResources() {
        workerHandler.removeCallbacksAndMessages(null)
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.let { projection ->
            runCatching { projection.unregisterCallback(projectionCallback) }
            runCatching { projection.stop() }
        }
        mediaProjection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        if (::workerHandler.isInitialized) releaseResources()
        if (::workerThread.isInitialized) workerThread.quitSafely()
        super.onDestroy()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startCaptureForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_service)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.capture_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun getDisplayInfo(): DisplayInfo {
        val windowManager = getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            DisplayInfo(bounds.width(), bounds.height(), resources.configuration.densityDpi)
        } else {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)
            DisplayInfo(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        }
    }

    private data class DisplayInfo(
        val width: Int,
        val height: Int,
        val densityDpi: Int,
    )
}
