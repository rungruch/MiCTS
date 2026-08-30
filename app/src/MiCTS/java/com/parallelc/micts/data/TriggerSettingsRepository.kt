package com.parallelc.micts.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import com.parallelc.micts.domain.AutoResolution
import com.parallelc.micts.domain.TriggerStrategy

data class TriggerSettings(
    val defaultDelay: Long,
    val tileDelay: Long,
    val vibrate: Boolean,
    val strategy: TriggerStrategy,
    val autoResolution: AutoResolution,
    val language: Language,
)

interface TriggerSettingsBackend {
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
}

class SharedPreferencesSettingsBackend(
    private val preferences: SharedPreferences,
) : TriggerSettingsBackend {
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

    override fun getString(key: String, defaultValue: String): String =
        preferences.getString(key, defaultValue) ?: defaultValue

    override fun putString(key: String, value: String) {
        preferences.edit { putString(key, value) }
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        preferences.edit { putInt(key, value) }
    }
}

class TriggerSettingsRepository internal constructor(
    private val backend: TriggerSettingsBackend,
) {
    constructor(preferences: SharedPreferences) : this(SharedPreferencesSettingsBackend(preferences))

    constructor(context: Context) : this(
        context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE),
    )

    fun load(): TriggerSettings = TriggerSettings(
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
        strategy = TriggerStrategy.fromStoredName(
            backend.getString(
                AppConfig.KEY_TRIGGER_STRATEGY,
                TriggerStrategy.AUTO.name,
            ),
        ),
        autoResolution = backend.getString(
            AppConfig.KEY_AUTO_RESOLUTION,
            AutoResolution.UNKNOWN.name,
        ).let { stored ->
            AutoResolution.entries.firstOrNull { it.name == stored }
        } ?: AutoResolution.UNKNOWN,
        language = Language.entries.getOrElse(
            backend.getInt(
                AppConfig.KEY_LANGUAGE,
                Language.FollowSystem.ordinal,
            ),
        ) { Language.FollowSystem },
    )

    fun setDefaultDelay(delay: Long) {
        backend.putLong(AppConfig.KEY_DEFAULT_DELAY, delay)
    }

    fun setTileDelay(delay: Long) {
        backend.putLong(AppConfig.KEY_TILE_DELAY, delay)
    }

    fun setVibrate(vibrate: Boolean) {
        backend.putBoolean(AppConfig.KEY_VIBRATE, vibrate)
    }

    fun setStrategy(strategy: TriggerStrategy) {
        backend.putString(AppConfig.KEY_TRIGGER_STRATEGY, strategy.name)
    }

    fun setAutoResolution(resolution: AutoResolution) {
        backend.putString(AppConfig.KEY_AUTO_RESOLUTION, resolution.name)
    }

    fun resetAutoResolution() {
        setAutoResolution(AutoResolution.UNKNOWN)
    }

    fun setLanguage(language: Language) {
        backend.putInt(AppConfig.KEY_LANGUAGE, language.ordinal)
    }
}
