package com.parallelc.micts.domain

enum class AiMessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system")
}

data class AiChatMessage(
    val role: AiMessageRole,
    val text: String,
    val imageBase64Url: String? = null,
)

data class AiConversationState(
    val messages: List<AiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

object AiPromptBuilder {
    const val DEFAULT_SUMMARIZE_PROMPT = "Summarize this screenshot"

    fun buildUserQuestion(
        selectedText: String,
        allRecognizedText: String,
        userQuestion: String,
    ): String {
        val trimmedQuestion = userQuestion.trim()
        val ocrContext = selectedText.trim().ifEmpty { allRecognizedText.trim() }

        if (ocrContext.isEmpty()) {
            return trimmedQuestion
        }

        return buildString {
            append("Recognized text from screenshot:\n---\n")
            append(ocrContext)
            append("\n---\n")
            append(trimmedQuestion)
        }
    }

    fun shouldAttachImage(sendImageEnabled: Boolean, imageBase64Url: String?): Boolean {
        return sendImageEnabled && !imageBase64Url.isNullOrBlank()
    }

    fun normalizeChatCompletionsUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().removeSuffix("/")
        return if (trimmed.endsWith("/chat/completions")) {
            trimmed
        } else {
            "$trimmed/chat/completions"
        }
    }

    fun normalizeModelsUrl(baseUrl: String): String {
        var trimmed = baseUrl.trim().removeSuffix("/")
        if (trimmed.endsWith("/chat/completions")) {
            trimmed = trimmed.removeSuffix("/chat/completions").removeSuffix("/")
        }
        return "$trimmed/models"
    }

    fun escapeJson(text: String): String {
        val builder = StringBuilder()
        for (char in text) {
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> {
                    if (char.code in 0x00..0x1F) {
                        builder.append(String.format("\\u%04x", char.code))
                    } else {
                        builder.append(char)
                    }
                }
            }
        }
        return builder.toString()
    }

    fun buildRequestBodyJson(
        model: String,
        messages: List<AiChatMessage>,
        sendImageEnabled: Boolean,
    ): String {
        val messagesJson = messages.joinToString(",") { message ->
            val attachImage = message.role == AiMessageRole.USER &&
                shouldAttachImage(sendImageEnabled, message.imageBase64Url)

            if (!attachImage) {
                """{"role":"${message.role.value}","content":"${escapeJson(message.text)}"}"""
            } else {
                """{"role":"${message.role.value}","content":[{"type":"text","text":"${escapeJson(message.text)}"},{"type":"image_url","image_url":{"url":"${escapeJson(message.imageBase64Url!!)}"}}]}"""
            }
        }
        return """{"model":"${escapeJson(model)}","messages":[$messagesJson],"stream":false}"""
    }
}
