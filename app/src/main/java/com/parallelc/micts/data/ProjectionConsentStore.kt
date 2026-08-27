package com.parallelc.micts.data

import android.content.Context
import android.content.Intent
import android.util.Base64
import com.parallelc.micts.config.AppConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Remembers the user's one-time MediaProjection approval so the system consent
 * dialog only appears on first use instead of before every trigger.
 *
 * Android may still invalidate a stored projection token (reboot, OEM policy,
 * security changes); callers must clear this store and re-prompt exactly once
 * when the token fails instead of retrying a dead token forever.
 */
class ProjectionConsentStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        AppConfig.CONFIG_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): Pair<Int, Intent>? {
        if (!preferences.contains(KEY_RESULT_CODE)) return null
        val encoded = preferences.getString(KEY_RESULT_DATA, null) ?: return null
        return runCatching {
            val resultCode = preferences.getInt(KEY_RESULT_CODE, Int.MIN_VALUE)
            val intent = ObjectInputStream(
                ByteArrayInputStream(Base64.decode(encoded, Base64.NO_WRAP)),
            ).readObject() as Intent
            resultCode to intent
        }.getOrNull().also { stored ->
            if (stored == null) clear()
        }
    }

    fun save(resultCode: Int, resultData: Intent) {
        val encoded = runCatching {
            ByteArrayOutputStream().use { stream ->
                ObjectOutputStream(stream).writeObject(resultData)
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }
        }.getOrNull()
        if (encoded == null) {
            // Never persist half a consent pair.
            clear()
            return
        }
        preferences.edit()
            .putInt(KEY_RESULT_CODE, resultCode)
            .putString(KEY_RESULT_DATA, encoded)
            .apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_RESULT_CODE).remove(KEY_RESULT_DATA).apply()
    }

    private companion object {
        const val KEY_RESULT_CODE = "projection_result_code"
        const val KEY_RESULT_DATA = "projection_result_data"
    }
}