package com.ultron.assistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

class YouTubeAction(private val context: Context) {

    fun searchOrPlay(query: String): ActionResult {
        if (query.isBlank()) {
            return ActionResult.Failure("Boss, kya search karna hai, batayein.")
        }

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val appUri = Uri.parse("vnd.youtube.search:$encoded")
        val webUri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")

        val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            setPackage("com.google.android.youtube")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
                ActionResult.Success("Yes Boss, YouTube par '$query' search kar raha hoon.")
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                ActionResult.Success("Boss, YouTube search web browser mein open kar diya.")
            }
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
            ActionResult.Success("Boss, YouTube search open kar diya.")
        }
    }
}
