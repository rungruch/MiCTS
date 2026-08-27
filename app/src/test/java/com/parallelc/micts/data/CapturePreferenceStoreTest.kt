package com.parallelc.micts.data

import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.domain.CaptureMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturePreferenceStoreTest {
    @Test
    fun modeAndLegacyExplanationPersist() {
        val backend = InMemoryCapturePreferenceBackend()
        CapturePreferenceStore(backend).apply {
            mode = CaptureMode.FAST_ACCESSIBILITY
            legacyExplanationSeen = true
        }

        val restored = CapturePreferenceStore(backend)
        assertEquals(CaptureMode.FAST_ACCESSIBILITY, restored.mode)
        assertTrue(restored.legacyExplanationSeen)
    }

    @Test
    fun migrationRemovesObsoleteProjectionStateAndInitializesUnset() {
        val backend = InMemoryCapturePreferenceBackend().apply {
            putString("projection_result_data", "stale-token")
            putString("projection_result_code", "-1")
            putBoolean("capture_armed", true)
            putString(AppConfig.KEY_CAPTURE_MODE, CaptureMode.ASK_EVERY_TIME.name)
            putString(AppConfig.KEY_TRIGGER_STRATEGY, "NATIVE_ONLY")
            putBoolean(AppConfig.KEY_LOCAL_TEXT_RECOGNITION, false)
        }

        val store = CapturePreferenceStore(backend)

        assertEquals(CaptureMode.UNSET, store.mode)
        assertFalse(backend.contains("projection_result_data"))
        assertFalse(backend.contains("projection_result_code"))
        assertFalse(backend.contains("capture_armed"))
        assertEquals(
            "NATIVE_ONLY",
            backend.getString(AppConfig.KEY_TRIGGER_STRATEGY, "AUTO"),
        )
        assertFalse(backend.getBoolean(AppConfig.KEY_LOCAL_TEXT_RECOGNITION, true))
        assertEquals(1, backend.getInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, 0))
    }

    @Test
    fun unknownModeUsesUnset() {
        val backend = InMemoryCapturePreferenceBackend().apply {
            putInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, 1)
            putString(AppConfig.KEY_CAPTURE_MODE, "FUTURE_MODE")
        }

        assertEquals(CaptureMode.UNSET, CapturePreferenceStore(backend).mode)
    }
}

private class InMemoryCapturePreferenceBackend : CapturePreferenceBackend {
    private val values = mutableMapOf<String, Any>()

    fun contains(key: String): Boolean = key in values

    override fun getString(key: String, defaultValue: String): String =
        values[key] as? String ?: defaultValue

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] as? Boolean ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        values[key] as? Int ?: defaultValue

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun remove(vararg keys: String) {
        keys.forEach(values::remove)
    }
}
