package com.ultron.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.assistant.theme.*
import com.ultron.assistant.viewmodel.NavSection

@Composable
fun SidebarNav(
    currentSection: NavSection,
    onSelectSection: (NavSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(UltronSurface)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavButton(
            title = "AI CHAT",
            icon = Icons.Default.ChatBubble,
            isSelected = currentSection == NavSection.AI_CHAT,
            onClick = { onSelectSection(NavSection.AI_CHAT) }
        )
        NavButton(
            title = "TERMINAL",
            icon = Icons.Default.Terminal,
            isSelected = currentSection == NavSection.TERMINAL,
            onClick = { onSelectSection(NavSection.TERMINAL) }
        )
        NavButton(
            title = "SETTINGS",
            icon = Icons.Default.Settings,
            isSelected = currentSection == NavSection.SETTINGS,
            onClick = { onSelectSection(NavSection.SETTINGS) }
        )
    }
}

@Composable
private fun NavButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) UltronCyanGlow else Color.Transparent
    val contentColor = if (isSelected) UltronCyan else UltronTextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = contentColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}
