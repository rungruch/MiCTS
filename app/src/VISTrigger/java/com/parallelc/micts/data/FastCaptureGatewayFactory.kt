package com.parallelc.micts.data

import android.content.Context
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CaptureResult
import com.parallelc.micts.domain.FastCaptureAvailability

object FastCaptureGatewayFactory {
    fun create(context: Context): FastCaptureGateway = object : FastCaptureGateway {
        override fun availability(): FastCaptureAvailability =
            FastCaptureAvailability.UNSUPPORTED

        override suspend fun awaitReady(timeoutMillis: Long): FastCaptureAvailability =
            FastCaptureAvailability.UNSUPPORTED

        override suspend fun capture(): CaptureResult = CaptureResult.Failure(
            CaptureFailureReason.ACCESSIBILITY_DISABLED,
        )
    }
}
