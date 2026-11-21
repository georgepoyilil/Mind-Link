package com.mindlink.journal.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// -------------------------------------------------------------
// Core Theme Definitions & Atmosphere
// -------------------------------------------------------------

enum class MindTheme(val id: String, val title: String, val subtitle: String, val isDark: Boolean, val isGlassy: Boolean = false) {
    AMOLED_BLACK(
        id = "amoled_black",
        title = "AMOLED Pitch Black",
        subtitle = "Pure #000000 pixels-off, ultra high contrast",
        isDark = true,
        isGlassy = false
    ),
    IOS_GLASSY(
        id = "ios_glassy",
        title = "iOS Liquid Glass (Dark)",
        subtitle = "Ultra-frosted midnight glass, iridescent specular rims",
        isDark = true,
        isGlassy = true
    ),
    IOS_GLASSY_LIGHT(
        id = "ios_glassy_light",
        title = "iOS Liquid Glass (Light)",
        subtitle = "Crystalline frosted acrylic with radiant specular gleam",
        isDark = false,
        isGlassy = true
    ),
    PIXEL_MINIMAL(
        id = "pixel_minimal",
        title = "Pixel Material You",
        subtitle = "Monet charcoal with warm pastel dynamic accents",
        isDark = true,
        isGlassy = false
    ),
    LIGHT_CLEAN(
        id = "light_clean",
        title = "Daylight Minimal",
        subtitle = "Crisp slate white with high-contrast ink",
        isDark = false,
        isGlassy = false
    ),
    LIGHT_PAPER(
        id = "light_paper",
        title = "Warm Paper Journal",
        subtitle = "Moleskine cream parchment with espresso ink",
        isDark = false,
        isGlassy = false
    );

    companion object {
        fun fromId(id: String?): MindTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AMOLED_BLACK
        }
    }
}

data class MindLinkColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassSurfaceVariant: Color,
    val glassBorder: Color,
    val glassBorderSubtle: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val glassCardBorder: Brush,
    val bottomDock: Color,
    val isDark: Boolean,
    val isGlassy: Boolean = false,
    val glassCardBackground: Brush = Brush.linearGradient(listOf(glassSurface, glassSurface)),
    val glassTopHighlight: Brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
    val glassInnerSheen: Brush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
)

// Brand Accents
val MindIndigo = Color(0xFF6366F1)
val MindViolet = Color(0xFF8B5CF6)
val MindPurple = Color(0xFFA855F7)
val MindCyan = Color(0xFF06B6D4)
val MindPink = Color(0xFFEC4899)
val MindEmerald = Color(0xFF10B981)
val MindAmber = Color(0xFFF59E0B)
val MindRose = Color(0xFFF43F5E)

// Unified Pastel Glass Palette (Classy, Apple Glass / VisionOS / Nothing OS aesthetic)
val PastelLavender = Color(0xFFA5B4FC)   // Primary soft lavender
val PastelViolet = Color(0xFFC4B5FD)     // Secondary soft violet
val PastelMint = Color(0xFF6EE7B7)       // Fresh muted mint (tasks & completion)
val PastelSky = Color(0xFF7DD3FC)        // Subtle airy sky blue (media & attachments)
val PastelPeach = Color(0xFFFDBA74)      // Warm pastel peach (notes & summaries)
val PastelRose = Color(0xFFFDA4AF)       // Soft muted rose (delete & trash)
val PastelAmber = Color(0xFFFDE68A)      // Soft pastel amber (archive & highlights)

// Unified Classy Glass Button Styles
val RawMindGlassButtonContainer = Color(0x2AA5B4FC)
val RawMindGlassButtonBorder = Color(0x4DA5B4FC)
val RawMindGlassButtonText = Color(0xFFE2E8F0)
val MindGlassButtonBorderBrush = Brush.linearGradient(
    listOf(
        Color(0x66A5B4FC),
        Color(0x33FFFFFF),
        Color(0x44C4B5FD)
    )
)

