package com.ultron.assistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ultron.assistant.data.SavedSocialAccount
import com.ultron.assistant.data.SocialAccountsRepository
import kotlinx.coroutines.flow.first
import java.net.URLEncoder

class MessagingAction(
    private val context: Context,
    private val socialAccountsRepository: SocialAccountsRepository
) {

    suspend fun prepareMessage(
        personQuery: String,
        platformName: String,
        messageText: String
    ): ActionResult {
        val cleanQuery = personQuery.trim().lowercase()
        val accounts = socialAccountsRepository.socialAccounts.first()
        val matchedAccount = accounts.find {
            (it.nickname.equals(cleanQuery, ignoreCase = true) || it.name.equals(cleanQuery, ignoreCase = true)) &&
            it.platform.equals(platformName, ignoreCase = true)
        }

        val platformLower = platformName.trim().lowercase()
        return when (platformLower) {
            "whatsapp" -> sendViaWhatsApp(matchedAccount, messageText, personQuery)
            "telegram" -> sendViaTelegram(matchedAccount, messageText, personQuery)
            "instagram" -> openInstagramDirect(matchedAccount)
            else -> sendGeneralSms(matchedAccount?.identifier ?: personQuery, messageText)
        }
    }

    private fun sendViaWhatsApp(
        account: SavedSocialAccount?,
        message: String,
        targetName: String
    ): ActionResult {
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val identifier = account?.identifier?.replace(Regex("[^0-9+]"), "")

        return try {
            val uri = if (!identifier.isNullOrBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$identifier&text=$encodedMessage")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.Success("Boss, WhatsApp message ready hai. Send button press karna hoga.")
            } else {
                // Fallback to standard share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Send via").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                ActionResult.Success("Boss, WhatsApp installed nahi hai, share chooser open kar diya.")
            }
        } catch (e: Exception) {
            ActionResult.Failure("WhatsApp launch karne mein dikkat aayi: ${e.message}")
        }
    }

    private fun sendViaTelegram(
        account: SavedSocialAccount?,
        message: String,
        targetName: String
    ): ActionResult {
        val encoded = URLEncoder.encode(message, "UTF-8")
        val handle = account?.identifier?.removePrefix("@")
        val uri = if (!handle.isNullOrBlank()) {
            Uri.parse("https://t.me/$handle?text=$encoded")
        } else {
            Uri.parse("tg://msg?text=$encoded")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Boss, Telegram message ready kar diya.")
        } catch (e: Exception) {
            ActionResult.Failure("Telegram open nahi ho saka.")
        }
    }

    private fun openInstagramDirect(account: SavedSocialAccount?): ActionResult {
        val username = account?.identifier ?: ""
        val uri = if (username.isNotBlank()) {
            Uri.parse("https://instagram.com/_u/$username")
        } else {
            Uri.parse("https://instagram.com")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.instagram.android")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Boss, Instagram open kar diya.")
        } catch (e: Exception) {
            ActionResult.Failure("Instagram open karne mein dikkat aayi.")
        }
    }

    private fun sendGeneralSms(phoneNumber: String, message: String): ActionResult {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Boss, SMS compose screen open kar diya. Send press karein.")
        } catch (e: Exception) {
            ActionResult.Failure("SMS app open karne mein error aaya.")
        }
    }
}
