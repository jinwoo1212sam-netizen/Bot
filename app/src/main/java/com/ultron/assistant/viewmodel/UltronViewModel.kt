package com.ultron.assistant.viewmodel

import android.app.Application
import android.net.Uri
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
import com.ultron.assistant.model.Conversation
import com.ultron.assistant.model.Message
import com.ultron.assistant.model.MessageSender
import com.ultron.assistant.tools.ToolRegistry
import com.ultron.assistant.voice.RecognitionState
import com.ultron.assistant.voice.SpeechRecognizerManager
import com.ultron.assistant.voice.TextToSpeechManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class NavSection {
    AI_CHAT,
    HISTORY,
    TERMINAL,
    SETTINGS
}

data class UiConfirmationDialog(
    val prompt: String,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit
)

@OptIn(ExperimentalCoroutinesApi::class)
class UltronViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as UltronApplication
    val prefsRepo = app.preferencesRepository
    private val contactsRepo = app.contactsRepository
    private val socialRepo = app.socialAccountsRepository
    private val customCmdRepo = app.customCommandsRepository
    val conversationRepo = app.conversationRepository
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

    // Navigation
    private val _currentSection = MutableStateFlow(NavSection.AI_CHAT)
    val currentSection: StateFlow<NavSection> = _currentSection.asStateFlow()

    // Current Conversation Session
    private val _currentConversationId = MutableStateFlow("default_session")
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    private val _currentConversationTitle = MutableStateFlow("Main Conversation")
    val currentConversationTitle: StateFlow<String> = _currentConversationTitle.asStateFlow()

    // Conversations & Messages
    val allConversations: StateFlow<List<Conversation>> =
        conversationRepo.conversations.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentMessages: StateFlow<List<Message>> = _currentConversationId.flatMapLatest { id ->
        conversationRepo.getMessages(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Legacy conversation flow for backward compatibility
    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllMessages()

    // AI Thinking state
    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // Multimodal Image selection
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    // Speech-to-text recognized draft
    private val _recognizedDraftText = MutableStateFlow("")
    val recognizedDraftText: StateFlow<String> = _recognizedDraftText.asStateFlow()

    // Confirmation dialog
    private val _confirmationDialog = MutableStateFlow<UiConfirmationDialog?>(null)
    val confirmationDialog: StateFlow<UiConfirmationDialog?> = _confirmationDialog.asStateFlow()

    // System logs
    private val _recentLogs = MutableStateFlow<List<String>>(listOf("ULTRON Core Online", "Local Engine ready"))
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    // Preferences Flows
    val userTitle = prefsRepo.userTitle.stateIn(viewModelScope, SharingStarted.Eagerly, "Boss")
    val assistantName = prefsRepo.assistantName.stateIn(viewModelScope, SharingStarted.Eagerly, "ULTRON")
    val aiEnabled = prefsRepo.aiEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val aiModel = prefsRepo.aiModel.stateIn(viewModelScope, SharingStarted.Eagerly, "ling-3.0-flash-vl:free")
    val aiProvider = prefsRepo.aiProvider.stateIn(viewModelScope, SharingStarted.Eagerly, "OpenRouter")
    val aiBaseUrl = prefsRepo.aiBaseUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "https://openrouter.ai/api/v1")
    val aiChatUrl = prefsRepo.aiChatUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "https://openrouter.ai/api/v1/chat/completions")
    val themeMode = prefsRepo.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "ULTRON_DARK")
    val autoSpeak = prefsRepo.autoSpeak.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val speechRate = prefsRepo.speechRate.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val maxContextMessages = prefsRepo.maxContextMessages.stateIn(viewModelScope, SharingStarted.Eagerly, 10)

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
            // Update draft so user can review/edit before sending
            _recognizedDraftText.value = recognizedText
        }
        viewModelScope.launch {
            speechRecognizerManager?.state?.collect { _speechState.value = it }
        }
        viewModelScope.launch {
            speechRecognizerManager?.audioRms?.collect { _audioRms.value = it }
        }
        viewModelScope.launch {
            // Ensure default conversation exists
            conversationRepo.getOrCreateLatestConversation()
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

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            prefsRepo.setSpeechRate(rate)
            ttsManager.setSpeechRate(rate)
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoSpeak(enabled)
        }
    }

    fun attachImage(uri: Uri, base64: String) {
        _selectedImageUri.value = uri
        _selectedImageBase64.value = base64
    }

    fun clearAttachedImage() {
        _selectedImageUri.value = null
        _selectedImageBase64.value = null
    }

    fun clearRecognizedDraft() {
        _recognizedDraftText.value = ""
    }

    fun selectConversation(conversation: Conversation) {
        _currentConversationId.value = conversation.id
        _currentConversationTitle.value = conversation.title
        _currentSection.value = NavSection.AI_CHAT
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val newConv = conversationRepo.createConversation("New Conversation")
            _currentConversationId.value = newConv.id
            _currentConversationTitle.value = newConv.title
            _currentSection.value = NavSection.AI_CHAT
            logCommand("Started new conversation session.")
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            conversationRepo.renameConversation(id, newTitle)
            if (_currentConversationId.value == id) {
                _currentConversationTitle.value = newTitle
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepo.deleteConversation(id)
            if (_currentConversationId.value == id) {
                val remaining = allConversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first())
                } else {
                    createNewConversation()
                }
            }
        }
    }

    fun clearCurrentConversation() {
        viewModelScope.launch {
            conversationRepo.clearConversationMessages(_currentConversationId.value)
            conversationDao.clearHistory()
            logCommand("Cleared current conversation.")
        }
    }

    fun deleteAllConversations() {
        viewModelScope.launch {
            conversationRepo.deleteAllConversations()
            conversationDao.clearHistory()
            createNewConversation()
            logCommand("Deleted all conversations.")
        }
    }

    fun handleUserInput(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val convId = _currentConversationId.value
        val imageBase64 = _selectedImageBase64.value
        val imageUri = _selectedImageUri.value?.toString()

        // Clear input draft and attached image
        clearRecognizedDraft()
        clearAttachedImage()

        viewModelScope.launch {
            // Save user message in repository
            val userMsg = Message(
                conversationId = convId,
                sender = MessageSender.USER,
                content = trimmed,
                imageUri = imageUri,
                imageBase64 = imageBase64
            )
            conversationRepo.addMessage(userMsg)

            // Also mirror to legacy entity
            conversationDao.insertMessage(
                ConversationEntity(
                    sender = "USER",
                    message = trimmed
                )
            )

            logCommand("User: \"$trimmed\"")

            val isAiOn = prefsRepo.aiEnabled.first()
            val aliases = parseAliases(prefsRepo.appAliasesJson.first())

            _isAiThinking.value = true

            if (isAiOn) {
                val maxLimit = prefsRepo.maxContextMessages.first()
                val recentDomainMsgs = conversationRepo.getRecentMessages(convId, maxLimit)
                val history = recentDomainMsgs.dropLast(1).map {
                    AiChatMessage(
                        role = if (it.sender == MessageSender.USER) "user" else "assistant",
                        content = it.content
                    )
                }

                val aiResponse = aiManager.generateAiChatResponse(trimmed, history, imageBase64)
                _isAiThinking.value = false

                val responseContent = aiResponse.responseText
                val assistantMsg = Message(
                    conversationId = convId,
                    sender = MessageSender.ASSISTANT,
                    content = responseContent,
                    actionStatus = if (aiResponse.isSuccess) "SUCCESS" else "ERROR"
                )
                conversationRepo.addMessage(assistantMsg)

                conversationDao.insertMessage(
                    ConversationEntity(
                        sender = "ULTRON",
                        message = responseContent,
                        actionStatus = if (aiResponse.isSuccess) "SUCCESS" else "FAILURE"
                    )
                )

                if (prefsRepo.autoSpeak.first()) {
                    ttsManager.speak(responseContent)
                }

                logCommand("ULTRON: ${responseContent.take(50)}...")
            } else {
                // Offline Local Command Engine
                val report = commandEngine.executeRawCommand(trimmed, aliases)
                _isAiThinking.value = false

                val firstAction = report.results.firstOrNull()
                val assistantMsg = Message(
                    conversationId = convId,
                    sender = MessageSender.ASSISTANT,
                    content = report.finalSpeech,
                    actionType = firstAction?.title,
                    actionStatus = if (firstAction?.isSuccess == true) "SUCCESS" else "FAILURE"
                )
                conversationRepo.addMessage(assistantMsg)

                conversationDao.insertMessage(
                    ConversationEntity(
                        sender = "ULTRON",
                        message = report.finalSpeech,
                        actionType = firstAction?.title,
                        actionStatus = if (firstAction?.isSuccess == true) "SUCCESS" else "FAILURE"
                    )
                )

                if (prefsRepo.autoSpeak.first()) {
                    ttsManager.speak(report.finalSpeech)
                }

                for (step in report.results) {
                    val symbol = if (step.isSuccess) "✓" else "✗"
                    logCommand("$symbol ${step.title}: ${step.message}")
                }
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
        } catch (_: Exception) {
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
