package com.parallelc.micts.data

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.util.Base64
import com.parallelc.micts.config.AppConfig

/**
 * Remembers the user's MediaProjection approval on Android 13 and
 * below so the system consent dialog appears on first use instead of before
 * every trigger. Android 14+ makes every consent token single-use at the platform
 * level, so callers must not store or reuse tokens there.
 *
 * The consent Intent contains an active IPC IBinder token that lives in memory.
 * Android may still invalidate a token (reboot, OEM policy); callers clear
 * this store and re-prompt when the token fails.
 */
class ProjectionConsentStore(context: Context) {
    fun load(): Pair<Int, Intent>? = cachedConsent

    fun save(resultCode: Int, resultData: Intent) {
        cachedConsent = resultCode to resultData
    }

    fun clear() {
        cachedConsent = null
    }

    companion object {
        @Volatile
        private var cachedConsent: Pair<Int, Intent>? = null
    }
}
