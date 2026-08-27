package com.parallelc.micts.data

import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.domain.AutoResolution
import com.parallelc.micts.domain.TriggerStrategy
import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerPreferenceStoreTest {
    @Test
    fun strategyAndResolutionPersistAcrossStoreInstances() {
        val backend = InMemoryTriggerPreferenceBackend()
        TriggerPreferenceStore(backend).apply {
            strategy = TriggerStrategy.LENS_FALLBACK
            autoResolution = AutoResolution.FALLBACK_CONFIRMED
        }

        val restored = TriggerPreferenceStore(backend)
        assertEquals(TriggerStrategy.LENS_FALLBACK, restored.strategy)
        assertEquals(AutoResolution.FALLBACK_CONFIRMED, restored.autoResolution)
    }

    @Test
    fun resetClearsOnlyAutoDetection() {
        val backend = InMemoryTriggerPreferenceBackend()
        val store = TriggerPreferenceStore(backend).apply {
            strategy = TriggerStrategy.AUTO
            autoResolution = AutoResolution.NATIVE_CONFIRMED
        }

        store.resetAutoResolution()

        val restored = TriggerPreferenceStore(backend)
        assertEquals(TriggerStrategy.AUTO, restored.strategy)
        assertEquals(AutoResolution.UNKNOWN, restored.autoResolution)
    }

    @Test
    fun unknownStoredValuesUseSafeDefaults() {
        val backend = InMemoryTriggerPreferenceBackend().apply {
            putString(AppConfig.KEY_TRIGGER_STRATEGY, "FUTURE_STRATEGY")
            putString(AppConfig.KEY_AUTO_RESOLUTION, "FUTURE_RESOLUTION")
        }
        val store = TriggerPreferenceStore(backend)

        assertEquals(TriggerStrategy.AUTO, store.strategy)
        assertEquals(AutoResolution.UNKNOWN, store.autoResolution)
    }
}

private class InMemoryTriggerPreferenceBackend : TriggerPreferenceBackend {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String, defaultValue: String): String =
        values[key] ?: defaultValue

    override fun putString(key: String, value: String) {
        values[key] = value
    }
}
