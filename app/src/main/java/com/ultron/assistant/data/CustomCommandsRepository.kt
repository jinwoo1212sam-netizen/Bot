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
data class CustomCommand(
    val id: String = System.currentTimeMillis().toString(),
    val triggerPhrase: String,
    val description: String,
    val actions: List<String> // e.g. ["OPEN_APP:chrome", "SET_VOLUME:30", "OPEN_APP:maps"]
)

class CustomCommandsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("saved_custom_commands")

    val customCommands: Flow<List<CustomCommand>> = context.dataStore.data.map { prefs ->
        val raw = prefs[key] ?: getInitialCommandsJson()
        try {
            json.decodeFromString<List<CustomCommand>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveCommand(cmd: CustomCommand) {
        context.dataStore.edit { prefs ->
            val raw = prefs[key] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<CustomCommand>>(raw).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            val existingIndex = currentList.indexOfFirst {
                it.id == cmd.id || it.triggerPhrase.equals(cmd.triggerPhrase, ignoreCase = true)
            }
            if (existingIndex >= 0) {
                currentList[existingIndex] = cmd
            } else {
                currentList.add(cmd)
            }
            prefs[key] = json.encodeToString(currentList)
        }
    }

    suspend fun deleteCommand(id: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[key] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<CustomCommand>>(raw).filter { it.id != id }
            } catch (e: Exception) {
                emptyList()
            }
            prefs[key] = json.encodeToString(currentList)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.remove(key) }
    }

    private fun getInitialCommandsJson(): String {
        val initial = listOf(
            CustomCommand(
                id = "1",
                triggerPhrase = "office mode",
                description = "Launch work tools and set quiet volume",
                actions = listOf("OPEN_APP:chrome", "SET_VOLUME:30")
            ),
            CustomCommand(
                id = "2",
                triggerPhrase = "gaming mode",
                description = "Maximum volume and launch YouTube",
                actions = listOf("SET_VOLUME:90", "OPEN_APP:youtube")
            )
        )
        return json.encodeToString(initial)
    }
}
