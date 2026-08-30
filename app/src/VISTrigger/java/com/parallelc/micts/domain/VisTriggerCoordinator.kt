package com.parallelc.micts.domain

internal object VisTriggerCoordinator {
    fun shouldShowFailure(result: NativeTriggerResult): Boolean =
        result != NativeTriggerResult.AcceptedUnverified
}
