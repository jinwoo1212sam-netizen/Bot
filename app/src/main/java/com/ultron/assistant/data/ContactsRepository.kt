package com.ultron.assistant.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedContact(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val nickname: String,
    val phoneNumber: String
)

class ContactsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("saved_contacts_list")

    val contacts: Flow<List<SavedContact>> = context.dataStore.data.map { prefs ->
        val raw = prefs[key] ?: getInitialContactsJson()
        try {
            json.decodeFromString<List<SavedContact>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveContact(contact: SavedContact) {
        context.dataStore.edit { prefs ->
            val raw = prefs[key] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<SavedContact>>(raw).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            val existingIndex = currentList.indexOfFirst { it.id == contact.id || it.nickname.equals(contact.nickname, ignoreCase = true) }
            if (existingIndex >= 0) {
                currentList[existingIndex] = contact
            } else {
                currentList.add(contact)
            }
            prefs[key] = json.encodeToString(currentList)
        }
    }

    suspend fun deleteContact(id: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[key] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<SavedContact>>(raw).filter { it.id != id }
            } catch (e: Exception) {
                emptyList()
            }
            prefs[key] = json.encodeToString(currentList)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.remove(key) }
    }

    private fun getInitialContactsJson(): String {
        val initial = listOf(
            SavedContact(id = "1", name = "Mummy", nickname = "Mummy", phoneNumber = "9876543210"),
            SavedContact(id = "2", name = "Rahul", nickname = "Rahul", phoneNumber = "9123456780")
        )
        return json.encodeToString(initial)
    }
}
