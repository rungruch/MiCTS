package com.parallelc.micts.data

import android.content.Context
import android.content.SharedPreferences
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.domain.CaptureMode

internal interface CapturePreferenceBackend {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun remove(vararg keys: String)
}

private class SharedPreferencesCaptureBackend(
    private val preferences: SharedPreferences,
) : CapturePreferenceBackend {
    override fun getString(key: String, defaultValue: String): String =
        preferences.getString(key, defaultValue) ?: defaultValue

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    override fun remove(vararg keys: String) {
        preferences.edit().apply {
            keys.forEach(::remove)
        }.apply()
    }
}

class CapturePreferenceStore internal constructor(
    private val backend: CapturePreferenceBackend,
) {
    constructor(context: Context) : this(
        SharedPreferencesCaptureBackend(
            context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE),
        ),
    )

    init {
        migrateIfNeeded()
    }

    var mode: CaptureMode
        get() {
            val stored = backend.getString(
                AppConfig.KEY_CAPTURE_MODE,
                CaptureMode.UNSET.name,
            )
            return CaptureMode.entries.firstOrNull { it.name == stored } ?: CaptureMode.UNSET
        }
        set(value) {
            backend.putString(AppConfig.KEY_CAPTURE_MODE, value.name)
        }

    var legacyExplanationSeen: Boolean
        get() = backend.getBoolean(AppConfig.KEY_LEGACY_CAPTURE_EXPLAINER_SEEN, false)
        set(value) {
            backend.putBoolean(AppConfig.KEY_LEGACY_CAPTURE_EXPLAINER_SEEN, value)
        }

    private fun migrateIfNeeded() {
        if (backend.getInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, 0) >= SCHEMA_VERSION) return
        backend.remove(
            LEGACY_RESULT_CODE,
            LEGACY_RESULT_DATA,
            LEGACY_CAPTURE_ARMED,
        )
        backend.putString(AppConfig.KEY_CAPTURE_MODE, CaptureMode.UNSET.name)
        backend.putInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, SCHEMA_VERSION)
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val LEGACY_RESULT_CODE = "projection_result_code"
        const val LEGACY_RESULT_DATA = "projection_result_data"
        const val LEGACY_CAPTURE_ARMED = "capture_armed"
    }
}
