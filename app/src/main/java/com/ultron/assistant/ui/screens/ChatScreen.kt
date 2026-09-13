package com.ultron.assistant.ui.screens

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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.assistant.data.ConversationEntity
import com.ultron.assistant.theme.*
import com.ultron.assistant.ui.components.VoiceVisualizer
import com.ultron.assistant.viewmodel.UltronViewModel
import com.ultron.assistant.voice.RecognitionState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState(initial = emptyList())
    val speechState by viewModel.speechState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val userTitle by viewModel.userTitle.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isListening = speechState is RecognitionState.Listening

    // Auto-scroll on new messages
    LaunchedEffect(conversations.size) {
        if (conversations.isNotEmpty()) {
            listState.animateScrollToItem(conversations.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UltronDarkBg)
    ) {
        // Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (conversations.isEmpty()) {
                item {
                    EmptyChatGreeting(userTitle = userTitle) { sample ->
                        viewModel.handleUserInput(sample)
                    }
                }
            }

            items(conversations, key = { it.id }) { message ->
                MessageBubble(message = message, userTitle = userTitle)
            }
        }

        // Live Audio Waveform Indicator
        VoiceVisualizer(
            isListening = isListening,
            isSpeaking = isSpeaking,
            rms = audioRms,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        // Bottom Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = UltronSurface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mic button
                IconButton(
                    onClick = { viewModel.toggleSpeechRecognition() },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isListening) UltronDanger else UltronCyanGlow)
                        .border(1.dp, if (isListening) UltronDanger else UltronCyan, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = if (isListening) Color.White else UltronCyan
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
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.handleUserInput(textInput)
                                textInput = ""
                            }
                        }
                    )
                )

                // Send button
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.handleUserInput(textInput)
                            textInput = ""
                        }
                    },
                    enabled = textInput.isNotBlank(),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (textInput.isNotBlank()) UltronCyan else UltronBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (textInput.isNotBlank()) Color.Black else UltronTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ConversationEntity, userTitle: String) {
    val isUser = message.sender == "USER"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) UltronCyanGlow else UltronSurfaceElevated
    val borderColor = if (isUser) UltronCyanMuted else UltronBorder

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Text(
            text = if (isUser) userTitle.uppercase() else "ULTRON",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUser) UltronCyan else UltronTextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = message.message,
                    color = UltronTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (!message.actionType.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
private fun EmptyChatGreeting(userTitle: String, onChipClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "⚡ ULTRON ONLINE",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = UltronCyan,
            letterSpacing = 2.sp
        )
        Text(
            text = "\"Good evening, $userTitle. Ready for your command.\"",
            color = UltronTextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "QUICK COMMANDS:",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = UltronTextMuted
        )

        val quickCommands = listOf(
            "YouTube kholo",
            "YouTube par Kesariya search karo",
            "Mummy ko call karo",
            "Volume 50 percent karo",
            "Tital ko WhatsApp par message karo: VC aa",
            "10 minute ka timer lagao"
        )

        for (cmd in quickCommands) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(UltronSurface)
                    .border(0.5.dp, UltronBorder, RoundedCornerShape(8.dp))
                    .clickable { onChipClick(cmd) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "» \"$cmd\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = UltronCyan
                )
            }
        }
    }
}
