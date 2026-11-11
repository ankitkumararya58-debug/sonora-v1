package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SonoraAccentSilver,
    onPrimary = SonoraBackground,
    primaryContainer = SonoraSurfaceVariant,
    onPrimaryContainer = SonoraTextPrimary,
    secondary = SonoraAccentCyan,
    onSecondary = SonoraBackground,
    background = SonoraBackground,
    onBackground = SonoraTextPrimary,
    surface = SonoraSurface,
    onSurface = SonoraTextPrimary,
    surfaceVariant = SonoraSurfaceVariant,
    onSurfaceVariant = SonoraTextSecondary,
    outline = SonoraBorder,
    outlineVariant = SonoraBorderHighlight
)

@Composable
fun SonoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
