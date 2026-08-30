package com.parallelc.micts.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.domain.AutoResolution
import com.parallelc.micts.domain.TriggerStrategy

internal interface TriggerPreferenceBackend {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}

private class SharedPreferencesTriggerBackend(
    private val preferences: SharedPreferences,
) : TriggerPreferenceBackend {
    override fun getString(key: String, defaultValue: String): String =
        preferences.getString(key, defaultValue) ?: defaultValue

    override fun putString(key: String, value: String) {
        preferences.edit { putString(key, value) }
    }
}

class TriggerPreferenceStore internal constructor(
    private val backend: TriggerPreferenceBackend,
) {
    constructor(context: Context) : this(
        SharedPreferencesTriggerBackend(
            context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE),
        ),
    )

    var strategy: TriggerStrategy
        get() {
            val stored = backend.getString(
                AppConfig.KEY_TRIGGER_STRATEGY,
                TriggerStrategy.AUTO.name,
            )
            val strategy = TriggerStrategy.fromStoredName(stored)
            if (strategy.name != stored) {
                backend.putString(AppConfig.KEY_TRIGGER_STRATEGY, strategy.name)
            }
            return strategy
        }
        set(value) {
            backend.putString(AppConfig.KEY_TRIGGER_STRATEGY, value.name)
        }

    var autoResolution: AutoResolution
        get() = getEnum(
            AppConfig.KEY_AUTO_RESOLUTION,
            AutoResolution.UNKNOWN,
        )
        set(value) {
            backend.putString(AppConfig.KEY_AUTO_RESOLUTION, value.name)
        }

    fun resetAutoResolution() {
        autoResolution = AutoResolution.UNKNOWN
    }

    private inline fun <reified T : Enum<T>> getEnum(
        key: String,
        defaultValue: T,
    ): T {
        val stored = backend.getString(key, defaultValue.name)
        return enumValues<T>().firstOrNull { it.name == stored } ?: defaultValue
    }
}
