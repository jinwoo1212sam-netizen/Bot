package com.ultron.assistant.command

import com.ultron.assistant.tools.ToolCall

sealed class ParsedCommand {
    data class Single(val toolCall: ToolCall, val originalPhrase: String) : ParsedCommand()
    data class Multi(val commands: List<Single>, val originalPhrase: String) : ParsedCommand()
    data class SpecialResponse(val responseText: String) : ParsedCommand()
    data class Unknown(val rawText: String) : ParsedCommand()
}

data class ExecutionStepResult(
    val title: String,
    val isSuccess: Boolean,
    val message: String
)

data class ExecutionReport(
    val results: List<ExecutionStepResult>,
    val finalSpeech: String
)
