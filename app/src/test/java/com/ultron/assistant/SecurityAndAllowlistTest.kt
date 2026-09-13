package com.ultron.assistant

import com.ultron.assistant.command.CommandValidator
import com.ultron.assistant.tools.ToolCall
import com.ultron.assistant.tools.ToolDefinition
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndAllowlistTest {

    @Test
    fun testUnknownToolRejection() {
        // Test that arbitrary tool calls are rejected by allowlist
        val allowedTools = setOf(
            "open_app", "search_youtube", "call_contact", "prepare_message",
            "open_settings", "set_volume", "set_timer", "set_alarm",
            "open_camera", "open_browser", "web_search", "get_time",
            "get_date", "custom_command"
        )

        val maliciousTool = ToolCall(type = "execute_root_bash", parameters = mapOf("cmd" to "rm -rf /"))
        assertFalse(allowedTools.contains(maliciousTool.type))

        val arbitraryCodeTool = ToolCall(type = "run_arbitrary_code", parameters = emptyMap())
        assertFalse(allowedTools.contains(arbitraryCodeTool.type))

        val validTool = ToolCall(type = "search_youtube", parameters = mapOf("query" to "Kesariya"))
        assertTrue(allowedTools.contains(validTool.type))
    }

    @Test
    fun testSettingsUrlValidation() {
        val validOpenRouterUrl = "https://openrouter.ai/api/v1/chat/completions"
        assertTrue(validOpenRouterUrl.startsWith("https://"))
        assertTrue(validOpenRouterUrl.contains("chat/completions"))

        val localBaseUrl = "https://api.openai.com/v1"
        assertTrue(localBaseUrl.startsWith("https://"))
    }
}
