package com.parallelc.micts.domain

enum class CaptureMode {
    UNSET,
    REMEMBER_CONSENT,
    ASK_EVERY_TIME,
}

sealed interface CapturePermissionAction {
    data object ShowCaptureSetup : CapturePermissionAction
    data object ShowConsentExplanation : CapturePermissionAction
    data object CaptureWithStoredConsent : CapturePermissionAction
    data object RequestMediaProjection : CapturePermissionAction
}

/**
 * Routes Lens-fallback capture consent without any Accessibility service.
 *
 * Android 13 (API 33) and below allow an approved MediaProjection consent
 * token to be reused for later one-shot captures while the app process remains
 * alive, so "remember consent" captures silently after a single approval.
 * Android 14 (API 34) makes every consent token single-use, so each capture
 * there must show the system dialog.
 */
class CapturePermissionCoordinator {
    fun nextAction(
        apiLevel: Int,
        mode: CaptureMode,
        consentStored: Boolean,
        explanationSeen: Boolean,
    ): CapturePermissionAction {
        if (apiLevel >= API_ANDROID_14) {
            // Tokens are single-use on Android 14+: explain once, then every
            // capture asks for fresh consent regardless of the stored mode.
            return if (explanationSeen || mode != CaptureMode.UNSET) {
                CapturePermissionAction.RequestMediaProjection
            } else {
                CapturePermissionAction.ShowConsentExplanation
            }
        }

        return when (mode) {
            CaptureMode.UNSET -> CapturePermissionAction.ShowCaptureSetup
            CaptureMode.ASK_EVERY_TIME -> CapturePermissionAction.RequestMediaProjection
            CaptureMode.REMEMBER_CONSENT ->
                if (consentStored) {
                    CapturePermissionAction.CaptureWithStoredConsent
                } else {
                    CapturePermissionAction.RequestMediaProjection
                }
        }
    }

    private companion object {
        const val API_ANDROID_14 = 34
    }
}
