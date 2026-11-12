package com.mindlink.journal.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Accents
val MindIndigo = Color(0xFF6366F1)
val MindViolet = Color(0xFF8B5CF6)
val MindPurple = Color(0xFFA855F7)
val MindCyan = Color(0xFF06B6D4)
val MindPink = Color(0xFFEC4899)
val MindEmerald = Color(0xFF10B981)
val MindAmber = Color(0xFFF59E0B)
val MindRose = Color(0xFFF43F5E)

// AMOLED Pitch Black Surfaces (Nothing Phone Essential Space inspired)
val MindBackgroundAmoled = Color(0xFF000000)          // Pure Pitch Black #000000 (Pixels Off)
val MindSurfaceAmoled = Color(0xFF070A12)             // Deep Obsidian Slate
val MindSurfaceElevatedAmoled = Color(0xFF0E1424)     // Subtle Elevated Slate
val MindSurfaceVariantAmoled = Color(0xFF131A2E)      // High-contrast translucent surface
val MindBorderAmoled = Color(0xFF1E2638)              // Subtle divider

// Frosted Glassmorphism Surfaces
val MindGlassSurface = Color(0xCC0A0F1E)             // 80% opacity pitch glass
val MindGlassSurfaceVariant = Color(0x99131A2E)      // 60% opacity secondary glass
val MindGlassSurfaceElevated = Color(0xE60E1424)     // 90% opacity elevated glass
val MindGlassBottomDock = Color(0xF2000000)          // 95% opacity pitch black bottom dock
val MindGlassBorder = Color(0x33818CF8)              // Subtle neon indigo rim
val MindGlassBorderSubtle = Color(0x1FFFFFFF)        // Frosted white highlight

// Prism & Holographic Refraction Brushes (Chromatic Light-Leak Effect)
val MindPrismGradient = Brush.linearGradient(
    listOf(
        Color(0xFF818CF8), // Electric Indigo
        Color(0xFFC084FC), // Lavender / Violet
        Color(0xFFF472B6), // Neon Pink
        Color(0xFF22D3EE)  // Bright Cyan
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

val MindPrismGlassCardBorder = Brush.linearGradient(
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
        Color(0x334F46E5), // Indigo Glow Top
        Color(0x1F7C3AED), // Violet Mid
        Color(0x0A000000)  // Pitch Black Fade Bottom
    )
)

val MindGlassCardBorder = MindPrismGlassCardBorder
val MindAiGlassBorder = MindPrismBorder

// Backward Compatibility Aliases
val MindBackgroundDark = MindBackgroundAmoled
val MindSurfaceDark = MindSurfaceAmoled
val MindSurfaceVariantDark = MindSurfaceVariantAmoled
val MindBorderDark = MindBorderAmoled

// Text Colors
val MindTextPrimary = Color(0xFFF8FAFC)
val MindTextSecondary = Color(0xFF94A3B8)
val MindTextTertiary = Color(0xFF64748B)

// Light Theme Surfaces
val MindBackgroundLight = Color(0xFFF8FAFC)
val MindSurfaceLight = Color(0xFFFFFFFF)
val MindSurfaceVariantLight = Color(0xFFF1F5F9)
val MindBorderLight = Color(0xFFE2E8F0)
val MindTextPrimaryLight = Color(0xFF0F172A)
val MindTextSecondaryLight = Color(0xFF475569)
