package com.ultron.assistant.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore

sealed class ActionResult {
    data class Success(val message: String, val extraData: String? = null) : ActionResult()
    data class Failure(val reason: String) : ActionResult()
    data class RequiresConfirmation(val prompt: String, val onConfirmAction: suspend () -> ActionResult) : ActionResult()
}

class AppLauncher(private val context: Context) {

    private val packageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "snapchat" to "com.snapchat.android",
        "chrome" to "com.android.chrome",
        "whatsapp" to "com.whatsapp",
        "telegram" to "org.telegram.messenger",
        "maps" to "com.google.android.apps.maps",
        "calculator" to "com.google.android.calculator",
        "gmail" to "com.google.android.gm"
    )

    fun launchApp(appNameOrAlias: String, aliases: Map<String, String> = emptyMap()): ActionResult {
        val normalized = appNameOrAlias.trim().lowercase()
        val resolvedName = aliases[normalized] ?: normalized

        // Special system handlers
        when (resolvedName) {
            "camera" -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return try {
                    context.startActivity(intent)
                    ActionResult.Success("Boss, camera khol diya.")
                } catch (e: Exception) {
                    ActionResult.Failure("Camera open karne mein dikkat aayi: ${e.message}")
                }
            }
            "gallery", "photos" -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return try {
                    context.startActivity(intent)
                    ActionResult.Success("Boss, gallery khol diya.")
                } catch (e: Exception) {
                    ActionResult.Failure("Gallery open karne mein dikkat aayi.")
                }
            }
        }

        val packageName = packageMap[resolvedName] ?: findPackageByName(resolvedName)

        if (packageName != null) {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                val displayName = resolvedName.replaceFirstChar { it.uppercase() }
                return ActionResult.Success("Yes Boss, $displayName open kar raha hoon.")
            }
        }

        // Web fallback for popular web-enabled services
        val webFallback = when (resolvedName) {
            "youtube" -> "https://www.youtube.com"
            "instagram" -> "https://www.instagram.com"
            "facebook" -> "https://www.facebook.com"
            "chrome" -> "https://www.google.com"
            else -> null
        }

        if (webFallback != null) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            return ActionResult.Success("Boss, app installed nahi hai, isliye browser mein open kar diya.")
        }

        return ActionResult.Failure("Boss, ye app phone mein installed nahi hai.")
    }

    private fun findPackageByName(query: String): String? {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label == query || label.contains(query)) {
                return app.packageName
            }
        }
        return null
    }
}
