package com.ultron.assistant.ai

import com.ultron.assistant.command.ExecutionReport
import com.ultron.assistant.command.ExecutionStepResult
import com.ultron.assistant.data.PreferencesRepository
import com.ultron.assistant.tools.ToolRegistry
import kotlinx.coroutines.flow.first

class AiManager(
    private val preferencesRepository: PreferencesRepository,
    private val toolRegistry: ToolRegistry,
    private val provider: AiProvider = OpenRouterProvider()
) {

    private fun buildSystemPrompt(userTitle: String, assistantName: String): String {
        return """
You are $assistantName, an intelligent and advanced Android AI assistant.
You address the user as "$userTitle".
Style: Helpful, intelligent, concise, articulate, and respectful.
Format responses cleanly using Markdown when appropriate (use bullet points, numbered lists, bold text, and code blocks with syntax tags).
When providing code, put code inside standard markdown code fences (e.g. ```kotlin ... ```).
Keep answers direct and informative.
""".trimIndent()
    }

    suspend fun generateAiChatResponse(
        userMessage: String,
        recentHistory: List<AiChatMessage> = emptyList(),
        imageBase64: String? = null
    ): AiResponse {
        val userTitle = preferencesRepository.userTitle.first()
        val assistantName = preferencesRepository.assistantName.first()
        val chatUrl = preferencesRepository.aiChatUrl.first()
        val apiKey = preferencesRepository.getApiKey()
        val model = preferencesRepository.aiModel.first()
        val maxContext = preferencesRepository.maxContextMessages.first()

        val messages = mutableListOf<AiChatMessage>()
        messages.add(AiChatMessage("system", buildSystemPrompt(userTitle, assistantName)))

        // Limit conversation history to configured context window
        val trimmedHistory = recentHistory.takeLast(maxContext)
        messages.addAll(trimmedHistory)

        // Add current user prompt with optional multimodal image
        messages.add(
            AiChatMessage(
                role = "user",
                content = userMessage,
                imageBase64 = imageBase64
            )
        )

        val request = AiRequest(
            model = model,
            messages = messages
        )

        return provider.generateResponse(chatUrl, apiKey, request)
    }

    suspend fun processUserMessage(
        userMessage: String,
        recentHistory: List<AiChatMessage> = emptyList(),
        aliases: Map<String, String> = emptyMap()
    ): ExecutionReport {
        val aiResponse = generateAiChatResponse(userMessage, recentHistory)

        if (!aiResponse.isSuccess) {
            return ExecutionReport(
                results = listOf(
                    ExecutionStepResult("AI Provider", false, aiResponse.errorMessage ?: "Request Failed")
                ),
                finalSpeech = aiResponse.responseText
            )
        }

        return ExecutionReport(
            results = listOf(
                ExecutionStepResult("ULTRON AI", true, "Response generated successfully")
            ),
            finalSpeech = aiResponse.responseText
        )
    }

    suspend fun testConnection(): Result<String> {
        val chatUrl = preferencesRepository.aiChatUrl.first()
        val apiKey = preferencesRepository.getApiKey()
        val model = preferencesRepository.aiModel.first()
        return provider.testConnection(chatUrl, apiKey, model)
    }
}
