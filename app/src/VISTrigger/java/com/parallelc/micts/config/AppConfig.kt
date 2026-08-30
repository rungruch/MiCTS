package com.parallelc.micts.config

import com.parallelc.micts.R
import java.util.Locale

enum class Language(val id: Int, val languageTag: String?, val toLocale: () -> Locale) {
    FollowSystem(R.string.follow_system, null, { Locale.getDefault() }),
    Arabic(R.string.arabic, "ar", { Locale.forLanguageTag("ar") }),
    ChineseSimplified(R.string.chinese_simplified, "zh-CN", { Locale.SIMPLIFIED_CHINESE }),
    ChineseTraditional(R.string.chinese_traditional, "zh-TW", { Locale.TRADITIONAL_CHINESE }),
    English(R.string.english, "en", { Locale.ENGLISH }),
    Greek(R.string.greek, "el", { Locale.forLanguageTag("el") }),
    Japanese(R.string.japanese, "ja", { Locale.JAPANESE }),
    Odia(R.string.odia, "or", { Locale.forLanguageTag("or") }),
    Persian(R.string.persian, "fa", { Locale.forLanguageTag("fa") }),
    Russian(R.string.russian, "ru", { Locale.forLanguageTag("ru") }),
    Spanish(R.string.spanish, "es", { Locale.forLanguageTag("es") }),
    Turkish(R.string.turkish, "tr", { Locale.forLanguageTag("tr") }),
    Vietnamese(R.string.vietnamese, "vi", { Locale.forLanguageTag("vi") }),
}

object AppConfig {
    const val CONFIG_NAME = "app_config"
    const val KEY_LANGUAGE = "language"
    const val KEY_DEFAULT_DELAY = "default_delay"
    const val KEY_TILE_DELAY = "tile_delay"
    const val KEY_VIBRATE = "vibrate"
    const val KEY_ASYNC_TRIGGER = "async_trigger"
    const val KEY_TRIGGER_STRATEGY = "trigger_strategy"
    const val KEY_AUTO_RESOLUTION = "auto_resolution"
    const val KEY_LOCAL_TEXT_RECOGNITION = "local_text_recognition"
    const val KEY_AI_ENABLED = "ai_enabled"
    const val KEY_AI_BASE_URL = "ai_base_url"
    const val KEY_AI_API_KEY = "ai_api_key"
    const val KEY_AI_MODEL = "ai_model"
    const val KEY_AI_SEND_IMAGE = "ai_send_image"
    const val KEY_AI_PRIVACY_ACCEPTED = "ai_privacy_accepted"

    const val DEFAULT_AI_BASE_URL = "https://api.deepseek.com/v1"
    const val DEFAULT_AI_MODEL = "deepseek-chat"

    val DEFAULT_CONFIG = mapOf<String, Any>(
        KEY_LANGUAGE to Language.FollowSystem.ordinal,
        KEY_DEFAULT_DELAY to 0L,
        KEY_TILE_DELAY to 400L,
        KEY_VIBRATE to false,
        KEY_ASYNC_TRIGGER to false,
        KEY_TRIGGER_STRATEGY to "AUTO",
        KEY_AUTO_RESOLUTION to "UNKNOWN",
        KEY_LOCAL_TEXT_RECOGNITION to true,
        KEY_AI_ENABLED to false,
        KEY_AI_BASE_URL to DEFAULT_AI_BASE_URL,
        KEY_AI_API_KEY to "",
        KEY_AI_MODEL to DEFAULT_AI_MODEL,
        KEY_AI_SEND_IMAGE to true,
        KEY_AI_PRIVACY_ACCEPTED to false,
    )
}
