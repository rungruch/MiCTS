package com.parallelc.micts.domain

sealed interface CapturePermissionAction {
    data object CaptureWithStoredConsent : CapturePermissionAction
    data object RequestMediaProjection : CapturePermissionAction
}

/**
 * Routes Lens-fallback capture consent without any Accessibility service.
 *
 * Android 13 (API 33) and below allow an approved MediaProjection consent
 * token to be reused for later one-shot captures while the app process remains
 * alive, so later captures can run silently after a single approval.
 * Android 14 (API 34) makes every consent token single-use, so each capture
 * there must show the system dialog.
 */
class CapturePermissionCoordinator {
    fun nextAction(
        apiLevel: Int,
        consentStored: Boolean,
    ): CapturePermissionAction =
        if (apiLevel < API_ANDROID_14 && consentStored) {
            CapturePermissionAction.CaptureWithStoredConsent
        } else {
            CapturePermissionAction.RequestMediaProjection
        }

    private companion object {
        const val API_ANDROID_14 = 34
    }
}
