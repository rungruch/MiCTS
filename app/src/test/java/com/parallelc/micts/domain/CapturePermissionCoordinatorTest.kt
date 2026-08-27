package com.parallelc.micts.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CapturePermissionCoordinatorTest {
    private val coordinator = CapturePermissionCoordinator()

    @Test
    fun unsetModeShowsSetupOnReusableConsentApis() {
        listOf(28, 29, 30, 31, 33).forEach { api ->
            assertEquals(CapturePermissionAction.ShowCaptureSetup, action(api))
        }
    }

    @Test
    fun rememberConsentCapturesSilentlyOnlyWithStoredToken() {
        listOf(28, 30, 33).forEach { api ->
            assertEquals(
                CapturePermissionAction.CaptureWithStoredConsent,
                action(api, CaptureMode.REMEMBER_CONSENT, consentStored = true),
            )
            assertEquals(
                CapturePermissionAction.RequestMediaProjection,
                action(api, CaptureMode.REMEMBER_CONSENT, consentStored = false),
            )
        }
    }

    @Test
    fun askEveryTimeAlwaysRequestsProjection() {
        listOf(28, 33, 34, 36).forEach { api ->
            assertEquals(
                CapturePermissionAction.RequestMediaProjection,
                action(api, CaptureMode.ASK_EVERY_TIME, consentStored = true),
            )
        }
    }

    @Test
    fun android14ExplainsSingleUseConsentOnceThenAlwaysAsks() {
        listOf(34, 35, 36).forEach { api ->
            assertEquals(
                CapturePermissionAction.ShowConsentExplanation,
                action(api, explanationSeen = false),
            )
            assertEquals(
                CapturePermissionAction.RequestMediaProjection,
                action(api, explanationSeen = true),
            )
        }
    }

    @Test
    fun android14NeverReusesStoredConsent() {
        // A restored REMEMBER_CONSENT backup must degrade to ask-every-time
        // because Android 14+ tokens are single-use.
        assertEquals(
            CapturePermissionAction.RequestMediaProjection,
            action(
                34,
                CaptureMode.REMEMBER_CONSENT,
                consentStored = true,
                explanationSeen = true,
            ),
        )
    }

    private fun action(
        api: Int,
        mode: CaptureMode = CaptureMode.UNSET,
        consentStored: Boolean = false,
        explanationSeen: Boolean = false,
    ) = coordinator.nextAction(api, mode, consentStored, explanationSeen)
}
