package com.ultron.assistant.command

import com.ultron.assistant.tools.ToolCall
import java.util.regex.Pattern

class LocalCommandParser {

    fun parse(rawInput: String): ParsedCommand {
        val cleanInput = normalizeInput(rawInput)

        if (cleanInput.isBlank()) {
            return ParsedCommand.Unknown(rawInput)
        }

        // Check for phone on / power on command
        if (isPhoneOnCommand(cleanInput)) {
            return ParsedCommand.SpecialResponse(
                "Boss, phone completely power off hone par normal Android app se power on nahi kiya ja sakta. Agar screen off hai toh main active hoon."
            )
        }

        // Multi-command detection (e.g. "YouTube kholo, Kesariya search karo aur volume 50 percent karo")
        val subPhrases = splitMultiCommands(cleanInput)
        if (subPhrases.size > 1) {
            val parsedList = mutableListOf<ParsedCommand.Single>()
            for (sub in subPhrases) {
                val parsedSingle = parseSingle(sub)
                if (parsedSingle is ParsedCommand.Single) {
                    parsedList.add(parsedSingle)
                }
            }
            if (parsedList.isNotEmpty()) {
                return ParsedCommand.Multi(parsedList, rawInput)
            }
        }

        return parseSingle(cleanInput)
    }

