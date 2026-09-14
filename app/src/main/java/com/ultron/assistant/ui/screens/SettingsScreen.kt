package com.ultron.assistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.assistant.UltronApplication
import com.ultron.assistant.theme.*
import com.ultron.assistant.viewmodel.UltronViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as UltronApplication
    val prefs = app.preferencesRepository

    val assistantName by viewModel.assistantName.collectAsState()
    val userTitle by viewModel.userTitle.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()
    val aiBaseUrl by viewModel.aiBaseUrl.collectAsState()
    val aiChatUrl by viewModel.aiChatUrl.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val maxContext by viewModel.maxContextMessages.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    var editableBaseUrl by remember { mutableStateOf(aiBaseUrl) }
    var editableChatUrl by remember { mutableStateOf(aiChatUrl) }
    var editableApiKey by remember { mutableStateOf(prefs.getApiKey()) }
    var editableModel by remember { mutableStateOf(aiModel) }
    var editableProvider by remember { mutableStateOf(aiProvider) }
    var editableUserTitle by remember { mutableStateOf(userTitle) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var showClearCurrentDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Keep fields in sync with state
    LaunchedEffect(aiBaseUrl, aiChatUrl, aiModel, aiProvider, userTitle) {
        editableBaseUrl = aiBaseUrl
        editableChatUrl = aiChatUrl
        editableModel = aiModel
        editableProvider = aiProvider
        editableUserTitle = userTitle
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(UltronDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. AI SETTINGS
        item {
            SectionHeader("AI SETTINGS")
            SettingsCard {
                // API Provider
                OutlinedTextField(
                    value = editableProvider,
                    onValueChange = {
                        editableProvider = it
                        scope.launch { prefs.setAiProvider(it) }
                    },
                    label = { Text("API Provider") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Base URL
                OutlinedTextField(
                    value = editableBaseUrl,
                    onValueChange = {
                        editableBaseUrl = it
                        scope.launch { prefs.setAiBaseUrl(it) }
                    },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Chat Completions Endpoint
                OutlinedTextField(
                    value = editableChatUrl,
                    onValueChange = {
                        editableChatUrl = it
                        scope.launch { prefs.setAiChatUrl(it) }
                    },
                    label = { Text("Chat Endpoint") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Model Name
                OutlinedTextField(
                    value = editableModel,
                    onValueChange = {
                        editableModel = it
                        scope.launch { prefs.setAiModel(it) }
                    },
                    label = { Text("Model Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Default: ling-3.0-flash-vl:free (Vision & Language)",
                    fontSize = 11.sp,
                    color = UltronTextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                // API Key (Never logged, stored in EncryptedSharedPreferences)
                OutlinedTextField(
                    value = editableApiKey,
                    onValueChange = {
                        editableApiKey = it
                        prefs.setApiKey(it)
                    },
                    label = { Text("OpenRouter API Key") },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle API Key visibility",
                                tint = UltronTextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Test Connection & Quick Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isTestingConnection = true
                                testResultText = "Connecting to OpenRouter..."
                                val res = viewModel.aiManager.testConnection()
                                isTestingConnection = false
                                testResultText = if (res.isSuccess) res.getOrNull() else "Error: ${res.exceptionOrNull()?.message}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UltronCyan),
                        enabled = !isTestingConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isTestingConnection) "Testing..." else "Test Connection",
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            editableBaseUrl = "https://openrouter.ai/api/v1"
                            editableChatUrl = "https://openrouter.ai/api/v1/chat/completions"
                            editableModel = "ling-3.0-flash-vl:free"
                            scope.launch {
                                prefs.setAiBaseUrl(editableBaseUrl)
                                prefs.setAiChatUrl(editableChatUrl)
                                prefs.setAiModel(editableModel)
                            }
                            Toast.makeText(context, "AI settings restored to default", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Reset Default", color = UltronTextSecondary, fontSize = 12.sp)
                    }
                }

                if (testResultText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = testResultText ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (testResultText?.startsWith("Connection Successful") == true) UltronSuccess else UltronDanger
                    )
                }
            }
        }

        // 2. VOICE SETTINGS
        item {
            SectionHeader("VOICE")
            SettingsCard {
                // Voice Output on/off
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Voice Output", fontWeight = FontWeight.SemiBold, color = UltronTextPrimary)
                        Text("Read AI responses aloud with TextToSpeech", fontSize = 11.sp, color = UltronTextSecondary)
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { viewModel.setAutoSpeak(it) }
                    )
                }

                Divider(color = UltronBorder, modifier = Modifier.padding(vertical = 10.dp))

                // Speech Speed Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Speed", fontSize = 13.sp, color = UltronTextPrimary)
                        Text(
                            text = "${"%.1f".format(speechRate)}x",
                            fontFamily = FontFamily.Monospace,
                            color = UltronCyan,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = UltronCyan,
                            activeTrackColor = UltronCyan
                        )
                    )
                }

                // Stop Speaking button
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { viewModel.stopSpeaking() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeaking) UltronDanger else UltronSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSpeaking) "STOP SPEAKING NOW" else "Stop Speaking (Inactive)",
                        color = if (isSpeaking) Color.White else UltronTextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. APPEARANCE SETTINGS
        item {
            SectionHeader("APPEARANCE")
            SettingsCard {
                Text(
                    text = "Theme Selection",
                    fontWeight = FontWeight.SemiBold,
                    color = UltronTextPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val themes = listOf(
                    "ULTRON_DARK" to "Dark Mode (Cybernetic)",
                    "LIGHT" to "Light Mode",
                    "SYSTEM" to "System Theme"
                )

                for ((mode, label) in themes) {
                    val selected = themeMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { scope.launch { prefs.setThemeMode(mode) } },
                            colors = RadioButtonDefaults.colors(selectedColor = UltronCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (selected) UltronCyan else UltronTextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 4. CHAT SETTINGS
        item {
            SectionHeader("CHAT & CONTEXT")
            SettingsCard {
                // User title address
                OutlinedTextField(
                    value = editableUserTitle,
                    onValueChange = {
                        editableUserTitle = it
                        scope.launch { prefs.setUserTitle(it) }
                    },
                    label = { Text("User Title (e.g. Boss)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Max context setting
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max Context History", fontSize = 13.sp, color = UltronTextPrimary)
                        Text(
                            text = "$maxContext messages",
                            fontFamily = FontFamily.Monospace,
                            color = UltronCyan,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Limits past messages sent to OpenRouter to save tokens and latency.",
                        fontSize = 11.sp,
                        color = UltronTextMuted
                    )
                    Slider(
                        value = maxContext.toFloat(),
                        onValueChange = { scope.launch { prefs.setMaxContextMessages(it.toInt()) } },
                        valueRange = 2f..24f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = UltronCyan,
                            activeTrackColor = UltronCyan
                        )
                    )
                }

                Divider(color = UltronBorder, modifier = Modifier.padding(vertical = 8.dp))

                // Clear current conversation
                Button(
                    onClick = { showClearCurrentDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Clear Current Conversation",
                        color = UltronTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Delete all conversations
                Button(
                    onClick = { showDeleteAllDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronDanger.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Delete All Conversations",
                        color = UltronDanger,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. ABOUT ULTRON
        item {
            SectionHeader("ABOUT")
            SettingsCard {
                Text("ULTRON AI ASSISTANT", fontWeight = FontWeight.Bold, color = UltronCyan, fontSize = 15.sp)
                Text("Version: 1.0.0 (Production Release)", fontSize = 12.sp, color = UltronTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A production-grade native Android AI assistant powered by OpenRouter (default: ling-3.0-flash-vl:free) with local offline command routing, Android SpeechRecognizer, TextToSpeech synthesis, Room SQLite storage, and Android Keystore encryption.",
                    fontSize = 12.sp,
                    color = UltronTextMuted,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Open Source Libraries:\n• AndroidX Jetpack Compose & Material 3\n• OkHttp 4 & Kotlinx Serialization\n• AndroidX Room & DataStore Preferences\n• AndroidX Security Crypto (EncryptedSharedPreferences)\n• Kotlinx Coroutines & StateFlow",
                    fontSize = 11.sp,
                    color = UltronTextMuted,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
    }

    // Clear Current Conversation Confirmation Dialog
    if (showClearCurrentDialog) {
        AlertDialog(
            onDismissRequest = { showClearCurrentDialog = false },
            title = {
                Text(
                    text = "Clear Current Conversation?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = UltronDanger
                )
            },
            text = {
                Text(
                    text = "This will erase all messages in the active chat session.",
                    color = UltronTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCurrentConversation()
                        showClearCurrentDialog = false
                        Toast.makeText(context, "Current chat cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronDanger)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCurrentDialog = false }) {
                    Text("Cancel", color = UltronTextSecondary)
                }
            },
            containerColor = UltronSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Delete All Conversations Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = {
                Text(
                    text = "Delete All Conversations?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = UltronDanger
                )
            },
            text = {
                Text(
                    text = "This will permanently remove every chat conversation and stored transcript.",
                    color = UltronTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllConversations()
                        showDeleteAllDialog = false
                        Toast.makeText(context, "All conversations deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronDanger)
                ) {
                    Text("Delete All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = UltronTextSecondary)
                }
            },
            containerColor = UltronSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = UltronCyan,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = UltronSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(UltronBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content
        )
    }
}
