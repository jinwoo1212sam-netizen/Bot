package com.ultron.assistant.ai

interface AiProvider {
    val providerName: String

    suspend fun generateResponse(
        endpointUrl: String,
        apiKey: String,
        request: AiRequest
    ): AiResponse

    suspend fun testConnection(
        endpointUrl: String,
        apiKey: String,
        model: String
    ): Result<String>
}
