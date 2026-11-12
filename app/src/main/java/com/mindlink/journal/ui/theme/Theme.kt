package com.mindlink.journal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MindIndigo,
    onPrimary = MindTextPrimary,
    primaryContainer = MindSurfaceVariantDark,
    onPrimaryContainer = MindIndigo,
    secondary = MindViolet,
    onSecondary = MindTextPrimary,
    secondaryContainer = MindSurfaceVariantDark,
    onSecondaryContainer = MindViolet,
    tertiary = MindCyan,
    background = MindBackgroundDark,
    onBackground = MindTextPrimary,
    surface = MindSurfaceDark,
    onSurface = MindTextPrimary,
    surfaceVariant = MindSurfaceVariantDark,
    onSurfaceVariant = MindTextSecondary,
    outline = MindBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = MindIndigo,
    onPrimary = MindSurfaceLight,
    primaryContainer = MindSurfaceVariantLight,
    onPrimaryContainer = MindIndigo,
    secondary = MindViolet,
    onSecondary = MindSurfaceLight,
    background = MindBackgroundLight,
    onBackground = MindTextPrimaryLight,
    surface = MindSurfaceLight,
    onSurface = MindTextPrimaryLight,
    surfaceVariant = MindSurfaceVariantLight,
    onSurfaceVariant = MindTextSecondaryLight,
    outline = MindBorderLight
)

@Composable
fun MindLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Defaults to sleek dark theme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
