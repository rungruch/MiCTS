package com.parallelc.micts.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiModelsTest {

    @Test
    fun buildUserQuestionUsesSelectedTextWhenPresent() {
        val question = AiPromptBuilder.buildUserQuestion(
            selectedText = "Selected OCR text",
            allRecognizedText = "Line 1\nLine 2\nLine 3",
            userQuestion = "Summarize this screenshot",
        )

        assertEquals(
            "Recognized text from screenshot:\n---\nSelected OCR text\n---\nSummarize this screenshot",
            question,
        )
    }

    @Test
    fun buildUserQuestionFallsBackToAllRecognizedTextWhenSelectionBlank() {
        val question = AiPromptBuilder.buildUserQuestion(
            selectedText = "   ",
            allRecognizedText = "Full page OCR text",
            userQuestion = "What does this say?",
        )

        assertEquals(
            "Recognized text from screenshot:\n---\nFull page OCR text\n---\nWhat does this say?",
            question,
        )
    }

    @Test
    fun buildUserQuestionReturnsPlainQuestionWhenNoOcrTextAvailable() {
        val question = AiPromptBuilder.buildUserQuestion(
            selectedText = "",
            allRecognizedText = "   ",
            userQuestion = "Hello world",
        )

        assertEquals("Hello world", question)
    }

    @Test
    fun shouldAttachImageReturnsTrueOnlyWhenEnabledAndImagePresent() {
        assertTrue(AiPromptBuilder.shouldAttachImage(true, "data:image/jpeg;base64,abc123xyz"))
        assertFalse(AiPromptBuilder.shouldAttachImage(false, "data:image/jpeg;base64,abc123xyz"))
        assertFalse(AiPromptBuilder.shouldAttachImage(true, null))
        assertFalse(AiPromptBuilder.shouldAttachImage(true, ""))
        assertFalse(AiPromptBuilder.shouldAttachImage(true, "   "))
    }

    @Test
    fun normalizeChatCompletionsUrlHandlesVariousFormats() {
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            AiPromptBuilder.normalizeChatCompletionsUrl("https://api.deepseek.com/v1"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            AiPromptBuilder.normalizeChatCompletionsUrl("https://api.deepseek.com/v1/"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            AiPromptBuilder.normalizeChatCompletionsUrl("https://api.deepseek.com/v1/chat/completions"),
        )
        assertEquals(
            "http://192.168.1.50:11434/v1/chat/completions",
            AiPromptBuilder.normalizeChatCompletionsUrl("  http://192.168.1.50:11434/v1/  "),
        )
    }

    @Test
    fun normalizeModelsUrlHandlesVariousFormats() {
        assertEquals(
            "https://api.deepseek.com/v1/models",
            AiPromptBuilder.normalizeModelsUrl("https://api.deepseek.com/v1"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/models",
            AiPromptBuilder.normalizeModelsUrl("https://api.deepseek.com/v1/"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/models",
            AiPromptBuilder.normalizeModelsUrl("https://api.deepseek.com/v1/chat/completions"),
        )
    }

    @Test
    fun escapeJsonEscapesSpecialCharacters() {
        val raw = "Hello \"World\"\nLine 2\tTabbed\\Backslash"
        val escaped = AiPromptBuilder.escapeJson(raw)
        assertEquals("Hello \\\"World\\\"\\nLine 2\\tTabbed\\\\Backslash", escaped)
    }

    @Test
    fun buildRequestBodyJsonFormatsTextOnlyMessages() {
        val messages = listOf(
            AiChatMessage(AiMessageRole.USER, "What is this?"),
            AiChatMessage(AiMessageRole.ASSISTANT, "It is a test."),
        )

        val json = AiPromptBuilder.buildRequestBodyJson(
            model = "deepseek-chat",
            messages = messages,
            sendImageEnabled = false,
        )

        assertEquals(
            """{"model":"deepseek-chat","messages":[{"role":"user","content":"What is this?"},{"role":"assistant","content":"It is a test."}],"stream":false}""",
            json,
        )
    }

    @Test
    fun buildRequestBodyJsonFormatsMultimodalMessagesWhenImageAttachedAndEnabled() {
        val messages = listOf(
            AiChatMessage(
                role = AiMessageRole.USER,
                text = "Explain image",
                imageBase64Url = "data:image/jpeg;base64,FAKEDATA",
            ),
        )

        val json = AiPromptBuilder.buildRequestBodyJson(
            model = "gpt-4o",
            messages = messages,
            sendImageEnabled = true,
        )

        assertEquals(
            """{"model":"gpt-4o","messages":[{"role":"user","content":[{"type":"text","text":"Explain image"},{"type":"image_url","image_url":{"url":"data:image/jpeg;base64,FAKEDATA"}}]}],"stream":false}""",
            json,
        )
    }

    @Test
    fun buildRequestBodyJsonDegradesToTextOnlyWhenImageSendDisabled() {
        val messages = listOf(
            AiChatMessage(
                role = AiMessageRole.USER,
                text = "Explain image",
                imageBase64Url = "data:image/jpeg;base64,FAKEDATA",
            ),
        )

        val json = AiPromptBuilder.buildRequestBodyJson(
            model = "deepseek-chat",
            messages = messages,
            sendImageEnabled = false,
        )

        assertEquals(
            """{"model":"deepseek-chat","messages":[{"role":"user","content":"Explain image"}],"stream":false}""",
            json,
        )
    }
}
