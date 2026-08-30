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
