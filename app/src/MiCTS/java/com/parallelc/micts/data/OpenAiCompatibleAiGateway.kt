package com.parallelc.micts.data

import android.graphics.Bitmap
import android.util.Base64
import com.parallelc.micts.domain.AiChatMessage
import com.parallelc.micts.domain.AiPromptBuilder
import com.parallelc.micts.domain.IntCropRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

class OpenAiCompatibleAiGateway(
    private val baseUrl: String,
    private val apiKey: String,
) : AiGateway {

    companion object {
        const val MAX_IMAGE_EDGE = 1568
        private const val JPEG_QUALITY = 85
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun encodeRegionToBase64(bitmap: Bitmap, crop: IntCropRect? = null): String =
            AiImageEncoder.encodeRegionToBase64(bitmap, crop)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(
        messages: List<AiChatMessage>,
        model: String,
        sendImageEnabled: Boolean,
    ): Result<String> = withContext(Dispatchers.IO) {
        val completionsUrl = AiPromptBuilder.normalizeChatCompletionsUrl(baseUrl)
        val jsonPayload = AiPromptBuilder.buildRequestBodyJson(model, messages, sendImageEnabled)
        val body = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)

        val requestBuilder = Request.Builder()
            .url(completionsUrl)
            .post(body)

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val hasImage = messages.any { it.imageBase64Url != null && sendImageEnabled }
                    if ((response.code == 400 || response.code == 422) && hasImage) {
                        return@withContext Result.failure(
                            IOException(
                                "HTTP ${response.code}: The model may not support images — try disabling 'Send screenshot to AI' in Settings.\n\nServer response: $responseBody"
                            )
                        )
                    }
                    val errorMessage = parseErrorMessage(responseBody) ?: "HTTP ${response.code}: $responseBody"
                    return@withContext Result.failure(IOException(errorMessage))
                }

                val parsed = parseChatResponse(responseBody)
                if (parsed != null) {
                    Result.success(parsed)
                } else {
                    Result.failure(IOException("Invalid response format from AI server: $responseBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(): Result<Int> = withContext(Dispatchers.IO) {
        val modelsUrl = AiPromptBuilder.normalizeModelsUrl(baseUrl)
        val requestBuilder = Request.Builder()
            .url(modelsUrl)
            .get()

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMessage = parseErrorMessage(responseBody) ?: "HTTP ${response.code}: $responseBody"
                    return@withContext Result.failure(IOException(errorMessage))
                }
                val count = runCatching {
                    val json = JSONObject(responseBody)
                    json.optJSONArray("data")?.length() ?: 0
                }.getOrDefault(0)
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseChatResponse(jsonString: String): String? = runCatching {
        val json = JSONObject(jsonString)
        val choices = json.getJSONArray("choices")
        if (choices.length() == 0) return null
        val message = choices.getJSONObject(0).getJSONObject("message")
        message.getString("content")
    }.getOrNull()

    private fun parseErrorMessage(jsonString: String): String? = runCatching {
        val json = JSONObject(jsonString)
        val error = json.optJSONObject("error")
        error?.optString("message")?.takeIf { it.isNotBlank() }
    }.getOrNull()

    override fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
