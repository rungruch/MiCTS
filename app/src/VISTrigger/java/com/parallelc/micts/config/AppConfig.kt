package com.parallelc.micts.config

object AppConfig {
    const val CONFIG_NAME = "app_config"
    const val KEY_LANGUAGE = "language"
    const val KEY_DEFAULT_DELAY = "default_delay"
    const val KEY_TILE_DELAY = "tile_delay"
    const val KEY_VIBRATE = "vibrate"

    val DEFAULT_CONFIG = mapOf<String, Any>(
        KEY_LANGUAGE to Language.FollowSystem.ordinal,
        KEY_DEFAULT_DELAY to 0L,
        KEY_TILE_DELAY to 400L,
        KEY_VIBRATE to false,
    )
}
