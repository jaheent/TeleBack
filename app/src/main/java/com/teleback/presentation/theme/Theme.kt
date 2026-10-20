package com.teleback.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val id: String, val label: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light")
}

enum class ThemeColor(val id: String, val label: String, val previewColor: Color) {
    GREEN("green", "Matrix Green", Color(0xFF00FFA3)),
    AMBER("amber", "Solar Amber", Color(0xFFFFB800)),
    CYAN("cyan", "Cyber Neon", Color(0xFF00D2FF))
}

enum class TeleBackThemeId(
    val id: String,
    val mode: ThemeMode,
    val color: ThemeColor,
    val displayName: String
) {
    DARK_GREEN("dark_green", ThemeMode.DARK, ThemeColor.GREEN, "Dark - Matrix Green"),
    DARK_AMBER("dark_amber", ThemeMode.DARK, ThemeColor.AMBER, "Dark - Solar Amber"),
    DARK_CYAN("dark_cyan", ThemeMode.DARK, ThemeColor.CYAN, "Dark - Cyber Neon"),
    LIGHT_GREEN("light_green", ThemeMode.LIGHT, ThemeColor.GREEN, "Light - Matrix Green"),
    LIGHT_AMBER("light_amber", ThemeMode.LIGHT, ThemeColor.AMBER, "Light - Solar Amber"),
    LIGHT_CYAN("light_cyan", ThemeMode.LIGHT, ThemeColor.CYAN, "Light - Cyber Neon");

    companion object {
        fun fromId(id: String?): TeleBackThemeId = when (id) {
            "dark_green", "matrix_mint" -> DARK_GREEN
            "dark_amber", "oled_amber" -> DARK_AMBER
            "dark_cyan", "dracula_neon" -> DARK_CYAN
            "light_green", "minimal_slate" -> LIGHT_GREEN
            "light_amber", "solarized_paper" -> LIGHT_AMBER
            "light_cyan", "nord_frost" -> LIGHT_CYAN
            else -> DARK_GREEN
        }

        fun fromModeAndColor(mode: ThemeMode, color: ThemeColor): TeleBackThemeId {
            return entries.firstOrNull { it.mode == mode && it.color == color } ?: DARK_GREEN
        }
    }
}

// 1. Dark - Matrix Green
private val DarkGreenScheme = darkColorScheme(
    primary = Color(0xFF00FFA3),
    onPrimary = Color(0xFF070A0F),
    secondary = Color(0xFF00D2FF),
    onSecondary = Color(0xFF070A0F),
    background = Color(0xFF070A0F),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF0D1219),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF161F2C),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF273549)
)

// 2. Dark - Solar Amber (True OLED black + Amber)
private val DarkAmberScheme = darkColorScheme(
    primary = Color(0xFFFFB800),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFFF7A00),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFF8E7),
    surface = Color(0xFF120E04),
    onSurface = Color(0xFFFFF8E7),
    surfaceVariant = Color(0xFF211805),
    onSurfaceVariant = Color(0xFFA89A80),
    outline = Color(0xFF42300A)
)

// 3. Dark - Cyber Neon (Deep navy + Neon Cyan & Purple)
private val DarkCyanScheme = darkColorScheme(
    primary = Color(0xFF00D2FF),
    onPrimary = Color(0xFF060A12),
    secondary = Color(0xFFB388FF),
    onSecondary = Color(0xFF060A12),
    background = Color(0xFF060A12),
    onBackground = Color(0xFFF0F6FF),
    surface = Color(0xFF0D1524),
    onSurface = Color(0xFFF0F6FF),
    surfaceVariant = Color(0xFF162238),
    onSurfaceVariant = Color(0xFF7F95B5),
    outline = Color(0xFF243657)
)

// 4. Light - Matrix Green (Crisp white/grey + Deep Terminal Green)
private val LightGreenScheme = lightColorScheme(
    primary = Color(0xFF00875A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0065FF),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

// 5. Light - Solar Amber (Warm cream/paper + Dark Amber)
private val LightAmberScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFB45309),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFDF5),
    onBackground = Color(0xFF291B00),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF291B00),
    surfaceVariant = Color(0xFFF7F2E6),
    onSurfaceVariant = Color(0xFF785E2F),
    outline = Color(0xFFE5D8B8)
)

// 6. Light - Cyber Neon (Ice blue-white + Vibrant Deep Cyan)
private val LightCyanScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF0F7FF),
    onBackground = Color(0xFF0C1E38),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0C1E38),
    surfaceVariant = Color(0xFFE0EDFC),
    onSurfaceVariant = Color(0xFF3B567D),
    outline = Color(0xFFBFDBFE)
)

fun getThemeColorScheme(themeId: TeleBackThemeId): ColorScheme = when (themeId) {
    TeleBackThemeId.DARK_GREEN -> DarkGreenScheme
    TeleBackThemeId.DARK_AMBER -> DarkAmberScheme
    TeleBackThemeId.DARK_CYAN -> DarkCyanScheme
    TeleBackThemeId.LIGHT_GREEN -> LightGreenScheme
    TeleBackThemeId.LIGHT_AMBER -> LightAmberScheme
    TeleBackThemeId.LIGHT_CYAN -> LightCyanScheme
}

@Composable
fun TeleBackTheme(
    themeId: TeleBackThemeId = TeleBackThemeId.DARK_GREEN,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = getThemeColorScheme(themeId),
        typography = TerminalTypography,
        content = content
    )
}
