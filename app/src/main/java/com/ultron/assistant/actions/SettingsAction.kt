package com.ultron.assistant.actions

import android.content.Context
import android.content.Intent
import android.provider.Settings

class SettingsAction(private val context: Context) {

    fun openSetting(settingName: String): ActionResult {
        val clean = settingName.trim().lowercase()

        val intentAction = when {
            clean.contains("wifi") || clean.contains("wi-fi") -> Settings.ACTION_WIFI_SETTINGS
            clean.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
            clean.contains("display") || clean.contains("brightness") -> Settings.ACTION_DISPLAY_SETTINGS
            clean.contains("sound") || clean.contains("audio") -> Settings.ACTION_SOUND_SETTINGS
            clean.contains("battery") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            clean.contains("notification") -> Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
            clean.contains("accessibility") -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            clean.contains("date") || clean.contains("time") -> Settings.ACTION_DATE_SETTINGS
            clean.contains("apps") || clean.contains("app") -> Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(intentAction).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult.Success("Boss, ${settingName.replaceFirstChar { it.uppercase() }} settings open kar diya.")
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
            ActionResult.Success("Boss, Settings screen open kar diya.")
        }
    }
}
