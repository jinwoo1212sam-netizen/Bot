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
You are $assistantName, a futuristic personal Android voice + text assistant.
You address the user exclusively as "$userTitle".
Style: Intelligent, concise, calm, helpful, slightly robotic.
You can ONLY perform supported Android actions by returning structured tool calls in your JSON output.
You MUST output valid JSON matching this exact schema:
{
  "actions": [
    {
      "type": "OPEN_APP",
      "app": "youtube"
    }
  ],
  "response": "Opening YouTube, $userTitle."
}

ALLOWED ACTION TYPES:
- "OPEN_APP" with param "app" (e.g. youtube, instagram, snapchat, chrome, whatsapp, camera, calculator)
- "SEARCH_YOUTUBE" with param "query"
- "CALL_CONTACT" with param "contact"
- "PREPARE_MESSAGE" with params "person", "platform" (whatsapp/telegram/sms), "message"
- "SET_VOLUME" with params "value" (0-100) or "mode" (increase/decrease/full/mute)
- "OPEN_SETTINGS" with param "setting" (wifi, bluetooth, sound, display, battery, etc.)
- "SET_TIMER" with param "seconds"
- "SET_ALARM" with params "hour", "minute"
- "OPEN_CAMERA"
- "WEB_SEARCH" with param "query"
- "GET_TIME"
- "GET_DATE"
- "CUSTOM_COMMAND" with param "trigger"

RULES:
1. Always address user as "$userTitle".
2. Keep the response text concise (e.g. "Yes, $userTitle.", "On it, $userTitle.", "YouTube open kar raha hoon, $userTitle.").
3. Never include executable code or shell scripts.
4. If no phone action is needed (e.g. answering a general question), leave "actions": [] empty.
""".trimIndent()
    }

    suspend fun processUserMessage(
        userMessage: String,
        recentHistory: List<AiChatMessage> = emptyList(),
        aliases: Map<String, String> = emptyMap()
    ): ExecutionReport {
        val userTitle = preferencesRepository.userTitle.first()
        val assistantName = preferencesRepository.assistantName.first()
        val chatUrl = preferencesRepository.aiChatUrl.first()
        val apiKey = preferencesRepository.getApiKey()
        val model = preferencesRepository.aiModel.first()

        val messages = mutableListOf<AiChatMessage>()
        messages.add(AiChatMessage("system", buildSystemPrompt(userTitle, assistantName)))
        messages.addAll(recentHistory.takeLast(6))
        messages.add(AiChatMessage("user", userMessage))

        val request = AiRequest(
            model = model,
            messages = messages
        )

        val aiResponse = provider.generateResponse(chatUrl, apiKey, request)

        if (!aiResponse.isSuccess) {
            return ExecutionReport(
                results = listOf(
                    ExecutionStepResult("AI Provider Error", false, aiResponse.errorMessage ?: "AI Request Failed")
                ),
                finalSpeech = aiResponse.responseText
            )
        }

        val stepResults = mutableListOf<ExecutionStepResult>()

        // Execute each validated action returned by the AI
        for (toolCall in aiResponse.toolCalls) {
            if (!toolRegistry.isToolAllowed(toolCall.type)) {
                stepResults.add(
                    ExecutionStepResult("Blocked Action", false, "Security: Action '${toolCall.type}' rejected by allowlist.")
                )
                continue
            }

            val actionResult = toolRegistry.executeTool(toolCall, aliases)
            when (actionResult) {
                is com.ultron.assistant.actions.ActionResult.Success -> {
                    stepResults.add(ExecutionStepResult(toolCall.type, true, actionResult.message))
                }
                is com.ultron.assistant.actions.ActionResult.Failure -> {
                    stepResults.add(ExecutionStepResult(toolCall.type, false, actionResult.reason))
                }
                is com.ultron.assistant.actions.ActionResult.RequiresConfirmation -> {
                    stepResults.add(ExecutionStepResult(toolCall.type, true, actionResult.prompt))
                }
            }
        }

        return ExecutionReport(
            results = stepResults,
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
