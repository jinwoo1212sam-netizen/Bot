package com.ultron.assistant.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class UltronThemeMode {
    ULTRON_DARK,
    CYBER,
    MINIMAL,
    CLASSIC
}

@Composable
fun UltronTheme(
    themeMode: UltronThemeMode = UltronThemeMode.ULTRON_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        UltronThemeMode.ULTRON_DARK -> darkColorScheme(
            primary = UltronCyan,
            onPrimary = Color.Black,
            primaryContainer = UltronCyanGlow,
            onPrimaryContainer = UltronCyan,
            secondary = UltronBlue,
            onSecondary = Color.White,
            background = UltronDarkBg,
            onBackground = UltronTextPrimary,
            surface = UltronSurface,
            onSurface = UltronTextPrimary,
            surfaceVariant = UltronSurfaceElevated,
            outline = UltronBorder
        )
        UltronThemeMode.CYBER -> darkColorScheme(
            primary = CyberAccent,
            onPrimary = Color.White,
            secondary = UltronCyan,
            background = CyberDarkBg,
            surface = CyberSurface,
            outline = CyberBorder,
            onBackground = Color.White,
            onSurface = Color.White
        )
        UltronThemeMode.MINIMAL -> darkColorScheme(
            primary = MinimalAccent,
            onPrimary = Color.Black,
            secondary = Color(0xFFAAAAAA),
            background = MinimalDarkBg,
            surface = MinimalSurface,
            outline = MinimalBorder,
            onBackground = Color.White,
            onSurface = Color.White
        )
        UltronThemeMode.CLASSIC -> darkColorScheme(
            primary = ClassicAccent,
            onPrimary = Color(0xFF0A192F),
            secondary = Color(0xFF8892B0),
            background = ClassicDarkBg,
            surface = ClassicSurface,
            outline = ClassicBorder,
            onBackground = Color(0xFFCCD6F6),
            onSurface = Color(0xFFCCD6F6)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
