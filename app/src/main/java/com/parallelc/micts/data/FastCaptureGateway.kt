package com.parallelc.micts.data

import com.parallelc.micts.domain.CaptureResult
import com.parallelc.micts.domain.FastCaptureAvailability

interface FastCaptureGateway {
    fun availability(): FastCaptureAvailability

    suspend fun awaitReady(timeoutMillis: Long): FastCaptureAvailability

    suspend fun capture(): CaptureResult
}
