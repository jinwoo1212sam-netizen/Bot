package com.ultron.assistant.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ultron.assistant.model.Message
import com.ultron.assistant.model.MessageSender
import com.ultron.assistant.theme.*
import com.ultron.assistant.ui.components.MarkdownText
import com.ultron.assistant.ui.components.VoiceVisualizer
import com.ultron.assistant.viewmodel.UltronViewModel
import com.ultron.assistant.voice.RecognitionState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentTitle by viewModel.currentConversationTitle.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val userTitle by viewModel.userTitle.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val recognizedDraft by viewModel.recognizedDraftText.collectAsState()
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val aiEnabled by viewModel.aiEnabled.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showVoiceControls by remember { mutableStateOf(false) }
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val isListening = speechState is RecognitionState.Listening

    // When voice recognizer outputs text, fill text field for review/editing before sending
    LaunchedEffect(recognizedDraft) {
        if (recognizedDraft.isNotBlank()) {
            textInput = recognizedDraft
        }
    }

    // Auto-scroll to bottom on new messages or thinking state
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker for multimodal input
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    viewModel.attachImage(it, base64Str)
                    Toast.makeText(context, "Image attached for analysis", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to read image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Mic permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleSpeechRecognition()
        } else {
            showMicPermissionDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UltronDarkBg)
    ) {
        // Futuristic Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = UltronSurface,
            tonalElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title and status
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ULTRON",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = UltronCyan,
                                letterSpacing = 2.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (aiEnabled) UltronCyan.copy(alpha = 0.15f) else UltronBorder)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (aiEnabled) "AI ONLINE" else "OFFLINE LOCAL",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (aiEnabled) UltronCyan else UltronTextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = currentTitle,
                            fontSize = 11.sp,
                            color = UltronTextSecondary,
                            maxLines = 1
                        )
                    }

                    // Top Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Stop speaking button (visible when TTS is actively speaking)
                        if (isSpeaking) {
                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(UltronDanger.copy(alpha = 0.2f))
                                    .border(1.dp, UltronDanger, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Speaking",
                                    tint = UltronDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Voice settings toggle
                        IconButton(
                            onClick = { showVoiceControls = !showVoiceControls },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (autoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Voice Controls",
                                tint = if (showVoiceControls) UltronCyan else UltronTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Clear conversation
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = UltronTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // New Chat
                        IconButton(
                            onClick = { viewModel.createNewConversation() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "New Chat",
                                tint = UltronCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Collapsible Voice Control Panel
                AnimatedVisibility(visible = showVoiceControls) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(UltronSurfaceElevated)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Voice Output (TTS)",
                                fontSize = 12.sp,
                                color = UltronTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Switch(
                                checked = autoSpeak,
                                onCheckedChange = { viewModel.setAutoSpeak(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Speed: ${"%.1f".format(speechRate)}x",
                                fontSize = 11.sp,
                                color = UltronTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(76.dp)
                            )
                            Slider(
                                value = speechRate,
                                onValueChange = { viewModel.setSpeechRate(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                colors = SliderDefaults.colors(
                                    thumbColor = UltronCyan,
                                    activeTrackColor = UltronCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSpeaking) {
                                TextButton(onClick = { viewModel.stopSpeaking() }) {
                                    Text("STOP", color = UltronDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Conversation Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && !isThinking) {
                item {
                    EmptyChatGreeting(
                        userTitle = userTitle,
                        modelName = aiModel,
                        onChipClick = { sample ->
                            viewModel.handleUserInput(sample)
                        }
                    )
                }
            }

            items(messages, key = { it.id }) { message ->
                MessageItemBubble(message = message, userTitle = userTitle)
            }

            // Thinking Indicator
            if (isThinking) {
                item {
                    ThinkingIndicatorBubble(modelName = aiModel)
                }
            }
        }

        // Live Audio Waveform Indicator (when speaking or listening)
        VoiceVisualizer(
            isListening = isListening,
            isSpeaking = isSpeaking,
            rms = audioRms,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        // Image Attachment Preview Pill (above input bar)
        if (selectedImageUri != null) {
            Surface(
                color = UltronSurfaceElevated,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = UltronCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Photo attached for Vision analysis",
                            fontSize = 12.sp,
                            color = UltronTextPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearAttachedImage() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = UltronDanger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Bottom Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = UltronSurface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Photo Picker Button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (selectedImageUri != null) UltronCyan.copy(alpha = 0.2f) else UltronSurfaceElevated)
                        .border(1.dp, if (selectedImageUri != null) UltronCyan else UltronBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Photo",
                        tint = if (selectedImageUri != null) UltronCyan else UltronTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Microphone button
                IconButton(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.toggleSpeechRecognition()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isListening) UltronDanger else UltronCyanGlow)
                        .border(1.dp, if (isListening) UltronDanger else UltronCyan, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = if (isListening) Color.White else UltronCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening for Boss..." else "Message or command...",
                            fontSize = 13.sp,
                            color = UltronTextMuted
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = UltronSurfaceElevated,
                        unfocusedContainerColor = UltronSurfaceElevated,
                        focusedBorderColor = UltronCyan,
                        unfocusedBorderColor = UltronBorder,
                        focusedTextColor = UltronTextPrimary,
                        unfocusedTextColor = UltronTextPrimary
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank() || selectedImageUri != null) {
                                viewModel.handleUserInput(textInput)
                                textInput = ""
                            }
                        }
                    )
                )

                // Send button
                val canSend = textInput.isNotBlank() || selectedImageUri != null
                IconButton(
                    onClick = {
                        if (canSend) {
                            viewModel.handleUserInput(textInput)
                            textInput = ""
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (canSend) UltronCyan else UltronBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.Black else UltronTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Microphone Permission Explanation Dialog
    if (showMicPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMicPermissionDialog = false },
            title = {
                Text(
                    text = "Microphone Access Required",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = UltronCyan
                )
            },
            text = {
                Text(
                    text = "ULTRON uses your microphone strictly for speech-to-text voice recognition when you tap the mic button. Your audio is processed via Android SpeechRecognizer and never saved.",
                    color = UltronTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMicPermissionDialog = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronCyan)
                ) {
                    Text("Grant Permission", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermissionDialog = false }) {
                    Text("Not Now", color = UltronTextSecondary)
                }
            },
            containerColor = UltronSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Clear Current Conversation Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Current Chat?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = UltronDanger
                )
            },
            text = {
                Text(
                    text = "This will remove all messages from the current conversation session.",
                    color = UltronTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCurrentConversation()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronDanger)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = UltronTextSecondary)
                }
            },
            containerColor = UltronSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun MessageItemBubble(message: Message, userTitle: String) {
    val isUser = message.sender == MessageSender.USER
    val align = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) UltronCyanGlow else UltronSurfaceElevated
    val borderColor = if (isUser) UltronCyanMuted else UltronBorder

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        // Sender name & Timestamp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isUser) userTitle.uppercase() else "ULTRON",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) UltronCyan else UltronTextSecondary
            )
            Text(
                text = "· ${message.formattedTime}",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = UltronTextMuted
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // If user sent an image
                if (message.imageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, UltronBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Attached Photo",
                            tint = UltronCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Photo Attachment",
                            fontSize = 11.sp,
                            color = UltronTextSecondary,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                        )
                    }
                }

                // Message Text with Markdown formatting support
                if (isUser) {
                    Text(
                        text = message.content,
                        color = UltronTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    MarkdownText(
                        markdown = message.content,
                        textColor = UltronTextPrimary
                    )
                }

                // Action tag if applicable
                if (!message.actionType.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (message.actionStatus == "SUCCESS") "✓ ${message.actionType}" else "✗ ${message.actionType}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (message.actionStatus == "SUCCESS") UltronSuccess else UltronDanger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicatorBubble(modelName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "ULTRON · PROCESSING",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = UltronCyan,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(UltronSurfaceElevated)
                .border(1.dp, UltronBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = UltronCyan
                )
                Text(
                    text = "Ultron is thinking ($modelName)...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = UltronTextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyChatGreeting(
    userTitle: String,
    modelName: String,
    onChipClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(UltronCyanGlow)
                .border(1.5.dp, UltronCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = UltronCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "ULTRON ASSISTANT",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = UltronCyan,
            letterSpacing = 2.sp
        )

        Text(
            text = "\"Online and standing by, $userTitle.\"",
            color = UltronTextSecondary,
            fontSize = 13.sp
        )

        Text(
            text = "AI Model: $modelName",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = UltronTextMuted
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "TRY ASKING:",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = UltronTextMuted
        )

        val prompts = listOf(
            "Explain quantum computing with code examples",
            "Write a Kotlin Coroutines flow example with explanation",
            "YouTube kholo aur Kesariya gaana chalao",
            "Set a 10 minute focus timer",
            "Volume 70 percent karo"
        )

        for (p in prompts) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(UltronSurface)
                    .border(0.5.dp, UltronBorder, RoundedCornerShape(8.dp))
                    .clickable { onChipClick(p) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "» $p",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = UltronCyan
                )
            }
        }
    }
}
