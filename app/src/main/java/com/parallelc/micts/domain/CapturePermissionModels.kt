package com.parallelc.micts.domain

enum class CaptureMode {
    UNSET,
    FAST_ACCESSIBILITY,
    ASK_EVERY_TIME,
}

enum class FastCaptureAvailability {
    UNSUPPORTED,
    DISABLED,
    CONNECTING,
    READY,
}

sealed interface CapturePermissionAction {
    data object ShowFastSetup : CapturePermissionAction
    data object ShowLegacyExplanation : CapturePermissionAction
    data object CaptureWithAccessibility : CapturePermissionAction
    data object WaitForAccessibility : CapturePermissionAction
    data object ShowAccessibilityRecovery : CapturePermissionAction
    data object RequestMediaProjection : CapturePermissionAction
}

class CapturePermissionCoordinator {
    fun nextAction(
        apiLevel: Int,
        mode: CaptureMode,
        fastAvailability: FastCaptureAvailability,
        legacyExplanationSeen: Boolean,
    ): CapturePermissionAction {
        if (apiLevel < 30) {
            return if (legacyExplanationSeen) {
                CapturePermissionAction.RequestMediaProjection
            } else {
                CapturePermissionAction.ShowLegacyExplanation
            }
        }

        return when (mode) {
            CaptureMode.UNSET -> CapturePermissionAction.ShowFastSetup
            CaptureMode.ASK_EVERY_TIME -> CapturePermissionAction.RequestMediaProjection
            CaptureMode.FAST_ACCESSIBILITY -> when (fastAvailability) {
                FastCaptureAvailability.READY ->
                    CapturePermissionAction.CaptureWithAccessibility
                FastCaptureAvailability.CONNECTING ->
                    CapturePermissionAction.WaitForAccessibility
                FastCaptureAvailability.DISABLED,
                FastCaptureAvailability.UNSUPPORTED,
                -> CapturePermissionAction.ShowAccessibilityRecovery
            }
        }
    }
}
