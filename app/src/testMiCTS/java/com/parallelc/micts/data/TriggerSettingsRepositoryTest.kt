package com.parallelc.micts.data

import com.parallelc.micts.config.Language
import com.parallelc.micts.domain.AutoResolution
import com.parallelc.micts.domain.TriggerStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerSettingsRepositoryTest {

    private class InMemoryTriggerSettingsBackend : TriggerSettingsBackend {
        val longs = mutableMapOf<String, Long>()
        val booleans = mutableMapOf<String, Boolean>()
        val strings = mutableMapOf<String, String>()
        val ints = mutableMapOf<String, Int>()

        override fun getLong(key: String, defaultValue: Long): Long = longs[key] ?: defaultValue
        override fun putLong(key: String, value: Long) { longs[key] = value }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleans[key] ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }

        override fun getString(key: String, defaultValue: String): String = strings[key] ?: defaultValue
        override fun putString(key: String, value: String) { strings[key] = value }

        override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
        override fun putInt(key: String, value: Int) { ints[key] = value }
    }

    @Test
    fun defaultsAreLoadedWhenBackendIsEmpty() {
        val backend = InMemoryTriggerSettingsBackend()
        val repo = TriggerSettingsRepository(backend)
        val settings = repo.load()

        assertEquals(0L, settings.defaultDelay)
        assertEquals(400L, settings.tileDelay)
        assertEquals(false, settings.vibrate)
        assertEquals(TriggerStrategy.AUTO, settings.strategy)
        assertEquals(AutoResolution.UNKNOWN, settings.autoResolution)
        assertEquals(Language.FollowSystem, settings.language)
    }

    @Test
    fun updatesPersistAndLoadCorrectly() {
        val backend = InMemoryTriggerSettingsBackend()
        val repo = TriggerSettingsRepository(backend)

        repo.setDefaultDelay(150L)
        repo.setTileDelay(600L)
        repo.setVibrate(true)
        repo.setStrategy(TriggerStrategy.NATIVE_ONLY)
        repo.setAutoResolution(AutoResolution.NATIVE_CONFIRMED)
        repo.setLanguage(Language.English)

        val updated = repo.load()
        assertEquals(150L, updated.defaultDelay)
        assertEquals(600L, updated.tileDelay)
        assertTrue(updated.vibrate)
        assertEquals(TriggerStrategy.NATIVE_ONLY, updated.strategy)
        assertEquals(AutoResolution.NATIVE_CONFIRMED, updated.autoResolution)
        assertEquals(Language.English, updated.language)
    }

    @Test
    fun resetAutoResolutionResetsToUnknown() {
        val backend = InMemoryTriggerSettingsBackend()
        val repo = TriggerSettingsRepository(backend)

        repo.setAutoResolution(AutoResolution.FALLBACK_CONFIRMED)
        assertEquals(AutoResolution.FALLBACK_CONFIRMED, repo.load().autoResolution)

        repo.resetAutoResolution()
        assertEquals(AutoResolution.UNKNOWN, repo.load().autoResolution)
    }
}
