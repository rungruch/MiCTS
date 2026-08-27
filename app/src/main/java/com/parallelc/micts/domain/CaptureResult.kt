package com.parallelc.micts.domain

sealed interface CaptureResult {
    data class Success(val probablyProtected: Boolean) : CaptureResult
    data object PermissionDenied : CaptureResult
    data class Failure(val reason: CaptureFailureReason) : CaptureResult
}

enum class CaptureFailureReason {
    INVALID_PERMISSION_RESULT,
    SERVICE_START_FAILED,
    PROJECTION_STOPPED,
    CONSENT_EXPIRED,
    TIMED_OUT,
    EMPTY_IMAGE,
    WRITE_FAILED,
    UNKNOWN,
}
