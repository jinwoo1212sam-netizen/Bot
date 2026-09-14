package com.ultron.assistant.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class OpenRouterProvider : AiProvider {

    override val providerName: String = "OpenRouter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
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
                responseText = "API key is missing. Please configure your OpenRouter API key in Settings.",
                errorMessage = "API key is missing."
            )
        }

        // Check if any message includes an image and if model is multimodal
        val hasImage = request.messages.any { it.imageBase64 != null }
        if (hasImage && !AiModelHelper.isVisionCapable(request.model)) {
            return@withContext AiResponse(
                isSuccess = false,
                responseText = "The selected model '${request.model}' does not support image analysis. Please choose a vision model like 'ling-3.0-flash-vl:free' in Settings.",
                errorMessage = "The selected model does not support images."
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
                            if (msg.imageBase64 != null) {
                                // Multimodal vision message array
                                putJsonArray("content") {
                                    add(buildJsonObject {
                                        put("type", "text")
                                        put("text", msg.content.ifBlank { "Describe or analyze this image." })
                                    })
                                    add(buildJsonObject {
                                        put("type", "image_url")
                                        putJsonObject("image_url") {
                                            put("url", "data:${msg.imageMimeType};base64,${msg.imageBase64}")
                                        }
                                    })
                                }
                            } else {
                                put("content", msg.content)
                            }
                        })
                    }
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
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val friendlyError = parseHttpError(response.code, body, request.model)
                    return@withContext AiResponse(
                        isSuccess = false,
                        responseText = friendlyError,
                        errorMessage = friendlyError
                    )
                }

                if (body.isBlank()) {
                    return@withContext AiResponse(
                        isSuccess = false,
                        responseText = "Empty response received from AI server.",
                        errorMessage = "Empty response"
                    )
                }

                parseApiResponse(body)
            }
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network host resolution failed", e)
            AiResponse(
                isSuccess = false,
                responseText = "Please check your internet connection. Unable to reach OpenRouter.",
                errorMessage = "No internet connection"
            )
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "OpenRouter call timed out", e)
            AiResponse(
                isSuccess = false,
                responseText = "Request timed out. OpenRouter took too long to respond.",
                errorMessage = "Timeout"
            )
        } catch (e: SSLException) {
            Log.e(TAG, "SSL handshake error", e)
            AiResponse(
                isSuccess = false,
                responseText = "Secure SSL connection failed. Check your device time and network security.",
                errorMessage = "SSL Error"
            )
        } catch (e: IOException) {
            Log.e(TAG, "Network IO error", e)
            AiResponse(
                isSuccess = false,
                responseText = "Network error: Unable to connect to OpenRouter. ${e.localizedMessage ?: ""}",
                errorMessage = e.localizedMessage
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in OpenRouter call", e)
            AiResponse(
                isSuccess = false,
                responseText = "An unexpected error occurred: ${e.localizedMessage ?: "Unknown error"}",
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
            return@withContext Result.failure(Exception("API Key cannot be empty. Please enter your OpenRouter key."))
        }

        try {
            val payload = buildJsonObject {
                put("model", model)
                put("max_tokens", 10)
                putJsonArray("messages") {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", "Respond with 'OK'.")
                    })
                }
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val httpRequest = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://ultron.assistant.app")
                .addHeader("X-Title", "ULTRON Assistant")
                .post(requestBody)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Connection Successful! Model '$model' is responding normally.")
                } else {
                    val error = parseHttpError(response.code, body, model)
                    Result.failure(Exception(error))
                }
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Connection timed out while contacting OpenRouter."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Connection test failed."))
        }
    }

    private fun parseHttpError(code: Int, body: String, model: String): String {
        val serverMessage = try {
            val root = json.parseToJsonElement(body).jsonObject
            val errorObj = root["error"]?.jsonObject
            errorObj?.get("message")?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) {
            null
        }

        return when (code) {
            401 -> "Invalid API key. Please check your OpenRouter API key in Settings."
            402 -> "Insufficient credits on your OpenRouter account."
            404 -> "The selected model '$model' is unavailable or not found."
            429 -> "Rate limit reached on OpenRouter. Please wait a moment before trying again."
            500, 502, 503, 504 -> "OpenRouter server is temporarily unavailable ($code). Please try again shortly."
            else -> serverMessage ?: "Unable to connect to OpenRouter (HTTP $code)."
        }
    }

    private fun parseApiResponse(jsonString: String): AiResponse {
        return try {
            val root = json.parseToJsonElement(jsonString) as? JsonObject
                ?: return AiResponse(isSuccess = false, responseText = "Invalid JSON response from server.", errorMessage = "Invalid JSON")

            val choices = root["choices"]?.jsonArray
            val firstChoice = choices?.firstOrNull()?.jsonObject
            val messageObj = firstChoice?.get("message")?.jsonObject
            val rawContent = messageObj?.get("content")?.jsonPrimitive?.contentOrNull

            if (rawContent.isNullOrBlank()) {
                return AiResponse(
                    isSuccess = false,
                    responseText = "Received empty response from assistant.",
                    errorMessage = "Empty content"
                )
            }

            AiResponse(
                isSuccess = true,
                responseText = rawContent.trim(),
                toolCalls = emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse API response", e)
            AiResponse(
                isSuccess = false,
                responseText = "Failed to parse response from OpenRouter: ${e.localizedMessage}",
                errorMessage = "Parse error"
            )
        }
    }

    companion object {
        private const val TAG = "OpenRouterProvider"
    }
}
