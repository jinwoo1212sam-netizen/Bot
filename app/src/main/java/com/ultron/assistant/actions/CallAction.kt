package com.ultron.assistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ultron.assistant.data.ContactsRepository
import com.ultron.assistant.data.PreferencesRepository
import com.ultron.assistant.permissions.PermissionManager
import kotlinx.coroutines.flow.first

class CallAction(
    private val context: Context,
    private val contactAction: ContactAction,
    private val preferencesRepository: PreferencesRepository
) {

    suspend fun executeCall(contactQuery: String, isConfirmed: Boolean = false): ActionResult {
        val contact = contactAction.resolveContact(contactQuery)
            ?: return ActionResult.Failure("Boss, '$contactQuery' naam ka contact nahi mila.")

        val confirmSetting = preferencesRepository.confirmCalls.first()
        if (confirmSetting && !isConfirmed) {
            return ActionResult.RequiresConfirmation("Boss, ${contact.name} ko call karun?") {
                performCall(contact.name, contact.phoneNumber)
            }
        }

        return performCall(contact.name, contact.phoneNumber)
    }

    private fun performCall(displayName: String, phoneNumber: String): ActionResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

        return if (PermissionManager.hasCallPhonePermission(context)) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
                ActionResult.Success("Boss, $displayName ko call kar diya.")
            } catch (e: Exception) {
                fallbackDialer(displayName, cleanNumber)
            }
        } else {
            fallbackDialer(displayName, cleanNumber)
        }
    }

    private fun fallbackDialer(displayName: String, phoneNumber: String): ActionResult {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            ActionResult.Success("Boss, dialer open kar diya $displayName ke number ke sath.")
        } catch (e: Exception) {
            ActionResult.Failure("Call dialer open nahi ho paya: ${e.message}")
        }
    }
}
