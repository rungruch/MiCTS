package com.parallelc.micts.domain

sealed interface NativeTriggerResult {
    data object AcceptedUnverified : NativeTriggerResult
    data object Rejected : NativeTriggerResult
    data class Error(val throwable: Throwable) : NativeTriggerResult
}
