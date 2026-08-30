package com.parallelc.micts.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturePreferenceMigrationTest {
    @Test
    fun migrationRemovesObsoleteCapturePreferencesAndPreservesOtherSettings() {
        val backend = InMemoryCapturePreferenceBackend().apply {
            putValue("capture_permission_schema", 2)
            putValue("capture_mode", "ASK_EVERY_TIME")
            putValue("legacy_capture_explainer_seen", true)
            putValue("projection_result_data", "stale-token")
            putValue("projection_result_code", -1)
            putValue("capture_armed", true)
            putValue("async_trigger", false)
            putValue("trigger_strategy", "NATIVE_ONLY")
        }

        CapturePreferenceMigration(backend).run()

        listOf(
            "capture_mode",
            "legacy_capture_explainer_seen",
            "projection_result_data",
            "projection_result_code",
            "capture_armed",
            "async_trigger",
        ).forEach { key -> assertFalse(backend.contains(key)) }
        assertEquals("NATIVE_ONLY", backend.value("trigger_strategy"))
        assertEquals(4, backend.getInt("capture_permission_schema", 0))
    }

    @Test
    fun completedMigrationDoesNotRunAgain() {
        val backend = InMemoryCapturePreferenceBackend().apply {
            putValue("capture_permission_schema", 4)
            putValue("capture_mode", "future-value")
        }

        CapturePreferenceMigration(backend).run()

        assertTrue(backend.contains("capture_mode"))
    }
}

private class InMemoryCapturePreferenceBackend : CapturePreferenceBackend {
    private val values = mutableMapOf<String, Any>()

    fun contains(key: String): Boolean = key in values

    fun putValue(key: String, value: Any) {
        values[key] = value
    }

    fun value(key: String): Any? = values[key]

    override fun getInt(key: String, defaultValue: Int): Int =
        values[key] as? Int ?: defaultValue

    override fun removeAndPutInt(
        intKey: String,
        intValue: Int,
        vararg keysToRemove: String,
    ) {
        keysToRemove.forEach(values::remove)
        values[intKey] = intValue
    }
}
