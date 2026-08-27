package com.parallelc.micts.data

object AiGatewayFactory {
    fun create(baseUrl: String, apiKey: String): AiGateway =
        OpenAiCompatibleAiGateway(baseUrl, apiKey)
}
