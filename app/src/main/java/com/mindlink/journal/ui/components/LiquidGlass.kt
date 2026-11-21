package com.mindlink.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Authentic Apple iOS / visionOS Glass Material Atmosphere.
 * Uses a smooth, sophisticated ambient wallpaper gradient that lets frosted materials
 * diffuse naturally without loud splotches or artificial light leaks.
 */
@Composable
fun LiquidGlassAtmosphere(
    isDark: Boolean,
    isGlassy: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                if (!isGlassy) {
                    drawRect(color = if (isDark) Color(0xFF000000) else Color(0xFFF8FAFC))
                    return@drawBehind
                }

                if (isDark) {
                    // Apple Space Black / Deep Midnight atmospheric gradient
                    val darkWallpaper = Brush.verticalGradient(
                        0.0f to Color(0xFF070B14),
                        0.40f to Color(0xFF0E1526),
                        0.80f to Color(0xFF0A0F1D),
                        1.0f to Color(0xFF050810)
                    )
                    drawRect(brush = darkWallpaper)
                } else {
                    // Apple Daylight opalescent mist gradient
                    val lightWallpaper = Brush.verticalGradient(
                        0.0f to Color(0xFFE4EBF5),
                        0.35f to Color(0xFFECF1F8),
                        0.75f to Color(0xFFE8EEF7),
                        1.0f to Color(0xFFDEE6F2)
                    )
                    drawRect(brush = lightWallpaper)
                }
            }
    ) {
        content()
    }
}

/**
 * Authentic Apple visionOS / iOS Liquid Glass Modifier.
 * 1. Soft diffused ambient drop shadow giving true 3D spatial depth
 * 2. Milky, silky frosted material with high legibility (no raw see-through wash)
 * 3. Delicate hair-thin specular border catching light from above (no rainbow leaks)
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(18.dp),
    cornerRadius: Dp = 18.dp,
    isGlassy: Boolean = true,
    isDark: Boolean = true,
    cardBackground: Brush? = null,
    cardBorder: Brush? = null,
    topHighlight: Brush? = null,
    elevation: Dp = 6.dp,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null
): Modifier {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    if (!isGlassy) {
        return this
            .clip(shape)
            .then(clickMod)
    }

    // Specular border: top catches light from above, bottom is soft
    val borderBrush = cardBorder ?: if (isDark) {
        Brush.verticalGradient(
            0.0f to Color(0x40FFFFFF),  // 25% white specular highlight at top
            0.5f to Color(0x20FFFFFF),  // 12% subtle edge
            1.0f to Color(0x0AFFFFFF)   // 4% bottom rim
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0xCCFFFFFF),  // Crisp 80% white specular top highlight
            0.5f to Color(0x66FFFFFF),  // Soft 40% translucent edge
            1.0f to Color(0x200F172A)   // Subtle shadow rim at bottom for contrast
        )
    }

    // Material fill: thick, silky frosted acrylic
    val materialFill = cardBackground ?: if (isDark) {
        Brush.verticalGradient(
            0.0f to Color(0xD91E2738),  // 85% smoky charcoal top
            0.5f to Color(0xBF161E2E),  // 75% frosted midnight
            1.0f to Color(0xB3101624)   // 70% deep frosted slate
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color(0xF2FFFFFF),  // 95% crisp sunlit white glass top
            0.4f to Color(0xD9FFFFFF),  // 85% silky frosted milk
            1.0f to Color(0xCCF1F5F9)   // 80% soft pearl mist
        )
    }

    val shadowAmbient = if (isDark) Color(0x55000000) else Color(0x180F172A)
    val shadowSpot = if (isDark) Color(0x66000000) else Color(0x1F0F172A)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowAmbient,
            spotColor = shadowSpot
        )
        .clip(shape)
        .background(materialFill)
        .then(clickMod)
        .drawBehind {
            val rPx = cornerRadius.toPx()
            // Hair-thin delicate specular rim
            drawRoundRect(
                brush = borderBrush,
                cornerRadius = CornerRadius(rPx, rPx),
                style = Stroke(width = borderWidth.toPx())
            )
        }
}
