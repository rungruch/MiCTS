package com.parallelc.micts.domain

enum class TriggerStrategy {
    AUTO,
    NATIVE_ONLY,
    LENS_FALLBACK,
    @Deprecated("Kept only to migrate settings from older releases")
    DIRECT_LENS;

    companion object {
        /** Maps the removed direct-Lens preference onto the supported fallback flow. */
        @Suppress("DEPRECATION")
        fun fromStoredName(value: String?): TriggerStrategy = when (value) {
            DIRECT_LENS.name -> LENS_FALLBACK
            else -> entries.firstOrNull { it.name == value } ?: AUTO
        }
    }
}

enum class AutoResolution {
    UNKNOWN,
    PENDING_CONFIRMATION,
    NATIVE_CONFIRMED,
    FALLBACK_CONFIRMED,
}

sealed interface TriggerAction {
    data object InvokeNative : TriggerAction
    data object RequestNativeConfirmation : TriggerAction
    data object RequestLensCapture : TriggerAction
    data object Finish : TriggerAction
}

data class TriggerTransition(
    val action: TriggerAction,
    val autoResolution: AutoResolution,
)

class TriggerCoordinator {
    fun nextAction(
        strategy: TriggerStrategy,
        autoResolution: AutoResolution,
    ): TriggerAction = when (strategy) {
        TriggerStrategy.NATIVE_ONLY -> TriggerAction.InvokeNative
        TriggerStrategy.LENS_FALLBACK -> TriggerAction.RequestLensCapture
        TriggerStrategy.AUTO -> when (autoResolution) {
            AutoResolution.UNKNOWN,
            AutoResolution.NATIVE_CONFIRMED -> TriggerAction.InvokeNative
            AutoResolution.PENDING_CONFIRMATION -> TriggerAction.RequestNativeConfirmation
            AutoResolution.FALLBACK_CONFIRMED -> TriggerAction.RequestLensCapture
        }
        else -> TriggerAction.RequestLensCapture
    }

    fun afterNative(
        strategy: TriggerStrategy,
        autoResolution: AutoResolution,
        result: NativeTriggerResult,
    ): TriggerTransition {
        if (strategy == TriggerStrategy.NATIVE_ONLY) {
            return TriggerTransition(TriggerAction.Finish, autoResolution)
        }

        if (strategy != TriggerStrategy.AUTO) {
            return TriggerTransition(TriggerAction.RequestLensCapture, autoResolution)
        }

        return when (result) {
            NativeTriggerResult.AcceptedUnverified -> {
                val nextResolution = if (autoResolution == AutoResolution.UNKNOWN) {
                    AutoResolution.PENDING_CONFIRMATION
                } else {
                    autoResolution
                }
                TriggerTransition(TriggerAction.Finish, nextResolution)
            }
            NativeTriggerResult.Rejected -> TriggerTransition(
                TriggerAction.RequestLensCapture,
                AutoResolution.FALLBACK_CONFIRMED,
            )
            is NativeTriggerResult.Error -> TriggerTransition(
                TriggerAction.RequestLensCapture,
                autoResolution,
            )
        }
    }

    fun afterConfirmation(nativeWorked: Boolean): TriggerTransition = if (nativeWorked) {
        TriggerTransition(TriggerAction.InvokeNative, AutoResolution.NATIVE_CONFIRMED)
    } else {
        TriggerTransition(TriggerAction.RequestLensCapture, AutoResolution.FALLBACK_CONFIRMED)
    }
}
