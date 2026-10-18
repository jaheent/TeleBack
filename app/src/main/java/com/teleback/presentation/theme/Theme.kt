package com.teleback.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TerminalColorScheme = darkColorScheme(
    primary = TerminalMint,
    onPrimary = TerminalBlack,
    primaryContainer = TerminalSurfaceVariant,
    onPrimaryContainer = TerminalMint,
    secondary = TerminalCyan,
    onSecondary = TerminalBlack,
    secondaryContainer = TerminalSurfaceVariant,
    onSecondaryContainer = TerminalCyan,
    tertiary = TerminalAmber,
    onTertiary = TerminalBlack,
    background = TerminalBlack,
    onBackground = TerminalTextPrimary,
    surface = TerminalDark,
    onSurface = TerminalTextPrimary,
    surfaceVariant = TerminalSurface,
    onSurfaceVariant = TerminalTextSecondary,
    outline = TerminalBorder
)

@Composable
fun TeleBackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = TerminalTypography,
        content = content
    )
}
