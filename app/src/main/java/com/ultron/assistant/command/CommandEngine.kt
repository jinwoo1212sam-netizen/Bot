package com.ultron.assistant.command

import com.ultron.assistant.actions.ActionResult
import com.ultron.assistant.tools.ToolRegistry
import kotlinx.coroutines.delay

class CommandEngine(
    private val localParser: LocalCommandParser,
    private val validator: CommandValidator,
    private val toolRegistry: ToolRegistry
) {

    suspend fun executeRawCommand(
        rawText: String,
        aliases: Map<String, String> = emptyMap()
    ): ExecutionReport {
        val parsed = localParser.parse(rawText)

        return when (parsed) {
            is ParsedCommand.SpecialResponse -> {
                ExecutionReport(
                    results = listOf(ExecutionStepResult("Security Notice", true, parsed.responseText)),
                    finalSpeech = parsed.responseText
                )
            }
            is ParsedCommand.Single -> {
                val stepResult = executeSingle(parsed, aliases)
                ExecutionReport(
                    results = listOf(stepResult),
                    finalSpeech = stepResult.message
                )
            }
            is ParsedCommand.Multi -> {
                val stepResults = mutableListOf<ExecutionStepResult>()
                for (single in parsed.commands) {
                    val res = executeSingle(single, aliases)
                    stepResults.add(res)
                    delay(300) // gentle pause between multi-action execution
                }
                val allSuccessful = stepResults.all { it.isSuccess }
                val speech = if (allSuccessful) "Done, Boss." else "Boss, kuch actions poore nahi ho sake."
                ExecutionReport(
                    results = stepResults,
                    finalSpeech = speech
                )
            }
            is ParsedCommand.Unknown -> {
                val msg = "Boss, ye command samajh nahi aaya. Kripya dobara bolein ya Settings se OpenRouter AI enable karein."
                ExecutionReport(
                    results = listOf(ExecutionStepResult("Unknown Command", false, msg)),
                    finalSpeech = msg
                )
            }
        }
    }

    private suspend fun executeSingle(
        single: ParsedCommand.Single,
        aliases: Map<String, String>
    ): ExecutionStepResult {
        val validation = validator.validate(single.toolCall)
        if (!validation.isValid) {
            return ExecutionStepResult(
                title = single.toolCall.type,
                isSuccess = false,
                message = validation.errorMessage ?: "Action invalid."
            )
        }

        val result = toolRegistry.executeTool(single.toolCall, aliases)
        return when (result) {
            is ActionResult.Success -> ExecutionStepResult(
                title = formatToolTitle(single.toolCall),
                isSuccess = true,
                message = result.message
            )
            is ActionResult.Failure -> ExecutionStepResult(
                title = formatToolTitle(single.toolCall),
                isSuccess = false,
                message = result.reason
            )
            is ActionResult.RequiresConfirmation -> ExecutionStepResult(
                title = formatToolTitle(single.toolCall),
                isSuccess = true,
                message = result.prompt
            )
        }
    }

    private fun formatToolTitle(toolCall: com.ultron.assistant.tools.ToolCall): String {
        return when (toolCall.type) {
            "open_app" -> "${toolCall.parameters["app"]?.replaceFirstChar { it.uppercase() }} opened"
            "search_youtube" -> "YouTube search opened"
            "set_volume" -> "Volume set"
            "call_contact" -> "Call placed"
            "prepare_message" -> "Message prepared"
            "open_settings" -> "Settings opened"
            "set_timer" -> "Timer set"
            "set_alarm" -> "Alarm set"
            else -> toolCall.type
        }
    }
}
