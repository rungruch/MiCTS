package com.parallelc.micts.capture

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.parallelc.micts.data.CaptureFiles
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CaptureResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MiCTSAccessibilityCaptureService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = 0
            flags = 0
        }
        AccessibilityCaptureBridge.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        AccessibilityCaptureBridge.detach(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AccessibilityCaptureBridge.detach(this)
        super.onDestroy()
    }

    internal suspend fun captureFrame(): CaptureResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return CaptureResult.Failure(CaptureFailureReason.ACCESSIBILITY_DISABLED)
        }
        CaptureFiles.prepareForCapture(this)
        var result = safeTakeScreenshotOnce()
        if (result == CaptureResult.Failure(CaptureFailureReason.SCREENSHOT_RATE_LIMITED)) {
            delay(RETRY_DELAY_MS)
            result = safeTakeScreenshotOnce()
        }
        if (result !is CaptureResult.Success) CaptureFiles.deleteCapture(this)
        return result
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun safeTakeScreenshotOnce(): CaptureResult = try {
        takeScreenshotOnce()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        CaptureResult.Failure(CaptureFailureReason.SCREENSHOT_INTERNAL_ERROR)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun takeScreenshotOnce(): CaptureResult {
        val attempt = suspendCancellableCoroutine<ScreenshotAttempt> { continuation ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        if (continuation.isActive) {
                            continuation.resume(ScreenshotAttempt.Success(screenshot))
                        } else {
                            screenshot.hardwareBuffer.close()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        if (continuation.isActive) {
                            continuation.resume(ScreenshotAttempt.Failure(errorCode))
                        }
                    }
                },
            )
        }

        return when (attempt) {
            is ScreenshotAttempt.Failure -> CaptureResult.Failure(mapFailure(attempt.errorCode))
            is ScreenshotAttempt.Success -> withContext(Dispatchers.IO) {
                val buffer = attempt.result.hardwareBuffer
                try {
                    val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                        buffer,
                        attempt.result.colorSpace,
                    ) ?: return@withContext CaptureResult.Failure(
                        CaptureFailureReason.EMPTY_IMAGE,
                    )
                    val softwareBitmap = try {
                        hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                    } finally {
                        hardwareBitmap.recycle()
                    } ?: return@withContext CaptureResult.Failure(
                        CaptureFailureReason.EMPTY_IMAGE,
                    )
                    try {
                        BitmapCaptureWriter.write(this@MiCTSAccessibilityCaptureService, softwareBitmap)
                    } finally {
                        softwareBitmap.recycle()
                    }
                } catch (_: Throwable) {
                    CaptureResult.Failure(CaptureFailureReason.SCREENSHOT_INTERNAL_ERROR)
                } finally {
                    buffer.close()
                }
            }
        }
    }

    private fun mapFailure(errorCode: Int): CaptureFailureReason = when (errorCode) {
        ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS ->
            CaptureFailureReason.ACCESSIBILITY_DISABLED
        ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT ->
            CaptureFailureReason.SCREENSHOT_RATE_LIMITED
        ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY,
        ERROR_TAKE_SCREENSHOT_INVALID_WINDOW,
        -> CaptureFailureReason.INVALID_DISPLAY
        ERROR_TAKE_SCREENSHOT_SECURE_WINDOW -> CaptureFailureReason.SECURE_WINDOW
        else -> CaptureFailureReason.SCREENSHOT_INTERNAL_ERROR
    }

    private sealed interface ScreenshotAttempt {
        data class Success(val result: ScreenshotResult) : ScreenshotAttempt
        data class Failure(val errorCode: Int) : ScreenshotAttempt
    }

    private companion object {
        const val RETRY_DELAY_MS = 500L
    }
}
