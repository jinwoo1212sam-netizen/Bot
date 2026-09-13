package com.ultron.assistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.assistant.UltronApplication
import com.ultron.assistant.permissions.PermissionManager
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
    val aiEnabled by viewModel.aiEnabled.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()

    var editableBaseUrl by remember { mutableStateOf("https://openrouter.ai/api/v1") }
    var editableChatUrl by remember { mutableStateOf("https://openrouter.ai/api/v1/chat/completions") }
    var editableApiKey by remember { mutableStateOf(prefs.getApiKey()) }
    var editableModel by remember { mutableStateOf(aiModel) }
    var editableAssistantName by remember { mutableStateOf(assistantName) }
    var editableUserTitle by remember { mutableStateOf(userTitle) }
    var confirmCalls by remember { mutableStateOf(true) }
    var autoSpeak by remember { mutableStateOf(true) }
    var wakeWordEnabled by remember { mutableStateOf(false) }
    var floatingEnabled by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(UltronDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GENERAL SECTION
        item {
            SectionHeader("GENERAL CONFIGURATION")
            SettingsCard {
                OutlinedTextField(
                    value = editableAssistantName,
                    onValueChange = {
                        editableAssistantName = it
                        scope.launch { prefs.setAssistantName(it) }
                    },
                    label = { Text("Assistant Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editableUserTitle,
                    onValueChange = {
                        editableUserTitle = it
                        scope.launch { prefs.setUserTitle(it) }
                    },
                    label = { Text("User Title / Preferred Address (e.g. Boss)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ASSISTANT MODES
        item {
            SectionHeader("ASSISTANT CONTROLS")
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Confirm Calls", fontWeight = FontWeight.SemiBold, color = UltronTextPrimary)
                        Text("\"Boss, Mummy ko call karun?\" verification prompt", fontSize = 11.sp, color = UltronTextSecondary)
                    }
                    Switch(
                        checked = confirmCalls,
                        onCheckedChange = {
                            confirmCalls = it
                            scope.launch { prefs.setConfirmCalls(it) }
                        }
                    )
                }

                Divider(color = UltronBorder, modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Floating Assistant", fontWeight = FontWeight.SemiBold, color = UltronTextPrimary)
                        Text("Pill overlay over other Android applications", fontSize = 11.sp, color = UltronTextSecondary)
                    }
                    Switch(
                        checked = floatingEnabled,
                        onCheckedChange = {
                            floatingEnabled = it
                            if (it && !PermissionManager.hasOverlayPermission(context)) {
                                context.startActivity(PermissionManager.openOverlaySettingsIntent(context))
                            }
                            scope.launch { prefs.setFloatingAssistantEnabled(it) }
                        }
                    )
                }

                Divider(color = UltronBorder, modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Wake Word (\"Ultron\")", fontWeight = FontWeight.SemiBold, color = UltronTextPrimary)
                        Text("Persistent foreground listening service", fontSize = 11.sp, color = UltronTextSecondary)
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = {
                            wakeWordEnabled = it
                            scope.launch { prefs.setWakeWordEnabled(it) }
                        }
                    )
                }
            }
        }

        // VOICE & TTS
        item {
            SectionHeader("VOICE & SPEECH SYNTHESIS")
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto Speak Responses", fontWeight = FontWeight.SemiBold, color = UltronTextPrimary)
                        Text("Ultron speaks responses via Android TextToSpeech", fontSize = 11.sp, color = UltronTextSecondary)
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = {
                            autoSpeak = it
                            scope.launch { prefs.setAutoSpeak(it) }
                        }
                    )
                }
            }
        }

        // AI PROVIDER (OPENROUTER / OPTIONAL)
        item {
            SectionHeader("AI PROVIDER (OPENROUTER / OPTIONAL)")
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AI Integration", fontWeight = FontWeight.Bold, color = UltronCyan)
                        Text(if (aiEnabled) "AI Enabled (Cloud LLM Active)" else "AI OFF (100% Local Command Engine)", fontSize = 11.sp, color = UltronTextSecondary)
                    }
                    Switch(
                        checked = aiEnabled,
                        onCheckedChange = {
                            scope.launch { prefs.setAiEnabled(it) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editableBaseUrl,
                    onValueChange = {
                        editableBaseUrl = it
                        scope.launch { prefs.setAiBaseUrl(it) }
                    },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editableChatUrl,
                    onValueChange = {
                        editableChatUrl = it
                        scope.launch { prefs.setAiChatUrl(it) }
                    },
                    label = { Text("Chat Completions Endpoint") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editableModel,
                    onValueChange = {
                        editableModel = it
                        scope.launch { prefs.setAiModel(it) }
                    },
                    label = { Text("Model Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editableApiKey,
                    onValueChange = {
                        editableApiKey = it
                        prefs.setApiKey(it)
                    },
                    label = { Text("OpenRouter API Key (Encrypted Keystore)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isTestingConnection = true
                                testResultText = "Testing connection..."
                                val res = viewModel.aiManager.testConnection()
                                isTestingConnection = false
                                testResultText = if (res.isSuccess) res.getOrNull() else "Error: ${res.exceptionOrNull()?.message}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UltronCyan),
                        enabled = !isTestingConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTestingConnection) "Testing..." else "Test Connection", color = Color.Black, fontFamily = FontFamily.Monospace)
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
                            Toast.makeText(context, "AI Config reset to default", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset AI", color = UltronTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }

                if (testResultText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testResultText ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (testResultText?.startsWith("Connection") == true) UltronSuccess else UltronDanger
                    )
                }
            }
        }

        // SECURITY & MEMORY
        item {
            SectionHeader("SECURITY & LOCAL MEMORY")
            SettingsCard {
                Button(
                    onClick = {
                        prefs.clearApiKey()
                        editableApiKey = ""
                        Toast.makeText(context, "API Key cleared from Encrypted Keystore", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Stored API Key", color = UltronDanger, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        viewModel.clearChatHistory()
                        Toast.makeText(context, "Chat History cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Conversation History", color = UltronTextPrimary, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        viewModel.clearMemory()
                        Toast.makeText(context, "Saved Contacts & Routines reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Saved Terminal Memory", color = UltronTextPrimary, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { PermissionManager.openAppSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Android App Permissions", color = UltronCyan, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // ABOUT
        item {
            SectionHeader("ABOUT ULTRON")
            SettingsCard {
                Text("ULTRON Assistant System", fontWeight = FontWeight.Bold, color = UltronCyan)
                Text("Version: 1.0.0-Release", fontSize = 12.sp, color = UltronTextSecondary)
                Text("Architecture: Native Android (Kotlin, Jetpack Compose, Coroutines, StateFlow, Room, DataStore, Android Intents, SpeechRecognizer, TextToSpeech)", fontSize = 11.sp, color = UltronTextMuted)
                Text("Security: Strict Action Allowlist, EncryptedSharedPreferences, Offline-first Local Command Engine", fontSize = 11.sp, color = UltronTextMuted)
            }
        }
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
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = UltronSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(UltronBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content
        )
    }
}
