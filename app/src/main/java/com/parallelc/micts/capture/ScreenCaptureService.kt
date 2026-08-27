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
import com.parallelc.micts.data.TriggerPreferenceStore
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.TriggerStrategy
import com.parallelc.micts.ui.activity.CropActivity
import com.parallelc.micts.ui.activity.MainActivity
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {
    companion object {
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 2301
        private const val CAPTURE_TIMEOUT_MS = 7_000L

        fun createIntent(context: Context, resultCode: Int, resultData: Intent): Intent =
            Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
    }

    private val completed = AtomicBoolean(false)
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            if (!completed.get()) {
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
        runCatching {
            val projectionManager = getSystemService(
                MediaProjectionManager::class.java,
            )
            val projection = projectionManager.getMediaProjection(resultCode, resultData)
                ?: error("Android did not create a MediaProjection")
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

    private fun captureImage(reader: ImageReader, width: Int, height: Int) {
        if (completed.get()) return
        val image = reader.acquireLatestImage() ?: return
        runCatching {
            val plane = image.planes.firstOrNull()
                ?: error("Captured image has no pixel plane")
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val paddedWidth = width + rowPadding / pixelStride
            val paddedBitmap = Bitmap.createBitmap(
                paddedWidth,
                height,
                Bitmap.Config.ARGB_8888,
            )
            paddedBitmap.copyPixelsFromBuffer(plane.buffer)
            val bitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
            if (bitmap !== paddedBitmap) paddedBitmap.recycle()

            val probablyProtected = isProbablyProtected(bitmap)
            val output = CaptureFiles.capture(this)
            val written = FileOutputStream(output).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            bitmap.recycle()
            if (!written) error("Could not encode capture")
            completeSuccess(probablyProtected)
        }.onFailure {
            completeFailure(CaptureFailureReason.WRITE_FAILED)
        }.also {
            image.close()
        }
    }

    private fun isProbablyProtected(bitmap: Bitmap): Boolean {
        val samplesPerAxis = 12
        for (xIndex in 0 until samplesPerAxis) {
            for (yIndex in 0 until samplesPerAxis) {
                val x = ((xIndex + 0.5f) * bitmap.width / samplesPerAxis)
                    .toInt().coerceIn(0, bitmap.width - 1)
                val y = ((yIndex + 0.5f) * bitmap.height / samplesPerAxis)
                    .toInt().coerceIn(0, bitmap.height - 1)
                if ((bitmap.getPixel(x, y) and 0x00FFFFFF) > 0x00010101) return false
            }
        }
        return true
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
        launchCropActivity(false, reason)
        releaseResources()
        stopSelf()
    }

    private fun routeCapture(probablyProtected: Boolean) {
        // "Google Lens directly" hands the full frame to Lens from within
        // CropActivity (a foreground Activity) because starting Lens from this
        // background Service trips Android's background-activity-start limit.
        val directLens = runCatching {
            TriggerPreferenceStore(this).strategy == TriggerStrategy.DIRECT_LENS && !probablyProtected
        }.getOrDefault(false)
        launchCropActivity(probablyProtected, null, autoLens = directLens)
    }

    private fun launchCropActivity(
        probablyProtected: Boolean,
        failureReason: CaptureFailureReason?,
        autoLens: Boolean = false,
    ) {
        val intent = CropActivity.createIntent(this, probablyProtected, failureReason, autoLens).apply {
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
