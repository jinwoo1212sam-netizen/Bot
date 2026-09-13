package com.ultron.assistant.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ultron.assistant.actions.ActionResult
import com.ultron.assistant.actions.AlarmAction
import com.ultron.assistant.actions.AppLauncher
import com.ultron.assistant.actions.CallAction
import com.ultron.assistant.actions.MessagingAction
import com.ultron.assistant.actions.SettingsAction
import com.ultron.assistant.actions.TimerAction
import com.ultron.assistant.actions.VolumeAction
import com.ultron.assistant.actions.YouTubeAction
import com.ultron.assistant.data.CustomCommandsRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolRegistry(
    private val context: Context,
    private val appLauncher: AppLauncher,
    private val youTubeAction: YouTubeAction,
    private val callAction: CallAction,
    private val messagingAction: MessagingAction,
    private val volumeAction: VolumeAction,
    private val settingsAction: SettingsAction,
    private val timerAction: TimerAction,
    private val alarmAction: AlarmAction,
    private val customCommandsRepository: CustomCommandsRepository
) {

    private val registry = mutableMapOf<String, ToolDefinition>()

    init {
        registerTool(
            ToolDefinition(
                name = "open_app",
                description = "Launch an installed Android app or system tool",
                requiredParameters = listOf("app")
            )
        )
        registerTool(
            ToolDefinition(
                name = "search_youtube",
                description = "Search or play songs/videos on YouTube",
                requiredParameters = listOf("query")
            )
        )
        registerTool(
            ToolDefinition(
                name = "call_contact",
                description = "Place a phone call to a saved or system contact",
                requiredParameters = listOf("contact"),
                requiredPermissions = listOf("android.permission.CALL_PHONE", "android.permission.READ_CONTACTS")
            )
        )
        registerTool(
            ToolDefinition(
                name = "prepare_message",
                description = "Compose a message on WhatsApp, Telegram, or SMS",
                requiredParameters = listOf("person", "platform", "message")
            )
        )
        registerTool(
            ToolDefinition(
                name = "open_settings",
                description = "Open device settings page (wifi, bluetooth, sound, display, battery, etc.)",
                requiredParameters = listOf("setting")
            )
        )
        registerTool(
            ToolDefinition(
                name = "set_volume",
                description = "Set media volume percentage (0-100) or adjust higher/lower",
                requiredParameters = listOf("value"),
                optionalParameters = listOf("mode") // "set", "increase", "decrease", "full", "mute"
            )
        )
        registerTool(
            ToolDefinition(
                name = "set_timer",
                description = "Set a countdown timer in seconds",
                requiredParameters = listOf("seconds"),
                optionalParameters = listOf("label")
            )
        )
        registerTool(
            ToolDefinition(
                name = "set_alarm",
                description = "Set an alarm at specific hour and minute",
                requiredParameters = listOf("hour", "minute"),
                optionalParameters = listOf("label")
            )
        )
        registerTool(
            ToolDefinition(
                name = "open_camera",
                description = "Open the phone camera app",
                requiredParameters = emptyList()
            )
        )
        registerTool(
            ToolDefinition(
                name = "open_browser",
                description = "Open web browser with optional URL",
                requiredParameters = emptyList(),
                optionalParameters = listOf("url")
            )
        )
        registerTool(
            ToolDefinition(
                name = "web_search",
                description = "Search the web via browser",
                requiredParameters = listOf("query")
            )
        )
        registerTool(
            ToolDefinition(
                name = "get_time",
                description = "Get current system time",
                requiredParameters = emptyList()
            )
        )
        registerTool(
            ToolDefinition(
                name = "get_date",
                description = "Get current date and day",
                requiredParameters = emptyList()
            )
        )
        registerTool(
            ToolDefinition(
                name = "custom_command",
                description = "Execute a user-defined custom command routine",
                requiredParameters = listOf("trigger")
            )
        )
    }

    private fun registerTool(tool: ToolDefinition) {
        registry[tool.name.lowercase()] = tool
    }

    fun isToolAllowed(toolName: String): Boolean {
        return registry.containsKey(toolName.lowercase())
    }

    fun getRegisteredTools(): List<ToolDefinition> = registry.values.toList()

    suspend fun executeTool(toolCall: ToolCall, aliases: Map<String, String> = emptyMap()): ActionResult {
        val toolName = toolCall.type.lowercase().replace("-", "_")
        if (!isToolAllowed(toolName)) {
            return ActionResult.Failure("Security rejection: Tool '$toolName' is not in the allowed registry.")
        }

        val params = toolCall.parameters

        return when (toolName) {
            "open_app" -> {
                val app = params["app"] ?: params["appName"] ?: ""
                if (app.isBlank()) ActionResult.Failure("App name is required.")
                else appLauncher.launchApp(app, aliases)
            }
            "search_youtube" -> {
                val query = params["query"] ?: params["song"] ?: params["video"] ?: ""
                youTubeAction.searchOrPlay(query)
            }
            "call_contact" -> {
                val contact = params["contact"] ?: params["name"] ?: ""
                callAction.executeCall(contact)
            }
            "prepare_message" -> {
                val person = params["person"] ?: params["contact"] ?: ""
                val platform = params["platform"] ?: "whatsapp"
                val message = params["message"] ?: params["text"] ?: ""
                messagingAction.prepareMessage(person, platform, message)
            }
            "open_settings" -> {
                val setting = params["setting"] ?: params["name"] ?: "general"
                settingsAction.openSetting(setting)
            }
            "set_volume" -> {
                val mode = params["mode"] ?: "set"
                val valueStr = params["value"] ?: "50"
                when (mode) {
                    "increase", "up" -> volumeAction.increaseVolume()
                    "decrease", "down" -> volumeAction.decreaseVolume()
                    "full", "max" -> volumeAction.setFullVolume()
                    "mute", "zero" -> volumeAction.muteVolume()
                    else -> {
                        val percent = valueStr.filter { it.isDigit() }.toIntOrNull() ?: 50
                        volumeAction.setVolumePercent(percent)
                    }
                }
            }
            "set_timer" -> {
                val seconds = params["seconds"]?.toIntOrNull() ?: 60
                val label = params["label"] ?: "Ultron Timer"
                timerAction.setTimer(seconds, label)
            }
            "set_alarm" -> {
                val hour = params["hour"]?.toIntOrNull() ?: 7
                val minute = params["minute"]?.toIntOrNull() ?: 0
                val label = params["label"] ?: "Ultron Alarm"
                alarmAction.setAlarm(hour, minute, label)
            }
            "open_camera" -> {
                appLauncher.launchApp("camera")
            }
            "open_browser", "web_search" -> {
                val query = params["query"] ?: ""
                val url = if (query.isNotBlank()) "https://www.google.com/search?q=${Uri.encode(query)}" else params["url"] ?: "https://www.google.com"
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult.Success("Boss, browser open kar diya.")
                } catch (e: Exception) {
                    ActionResult.Failure("Browser open nahi ho paya.")
                }
            }
            "get_time" -> {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                ActionResult.Success("Boss, abhi $time ho raha hai.")
            }
            "get_date" -> {
                val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
                ActionResult.Success("Boss, aaj $date hai.")
            }
            "custom_command" -> {
                val trigger = params["trigger"] ?: ""
                executeCustomCommand(trigger, aliases)
            }
            else -> ActionResult.Failure("Tool '$toolName' is not supported.")
        }
    }

    private suspend fun executeCustomCommand(trigger: String, aliases: Map<String, String>): ActionResult {
        val commands = customCommandsRepository.customCommands.first()
        val match = commands.find { it.triggerPhrase.equals(trigger, ignoreCase = true) }
            ?: return ActionResult.Failure("Boss, '$trigger' command nahi mila.")

        for (actionStr in match.actions) {
            val parts = actionStr.split(":", limit = 2)
            val actionKey = parts[0].uppercase()
            val arg = parts.getOrNull(1) ?: ""
            when (actionKey) {
                "OPEN_APP" -> appLauncher.launchApp(arg, aliases)
                "SET_VOLUME" -> volumeAction.setVolumePercent(arg.toIntOrNull() ?: 50)
                "SEARCH_YOUTUBE" -> youTubeAction.searchOrPlay(arg)
            }
        }
        return ActionResult.Success("Boss, '${match.triggerPhrase}' routine poori ho gayi.")
    }
}
