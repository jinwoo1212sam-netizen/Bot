package com.ultron.assistant.ai

import com.ultron.assistant.tools.ToolCall
import kotlinx.serialization.Serializable

@Serializable
data class AiChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class AiRequest(
    val model: String,
    val messages: List<AiChatMessage>,
    val temperature: Float = 0.3f,
    val maxTokens: Int = 500
)

@Serializable
data class AiStructuredAction(
    val type: String,
    val app: String? = null,
    val query: String? = null,
    val contact: String? = null,
    val person: String? = null,
    val platform: String? = null,
    val message: String? = null,
    val value: String? = null,
    val setting: String? = null,
    val seconds: String? = null,
    val hour: String? = null,
    val minute: String? = null
) {
    fun toToolCall(): ToolCall {
        val params = mutableMapOf<String, String>()
        app?.let { params["app"] = it }
        query?.let { params["query"] = it }
        contact?.let { params["contact"] = it }
        person?.let { params["person"] = it }
        platform?.let { params["platform"] = it }
        message?.let { params["message"] = it }
        value?.let { params["value"] = it }
        setting?.let { params["setting"] = it }
        seconds?.let { params["seconds"] = it }
        hour?.let { params["hour"] = it }
        minute?.let { params["minute"] = it }
        return ToolCall(type.lowercase(), params)
    }
}

@Serializable
data class AiStructuredPayload(
    val actions: List<AiStructuredAction> = emptyList(),
    val response: String
)

data class AiResponse(
    val isSuccess: Boolean,
    val responseText: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val errorMessage: String? = null
)
