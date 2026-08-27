package com.parallelc.micts.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AiKeyStorageFactory {
    private const val ENCRYPTED_PREFS_NAME = "secure_ai_config"
    private const val KEY_API_KEY = "ai_api_key"

    fun create(context: Context): AiKeyStorage {
        val prefs: SharedPreferences = runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
        }

        return object : AiKeyStorage {
            override fun getApiKey(): String = prefs.getString(KEY_API_KEY, "").orEmpty()
            override fun setApiKey(key: String) {
                prefs.edit().putString(KEY_API_KEY, key).apply()
            }
        }
    }
}