// Raw AMOLED Constants
val RawMindBackgroundAmoled = Color(0xFF000000)
val RawMindSurfaceAmoled = Color(0xFF070A12)
val RawMindSurfaceElevatedAmoled = Color(0xFF0E1424)
val RawMindSurfaceVariantAmoled = Color(0xFF131A2E)
val RawMindBorderAmoled = Color(0xFF1E2638)

// Raw Frosted Glassmorphism Surfaces
val RawMindGlassSurface = Color(0xCC070A12)
val RawMindGlassSurfaceVariant = Color(0x99131A2E)
val RawMindGlassSurfaceElevated = Color(0xE60E1424)
val RawMindGlassBottomDock = Color(0xF2000000)
val RawMindGlassBorder = Color(0x33818CF8)
val RawMindGlassBorderSubtle = Color(0x1FFFFFFF)

// Prism & Holographic Refraction Brushes
val MindPrismGradient = Brush.linearGradient(
    listOf(
        Color(0xFF818CF8),
        Color(0xFFC084FC),
        Color(0xFFF472B6),
        Color(0xFF22D3EE)
    )
)

val MindPrismBorder = Brush.linearGradient(
    listOf(
        Color(0xCC818CF8),
        Color(0x88C084FC),
        Color(0x99EC4899),
        Color(0x7706B6D4),
        Color(0x33818CF8)
    )
)

val RawMindPrismGlassCardBorder = Brush.linearGradient(
    listOf(
        Color(0x40FFFFFF),
        Color(0x33818CF8),
        Color(0x22C084FC),
        Color(0x1A06B6D4),
        Color(0x10FFFFFF)
    )
)

val MindAiGlassGradient = Brush.verticalGradient(
    listOf(
        Color(0x334F46E5),
        Color(0x1F7C3AED),
        Color(0x0A000000)
    )
)

val MindAiGlassBorder = MindPrismBorder

// Light Theme Static Surface References
val MindBackgroundLight = Color(0xFFF8FAFC)
val MindSurfaceLight = Color(0xFFFFFFFF)
val MindSurfaceVariantLight = Color(0xFFF1F5F9)
val MindBorderLight = Color(0xFFE2E8F0)
val MindTextPrimaryLight = Color(0xFF0F172A)
val MindTextSecondaryLight = Color(0xFF475569)

// -------------------------------------------------------------
// Per-Theme Color Palette Builder
// -------------------------------------------------------------

