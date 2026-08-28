package com.parallelc.micts.data

import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.domain.CaptureMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturePreferenceStoreTest {
    @Test
    fun modeAndConsentExplanationPersist() {
        val backend = InMemoryCapturePreferenceBackend()
        CapturePreferenceStore(backend, apiLevel = 33).apply {
            mode = CaptureMode.REMEMBER_CONSENT
            consentExplanationSeen = true
        }

        val restored = CapturePreferenceStore(backend, apiLevel = 33)
        assertEquals(CaptureMode.REMEMBER_CONSENT, restored.mode)
        assertTrue(restored.consentExplanationSeen)
    }

    @Test
    fun freshInstallInitializesUnset() {
        val store = CapturePreferenceStore(InMemoryCapturePreferenceBackend(), apiLevel = 33)
        assertEquals(CaptureMode.UNSET, store.mode)
    }

    @Test
    fun migrationMapsRemovedFastCaptureToRememberConsentBelowAndroid14() {
        val backend = backendWithLegacyFastCapture()

        val store = CapturePreferenceStore(backend, apiLevel = 33)

        assertEquals(CaptureMode.REMEMBER_CONSENT, store.mode)
        assertMigrationCleanup(backend)
    }

    @Test
    fun migrationMapsRemovedFastCaptureToAskEveryTimeOnAndroid14() {
        val backend = backendWithLegacyFastCapture()

        val store = CapturePreferenceStore(backend, apiLevel = 34)

        assertEquals(CaptureMode.ASK_EVERY_TIME, store.mode)
        assertMigrationCleanup(backend)
    }

    @Test
    fun migrationPreservesAskEveryTimeAndUnrelatedSettings() {
        val backend = InMemoryCapturePreferenceBackend().apply {
            putString(AppConfig.KEY_CAPTURE_MODE, CaptureMode.ASK_EVERY_TIME.name)
            putString(AppConfig.KEY_TRIGGER_STRATEGY, "NATIVE_ONLY")
        }

        val store = CapturePreferenceStore(backend, apiLevel = 31)

        assertEquals(CaptureMode.ASK_EVERY_TIME, store.mode)
        assertEquals(
            "NATIVE_ONLY",
            backend.getString(AppConfig.KEY_TRIGGER_STRATEGY, "AUTO"),
        )
    }

    @Test
    fun unknownModeUsesUnset() {
        val backend = InMemoryCapturePreferenceBackend().apply {
            putInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, 2)
            putString(AppConfig.KEY_CAPTURE_MODE, "FUTURE_MODE")
        }

        assertEquals(CaptureMode.UNSET, CapturePreferenceStore(backend, apiLevel = 33).mode)
    }

    private fun backendWithLegacyFastCapture() = InMemoryCapturePreferenceBackend().apply {
        putInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, 1)
        putString(AppConfig.KEY_CAPTURE_MODE, "FAST_ACCESSIBILITY")
        putString("projection_result_data", "stale-token")
        putString("projection_result_code", "-1")
        putBoolean("capture_armed", true)
    }

    private fun assertMigrationCleanup(backend: InMemoryCapturePreferenceBackend) {
        assertFalse(backend.contains("projection_result_data"))
        assertFalse(backend.contains("projection_result_code"))
        assertFalse(backend.contains("capture_armed"))
        assertEquals(2, backend.getInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, 0))
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

    override fun getBoolean(key: String, defaultValue: Boolean) =
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
