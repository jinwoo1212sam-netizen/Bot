package com.ultron.assistant.command

import com.ultron.assistant.tools.ToolCall
import com.ultron.assistant.tools.ToolRegistry

class CommandValidator(private val toolRegistry: ToolRegistry) {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validate(toolCall: ToolCall): ValidationResult {
        if (!toolRegistry.isToolAllowed(toolCall.type)) {
            return ValidationResult(false, "Unknown or forbidden tool call: ${toolCall.type}")
        }
        return ValidationResult(true)
    }
}
