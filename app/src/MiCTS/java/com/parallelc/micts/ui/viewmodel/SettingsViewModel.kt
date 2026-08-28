package com.parallelc.micts.ui.viewmodel

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences: SharedPreferences =
        application.getSharedPreferences(AppConfig.CONFIG_NAME, MODE_PRIVATE)
    private val _appConfig = MutableStateFlow(loadConfig())
    val appConfig: StateFlow<Map<String, Any>> = _appConfig.asStateFlow()

    private val _locale = MutableStateFlow(currentLanguage().toLocale())
    val locale: StateFlow<Locale> = _locale.asStateFlow()

    val scrollState = ScrollState(0)
    val menuExpanded = mutableStateOf(false)
    val languageExpanded = mutableStateOf(false)

    fun updateAppConfig(key: String, value: Any) {
        if (key == AppConfig.KEY_LANGUAGE) {
            _locale.value = Language.entries[value as Int].toLocale()
        }
        _appConfig.value = _appConfig.value.toMutableMap().apply { put(key, value) }
        preferences.edit().apply {
            when (value) {
                is Long -> putLong(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
            }
        }.apply()
    }

    private fun loadConfig(): Map<String, Any> =
        AppConfig.DEFAULT_CONFIG + preferences.all
            .filterValues { it != null }
            .mapValues { it.value as Any }

    private fun currentLanguage(): Language = Language.entries.getOrElse(
        (_appConfig.value[AppConfig.KEY_LANGUAGE] as? Int) ?: Language.FollowSystem.ordinal,
    ) { Language.FollowSystem }
}
