package com.ultron.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.ultron.assistant.data.SavedContact
import com.ultron.assistant.data.SavedSocialAccount
import com.ultron.assistant.data.CustomCommand
import com.ultron.assistant.theme.*
import com.ultron.assistant.viewmodel.UltronViewModel

enum class TerminalTab {
    CONTACTS,
    SOCIAL,
    CUSTOM_COMMANDS,
    APP_ALIASES,
    COMMAND_LOG
}

@Composable
fun TerminalScreen(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(TerminalTab.CONTACTS) }

    val contacts by viewModel.savedContacts.collectAsState()
    val socialAccounts by viewModel.savedSocialAccounts.collectAsState()
    val customCommands by viewModel.savedCustomCommands.collectAsState()
    val logs by viewModel.recentLogs.collectAsState()

    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddSocialDialog by remember { mutableStateOf(false) }
    var showAddCommandDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UltronDarkBg)
    ) {
        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(UltronSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TerminalTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val title = when (tab) {
                    TerminalTab.CONTACTS -> "CONTACTS"
                    TerminalTab.SOCIAL -> "SOCIAL"
                    TerminalTab.CUSTOM_COMMANDS -> "ROUTINES"
                    TerminalTab.APP_ALIASES -> "ALIASES"
                    TerminalTab.COMMAND_LOG -> "LOGS"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) UltronCyanGlow else Color.Transparent)
                        .border(
                            0.5.dp,
                            if (isSelected) UltronCyan else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) UltronCyan else UltronTextSecondary
                    )
                }
            }
        }

        Divider(color = UltronBorder, thickness = 1.dp)

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (selectedTab) {
                TerminalTab.CONTACTS -> ContactsTabContent(
                    contacts = contacts,
                    onAddClick = { showAddContactDialog = true },
                    onDelete = { viewModel.deleteContact(it) }
                )
                TerminalTab.SOCIAL -> SocialTabContent(
                    accounts = socialAccounts,
                    onAddClick = { showAddSocialDialog = true },
                    onDelete = { viewModel.deleteSocialAccount(it) }
                )
                TerminalTab.CUSTOM_COMMANDS -> CustomCommandsTabContent(
                    commands = customCommands,
                    onAddClick = { showAddCommandDialog = true },
                    onDelete = { viewModel.deleteCustomCommand(it) }
                )
                TerminalTab.APP_ALIASES -> AppAliasesTabContent()
                TerminalTab.COMMAND_LOG -> CommandLogTabContent(logs = logs)
            }
        }
    }

    // Dialogs
    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onSave = { name, phone, nick ->
                viewModel.addContact(name, phone, nick)
                showAddContactDialog = false
            }
        )
    }

    if (showAddSocialDialog) {
        AddSocialDialog(
            onDismiss = { showAddSocialDialog = false },
            onSave = { name, platform, id, nick ->
                viewModel.addSocialAccount(name, platform, id, nick)
                showAddSocialDialog = false
            }
        )
    }

    if (showAddCommandDialog) {
        AddCommandDialog(
            onDismiss = { showAddCommandDialog = false },
            onSave = { trigger, desc, actions ->
                viewModel.addCustomCommand(trigger, desc, actions)
                showAddCommandDialog = false
            }
        )
    }
}

