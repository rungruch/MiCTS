package com.parallelc.micts.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CapturePermissionCoordinatorTest {
    private val coordinator = CapturePermissionCoordinator()

    @Test
    fun api28And29ExplainOnceThenRequestProjection() {
        listOf(28, 29).forEach { api ->
            assertEquals(
                CapturePermissionAction.ShowLegacyExplanation,
                action(api, legacySeen = false),
            )
            assertEquals(
                CapturePermissionAction.RequestMediaProjection,
                action(api, legacySeen = true),
            )
        }
    }

    @Test
    fun unsetModeShowsFastSetupOnSupportedApis() {
        listOf(30, 33, 34, 36).forEach { api ->
            assertEquals(CapturePermissionAction.ShowFastSetup, action(api))
        }
    }

    @Test
    fun askEveryTimeAlwaysRequestsProjection() {
        FastCaptureAvailability.entries.forEach { availability ->
            assertEquals(
                CapturePermissionAction.RequestMediaProjection,
                action(36, CaptureMode.ASK_EVERY_TIME, availability),
            )
        }
    }

    @Test
    fun fastModeRoutesReadyConnectingAndUnavailableStates() {
        listOf(30, 31, 33, 34, 36).forEach { api ->
            assertEquals(
                CapturePermissionAction.CaptureWithAccessibility,
                action(api, CaptureMode.FAST_ACCESSIBILITY, FastCaptureAvailability.READY),
            )
            assertEquals(
                CapturePermissionAction.WaitForAccessibility,
                action(api, CaptureMode.FAST_ACCESSIBILITY, FastCaptureAvailability.CONNECTING),
            )
            listOf(
                FastCaptureAvailability.DISABLED,
                FastCaptureAvailability.UNSUPPORTED,
            ).forEach { availability ->
                assertEquals(
                    CapturePermissionAction.ShowAccessibilityRecovery,
                    action(api, CaptureMode.FAST_ACCESSIBILITY, availability),
                )
            }
        }
    }

    private fun action(
        api: Int,
        mode: CaptureMode = CaptureMode.UNSET,
        availability: FastCaptureAvailability = FastCaptureAvailability.DISABLED,
        legacySeen: Boolean = false,
    ) = coordinator.nextAction(api, mode, availability, legacySeen)
}
