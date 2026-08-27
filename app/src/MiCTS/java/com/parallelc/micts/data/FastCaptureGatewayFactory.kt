package com.parallelc.micts.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import com.parallelc.micts.capture.AccessibilityCaptureBridge
import com.parallelc.micts.capture.MiCTSAccessibilityCaptureService
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CaptureResult
import com.parallelc.micts.domain.FastCaptureAvailability
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

object FastCaptureGatewayFactory {
    fun create(context: Context): FastCaptureGateway =
        AccessibilityFastCaptureGateway(context.applicationContext)
}

private class AccessibilityFastCaptureGateway(
    private val context: Context,
) : FastCaptureGateway {
    override fun availability(): FastCaptureAvailability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return FastCaptureAvailability.UNSUPPORTED
        }
        if (AccessibilityCaptureBridge.service.value != null) {
            return FastCaptureAvailability.READY
        }
        return if (isServiceEnabled()) {
            FastCaptureAvailability.CONNECTING
        } else {
            FastCaptureAvailability.DISABLED
        }
    }

    override suspend fun awaitReady(timeoutMillis: Long): FastCaptureAvailability {
        when (val current = availability()) {
            FastCaptureAvailability.READY,
            FastCaptureAvailability.DISABLED,
            FastCaptureAvailability.UNSUPPORTED,
            -> return current
            FastCaptureAvailability.CONNECTING -> Unit
        }
        val connected = withTimeoutOrNull(timeoutMillis) {
            AccessibilityCaptureBridge.service.filterNotNull().first()
        }
        return if (connected != null) {
            FastCaptureAvailability.READY
        } else if (isServiceEnabled()) {
            FastCaptureAvailability.CONNECTING
        } else {
            FastCaptureAvailability.DISABLED
        }
    }

    override suspend fun capture(): CaptureResult {
        val service = AccessibilityCaptureBridge.service.value
            ?: return CaptureResult.Failure(CaptureFailureReason.ACCESSIBILITY_DISCONNECTED)
        return service.captureFrame()
    }

    private fun isServiceEnabled(): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val expected = ComponentName(context, MiCTSAccessibilityCaptureService::class.java)
        return manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
        ).any { info ->
            val serviceInfo = info.resolveInfo.serviceInfo
            ComponentName(serviceInfo.packageName, serviceInfo.name) == expected
        }
    }
}
