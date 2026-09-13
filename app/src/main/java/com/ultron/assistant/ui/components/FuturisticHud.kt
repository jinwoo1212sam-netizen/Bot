package com.ultron.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.assistant.theme.*

@Composable
fun FuturisticHud(
    assistantName: String,
    userTitle: String,
    aiEnabled: Boolean,
    aiModel: String,
    aiProvider: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(UltronSurface)
            .border(width = 1.dp, color = UltronBorder)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Title & Greeting
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isListening) UltronCyan else if (isSpeaking) UltronSuccess else UltronBlue)
                )
                Text(
                    text = assistantName,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = UltronCyan,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "• Boss Mode",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = UltronTextSecondary
                )
            }

            // Right: AI Mode Badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    text = if (aiEnabled) "AI ONLINE" else "LOCAL ONLY",
                    color = if (aiEnabled) UltronSuccess else UltronCyanMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sub HUD details (Model, Provider, Audio State)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (aiEnabled) "[$aiProvider : $aiModel]" else "[Engine: LocalCommandParser v1.0]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = UltronTextMuted
            )

            val stateText = when {
                isListening -> "🎙 LISTENING"
                isSpeaking -> "🔊 SPEAKING"
                else -> "READY"
            }
            Text(
                text = stateText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isListening) UltronCyan else if (isSpeaking) UltronSuccess else UltronTextMuted
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
