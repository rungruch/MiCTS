package com.parallelc.micts.data

import com.parallelc.micts.domain.AiChatMessage

object AiGatewayFactory {
    fun create(baseUrl: String, apiKey: String): AiGateway = object : AiGateway {
        override suspend fun chat(
            messages: List<AiChatMessage>,
            model: String,
            sendImageEnabled: Boolean,
        ): Result<String> = Result.failure(UnsupportedOperationException("AI is not supported in VISTrigger"))

        override suspend fun testConnection(): Result<Int> =
            Result.failure(UnsupportedOperationException("AI is not supported in VISTrigger"))

        override fun close() = Unit
    }
}
