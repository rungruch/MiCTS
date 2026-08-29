package com.parallelc.micts.config

import com.parallelc.micts.R
import java.util.Locale

enum class Language(val id: Int, val toLocale: () -> Locale) {
    FollowSystem(R.string.follow_system, { Locale.getDefault() }),
    Arabic(R.string.arabic, { Locale.forLanguageTag("ar") }),
    ChineseSimplified(R.string.chinese_simplified, { Locale.SIMPLIFIED_CHINESE }),
    ChineseTraditional(R.string.chinese_traditional, { Locale.TRADITIONAL_CHINESE }),
    English(R.string.english, { Locale.ENGLISH }),
    Greek(R.string.greek, { Locale.forLanguageTag("el") }),
    Japanese(R.string.japanese, { Locale.JAPANESE }),
    Odia(R.string.odia, { Locale.forLanguageTag("or") }),
    Persian(R.string.persian, { Locale.forLanguageTag("fa") }),
    Russian(R.string.russian, { Locale.forLanguageTag("ru") }),
    Spanish(R.string.spanish, { Locale.forLanguageTag("es") }),
    Turkish(R.string.turkish, { Locale.forLanguageTag("tr") }),
    Vietnamese(R.string.vietnamese, { Locale.forLanguageTag("vi") }),
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
