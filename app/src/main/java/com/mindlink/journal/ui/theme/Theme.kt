package com.mindlink.journal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalAppFontScale = staticCompositionLocalOf { 1.0f }
val LocalAppTheme = staticCompositionLocalOf { MindTheme.AMOLED_BLACK }
val LocalMindLinkColors = staticCompositionLocalOf { getThemeColors(MindTheme.AMOLED_BLACK) }
val LocalShowEmojis = staticCompositionLocalOf { true }
val LocalLineSpacingMultiplier = staticCompositionLocalOf { 1.45f }

@Composable
fun MindLinkTheme(
    themeKey: String = "amoled_black",
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    showEmojis: Boolean = true,
    lineSpacingMultiplier: Float = 1.45f,
    content: @Composable () -> Unit
) {
    val mindTheme = remember(themeKey) { MindTheme.fromId(themeKey) }
    val colors = remember(mindTheme) { getThemeColors(mindTheme) }
    val typography = remember(fontScale) { getScaledTypography(fontScale) }
    val currentDensity = LocalDensity.current

    // Smooth bounded density scaling for icons and base metrics without clipping fixed-height chips
    val boundedDensityScale = ((fontScale - 1.0f) * 0.30f + 1.0f).coerceIn(0.90f, 1.15f)
    val effectiveDensity = remember(currentDensity.density, currentDensity.fontScale, boundedDensityScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * boundedDensityScale
        )
    }

    val colorScheme = remember(colors) {
        if (colors.isDark) {
            darkColorScheme(
                primary = colors.accentPrimary,
                onPrimary = colors.background,
                primaryContainer = colors.surfaceVariant,
                onPrimaryContainer = colors.accentPrimary,
                secondary = colors.accentSecondary,
                onSecondary = colors.background,
                secondaryContainer = colors.surfaceVariant,
                onSecondaryContainer = colors.accentSecondary,
                tertiary = MindCyan,
                background = colors.background,
                onBackground = colors.textPrimary,
                surface = colors.surface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.surfaceVariant,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.border
            )
        } else {
            lightColorScheme(
                primary = colors.accentPrimary,
                onPrimary = Color.White,
                primaryContainer = colors.surfaceVariant,
                onPrimaryContainer = colors.accentPrimary,
                secondary = colors.accentSecondary,
                onSecondary = Color.White,
                secondaryContainer = colors.surfaceVariant,
                onSecondaryContainer = colors.accentSecondary,
                tertiary = MindCyan,
                background = colors.background,
                onBackground = colors.textPrimary,
                surface = colors.surface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.surfaceVariant,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.border
            )
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides mindTheme,
        LocalMindLinkColors provides colors,
        LocalShowEmojis provides showEmojis,
        LocalAppFontScale provides fontScale,
        LocalLineSpacingMultiplier provides lineSpacingMultiplier,
        LocalDensity provides effectiveDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