fun getThemeColors(theme: MindTheme): MindLinkColors = when (theme) {
    MindTheme.AMOLED_BLACK -> MindLinkColors(
        background = Color(0xFF000000),
        surface = Color(0xFF070A12),
        surfaceElevated = Color(0xFF0E1424),
        surfaceVariant = Color(0xFF131A2E),
        glassSurface = Color(0xCC070A12),
        glassSurfaceElevated = Color(0xE60E1424),
        glassSurfaceVariant = Color(0x99131A2E),
        glassBorder = Color(0x33818CF8),
        glassBorderSubtle = Color(0x1FFFFFFF),
        border = Color(0xFF1E2638),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textTertiary = Color(0xFF64748B),
        accentPrimary = PastelLavender,
        accentSecondary = PastelViolet,
        glassCardBorder = RawMindPrismGlassCardBorder,
        bottomDock = Color(0xF2000000),
        isDark = true
    )
    MindTheme.IOS_GLASSY -> MindLinkColors(
        background = Color(0xFF080C16), // Apple Space Black / Deep Midnight
        surface = Color(0xD91E2738),    // 85% silky frosted smoky slate
        surfaceElevated = Color(0xE6242F42), // 90% elevated frosted glass
        surfaceVariant = Color(0x99182030),  // 60% frosted variant
        glassSurface = Color(0xD91E2738),    // Silky frosted acrylic
        glassSurfaceElevated = Color(0xE6242F42),
        glassSurfaceVariant = Color(0x99182030),
        glassBorder = Color(0x33FFFFFF),    // Delicate 20% white specular rim
        glassBorderSubtle = Color(0x1AFFFFFF), // 10% subtle edge
        border = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textTertiary = Color(0xFF64748B),
        accentPrimary = Color(0xFF38BDF8),  // Apple vibrant cyan
        accentSecondary = Color(0xFF818CF8), // Apple electric indigo
        glassCardBorder = Brush.verticalGradient(
            listOf(
                Color(0x40FFFFFF), // Subtle top specular white highlight
                Color(0x20FFFFFF), // Mid translucent edge
                Color(0x0AFFFFFF)  // Soft bottom rim
            )
        ),
        bottomDock = Color(0xE6080C16),     // Frosted midnight dock
        isDark = true,
        isGlassy = true,
        glassCardBackground = Brush.verticalGradient(
            listOf(
                Color(0xD91E2738), // 85% smoky charcoal top
                Color(0xBF161E2E), // 75% frosted midnight
                Color(0xB3101624)  // 70% deep frosted slate
            )
        ),
        glassTopHighlight = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.30f),
                Color.Transparent
            )
        ),
        glassInnerSheen = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    )
    MindTheme.IOS_GLASSY_LIGHT -> MindLinkColors(
        background = Color(0xFFE8EEF7),     // Apple Daylight Opalescent Mist
        surface = Color(0xE6FFFFFF),        // 90% silky frosted white milk
        surfaceElevated = Color(0xF5FFFFFF), // 96% elevated frosted white
        surfaceVariant = Color(0xCCF1F5F9),  // 80% soft frosted mist
        glassSurface = Color(0xE6FFFFFF),   // Milky frosted glass
        glassSurfaceElevated = Color(0xF5FFFFFF),
        glassSurfaceVariant = Color(0xCCF1F5F9),
        glassBorder = Color(0x80FFFFFF),    // Crisp 50% white specular rim
        glassBorderSubtle = Color(0x40FFFFFF), // 25% white rim
        border = Color(0x200F172A),         // Soft depth rim
        textPrimary = Color(0xFF0F172A),    // Crisp Apple slate ink
        textSecondary = Color(0xFF334155),  // Subtle slate
        textTertiary = Color(0xFF64748B),
        accentPrimary = Color(0xFF0284C7),  // Apple vibrant sky blue
        accentSecondary = Color(0xFF6366F1), // Apple royal indigo
        glassCardBorder = Brush.verticalGradient(
            listOf(
                Color(0xCCFFFFFF), // Crisp 80% white specular top highlight
                Color(0x66FFFFFF), // Soft 40% translucent edge
                Color(0x200F172A)  // Subtle shadow rim at bottom for contrast
            )
        ),
        bottomDock = Color(0xE6E8EEF7),     // Frosted daylight dock
        isDark = false,
        isGlassy = true,
        glassCardBackground = Brush.verticalGradient(
            listOf(
                Color(0xF2FFFFFF), // 95% crisp sunlit white glass top
                Color(0xD9FFFFFF), // 85% silky frosted milk
                Color(0xCCF1F5F9)  // 80% soft pearl mist
            )
        ),
        glassTopHighlight = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.50f),
                Color.Transparent
            )
        ),
        glassInnerSheen = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.15f),
                Color.Transparent
            )
        )
    )
    MindTheme.PIXEL_MINIMAL -> MindLinkColors(
        background = Color(0xFF131314),
        surface = Color(0xFF1E1F20),
        surfaceElevated = Color(0xFF282A2C),
        surfaceVariant = Color(0xFF333538),
        glassSurface = Color(0xF21E1F20),
        glassSurfaceElevated = Color(0xF7282A2C),
        glassSurfaceVariant = Color(0xE6333538),
        glassBorder = Color(0x3D8E918F),
        glassBorderSubtle = Color(0x1FFFFFFF),
        border = Color(0xFF333538),
        textPrimary = Color(0xFFE3E3E3),
        textSecondary = Color(0xFFC4C7C5),
        textTertiary = Color(0xFF8E918F),
        accentPrimary = Color(0xFFA8C7FA),
        accentSecondary = Color(0xFFC4EED0),
        glassCardBorder = Brush.linearGradient(
            listOf(
                Color(0x40A8C7FA),
                Color(0x268E918F),
                Color(0x40C4EED0)
            )
        ),
        bottomDock = Color(0xF5131314),
        isDark = true
    )
    MindTheme.LIGHT_CLEAN -> MindLinkColors(
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1F5F9),
        glassSurface = Color(0xF7FFFFFF),
        glassSurfaceElevated = Color(0xFCFFFFFF),
        glassSurfaceVariant = Color(0xF0F1F5F9),
        glassBorder = Color(0x336366F1),
        glassBorderSubtle = Color(0xFFE2E8F0),
        border = Color(0xFFE2E8F0),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textTertiary = Color(0xFF94A3B8),
        accentPrimary = Color(0xFF4F46E5),
        accentSecondary = Color(0xFF7C3AED),
        glassCardBorder = Brush.linearGradient(
            listOf(
                Color(0xFFE2E8F0),
                Color(0x334F46E5),
                Color(0xFFE2E8F0)
            )
        ),
        bottomDock = Color(0xF5FFFFFF),
        isDark = false
    )
    MindTheme.LIGHT_PAPER -> MindLinkColors(
        background = Color(0xFFF5F2EB),
        surface = Color(0xFFFCFBF9),
        surfaceElevated = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEBE6DC),
        glassSurface = Color(0xF5FCFBF9),
        glassSurfaceElevated = Color(0xFAFFFFFF),
        glassSurfaceVariant = Color(0xF0EBE6DC),
        glassBorder = Color(0x40D97706),
        glassBorderSubtle = Color(0xFFDDD6CE),
        border = Color(0xFFDDD6CE),
        textPrimary = Color(0xFF292524),
        textSecondary = Color(0xFF57534E),
        textTertiary = Color(0xFF8D8378),
        accentPrimary = Color(0xFF92400E),
        accentSecondary = Color(0xFF78350F),
        glassCardBorder = Brush.linearGradient(
            listOf(
                Color(0xFFDDD6CE),
                Color(0x40B45309),
                Color(0xFFDDD6CE)
            )
        ),
        bottomDock = Color(0xF5F5F2EB),
        isDark = false
    )
}

