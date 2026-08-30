package com.parallelc.micts.ui.viewmodel

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parallelc.micts.config.Language
import com.parallelc.micts.data.VisTriggerSettings
import com.parallelc.micts.data.VisTriggerSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SettingsViewModel(
    application: Application,
    private val repository: VisTriggerSettingsRepository = VisTriggerSettingsRepository(application),
) : AndroidViewModel(application) {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                SettingsViewModel(application)
            }
        }
    }

    private val _settings = MutableStateFlow(repository.load())
    val settings: StateFlow<VisTriggerSettings> = _settings.asStateFlow()

    private val _locale = MutableStateFlow(resolveLanguage().toLocale())
    val locale: StateFlow<Locale> = _locale.asStateFlow()

    fun setDefaultDelay(delay: Long) {
        repository.setDefaultDelay(delay)
        _settings.value = _settings.value.copy(defaultDelay = delay)
    }

    fun setTileDelay(delay: Long) {
        repository.setTileDelay(delay)
        _settings.value = _settings.value.copy(tileDelay = delay)
    }

    fun setVibrate(vibrate: Boolean) {
        repository.setVibrate(vibrate)
        _settings.value = _settings.value.copy(vibrate = vibrate)
    }

    fun setLanguage(language: Language) {
        repository.setLanguage(language)
        _settings.value = _settings.value.copy(language = language)
        _locale.value = language.toLocale()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val localeManager =
                    getApplication<Application>().getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = if (language.languageTag == null) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(language.languageTag)
                }
            }
        }
    }

    private fun resolveLanguage(): Language {
        val storedLanguage = _settings.value.language
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return storedLanguage

        val localeManager = getApplication<Application>().getSystemService(LocaleManager::class.java)
        val appLocales = localeManager?.applicationLocales
        if (appLocales != null && !appLocales.isEmpty) {
            val firstLocale = appLocales.get(0)
            return Language.entries.firstOrNull { language ->
                language.languageTag != null && (
                    language.languageTag == firstLocale.toLanguageTag() ||
                        language.languageTag.startsWith(firstLocale.language)
                    )
            } ?: Language.FollowSystem
        }

        if (storedLanguage != Language.FollowSystem && storedLanguage.languageTag != null) {
            runCatching {
                localeManager?.applicationLocales =
                    LocaleList.forLanguageTags(storedLanguage.languageTag)
            }
            return storedLanguage
        }
        return Language.FollowSystem
    }
}
