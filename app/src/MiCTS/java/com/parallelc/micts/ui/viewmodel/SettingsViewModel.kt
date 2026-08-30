package com.parallelc.micts.ui.viewmodel

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import com.parallelc.micts.data.TriggerSettingsRepository
import com.parallelc.micts.domain.AutoResolution
import com.parallelc.micts.domain.TriggerStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SettingsViewModel(
    application: Application,
    private val repository: TriggerSettingsRepository = TriggerSettingsRepository(application),
) : AndroidViewModel(application) {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                SettingsViewModel(app)
            }
        }
    }

    private val _appConfig = MutableStateFlow(loadConfig())
    val appConfig: StateFlow<Map<String, Any>> = _appConfig.asStateFlow()

    private val _locale = MutableStateFlow(currentLanguage().toLocale())
    val locale: StateFlow<Locale> = _locale.asStateFlow()

    val scrollState = ScrollState(0)
    val menuExpanded = mutableStateOf(false)
    val languageExpanded = mutableStateOf(false)

    fun updateAppConfig(key: String, value: Any) {
        when (key) {
            AppConfig.KEY_LANGUAGE -> {
                val language = Language.entries.getOrElse(value as Int) { Language.FollowSystem }
                repository.setLanguage(language)
                _locale.value = language.toLocale()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching {
                        val localeManager = getApplication<Application>().getSystemService(LocaleManager::class.java)
                        val localeList = if (language.languageTag != null) {
                            LocaleList.forLanguageTags(language.languageTag)
                        } else {
                            LocaleList.getEmptyLocaleList()
                        }
                        localeManager?.applicationLocales = localeList
                    }
                }
            }
            AppConfig.KEY_DEFAULT_DELAY -> repository.setDefaultDelay((value as Number).toLong())
            AppConfig.KEY_TILE_DELAY -> repository.setTileDelay((value as Number).toLong())
            AppConfig.KEY_VIBRATE -> repository.setVibrate(value as Boolean)
            AppConfig.KEY_TRIGGER_STRATEGY -> repository.setStrategy(TriggerStrategy.fromStoredName(value as String))
            AppConfig.KEY_AUTO_RESOLUTION -> repository.setAutoResolution(
                AutoResolution.entries.firstOrNull { it.name == value } ?: AutoResolution.UNKNOWN
            )
        }
        _appConfig.value = _appConfig.value.toMutableMap().apply { put(key, value) }
    }

    private fun loadConfig(): Map<String, Any> {
        val settings = repository.load()
        val language = currentLanguage()
        return mapOf(
            AppConfig.KEY_DEFAULT_DELAY to settings.defaultDelay,
            AppConfig.KEY_TILE_DELAY to settings.tileDelay,
            AppConfig.KEY_VIBRATE to settings.vibrate,
            AppConfig.KEY_TRIGGER_STRATEGY to settings.strategy.name,
            AppConfig.KEY_AUTO_RESOLUTION to settings.autoResolution.name,
            AppConfig.KEY_LANGUAGE to language.ordinal,
        )
    }

    private fun currentLanguage(): Language {
        val storedLanguage = repository.load().language
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getApplication<Application>().getSystemService(LocaleManager::class.java)
            val appLocales = localeManager?.applicationLocales
            if (appLocales != null && !appLocales.isEmpty) {
                val firstLocale = appLocales.get(0)
                return Language.entries.firstOrNull {
                    it.languageTag != null && (
                        it.languageTag == firstLocale.toLanguageTag() ||
                        it.languageTag.startsWith(firstLocale.language)
                    )
                } ?: Language.FollowSystem
            }
            if (storedLanguage != Language.FollowSystem && storedLanguage.languageTag != null) {
                runCatching {
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(storedLanguage.languageTag)
                }
                return storedLanguage
            }
            return Language.FollowSystem
        }
        return storedLanguage
    }
}
