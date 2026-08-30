package com.parallelc.micts.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.parallelc.micts.config.AppConfig

internal interface CapturePreferenceBackend {
    fun getInt(key: String, defaultValue: Int): Int
    fun removeAndPutInt(intKey: String, intValue: Int, vararg keysToRemove: String)
}

private class SharedPreferencesCaptureBackend(
    private val preferences: SharedPreferences,
) : CapturePreferenceBackend {
    override fun getInt(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    override fun removeAndPutInt(
        intKey: String,
        intValue: Int,
        vararg keysToRemove: String,
    ) {
        preferences.edit {
            keysToRemove.forEach(::remove)
            putInt(intKey, intValue)
        }
    }
}

internal class CapturePreferenceMigration(
    private val backend: CapturePreferenceBackend,
) {
    constructor(context: Context) : this(
        SharedPreferencesCaptureBackend(
            context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE),
        ),
    )

    fun run() {
        if (backend.getInt(KEY_SCHEMA, 0) >= SCHEMA_VERSION) return
        backend.removeAndPutInt(
            KEY_SCHEMA,
            SCHEMA_VERSION,
            KEY_CAPTURE_MODE,
            KEY_CAPTURE_EXPLANATION_SEEN,
            LEGACY_RESULT_CODE,
            LEGACY_RESULT_DATA,
            LEGACY_CAPTURE_ARMED,
            LEGACY_ASYNC_TRIGGER,
        )
    }

    private companion object {
        const val SCHEMA_VERSION = 4
        const val KEY_SCHEMA = "capture_permission_schema"
        const val KEY_CAPTURE_MODE = "capture_mode"
        const val KEY_CAPTURE_EXPLANATION_SEEN = "legacy_capture_explainer_seen"
        const val LEGACY_RESULT_CODE = "projection_result_code"
        const val LEGACY_RESULT_DATA = "projection_result_data"
        const val LEGACY_CAPTURE_ARMED = "capture_armed"
        const val LEGACY_ASYNC_TRIGGER = "async_trigger"
    }
}
