package com.parallelc.micts.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.parallelc.micts.config.AppConfig

internal interface VisTriggerPreferenceMigrationBackend {
    fun getInt(key: String, defaultValue: Int): Int
    fun removeAndPutInt(intKey: String, intValue: Int, keysToRemove: Set<String>)
}

private class SharedPreferencesVisTriggerMigrationBackend(
    private val preferences: SharedPreferences,
) : VisTriggerPreferenceMigrationBackend {
    override fun getInt(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    override fun removeAndPutInt(
        intKey: String,
        intValue: Int,
        keysToRemove: Set<String>,
    ) {
        preferences.edit {
            keysToRemove.forEach(::remove)
            putInt(intKey, intValue)
        }
    }
}

internal class VisTriggerPreferenceMigration(
    private val backend: VisTriggerPreferenceMigrationBackend,
) {
    constructor(context: Context) : this(
        SharedPreferencesVisTriggerMigrationBackend(
            context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE),
        ),
    )

    fun run() {
        if (backend.getInt(KEY_SCHEMA, 0) >= SCHEMA_VERSION) return
        backend.removeAndPutInt(KEY_SCHEMA, SCHEMA_VERSION, OBSOLETE_KEYS)
    }

    internal companion object {
        const val KEY_SCHEMA = "vistrigger_non_root_schema"
        const val SCHEMA_VERSION = 1
        val OBSOLETE_KEYS = setOf(
            "async_trigger",
            "trigger_strategy",
            "auto_resolution",
            "local_text_recognition",
            "ai_enabled",
            "ai_base_url",
            "ai_api_key",
            "ai_model",
            "ai_send_image",
            "ai_privacy_accepted",
            "capture_permission_schema",
            "capture_mode",
            "legacy_capture_explainer_seen",
            "projection_result_code",
            "projection_result_data",
            "capture_armed",
        )
    }
}
