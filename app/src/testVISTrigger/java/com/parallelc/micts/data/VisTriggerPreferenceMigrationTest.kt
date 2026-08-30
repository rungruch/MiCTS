package com.parallelc.micts.data

import com.parallelc.micts.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisTriggerPreferenceMigrationTest {
    @Test
    fun migrationRemovesOnlyObsoleteLocalFeatures() {
        val backend = InMemoryMigrationBackend().apply {
            values[AppConfig.KEY_DEFAULT_DELAY] = 200L
            values[AppConfig.KEY_TILE_DELAY] = 500L
            values[AppConfig.KEY_VIBRATE] = true
            values[AppConfig.KEY_LANGUAGE] = 4
            values["trigger_strategy"] = "AUTO"
            values["ai_api_key"] = "legacy-key"
            values["capture_mode"] = "legacy"
            values["unrelated_preference"] = "keep"
        }

        VisTriggerPreferenceMigration(backend).run()

        VisTriggerPreferenceMigration.OBSOLETE_KEYS.forEach { key ->
            assertFalse("Expected obsolete key to be removed: $key", key in backend.values)
        }
        assertEquals(200L, backend.values[AppConfig.KEY_DEFAULT_DELAY])
        assertEquals(500L, backend.values[AppConfig.KEY_TILE_DELAY])
        assertEquals(true, backend.values[AppConfig.KEY_VIBRATE])
        assertEquals(4, backend.values[AppConfig.KEY_LANGUAGE])
        assertEquals("keep", backend.values["unrelated_preference"])
        assertEquals(
            VisTriggerPreferenceMigration.SCHEMA_VERSION,
            backend.values[VisTriggerPreferenceMigration.KEY_SCHEMA],
        )
    }

    @Test
    fun completedMigrationDoesNotRunAgain() {
        val backend = InMemoryMigrationBackend().apply {
            values[VisTriggerPreferenceMigration.KEY_SCHEMA] =
                VisTriggerPreferenceMigration.SCHEMA_VERSION
            values["trigger_strategy"] = "future-value"
        }

        VisTriggerPreferenceMigration(backend).run()

        assertTrue("trigger_strategy" in backend.values)
    }
}

private class InMemoryMigrationBackend : VisTriggerPreferenceMigrationBackend {
    val values = mutableMapOf<String, Any>()

    override fun getInt(key: String, defaultValue: Int): Int =
        values[key] as? Int ?: defaultValue

    override fun removeAndPutInt(
        intKey: String,
        intValue: Int,
        keysToRemove: Set<String>,
    ) {
        keysToRemove.forEach(values::remove)
        values[intKey] = intValue
    }
}
