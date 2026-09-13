package com.ultron.assistant.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenRouterProvider : AiProvider {

    override val providerName: String = "OpenRouter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun generateResponse(
        endpointUrl: String,
        apiKey: String,
        request: AiRequest
    ): AiResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AiResponse(
                isSuccess = false,
                responseText = "Boss, OpenRouter API key configured nahi hai. Settings mein enter karein.",
                errorMessage = "Missing API Key"
            )
        }

        try {
            val payload = buildJsonObject {
                put("model", request.model)
                put("temperature", request.temperature)
                put("max_tokens", request.maxTokens)
                putJsonArray("messages") {
                    for (msg in request.messages) {
                        add(buildJsonObject {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                }
                // Request JSON response format
                putJsonObject("response_format") {
                    put("type", "json_object")
                }
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val httpRequest = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://ultron.assistant.app")
                .addHeader("X-Title", "ULTRON Android Assistant")
                .post(requestBody)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = when (code) {
                        401 -> "Invalid API Key or unauthorized."
                        404 -> "Model or endpoint not found."
                        429 -> "Rate limit reached on OpenRouter."
                        else -> "API Error code $code: $body"
                    }
                    return@withContext AiResponse(
                        isSuccess = false,
                        responseText = "Boss, AI API unavailable: $errorMsg",
                        errorMessage = errorMsg
                    )
                }

                parseApiResponse(body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenRouter call failed", e)
            AiResponse(
                isSuccess = false,
                responseText = "Boss, AI API se connect nahi ho paya: ${e.message}",
                errorMessage = e.localizedMessage
            )
        }
    }

    override suspend fun testConnection(
        endpointUrl: String,
        apiKey: String,
        model: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API Key cannot be empty."))
        }
        val testReq = AiRequest(
            model = model,
            messages = listOf(
                AiChatMessage("user", "Hello Ultron test ping. Respond with JSON: {\"actions\":[],\"response\":\"Ping OK, Boss.\"}")
            ),
            maxTokens = 60
        )
        val res = generateResponse(endpointUrl, apiKey, testReq)
        if (res.isSuccess) {
            Result.success("Connection Successful! Model: $model")
        } else {
            Result.failure(Exception(res.errorMessage ?: res.responseText))
        }
    }

    private fun parseApiResponse(jsonString: String): AiResponse {
        return try {
            val root = json.parseToJsonElement(jsonString)
            val choices = (root as? kotlinx.serialization.json.JsonObject)?.get("choices") as? kotlinx.serialization.json.JsonArray
            val firstChoice = choices?.firstOrNull() as? kotlinx.serialization.json.JsonObject
            val messageObj = firstChoice?.get("message") as? kotlinx.serialization.json.JsonObject
            val rawContent = (messageObj?.get("content") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "{}"

            // Extract JSON object from raw content (in case fenced in ```json)
            val cleanJson = extractJsonPayload(rawContent)
            val structured = json.decodeFromString<AiStructuredPayload>(cleanJson)

            val toolCalls = structured.actions.map { it.toToolCall() }
            AiResponse(
                isSuccess = true,
                responseText = structured.response,
                toolCalls = toolCalls
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse structured JSON from AI", e)
            AiResponse(
                isSuccess = true,
                responseText = "Yes Boss, request received.",
                toolCalls = emptyList()
            )
        }
    }

    private fun extractJsonPayload(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json")
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```")
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```")
        }
        return trimmed.trim()
    }

    companion object {
        private const val TAG = "OpenRouterProvider"
    }
}
