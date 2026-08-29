package com.parallelc.micts.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CapturePermissionCoordinatorTest {
    private val coordinator = CapturePermissionCoordinator()

    @Test
    fun reusableConsentApisRequestProjectionWithoutStoredConsent() {
        listOf(28, 29, 30, 31, 33).forEach { api ->
            assertEquals(
                CapturePermissionAction.RequestMediaProjection,
                action(api, consentStored = false),
            )
        }
    }

    @Test
    fun reusableConsentApisCaptureSilentlyWithStoredConsent() {
        listOf(28, 29, 30, 31, 33).forEach { api ->
            assertEquals(
                CapturePermissionAction.CaptureWithStoredConsent,
                action(api, consentStored = true),
            )
        }
    }

    @Test
    fun android14AndLaterAlwaysRequestFreshProjection() {
        listOf(34, 35, 36, 37).forEach { api ->
            listOf(false, true).forEach { consentStored ->
                assertEquals(
                    CapturePermissionAction.RequestMediaProjection,
                    action(api, consentStored),
                )
            }
        }
    }

    private fun action(
        api: Int,
        consentStored: Boolean = false,
    ) = coordinator.nextAction(api, consentStored)
}
