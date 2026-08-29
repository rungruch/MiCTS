package com.parallelc.micts.data

import android.content.Intent

/**
 * Remembers the user's MediaProjection approval on Android 13 and
 * below so the system consent dialog appears on first use instead of before
 * every trigger. Android 14+ makes every consent token single-use at the platform
 * level, so callers must not store or reuse tokens there.
 *
 * The consent Intent contains an active IPC IBinder token that lives in memory.
 * The store is deliberately process-scoped: the token is never persisted to
 * disk, so process death requires a fresh approval. Android may also invalidate
 * a live token; callers clear this store and re-prompt when it fails.
 */
object ProjectionConsentStore {
    fun load(): Pair<Int, Intent>? = cachedConsent

    fun save(resultCode: Int, resultData: Intent) {
        cachedConsent = resultCode to resultData
    }

    fun clear() {
        cachedConsent = null
    }

    @Volatile
    private var cachedConsent: Pair<Int, Intent>? = null
}
