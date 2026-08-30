package com.parallelc.micts.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language

data class VisTriggerSettings(
    val defaultDelay: Long,
    val tileDelay: Long,
    val vibrate: Boolean,
    val language: Language,
) {
    fun delayFor(fromTile: Boolean): Long = if (fromTile) tileDelay else defaultDelay
}

internal interface VisTriggerSettingsBackend {
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
}

private class SharedPreferencesVisTriggerSettingsBackend(
    private val preferences: SharedPreferences,
) : VisTriggerSettingsBackend {
    override fun getLong(key: String, defaultValue: Long): Long =
        preferences.getLong(key, defaultValue)

    override fun putLong(key: String, value: Long) {
        preferences.edit { putLong(key, value) }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit { putBoolean(key, value) }
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        preferences.edit { putInt(key, value) }
    }
}

class VisTriggerSettingsRepository internal constructor(
    private val backend: VisTriggerSettingsBackend,
) {
    constructor(context: Context) : this(
        SharedPreferencesVisTriggerSettingsBackend(
            context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE),
        ),
    )

    fun load(): VisTriggerSettings = VisTriggerSettings(
        defaultDelay = backend.getLong(
            AppConfig.KEY_DEFAULT_DELAY,
            AppConfig.DEFAULT_CONFIG[AppConfig.KEY_DEFAULT_DELAY] as Long,
        ),
        tileDelay = backend.getLong(
            AppConfig.KEY_TILE_DELAY,
            AppConfig.DEFAULT_CONFIG[AppConfig.KEY_TILE_DELAY] as Long,
        ),
        vibrate = backend.getBoolean(
            AppConfig.KEY_VIBRATE,
            AppConfig.DEFAULT_CONFIG[AppConfig.KEY_VIBRATE] as Boolean,
        ),
        language = Language.entries.getOrElse(
            backend.getInt(AppConfig.KEY_LANGUAGE, Language.FollowSystem.ordinal),
        ) { Language.FollowSystem },
    )

    fun setDefaultDelay(delay: Long) = backend.putLong(AppConfig.KEY_DEFAULT_DELAY, delay)

    fun setTileDelay(delay: Long) = backend.putLong(AppConfig.KEY_TILE_DELAY, delay)

    fun setVibrate(vibrate: Boolean) = backend.putBoolean(AppConfig.KEY_VIBRATE, vibrate)

    fun setLanguage(language: Language) = backend.putInt(AppConfig.KEY_LANGUAGE, language.ordinal)
}
