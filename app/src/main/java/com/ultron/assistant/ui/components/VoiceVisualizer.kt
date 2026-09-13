package com.ultron.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.assistant.theme.*

@Composable
fun VoiceVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    rms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening || isSpeaking) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = 18
                val barWidth = 6.dp.toPx()
                val spacing = 8.dp.toPx()
                val totalWidth = barCount * barWidth + (barCount - 1) * spacing
                val startX = (size.width - totalWidth) / 2f
                val centerY = size.height / 2f

                for (i in 0 until barCount) {
                    val distanceFromCenter = Math.abs(i - barCount / 2f) / (barCount / 2f)
                    val factor = 1f - distanceFromCenter * 0.6f
                    val dynamicHeight = if (isListening) {
                        (20.dp.toPx() + rms * 32.dp.toPx() * factor).coerceIn(8.dp.toPx(), 44.dp.toPx())
                    } else {
                        // Speaking wave
                        (14.dp.toPx() + 24.dp.toPx() * pulseAlpha * factor)
                    }

                    val color = if (isListening) UltronCyan else UltronSuccess

                    drawRoundRect(
                        color = color.copy(alpha = if (isListening) 0.85f else pulseAlpha),
                        topLeft = Offset(startX + i * (barWidth + spacing), centerY - dynamicHeight / 2f),
                        size = Size(barWidth, dynamicHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(UltronCyan.copy(alpha = pulseAlpha))
                )
                Text(
                    text = "ULTRON CORE STANDBY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = UltronTextMuted,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
