package com.ultron.assistant.ai

import com.ultron.assistant.tools.ToolCall
import kotlinx.serialization.Serializable

@Serializable
data class AiChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String,
    val imageBase64: String? = null,
    val imageMimeType: String = "image/jpeg"
)

@Serializable
data class AiRequest(
    val model: String,
    val messages: List<AiChatMessage>,
    val temperature: Float = 0.5f,
    val maxTokens: Int = 1200
)

object AiModelHelper {
    /**
     * Checks whether the given model name is likely multimodal / vision-capable.
     * Default model "ling-3.0-flash-vl:free" has the "-vl" (Vision-Language) suffix.
     */
    fun isVisionCapable(modelName: String): Boolean {
        val name = modelName.lowercase()
        return name.contains("-vl") ||
                name.contains("vision") ||
                name.contains("4o") ||
                name.contains("gemini") ||
                name.contains("claude-3") ||
                name.contains("pixtral") ||
                name.contains("llava") ||
                name.contains("multimodal")
    }
}

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

data class AiResponse(
    val isSuccess: Boolean,
    val responseText: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val errorMessage: String? = null
)
