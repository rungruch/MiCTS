package com.parallelc.micts.data

import android.content.Context

object AiKeyStorageFactory {
    fun create(context: Context): AiKeyStorage = object : AiKeyStorage {
        override fun getApiKey(): String = ""
        override fun setApiKey(key: String) = Unit
    }
}