@Composable
private fun ContactsTabContent(
    contacts: List<SavedContact>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VOICE CALL CONTACTS (${contacts.size})",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = UltronCyan,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = UltronCyan),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Contact", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts, key = { it.id }) { contact ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = UltronSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(UltronBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(contact.name, fontWeight = FontWeight.Bold, color = UltronTextPrimary)
                            Text("Nickname: \"${contact.nickname}\"", fontSize = 12.sp, color = UltronCyan)
                            Text("Phone: ${contact.phoneNumber}", fontSize = 12.sp, color = UltronTextSecondary)
                        }
                        IconButton(onClick = { onDelete(contact.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = UltronDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialTabContent(
    accounts: List<SavedSocialAccount>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOCIAL MESSAGING ACCOUNTS (${accounts.size})",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = UltronCyan,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = UltronCyan),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Account", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts, key = { it.id }) { acc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = UltronSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(UltronBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(acc.name, fontWeight = FontWeight.Bold, color = UltronTextPrimary)
                            Text("Platform: ${acc.platform}", fontSize = 12.sp, color = UltronSuccess)
                            Text("ID/Phone: ${acc.identifier}", fontSize = 12.sp, color = UltronTextSecondary)
                        }
                        IconButton(onClick = { onDelete(acc.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = UltronDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomCommandsTabContent(
    commands: List<CustomCommand>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CUSTOM ROUTINES (${commands.size})",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = UltronCyan,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = UltronCyan),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Routine", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(commands, key = { it.id }) { cmd ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = UltronSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(UltronBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Trigger: \"${cmd.triggerPhrase}\"", fontWeight = FontWeight.Bold, color = UltronCyan)
                            Text(cmd.description, fontSize = 12.sp, color = UltronTextSecondary)
                            Text("Actions: ${cmd.actions.joinToString(" → ")}", fontSize = 11.sp, color = UltronTextMuted)
                        }
                        IconButton(onClick = { onDelete(cmd.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = UltronDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppAliasesTabContent() {
    val defaultAliases = listOf(
        "YT" to "YouTube",
        "IG" to "Instagram",
        "Snap" to "Snapchat",
        "FB" to "Facebook",
        "Maps" to "Google Maps"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "APP ALIASES MAP",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = UltronCyan,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Spoken aliases resolve directly into Android application package launches.",
            fontSize = 12.sp,
            color = UltronTextSecondary,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(defaultAliases) { (alias, app) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = UltronSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(UltronBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\"$alias\"", fontWeight = FontWeight.Bold, color = UltronCyan, fontFamily = FontFamily.Monospace)
                        Text("➔", color = UltronTextMuted)
                        Text(app, fontWeight = FontWeight.SemiBold, color = UltronTextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandLogTabContent(logs: List<String>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "TERMINAL COMMAND EXECUTION STREAM",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = UltronCyan,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(UltronSurfaceElevated)
                .border(1.dp, UltronBorder, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(logs) { log ->
                    Text(
                        text = "› $log",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (log.startsWith("✓")) UltronSuccess else if (log.startsWith("✗")) UltronDanger else UltronTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, nickname: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Contact to Ultron", color = UltronCyan, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name (e.g. Mummy)") })
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Voice Nickname (e.g. Mummy)") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onSave(name, phone, nickname) },
                colors = ButtonDefaults.buttonColors(containerColor = UltronCyan)
            ) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = UltronTextSecondary) }
        },
        containerColor = UltronSurface
    )
}

@Composable
private fun AddSocialDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, platform: String, identifier: String, nickname: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("WhatsApp") }
    var identifier by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Social Account", color = UltronCyan, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Contact Name (e.g. Tital)") })
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Voice Nickname (e.g. Tital)") })
                OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("Platform (WhatsApp/Telegram/Instagram)") })
                OutlinedTextField(value = identifier, onValueChange = { identifier = it }, label = { Text("Phone / Handle") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && identifier.isNotBlank()) onSave(name, platform, identifier, nickname) },
                colors = ButtonDefaults.buttonColors(containerColor = UltronCyan)
            ) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = UltronTextSecondary) }
        },
        containerColor = UltronSurface
    )
}

@Composable
private fun AddCommandDialog(
    onDismiss: () -> Unit,
    onSave: (trigger: String, desc: String, actions: List<String>) -> Unit
) {
    var trigger by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var action1 by remember { mutableStateOf("OPEN_APP:chrome") }
    var action2 by remember { mutableStateOf("SET_VOLUME:30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Routine", color = UltronCyan, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = trigger, onValueChange = { trigger = it }, label = { Text("Trigger (e.g. office mode)") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                OutlinedTextField(value = action1, onValueChange = { action1 = it }, label = { Text("Action 1 (TYPE:arg)") })
                OutlinedTextField(value = action2, onValueChange = { action2 = it }, label = { Text("Action 2 (TYPE:arg)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trigger.isNotBlank()) {
                        val acts = listOf(action1, action2).filter { it.isNotBlank() }
                        onSave(trigger, desc, acts)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = UltronCyan)
            ) {
                Text("Save Routine", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = UltronTextSecondary) }
        },
        containerColor = UltronSurface
    )
}