// -------------------------------------------------------------
// Dynamic Theme Property Accessors (Backwards Compatible)
// -------------------------------------------------------------

val MindBackgroundAmoled: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.background
val MindSurfaceAmoled: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.surface
val MindSurfaceElevatedAmoled: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.surfaceElevated
val MindSurfaceVariantAmoled: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.surfaceVariant
val MindBorderAmoled: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.border

val MindGlassSurface: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassSurface
val MindGlassSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassSurfaceVariant
val MindGlassSurfaceElevated: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.surfaceElevated
val MindGlassBottomDock: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.bottomDock
val MindGlassBorder: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassBorder
val MindGlassBorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassBorderSubtle

val MindPrismGlassCardBorder: Brush @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassCardBorder
val MindGlassCardBorder: Brush @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassCardBorder

val MindBackgroundDark: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.background
val MindSurfaceDark: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.surface
val MindSurfaceVariantDark: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.surfaceVariant
val MindBorderDark: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.border

val MindTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.textPrimary
val MindTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.textSecondary
val MindTextTertiary: Color @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.textTertiary

val MindGlassButtonContainer: Color @Composable @ReadOnlyComposable get() =
    if (LocalMindLinkColors.current.isDark) Color(0x2AA5B4FC) else LocalMindLinkColors.current.surfaceVariant

val MindGlassButtonBorder: Color @Composable @ReadOnlyComposable get() =
    if (LocalMindLinkColors.current.isDark) Color(0x4DA5B4FC) else LocalMindLinkColors.current.border

val MindGlassButtonText: Color @Composable @ReadOnlyComposable get() =
    if (LocalMindLinkColors.current.isDark) Color(0xFFE2E8F0) else LocalMindLinkColors.current.textPrimary

val MindIsGlassy: Boolean @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.isGlassy
val MindGlassCardBackground: Brush @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassCardBackground
val MindGlassTopHighlight: Brush @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassTopHighlight
val MindGlassInnerSheen: Brush @Composable @ReadOnlyComposable get() = LocalMindLinkColors.current.glassInnerSheen
