package com.parallelc.micts.data

import com.parallelc.micts.domain.AiChatMessage
import java.io.Closeable

interface AiGateway : Closeable {
    suspend fun chat(
        messages: List<AiChatMessage>,
        model: String,
        sendImageEnabled: Boolean,
    ): Result<String>

    suspend fun testConnection(): Result<Int>
}
