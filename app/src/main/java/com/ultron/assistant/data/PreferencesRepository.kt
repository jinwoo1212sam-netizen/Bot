package com.ultron.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ultron.assistant.security.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ultron_prefs")

class PreferencesRepository(
    private val context: Context,
    private val secureStorage: SecureStorage
) {

    private object Keys {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val USER_TITLE = stringPreferencesKey("user_title")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val FLOATING_ASSISTANT_ENABLED = booleanPreferencesKey("floating_assistant_enabled")
        val CONFIRM_CALLS = booleanPreferencesKey("confirm_calls")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")

        // AI Settings
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_CHAT_URL = stringPreferencesKey("ai_chat_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val MAX_CONTEXT_MESSAGES = androidx.datastore.preferences.core.intPreferencesKey("max_context_messages")
        val APP_ALIASES_JSON = stringPreferencesKey("app_aliases_json")
    }

    val assistantName: Flow<String> = context.dataStore.data.map {
        it[Keys.ASSISTANT_NAME] ?: "ULTRON"
    }

    val userTitle: Flow<String> = context.dataStore.data.map {
        it[Keys.USER_TITLE] ?: "Boss"
    }

    val themeMode: Flow<String> = context.dataStore.data.map {
        it[Keys.THEME_MODE] ?: "ULTRON_DARK"
    }

    val language: Flow<String> = context.dataStore.data.map {
        it[Keys.LANGUAGE] ?: "hi-IN"
    }

    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.WAKE_WORD_ENABLED] ?: false
    }

    val floatingAssistantEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.FLOATING_ASSISTANT_ENABLED] ?: false
    }

    val confirmCalls: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.CONFIRM_CALLS] ?: true
    }

    val autoSpeak: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.AUTO_SPEAK] ?: true
    }

    val speechRate: Flow<Float> = context.dataStore.data.map {
        it[Keys.SPEECH_RATE] ?: 1.0f
    }

    val speechPitch: Flow<Float> = context.dataStore.data.map {
        it[Keys.SPEECH_PITCH] ?: 0.95f
    }

    val aiEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.AI_ENABLED] ?: false
    }

    val aiProvider: Flow<String> = context.dataStore.data.map {
        it[Keys.AI_PROVIDER] ?: "OpenRouter"
    }

    val aiBaseUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.AI_BASE_URL] ?: "https://openrouter.ai/api/v1"
    }

    val aiChatUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.AI_CHAT_URL] ?: "https://openrouter.ai/api/v1/chat/completions"
    }

    val aiModel: Flow<String> = context.dataStore.data.map {
        it[Keys.AI_MODEL] ?: "ling-3.0-flash-vl:free"
    }

    val maxContextMessages: Flow<Int> = context.dataStore.data.map {
        it[Keys.MAX_CONTEXT_MESSAGES] ?: 10
    }

    val appAliasesJson: Flow<String> = context.dataStore.data.map {
        it[Keys.APP_ALIASES_JSON] ?: "{\"yt\":\"youtube\",\"ig\":\"instagram\",\"snap\":\"snapchat\",\"fb\":\"facebook\"}"
    }

    suspend fun setAssistantName(name: String) = context.dataStore.edit { it[Keys.ASSISTANT_NAME] = name }
    suspend fun setUserTitle(title: String) = context.dataStore.edit { it[Keys.USER_TITLE] = title }
    suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    suspend fun setLanguage(lang: String) = context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    suspend fun setWakeWordEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    suspend fun setFloatingAssistantEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.FLOATING_ASSISTANT_ENABLED] = enabled }
    suspend fun setConfirmCalls(confirm: Boolean) = context.dataStore.edit { it[Keys.CONFIRM_CALLS] = confirm }
    suspend fun setAutoSpeak(auto: Boolean) = context.dataStore.edit { it[Keys.AUTO_SPEAK] = auto }
    suspend fun setSpeechRate(rate: Float) = context.dataStore.edit { it[Keys.SPEECH_RATE] = rate }
    suspend fun setSpeechPitch(pitch: Float) = context.dataStore.edit { it[Keys.SPEECH_PITCH] = pitch }

    suspend fun setAiEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.AI_ENABLED] = enabled }
    suspend fun setAiProvider(provider: String) = context.dataStore.edit { it[Keys.AI_PROVIDER] = provider }
    suspend fun setAiBaseUrl(url: String) = context.dataStore.edit { it[Keys.AI_BASE_URL] = url }
    suspend fun setAiChatUrl(url: String) = context.dataStore.edit { it[Keys.AI_CHAT_URL] = url }
    suspend fun setAiModel(model: String) = context.dataStore.edit { it[Keys.AI_MODEL] = model }
    suspend fun setMaxContextMessages(max: Int) = context.dataStore.edit { it[Keys.MAX_CONTEXT_MESSAGES] = max }
    suspend fun setAppAliasesJson(json: String) = context.dataStore.edit { it[Keys.APP_ALIASES_JSON] = json }

    fun getApiKey(): String = secureStorage.getString(SecureStorage.KEY_API_KEY)
    fun setApiKey(apiKey: String) = secureStorage.saveString(SecureStorage.KEY_API_KEY, apiKey)
    fun clearApiKey() = secureStorage.remove(SecureStorage.KEY_API_KEY)

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
        secureStorage.clearAll()
    }
}
