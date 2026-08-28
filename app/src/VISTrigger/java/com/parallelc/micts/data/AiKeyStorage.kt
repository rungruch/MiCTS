package com.parallelc.micts.data

interface AiKeyStorage {
    fun getApiKey(): String
    fun setApiKey(key: String)
}
