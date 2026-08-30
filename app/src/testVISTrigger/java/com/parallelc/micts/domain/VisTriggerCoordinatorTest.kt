package com.parallelc.micts.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisTriggerCoordinatorTest {
    @Test
    fun acceptedNativeRequestFinishesWithoutFailureMessage() {
        assertFalse(
            VisTriggerCoordinator.shouldShowFailure(NativeTriggerResult.AcceptedUnverified),
        )
    }

    @Test
    fun rejectedOrErroredNativeRequestShowsFailureMessage() {
        assertTrue(VisTriggerCoordinator.shouldShowFailure(NativeTriggerResult.Rejected))
        assertTrue(
            VisTriggerCoordinator.shouldShowFailure(
                NativeTriggerResult.Error(IllegalStateException("test")),
            ),
        )
    }
}
