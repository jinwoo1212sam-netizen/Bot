package com.ultron.assistant.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ToolCall(
    val type: String,
    val parameters: Map<String, String> = emptyMap()
)

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val requiredParameters: List<String>,
    val optionalParameters: List<String> = emptyList(),
    val requiredPermissions: List<String> = emptyList()
)
