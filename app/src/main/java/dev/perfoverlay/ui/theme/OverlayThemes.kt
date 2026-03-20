package dev.perfoverlay.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Predefined overlay themes for PerfOverlay.
 * Each theme defines the accent colors used for different stat categories,
 * plus a primary glow color for the glassmorphism card border highlight.
 */
enum class OverlayTheme(
    val displayName: String,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentWarn: Color,
    val accentDanger: Color,
    val accentInfo: Color,
    val cardGlow: Color,
    val gradientStart: Color,
    val gradientEnd: Color
) {
    OCEAN(
        displayName = "Ocean",
        accentPrimary = Color(0xFF4A9EFF),
        accentSecondary = Color(0xFF4AE68C),
        accentWarn = Color(0xFFFFD93D),
        accentDanger = Color(0xFFFF6B6B),
        accentInfo = Color(0xFF66CFFF),
        cardGlow = Color(0x334A9EFF),
        gradientStart = Color(0xFF0A1628),
        gradientEnd = Color(0xFF0D2137)
    ),
    AMETHYST(
        displayName = "Amethyst",
        accentPrimary = Color(0xFFA855F7),
        accentSecondary = Color(0xFFEC4899),
        accentWarn = Color(0xFFFBBF24),
        accentDanger = Color(0xFFF87171),
        accentInfo = Color(0xFF818CF8),
        cardGlow = Color(0x33A855F7),
        gradientStart = Color(0xFF1A0A2E),
        gradientEnd = Color(0xFF2D1248)
    ),
    EMERALD(
        displayName = "Emerald",
        accentPrimary = Color(0xFF10B981),
        accentSecondary = Color(0xFF34D399),
        accentWarn = Color(0xFFFBBF24),
        accentDanger = Color(0xFFEF4444),
        accentInfo = Color(0xFF6EE7B7),
        cardGlow = Color(0x3310B981),
        gradientStart = Color(0xFF0A1F18),
        gradientEnd = Color(0xFF0D2B1F)
    ),
    SUNSET(
        displayName = "Sunset",
        accentPrimary = Color(0xFFF97316),
        accentSecondary = Color(0xFFFB923C),
        accentWarn = Color(0xFFFBBF24),
        accentDanger = Color(0xFFEF4444),
        accentInfo = Color(0xFFE879F9),
        cardGlow = Color(0x33F97316),
        gradientStart = Color(0xFF1F0A00),
        gradientEnd = Color(0xFF2B1200)
    ),
    MONOCHROME(
        displayName = "Mono",
        accentPrimary = Color(0xFFE0E0E0),
        accentSecondary = Color(0xFFBDBDBD),
        accentWarn = Color(0xFFFFD93D),
        accentDanger = Color(0xFFFF6B6B),
        accentInfo = Color(0xFF9E9E9E),
        cardGlow = Color(0x33FFFFFF),
        gradientStart = Color(0xFF0A0A0A),
        gradientEnd = Color(0xFF1A1A1A)
    ),
    CYBERPUNK(
        displayName = "Cyber",
        accentPrimary = Color(0xFF00F0FF),
        accentSecondary = Color(0xFFFF00A0),
        accentWarn = Color(0xFFFFFF00),
        accentDanger = Color(0xFFFF3366),
        accentInfo = Color(0xFF00FF88),
        cardGlow = Color(0x3300F0FF),
        gradientStart = Color(0xFF0A000F),
        gradientEnd = Color(0xFF14001E)
    );

    companion object {
        val default = OCEAN
        fun fromName(name: String?): OverlayTheme =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: default
    }
}