    private fun parseSingle(input: String): ParsedCommand {
        val text = normalizeInput(input)

        // 1. YouTube Search / Play (Variable arbitrary song/query extraction)
        // e.g. "youtube par kesariya search karo", "youtube par arijit singh ka song search karo", "kesariya chalao", "believer chalao"
        val ytPattern1 = Pattern.compile("(?:youtube\\s+(?:par|pe|me|in)\\s+)?(.+?)\\s+(?:search\\s+karo|chalao|play\\s+karo|play|search)", Pattern.CASE_INSENSITIVE)
        val m1 = ytPattern1.matcher(text)
        if (m1.find()) {
            var query = m1.group(1)?.trim() ?: ""
            // Cleanup query
            query = query.removePrefix("youtube par ").removePrefix("youtube pe ").removePrefix("youtube me ").trim()
            if (query.isNotBlank() && !isGeneralKeyword(query)) {
                return ParsedCommand.Single(
                    ToolCall("search_youtube", mapOf("query" to query)),
                    text
                )
            }
        }

        if (text.startsWith("play ") || text.startsWith("search ")) {
            val query = text.substringAfter(" ").removePrefix("youtube ").trim()
            if (query.isNotBlank()) {
                return ParsedCommand.Single(
                    ToolCall("search_youtube", mapOf("query" to query)),
                    text
                )
            }
        }

        if (text.endsWith(" chalao") || text.endsWith(" bajao")) {
            val query = text.removeSuffix(" chalao").removeSuffix(" bajao").removePrefix("youtube par ").trim()
            if (query.isNotBlank()) {
                return ParsedCommand.Single(
                    ToolCall("search_youtube", mapOf("query" to query)),
                    text
                )
            }
        }

        // 2. Messaging Flow (e.g. "Tital ko WhatsApp par message karo: VC aa" or "WhatsApp par message karo Tital ko: VC aa")
        val msgPattern = Pattern.compile("(.+?)\\s+ko\\s+(whatsapp|telegram|instagram|sms)\\s+(?:par|pe)?\\s*message\\s+karo\\s*[:\\-]?\\s*(.*)", Pattern.CASE_INSENSITIVE)
        val msgMatcher = msgPattern.matcher(text)
        if (msgMatcher.find()) {
            val person = msgMatcher.group(1)?.trim() ?: ""
            val platform = msgMatcher.group(2)?.trim() ?: "whatsapp"
            val msg = msgMatcher.group(3)?.trim() ?: ""
            return ParsedCommand.Single(
                ToolCall("prepare_message", mapOf("person" to person, "platform" to platform, "message" to msg)),
                text
            )
        }

        // 3. Calling Flow (e.g. "mummy ko call karo", "rahul ko call karo", "meri mummy ko call kar", "call rahul")
        val callPattern = Pattern.compile("(?:meri\\s+)?(.+?)\\s+ko\\s+(?:call\\s+(?:karo|kar|kariye)|phone\\s+(?:lagao|karo))", Pattern.CASE_INSENSITIVE)
        val callMatcher = callPattern.matcher(text)
        if (callMatcher.find()) {
            val contact = callMatcher.group(1)?.trim() ?: ""
            return ParsedCommand.Single(
                ToolCall("call_contact", mapOf("contact" to contact)),
                text
            )
        }

        if (text.startsWith("call ") || text.startsWith("phone ")) {
            val contact = text.substringAfter(" ").removeSuffix(" ko").trim()
            if (contact.isNotBlank()) {
                return ParsedCommand.Single(
                    ToolCall("call_contact", mapOf("contact" to contact)),
                    text
                )
            }
        }

        // 4. Volume Control
        // "volume 50 percent karo", "volume 50% karo", "media volume 50 karo"
        val volPercentPattern = Pattern.compile("(?:media\\s+)?volume\\s+(\\d+)\\s*(?:percent|%|pratishat)?\\s*(?:karo|set\\s+karo)?", Pattern.CASE_INSENSITIVE)
        val volMatcher = volPercentPattern.matcher(text)
        if (volMatcher.find()) {
            val percent = volMatcher.group(1) ?: "50"
            return ParsedCommand.Single(
                ToolCall("set_volume", mapOf("mode" to "set", "value" to percent)),
                text
            )
        }

        if (text.contains("volume full") || text.contains("volume max") || text.contains("full volume")) {
            return ParsedCommand.Single(
                ToolCall("set_volume", mapOf("mode" to "full", "value" to "100")),
                text
            )
        }

        if (text.contains("volume badhao") || text.contains("volume up") || text.contains("awaz badhao")) {
            return ParsedCommand.Single(
                ToolCall("set_volume", mapOf("mode" to "increase", "value" to "10")),
                text
            )
        }

        if (text.contains("volume kam karo") || text.contains("volume down") || text.contains("awaz kam karo")) {
            return ParsedCommand.Single(
                ToolCall("set_volume", mapOf("mode" to "decrease", "value" to "10")),
                text
            )
        }

        if (text.contains("volume mute") || text.contains("mute karo") || text.contains("awaz band karo")) {
            return ParsedCommand.Single(
                ToolCall("set_volume", mapOf("mode" to "mute", "value" to "0")),
                text
            )
        }

        // 5. Timer Control (e.g. "10 minute ka timer lagao", "5 minute timer", "30 second ka timer")
        val timerPattern = Pattern.compile("(\\d+)\\s*(minute|second|min|sec)\\s*(?:ka)?\\s*timer\\s*(?:lagao|set\\s*karo)?", Pattern.CASE_INSENSITIVE)
        val timerMatcher = timerPattern.matcher(text)
        if (timerMatcher.find()) {
            val num = timerMatcher.group(1)?.toIntOrNull() ?: 1
            val unit = timerMatcher.group(2)?.lowercase() ?: "minute"
            val totalSeconds = if (unit.startsWith("min")) num * 60 else num
            return ParsedCommand.Single(
                ToolCall("set_timer", mapOf("seconds" to totalSeconds.toString())),
                text
            )
        }

        // 6. Alarm Control (e.g. "7 baje alarm lagao", "subah 6 baje alarm set karo")
        val alarmPattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(?:baje)?\\s*alarm\\s*(?:lagao|set\\s*karo)?", Pattern.CASE_INSENSITIVE)
        val alarmMatcher = alarmPattern.matcher(text)
        if (alarmMatcher.find()) {
            var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 7
            val minute = alarmMatcher.group(2)?.toIntOrNull() ?: 0
            if (text.contains("shaam") || text.contains("raat") || text.contains("pm")) {
                if (hour < 12) hour += 12
            }
            return ParsedCommand.Single(
                ToolCall("set_alarm", mapOf("hour" to hour.toString(), "minute" to minute.toString())),
                text
            )
        }

        // 7. System Settings
        if (text.contains("settings") || text.contains("setting")) {
            val settingType = when {
                text.contains("wifi") || text.contains("wi-fi") -> "wifi"
                text.contains("bluetooth") -> "bluetooth"
                text.contains("display") || text.contains("brightness") -> "display"
                text.contains("sound") || text.contains("audio") -> "sound"
                text.contains("battery") -> "battery"
                text.contains("notification") -> "notification"
                text.contains("accessibility") -> "accessibility"
                text.contains("date") || text.contains("time") -> "date"
                text.contains("app") -> "apps"
                else -> "general"
            }
            return ParsedCommand.Single(
                ToolCall("open_settings", mapOf("setting" to settingType)),
                text
            )
        }

        // 8. Time & Date
        if (text.contains("time kya") || text.contains("kya time") || text.contains("time batao") || text.contains("current time")) {
            return ParsedCommand.Single(
                ToolCall("get_time", emptyMap()),
                text
            )
        }

        if (text.contains("date kya") || text.contains("aaj ki date") || text.contains("date batao") || text.contains("today's date")) {
            return ParsedCommand.Single(
                ToolCall("get_date", emptyMap()),
                text
            )
        }

        // 9. Camera / Photo
        if (text.contains("camera kholo") || text.contains("open camera") || text.contains("photo khicho") || text.contains("camera open karo")) {
            return ParsedCommand.Single(
                ToolCall("open_camera", emptyMap()),
                text
            )
        }

        // 10. App Opening (e.g. "youtube kholo", "instagram kholo", "chrome kholo", "whatsapp kholo", "open snapchat")
        val appPattern = Pattern.compile("(?:open\\s+|launch\\s+)?([a-zA-Z0-9_-]+)\\s+(?:kholo|open\\s*karo|chalu\\s*karo|start\\s*karo|chalao)", Pattern.CASE_INSENSITIVE)
        val appMatcher = appPattern.matcher(text)
        if (appMatcher.find()) {
            val app = appMatcher.group(1)?.trim() ?: ""
            if (app.isNotBlank() && !isGeneralKeyword(app)) {
                return ParsedCommand.Single(
                    ToolCall("open_app", mapOf("app" to app)),
                    text
                )
            }
        }

        if (text.startsWith("open ") || text.startsWith("launch ")) {
            val app = text.substringAfter(" ").trim()
            if (app.isNotBlank()) {
                return ParsedCommand.Single(
                    ToolCall("open_app", mapOf("app" to app)),
                    text
                )
            }
        }

        // 11. Custom Command / Mode (e.g. "office mode", "gaming mode")
        if (text.endsWith(" mode") || text.contains("office mode") || text.contains("gaming mode")) {
            return ParsedCommand.Single(
                ToolCall("custom_command", mapOf("trigger" to text)),
                text
            )
        }

        return ParsedCommand.Unknown(text)
    }

    private fun normalizeInput(raw: String): String {
        var clean = raw.lowercase().trim()
        // Strip common wake words and polite fillers
        clean = clean.removePrefix("ultron,").removePrefix("ultron ")
            .removePrefix("hey ultron,").removePrefix("hey ultron ")
            .removePrefix("ok ultron,").removePrefix("ok ultron ")
            .removePrefix("plz ").removePrefix("please ")
            .removePrefix("bhai ").removePrefix("jarvis ")
            .trim()
        return clean
    }

    private fun splitMultiCommands(input: String): List<String> {
        val parts = input.split(Regex("[,;]|\\baur\\b|\\band\\b|\\bthen\\b|\\bphir\\b"))
        return parts.map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun isPhoneOnCommand(input: String): Boolean {
        return input.contains("phone on kar") ||
                input.contains("phone start kar") ||
                input.contains("phone switch on") ||
                input.contains("turn on phone") ||
                input.contains("power on")
    }

    private fun isGeneralKeyword(word: String): Boolean {
        return word in listOf("par", "pe", "me", "ko", "se", "aur", "and", "karo", "khol", "kholo")
    }
}
