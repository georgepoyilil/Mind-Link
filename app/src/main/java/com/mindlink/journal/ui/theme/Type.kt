package com.mindlink.journal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mindlink.journal.R

/**
 * Genuine Calibri Humanist Font Family:
 * Warm, rounded, highly legible font for journal thoughts and notes.
 */
val CalibriFontFamily = FontFamily(
    Font(R.font.calibri, FontWeight.Normal),
    Font(R.font.calibri, FontWeight.Medium),
    Font(R.font.calibri_bold, FontWeight.Bold),
    Font(R.font.calibri_bold, FontWeight.SemiBold),
    Font(R.font.calibri_italic, FontWeight.Normal, FontStyle.Italic)
)

/**
 * Returns a Typography instance with proportionally scaled font sizes and line heights.
 * Line heights scale by ~1.38x-1.45x so multi-line text never collides or clips when scaling up.
 */
fun getScaledTypography(fontScale: Float = 1.0f): Typography {
    val scale = fontScale.coerceIn(0.85f, 1.35f)
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (26 * scale).sp,
            lineHeight = (34 * scale).sp,
            letterSpacing = (-0.4).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (21 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = (-0.2).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18 * scale).sp,
            lineHeight = (24 * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * scale).sp,
            lineHeight = (22 * scale).sp,
            letterSpacing = (-0.1).sp
        ),
        titleMedium = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14.5f * scale).sp,
            lineHeight = (20 * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13 * scale).sp,
            lineHeight = (18 * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15 * scale).sp,
            lineHeight = (22 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * scale).sp,
            lineHeight = (19 * scale).sp,
            letterSpacing = 0.1.sp
        ),
        bodySmall = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16.5f * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * scale).sp,
            lineHeight = (16.5f * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * scale).sp,
            lineHeight = (15 * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = CalibriFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (10 * scale).sp,
            lineHeight = (14 * scale).sp
        )
    )
}

/**
 * Default Typography at 1.0x baseline.
 */
val Typography = getScaledTypography(1.0f)

enum class NoteFontOption(val key: String, val label: String, val fontFamily: FontFamily) {
    CALIBRI("Sans", "Calibri", CalibriFontFamily),
    SERIF("Serif", "Serif", FontFamily.Serif),
    MONO("Mono", "Mono", FontFamily.Monospace),
    CURSIVE("Cursive", "Script", FontFamily.Cursive);

    companion object {
        fun fromKey(key: String?): NoteFontOption {
            if (key == null) return CALIBRI
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: CALIBRI
        }
    }
}

fun resolveNoteFontFamily(key: String?): FontFamily {
    return NoteFontOption.fromKey(key).fontFamily
}
