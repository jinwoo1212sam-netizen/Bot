package com.ultron.assistant

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ultron.assistant.permissions.PermissionManager
import com.ultron.assistant.service.FloatingAssistantService
import com.ultron.assistant.service.UltronForegroundService
import com.ultron.assistant.theme.UltronAssistantTheme
import com.ultron.assistant.theme.UltronDarkBg
import com.ultron.assistant.ui.components.FuturisticHud
import com.ultron.assistant.ui.components.SidebarNav
import com.ultron.assistant.ui.screens.ChatScreen
import com.ultron.assistant.ui.screens.SettingsScreen
import com.ultron.assistant.ui.screens.TerminalScreen
import com.ultron.assistant.viewmodel.NavSection
import com.ultron.assistant.viewmodel.UltronViewModel
import com.ultron.assistant.voice.RecognitionState

class MainActivity : ComponentActivity() {

    private val viewModel: UltronViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted status
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request standard runtime permissions
        requestRequiredPermissions()

        setContent {
            val currentSection by viewModel.currentSection.collectAsState()
            val speechState by viewModel.speechState.collectAsState()
            val isSpeaking by viewModel.isSpeaking.collectAsState()
            val assistantName by viewModel.assistantName.collectAsState()
            val userTitle by viewModel.userTitle.collectAsState()
            val aiEnabled by viewModel.aiEnabled.collectAsState()
            val aiModel by viewModel.aiModel.collectAsState()
            val aiProvider by viewModel.aiProvider.collectAsState()
            val confirmationDialog by viewModel.confirmationDialog.collectAsState()

            val isListening = speechState is RecognitionState.Listening

            UltronAssistantTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = UltronDarkBg,
                    topBar = {
                        FuturisticHud(
                            assistantName = assistantName,
                            userTitle = userTitle,
                            aiEnabled = aiEnabled,
                            aiModel = aiModel,
                            aiProvider = aiProvider,
                            isListening = isListening,
                            isSpeaking = isSpeaking
                        )
                    },
                    bottomBar = {
                        SidebarNav(
                            currentSection = currentSection,
                            onSelectSection = { viewModel.setSection(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentSection) {
                            NavSection.AI_CHAT -> ChatScreen(viewModel = viewModel)
                            NavSection.TERMINAL -> TerminalScreen(viewModel = viewModel)
                            NavSection.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }

                        // Confirmation Dialog for Calls / Destructive Actions
                        confirmationDialog?.let { dialog ->
                            AlertDialog(
                                onDismissRequest = { dialog.onDismiss() },
                                title = { Text("ULTRON Verification", color = Color.White) },
                                text = { Text(dialog.prompt, color = Color.LightGray) },
                                confirmButton = {
                                    Button(onClick = { dialog.onConfirm() }) {
                                        Text("Yes, Proceed")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { dialog.onDismiss() }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
