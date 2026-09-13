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
data class SavedSocialAccount(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val nickname: String,
    val platform: String, // "WhatsApp", "Instagram", "Facebook", "Snapchat", "Telegram"
    val identifier: String // username or phone
)

class SocialAccountsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("saved_social_accounts")

    val socialAccounts: Flow<List<SavedSocialAccount>> = context.dataStore.data.map { prefs ->
        val raw = prefs[key] ?: getInitialAccountsJson()
        try {
            json.decodeFromString<List<SavedSocialAccount>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveAccount(account: SavedSocialAccount) {
        context.dataStore.edit { prefs ->
            val raw = prefs[key] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<SavedSocialAccount>>(raw).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            val existingIndex = currentList.indexOfFirst {
                it.id == account.id || (it.nickname.equals(account.nickname, ignoreCase = true) && it.platform.equals(account.platform, ignoreCase = true))
            }
            if (existingIndex >= 0) {
                currentList[existingIndex] = account
            } else {
                currentList.add(account)
            }
            prefs[key] = json.encodeToString(currentList)
        }
    }

    suspend fun deleteAccount(id: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[key] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<SavedSocialAccount>>(raw).filter { it.id != id }
            } catch (e: Exception) {
                emptyList()
            }
            prefs[key] = json.encodeToString(currentList)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.remove(key) }
    }

    private fun getInitialAccountsJson(): String {
        val initial = listOf(
            SavedSocialAccount(id = "1", name = "Tital", nickname = "Tital", platform = "WhatsApp", identifier = "+919876543210"),
            SavedSocialAccount(id = "2", name = "Alex", nickname = "Alex", platform = "Telegram", identifier = "alex_dev")
        )
        return json.encodeToString(initial)
    }
}
