package com.parallelc.micts.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerCoordinatorTest {
    private val coordinator = TriggerCoordinator()

    @Test
    fun firstAutoLaunchInvokesNative() {
        assertEquals(
            TriggerAction.InvokeNative,
            coordinator.nextAction(TriggerStrategy.AUTO, AutoResolution.UNKNOWN),
        )
    }

    @Test
    fun acceptedFirstNativeAttemptBecomesPendingConfirmation() {
        assertEquals(
            TriggerTransition(
                TriggerAction.Finish,
                AutoResolution.PENDING_CONFIRMATION,
            ),
            coordinator.afterNative(
                TriggerStrategy.AUTO,
                AutoResolution.UNKNOWN,
                NativeTriggerResult.AcceptedUnverified,
            ),
        )
    }

    @Test
    fun pendingAutoLaunchRequestsConfirmation() {
        assertEquals(
            TriggerAction.RequestNativeConfirmation,
            coordinator.nextAction(
                TriggerStrategy.AUTO,
                AutoResolution.PENDING_CONFIRMATION,
            ),
        )
    }

    @Test
    fun confirmationKeepsNativeAndInvokesItImmediately() {
        assertEquals(
            TriggerTransition(
                TriggerAction.InvokeNative,
                AutoResolution.NATIVE_CONFIRMED,
            ),
            coordinator.afterConfirmation(nativeWorked = true),
        )
    }

    @Test
    fun failedConfirmationRemembersAndStartsLens() {
        assertEquals(
            TriggerTransition(
                TriggerAction.RequestLensCapture,
                AutoResolution.FALLBACK_CONFIRMED,
            ),
            coordinator.afterConfirmation(nativeWorked = false),
        )
    }

    @Test
    fun binderRejectionImmediatelyAndPermanentlyFallsBack() {
        assertEquals(
            TriggerTransition(
                TriggerAction.RequestLensCapture,
                AutoResolution.FALLBACK_CONFIRMED,
            ),
            coordinator.afterNative(
                TriggerStrategy.AUTO,
                AutoResolution.UNKNOWN,
                NativeTriggerResult.Rejected,
            ),
        )
    }

    @Test
    fun transientNativeErrorFallsBackWithoutChangingDetection() {
        val failure = NativeTriggerResult.Error(IllegalStateException("test"))
        assertEquals(
            TriggerTransition(
                TriggerAction.RequestLensCapture,
                AutoResolution.NATIVE_CONFIRMED,
            ),
            coordinator.afterNative(
                TriggerStrategy.AUTO,
                AutoResolution.NATIVE_CONFIRMED,
                failure,
            ),
        )
    }

    @Test
    fun manualStrategiesIgnoreAutoResolution() {
        assertEquals(
            TriggerAction.InvokeNative,
            coordinator.nextAction(
                TriggerStrategy.NATIVE_ONLY,
                AutoResolution.FALLBACK_CONFIRMED,
            ),
        )
        assertEquals(
            TriggerAction.RequestLensCapture,
            coordinator.nextAction(
                TriggerStrategy.LENS_FALLBACK,
                AutoResolution.NATIVE_CONFIRMED,
            ),
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun directLensRequestsCaptureLikeOtherManualFallbacks() {
        assertEquals(
            TriggerAction.RequestLensCapture,
            coordinator.nextAction(TriggerStrategy.DIRECT_LENS, AutoResolution.UNKNOWN),
        )
        assertEquals(
            TriggerTransition(TriggerAction.RequestLensCapture, AutoResolution.UNKNOWN),
            coordinator.afterNative(
                TriggerStrategy.DIRECT_LENS,
                AutoResolution.UNKNOWN,
                NativeTriggerResult.AcceptedUnverified,
            ),
        )
        assertEquals(
            TriggerTransition(TriggerAction.RequestLensCapture, AutoResolution.FALLBACK_CONFIRMED),
            coordinator.afterNative(
                TriggerStrategy.DIRECT_LENS,
                AutoResolution.FALLBACK_CONFIRMED,
                NativeTriggerResult.Rejected,
            ),
        )
    }
}
