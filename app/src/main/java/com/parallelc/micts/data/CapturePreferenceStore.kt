package com.parallelc.micts.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
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
    private val apiLevel: Int,
) {
    constructor(context: Context) : this(
        SharedPreferencesCaptureBackend(
            context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE),
        ),
        Build.VERSION.SDK_INT,
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

    var consentExplanationSeen: Boolean
        get() = backend.getBoolean(AppConfig.KEY_CAPTURE_EXPLANATION_SEEN, false)
        set(value) {
            backend.putBoolean(AppConfig.KEY_CAPTURE_EXPLANATION_SEEN, value)
        }

    private fun migrateIfNeeded() {
        if (backend.getInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, 0) >= SCHEMA_VERSION) return
        backend.remove(
            LEGACY_RESULT_CODE,
            LEGACY_RESULT_DATA,
            LEGACY_CAPTURE_ARMED,
        )
        // The removed Accessibility "fast capture" mode maps to the closest
        // no-root equivalent: remembered consent where Android allows token
        // reuse, ask-every-time on Android 14+ where tokens are single-use.
        val migratedMode = when (
            backend.getString(AppConfig.KEY_CAPTURE_MODE, CaptureMode.UNSET.name)
        ) {
            LEGACY_MODE_FAST_ACCESSIBILITY ->
                if (apiLevel >= API_ANDROID_14) {
                    CaptureMode.ASK_EVERY_TIME.name
                } else {
                    CaptureMode.REMEMBER_CONSENT.name
                }
            else -> backend.getString(AppConfig.KEY_CAPTURE_MODE, CaptureMode.UNSET.name)
        }
        backend.putString(AppConfig.KEY_CAPTURE_MODE, migratedMode)
        backend.putInt(AppConfig.KEY_CAPTURE_PERMISSION_SCHEMA, SCHEMA_VERSION)
    }

    private companion object {
        const val SCHEMA_VERSION = 2
        const val API_ANDROID_14 = 34
        const val LEGACY_MODE_FAST_ACCESSIBILITY = "FAST_ACCESSIBILITY"
        const val LEGACY_RESULT_CODE = "projection_result_code"
        const val LEGACY_RESULT_DATA = "projection_result_data"
        const val LEGACY_CAPTURE_ARMED = "capture_armed"
    }
}
