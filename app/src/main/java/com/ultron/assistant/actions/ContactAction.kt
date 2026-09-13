package com.ultron.assistant.actions

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.ultron.assistant.data.ContactsRepository
import com.ultron.assistant.data.SavedContact
import com.ultron.assistant.permissions.PermissionManager
import kotlinx.coroutines.flow.first

class ContactAction(
    private val context: Context,
    private val contactsRepository: ContactsRepository
) {

    suspend fun resolveContact(query: String): SavedContact? {
        val clean = query.trim().lowercase()

        // 1. Check local saved terminal contacts first (user-configured)
        val localContacts = contactsRepository.contacts.first()
        val localMatch = localContacts.find {
            it.nickname.equals(clean, ignoreCase = true) ||
            it.name.equals(clean, ignoreCase = true) ||
            it.name.lowercase().contains(clean)
        }
        if (localMatch != null) return localMatch

        // 2. Query Android ContactsContract if permission is granted
        if (PermissionManager.hasContactsPermission(context)) {
            val systemContact = querySystemContacts(clean)
            if (systemContact != null) return systemContact
        }

        return null
    }

    private fun querySystemContacts(searchName: String): SavedContact? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$searchName%")

        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                SavedContact(name = name, nickname = name, phoneNumber = number)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }
}
