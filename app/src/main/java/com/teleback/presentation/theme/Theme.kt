package com.teleback.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class TeleBackThemeId(val id: String, val displayName: String, val isDark: Boolean) {
    MATRIX_MINT("matrix_mint", "Matrix Mint (Dark)", true),
    OLED_AMBER("oled_amber", "OLED Amber (Dark)", true),
    DRACULA_NEON("dracula_neon", "Dracula Neon (Dark)", true),
    SOLARIZED_PAPER("solarized_paper", "Solarized Paper (Light)", false),
    NORD_FROST("nord_frost", "Nord Frost (Light)", false),
    MINIMAL_SLATE("minimal_slate", "Minimal Slate (Light)", false);

    companion object {
        fun fromId(id: String?): TeleBackThemeId = entries.firstOrNull { it.id == id } ?: MATRIX_MINT
    }
}

// 1. Matrix Mint (Default Dark)
private val MatrixMintScheme = darkColorScheme(
    primary = Color(0xFF00FFA3),
    onPrimary = Color(0xFF070A0F),
    secondary = Color(0xFF00D2FF),
    onSecondary = Color(0xFF070A0F),
    tertiary = Color(0xFFFFB800),
    onTertiary = Color(0xFF070A0F),
    background = Color(0xFF070A0F),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF0D1117),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF161B22),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D)
)

// 2. OLED Amber (Dark)
private val OledAmberScheme = darkColorScheme(
    primary = Color(0xFFFFB800),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFFF7A00),
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFFFFD54F),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFF8E7),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFFF8E7),
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFFA89A80),
    outline = Color(0xFF333333)
)

// 3. Dracula Neon (Dark)
private val DraculaNeonScheme = darkColorScheme(
    primary = Color(0xFF50FA7B),
    onPrimary = Color(0xFF181824),
    secondary = Color(0xFF8BE9FD),
    onSecondary = Color(0xFF181824),
    tertiary = Color(0xFFFF79C6),
    onTertiary = Color(0xFF181824),
    background = Color(0xFF181824),
    onBackground = Color(0xFFF8F8F2),
    surface = Color(0xFF21222C),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF282A36),
    onSurfaceVariant = Color(0xFF6272A4),
    outline = Color(0xFF44475A)
)

// 4. Solarized Paper (Light)
private val SolarizedPaperScheme = lightColorScheme(
    primary = Color(0xFF2AA198),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF268BD2),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFB58900),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFDF6E3),
    onBackground = Color(0xFF073642),
    surface = Color(0xFFF7F0D8),
    onSurface = Color(0xFF073642),
    surfaceVariant = Color(0xFFEEE8D5),
    onSurfaceVariant = Color(0xFF586E75),
    outline = Color(0xFF93A1A1)
)

// 5. Nord Frost (Light)
private val NordFrostScheme = lightColorScheme(
    primary = Color(0xFF5E81AC),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF88C0D0),
    onSecondary = Color(0xFF2E3440),
    tertiary = Color(0xFF81A1C1),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFECEFF4),
    onBackground = Color(0xFF2E3440),
    surface = Color(0xFFE5E9F0),
    onSurface = Color(0xFF2E3440),
    surfaceVariant = Color(0xFFD8DEE9),
    onSurfaceVariant = Color(0xFF4C566A),
    outline = Color(0xFF4C566A)
)

// 6. Minimal Slate (Light)
private val MinimalSlateScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2563EB),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE9ECEF),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFCED4DA)
)

fun getThemeColorScheme(themeId: TeleBackThemeId): ColorScheme {
    return when (themeId) {
        TeleBackThemeId.MATRIX_MINT -> MatrixMintScheme
        TeleBackThemeId.OLED_AMBER -> OledAmberScheme
        TeleBackThemeId.DRACULA_NEON -> DraculaNeonScheme
        TeleBackThemeId.SOLARIZED_PAPER -> SolarizedPaperScheme
        TeleBackThemeId.NORD_FROST -> NordFrostScheme
        TeleBackThemeId.MINIMAL_SLATE -> MinimalSlateScheme
    }
}

@Composable
fun TeleBackTheme(
    themeId: TeleBackThemeId = TeleBackThemeId.MATRIX_MINT,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = getThemeColorScheme(themeId),
        typography = TerminalTypography,
        content = content
    )
}
