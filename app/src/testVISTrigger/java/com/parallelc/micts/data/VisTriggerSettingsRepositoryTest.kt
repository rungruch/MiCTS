package com.parallelc.micts.data

import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisTriggerSettingsRepositoryTest {
    @Test
    fun emptyBackendUsesLeanVisDefaults() {
        val repository = VisTriggerSettingsRepository(InMemorySettingsBackend())

        val settings = repository.load()

        assertEquals(0L, settings.defaultDelay)
        assertEquals(400L, settings.tileDelay)
        assertFalse(settings.vibrate)
        assertEquals(Language.FollowSystem, settings.language)
    }

    @Test
    fun supportedSettingsPersistWithoutRootConfiguration() {
        val backend = InMemorySettingsBackend()
        val repository = VisTriggerSettingsRepository(backend)

        repository.setDefaultDelay(120L)
        repository.setTileDelay(650L)
        repository.setVibrate(true)
        repository.setLanguage(Language.English)

        val settings = repository.load()
        assertEquals(120L, settings.defaultDelay)
        assertEquals(650L, settings.tileDelay)
        assertTrue(settings.vibrate)
        assertEquals(Language.English, settings.language)
        assertEquals(120L, settings.delayFor(fromTile = false))
        assertEquals(650L, settings.delayFor(fromTile = true))
    }

    @Test
    fun invalidStoredLanguageFallsBackToSystem() {
        val backend = InMemorySettingsBackend().apply {
            ints[AppConfig.KEY_LANGUAGE] = Int.MAX_VALUE
        }

        assertEquals(
            Language.FollowSystem,
            VisTriggerSettingsRepository(backend).load().language,
        )
    }
}

private class InMemorySettingsBackend : VisTriggerSettingsBackend {
    val longs = mutableMapOf<String, Long>()
    val booleans = mutableMapOf<String, Boolean>()
    val ints = mutableMapOf<String, Int>()

    override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue
    override fun putLong(key: String, value: Long) {
        longs[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        booleans[key] ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
    override fun putInt(key: String, value: Int) {
        ints[key] = value
    }
}
