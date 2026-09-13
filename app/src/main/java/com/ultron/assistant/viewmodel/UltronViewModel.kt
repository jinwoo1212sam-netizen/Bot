package com.ultron.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultron.assistant.UltronApplication
import com.ultron.assistant.actions.*
import com.ultron.assistant.ai.AiChatMessage
import com.ultron.assistant.ai.AiManager
import com.ultron.assistant.command.CommandEngine
import com.ultron.assistant.command.CommandValidator
import com.ultron.assistant.command.ExecutionReport
import com.ultron.assistant.command.LocalCommandParser
import com.ultron.assistant.data.ConversationEntity
import com.ultron.assistant.data.SavedContact
import com.ultron.assistant.data.SavedSocialAccount
import com.ultron.assistant.data.CustomCommand
import com.ultron.assistant.tools.ToolRegistry
import com.ultron.assistant.voice.RecognitionState
import com.ultron.assistant.voice.SpeechRecognizerManager
import com.ultron.assistant.voice.TextToSpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class NavSection {
    AI_CHAT,
    TERMINAL,
    SETTINGS
}

data class UiConfirmationDialog(
    val prompt: String,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit
)

class UltronViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as UltronApplication
    private val prefsRepo = app.preferencesRepository
    private val contactsRepo = app.contactsRepository
    private val socialRepo = app.socialAccountsRepository
    private val customCmdRepo = app.customCommandsRepository
    private val conversationDao = app.database.conversationDao()

    // Actions
    val appLauncher = AppLauncher(application)
    val youTubeAction = YouTubeAction(application)
    val contactAction = ContactAction(application, contactsRepo)
    val callAction = CallAction(application, contactAction, prefsRepo)
    val messagingAction = MessagingAction(application, socialRepo)
    val volumeAction = VolumeAction(application)
    val settingsAction = SettingsAction(application)
    val timerAction = TimerAction(application)
    val alarmAction = AlarmAction(application)

    // Tools & Engine
    val toolRegistry = ToolRegistry(
        application,
        appLauncher,
        youTubeAction,
        callAction,
        messagingAction,
        volumeAction,
        settingsAction,
        timerAction,
        alarmAction,
        customCmdRepo
    )
    private val commandValidator = CommandValidator(toolRegistry)
    private val localParser = LocalCommandParser()
    val commandEngine = CommandEngine(localParser, commandValidator, toolRegistry)
    val aiManager = AiManager(prefsRepo, toolRegistry)

    // Voice
    val ttsManager = TextToSpeechManager(application)
    var speechRecognizerManager: SpeechRecognizerManager? = null

    // StateFlows
    private val _currentSection = MutableStateFlow(NavSection.AI_CHAT)
    val currentSection: StateFlow<NavSection> = _currentSection.asStateFlow()

    private val _confirmationDialog = MutableStateFlow<UiConfirmationDialog?>(null)
    val confirmationDialog: StateFlow<UiConfirmationDialog?> = _confirmationDialog.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<String>>(listOf("System initialized", "Local Engine ready"))
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllMessages()

    val userTitle = prefsRepo.userTitle.stateIn(viewModelScope, SharingStarted.Eagerly, "Boss")
    val assistantName = prefsRepo.assistantName.stateIn(viewModelScope, SharingStarted.Eagerly, "ULTRON")
    val aiEnabled = prefsRepo.aiEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val aiModel = prefsRepo.aiModel.stateIn(viewModelScope, SharingStarted.Eagerly, "ling-3.0-flash-vl:free")
    val aiProvider = prefsRepo.aiProvider.stateIn(viewModelScope, SharingStarted.Eagerly, "OpenRouter")
    val themeMode = prefsRepo.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "ULTRON_DARK")

    val savedContacts = contactsRepo.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val savedSocialAccounts = socialRepo.socialAccounts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val savedCustomCommands = customCmdRepo.customCommands.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isSpeaking = ttsManager.isSpeaking

    private val _speechState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val speechState: StateFlow<RecognitionState> = _speechState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    init {
        speechRecognizerManager = SpeechRecognizerManager(application) { recognizedText ->
            handleUserInput(recognizedText)
        }
        viewModelScope.launch {
            speechRecognizerManager?.state?.collect { _speechState.value = it }
        }
        viewModelScope.launch {
            speechRecognizerManager?.audioRms?.collect { _audioRms.value = it }
        }
    }

    fun setSection(section: NavSection) {
        _currentSection.value = section
    }

    fun toggleSpeechRecognition() {
        if (_speechState.value is RecognitionState.Listening) {
            speechRecognizerManager?.stopListening()
        } else {
            speechRecognizerManager?.startListening()
        }
    }

    fun handleUserInput(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            // Save user message in Room
            conversationDao.insertMessage(
                ConversationEntity(
                    sender = "USER",
                    message = trimmed
                )
            )

            logCommand("User said: \"$trimmed\"")

            val isAiOn = prefsRepo.aiEnabled.first()
            val aliases = parseAliases(prefsRepo.appAliasesJson.first())

            val report: ExecutionReport = if (isAiOn) {
                val recentEntities = conversationDao.getRecentMessages()
                val history = recentEntities.takeLast(4).map {
                    AiChatMessage(
                        role = if (it.sender == "USER") "user" else "assistant",
                        content = it.message
                    )
                }
                aiManager.processUserMessage(trimmed, history, aliases)
            } else {
                commandEngine.executeRawCommand(trimmed, aliases)
            }

            // Save ULTRON response in Room
            val firstAction = report.results.firstOrNull()
            conversationDao.insertMessage(
                ConversationEntity(
                    sender = "ULTRON",
                    message = report.finalSpeech,
                    actionType = firstAction?.title,
                    actionStatus = if (firstAction?.isSuccess == true) "SUCCESS" else "FAILURE"
                )
            )

            // Speak if auto-speak enabled
            if (prefsRepo.autoSpeak.first()) {
                ttsManager.speak(report.finalSpeech)
            }

            // Log results
            for (step in report.results) {
                val symbol = if (step.isSuccess) "✓" else "✗"
                logCommand("$symbol ${step.title}: ${step.message}")
            }
        }
    }

    private fun logCommand(entry: String) {
        val current = _recentLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) current.removeAt(current.lastIndex)
        _recentLogs.value = current
    }

    private fun parseAliases(jsonStr: String): Map<String, String> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<Map<String, String>>(jsonStr)
        } catch (e: Exception) {
            mapOf("yt" to "youtube", "ig" to "instagram", "snap" to "snapchat")
        }
    }

    // Contact actions
    fun addContact(name: String, phone: String, nickname: String) {
        viewModelScope.launch {
            contactsRepo.saveContact(
                SavedContact(name = name, nickname = nickname.ifBlank { name }, phoneNumber = phone)
            )
            logCommand("Added contact: $name ($phone)")
        }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch { contactsRepo.deleteContact(id) }
    }

    // Social actions
    fun addSocialAccount(name: String, platform: String, identifier: String, nickname: String) {
        viewModelScope.launch {
            socialRepo.saveAccount(
                SavedSocialAccount(name = name, nickname = nickname.ifBlank { name }, platform = platform, identifier = identifier)
            )
            logCommand("Added social: $name on $platform ($identifier)")
        }
    }

    fun deleteSocialAccount(id: String) {
        viewModelScope.launch { socialRepo.deleteAccount(id) }
    }

    // Custom Commands
    fun addCustomCommand(trigger: String, desc: String, actions: List<String>) {
        viewModelScope.launch {
            customCmdRepo.saveCommand(
                CustomCommand(triggerPhrase = trigger, description = desc, actions = actions)
            )
            logCommand("Added custom routine: $trigger")
        }
    }

    fun deleteCustomCommand(id: String) {
        viewModelScope.launch { customCmdRepo.deleteCommand(id) }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            conversationDao.clearHistory()
            logCommand("Chat history cleared.")
        }
    }

    fun clearMemory() {
        viewModelScope.launch {
            contactsRepo.clearAll()
            socialRepo.clearAll()
            customCmdRepo.clearAll()
            logCommand("Terminal memory cleared.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.destroy()
        speechRecognizerManager?.stopListening()
    }
}
