package com.ultron.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.assistant.model.Conversation
import com.ultron.assistant.theme.*
import com.ultron.assistant.viewmodel.UltronViewModel

@Composable
fun HistoryScreen(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.allConversations.collectAsState()
    val currentId by viewModel.currentConversationId.collectAsState()

    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var renameDialogText by remember { mutableStateOf("") }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UltronDarkBg)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ARCHIVED SESSIONS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = UltronCyan,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${conversations.size} Conversations Saved Locally",
                    fontSize = 12.sp,
                    color = UltronTextSecondary
                )
            }

            Button(
                onClick = { viewModel.createNewConversation() },
                colors = ButtonDefaults.buttonColors(containerColor = UltronCyan),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New Chat",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Divider(color = UltronBorder, modifier = Modifier.padding(bottom = 12.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = UltronTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No saved conversations",
                        color = UltronTextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    val isCurrent = conv.id == currentId
                    ConversationHistoryCard(
                        conversation = conv,
                        isActive = isCurrent,
                        onSelect = { viewModel.selectConversation(conv) },
                        onRename = {
                            conversationToRename = conv
                            renameDialogText = conv.title
                        },
                        onDelete = { conversationToDelete = conv }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showDeleteAllDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = UltronDanger),
                        border = androidx.compose.foundation.BorderStroke(1.dp, UltronDanger),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DELETE ALL CONVERSATIONS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (conversationToRename != null) {
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = {
                Text(
                    text = "Rename Conversation",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = UltronCyan
                )
            },
            text = {
                OutlinedTextField(
                    value = renameDialogText,
                    onValueChange = { renameDialogText = it },
                    label = { Text("Conversation Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UltronCyan,
                        focusedTextColor = UltronTextPrimary,
                        unfocusedTextColor = UltronTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        conversationToRename?.let {
                            viewModel.renameConversation(it.id, renameDialogText)
                        }
                        conversationToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronCyan)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) {
                    Text("Cancel", color = UltronTextSecondary)
                }
            },
            containerColor = UltronSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Delete Single Conversation Dialog
    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = {
                Text(
                    text = "Delete Conversation?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = UltronDanger
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${conversationToDelete?.title}\"? This action cannot be undone.",
                    color = UltronTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        conversationToDelete?.let { viewModel.deleteConversation(it.id) }
                        conversationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltronDanger)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel", color = UltronTextSecondary)
                }
            },
            containerColor = UltronSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Delete All Conversations Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = {
                Text(
                    text = "Clear Entire History?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = UltronDanger
                )
            },
            text = {
                Text(
                    text = "This will delete all saved conversation transcripts from local memory.",
                    color = UltronTextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllConversations()
                        showDeleteAllDialog = false
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
private fun ConversationHistoryCard(
    conversation: Conversation,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) UltronSurfaceElevated else UltronSurface
        ),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isActive) UltronCyan else UltronBorder
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(UltronCyan)
                        )
                    }
                    Text(
                        text = conversation.title,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (isActive) UltronCyan else UltronTextPrimary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = conversation.formattedDate,
                    fontSize = 11.sp,
                    color = UltronTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRename,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = UltronTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = UltronDanger.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
