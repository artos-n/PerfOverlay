package dev.perfoverlay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Glassmorphism-inspired color palette
val GlassWhite = Color(0xCCFFFFFF)
val GlassBlack = Color(0x99000000)
val GlassBlue = Color(0x664A9EFF)
val GlassGreen = Color(0x664AE68C)
val GlassRed = Color(0x66FF6B6B)
val GlassYellow = Color(0x66FFD93D)
val GlassPurple = Color(0x66A855F7)

val AccentBlue = Color(0xFF4A9EFF)
val AccentGreen = Color(0xFF4AE68C)
val AccentRed = Color(0xFFFF6B6B)
val AccentYellow = Color(0xFFFFD93D)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentGreen,
    tertiary = GlassPurple,
    surface = Color(0xFF1C1C1E),
    background = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentGreen,
    tertiary = GlassPurple,
    surface = Color(0xFFF2F2F7),
    background = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    onBackground = Color(0xFF000000)
)

@Composable
fun PerfOverlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
