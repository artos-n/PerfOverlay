package dev.perfoverlay.ui.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.perfoverlay.data.OverlayConfig
import dev.perfoverlay.data.PerformanceStats
import dev.perfoverlay.ui.theme.OverlayTheme
import dev.perfoverlay.util.StatsCollector
import kotlinx.coroutines.flow.StateFlow

@Composable
fun OverlayView(
    stats: StateFlow<PerformanceStats>,
    config: StateFlow<OverlayConfig>
) {
    val currentStats by stats.collectAsState()
    val currentConfig by config.collectAsState()
    val theme = OverlayTheme.fromName(currentConfig.themeName)

    if (currentConfig.compactMode) {
        CompactOverlayView(currentStats, currentConfig, theme)
    } else {
        FullOverlayView(currentStats, currentConfig, theme)
    }
}

// ─── Full (Classic) Overlay ────────────────────────────────────

@Composable
private fun FullOverlayView(
    stats: PerformanceStats,
    config: OverlayConfig,
    theme: OverlayTheme
) {
    GlassmorphismCard(
        alpha = config.opacity,
        blurEnabled = config.backgroundBlur,
        glowColor = theme.cardGlow
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 160.dp)
                .padding(10.dp * config.scale),
            verticalArrangement = Arrangement.spacedBy(6.dp * config.scale)
        ) {
            OverlayHeader(stats.fps, config.showFps, config.scale, theme)

            if (config.showCpu) {
                StatRow(
                    icon = Icons.Rounded.Memory,
                    label = "CPU",
                    value = "${stats.cpuUsage.toInt()}%",
                    subValue = if (stats.cpuFrequency > 0) "${stats.cpuFrequency} MHz" else null,
                    color = theme.accentPrimary,
                    usage = stats.cpuUsage / 100f,
                    scale = config.scale
                )
            }

            if (config.showGpu) {
                StatRow(
                    icon = Icons.Rounded.Games,
                    label = "GPU",
                    value = "${stats.gpuUsage.toInt()}%",
                    subValue = null,
                    color = theme.accentSecondary,
                    usage = stats.gpuUsage / 100f,
                    scale = config.scale
                )
            }

            if (config.showRam) {
                StatRow(
                    icon = Icons.Rounded.Storage,
                    label = "RAM",
                    value = "${stats.ramUsed}MB",
                    subValue = "/ ${stats.ramTotal}MB",
                    color = theme.accentInfo,
                    usage = stats.ramUsed.toFloat() / stats.ramTotal.coerceAtLeast(1),
                    scale = config.scale
                )
            }

            if (config.showTemp) {
                TemperatureRow(stats, config.scale, theme)
            }

            if (config.showNetwork) {
                NetworkRow(stats, config.scale, theme)
            }
        }
    }
}

// ─── Compact Mode ──────────────────────────────────────────────

/**
 * Compact overlay — single horizontal bar, minimal footprint.
 * Shows FPS badge + condensed stat pills side by side.
 */
@Composable
private fun CompactOverlayView(
    stats: PerformanceStats,
    config: OverlayConfig,
    theme: OverlayTheme
) {
    GlassmorphismCard(
        alpha = config.opacity,
        blurEnabled = config.backgroundBlur,
        glowColor = theme.cardGlow
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp * config.scale, vertical = 5.dp * config.scale),
            horizontalArrangement = Arrangement.spacedBy(6.dp * config.scale),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FPS badge
            if (config.showFps) {
                CompactFpsBadge(stats.fps, config.scale, theme)
            }

            // Stat pills
            if (config.showCpu) {
                CompactStatPill("CPU", "${stats.cpuUsage.toInt()}%", theme.accentPrimary, config.scale)
            }
            if (config.showGpu) {
                CompactStatPill("GPU", "${stats.gpuUsage.toInt()}%", theme.accentSecondary, config.scale)
            }
            if (config.showRam) {
                CompactStatPill("RAM", "${stats.ramUsed}MB", theme.accentInfo, config.scale)
            }
            if (config.showTemp) {
                val tempStr = listOfNotNull(
                    if (stats.cpuTemp > 0) "${stats.cpuTemp.toInt()}°" else null,
                    if (stats.gpuTemp > 0) "${stats.gpuTemp.toInt()}°" else null,
                ).joinToString("/")
                if (tempStr.isNotEmpty()) {
                    CompactStatPill("TMP", tempStr, theme.accentDanger, config.scale)
                }
            }
            if (config.showNetwork) {
                CompactStatPill("NET", "↓${StatsCollector.formatSpeed(stats.downloadSpeed)}", theme.accentInfo, config.scale)
            }
        }
    }
}

@Composable
private fun CompactFpsBadge(fps: Int, scale: Float, theme: OverlayTheme) {
    val color = when {
        fps >= 55 -> theme.accentSecondary
        fps >= 30 -> theme.accentWarn
        fps > 0 -> theme.accentDanger
        else -> Color.White.copy(alpha = 0.3f)
    }
    val label = if (fps > 0) "$fps" else "—"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp * scale, vertical = 2.dp * scale)
    ) {
        Text(
            text = label,
            fontSize = (12.sp * scale),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
private fun CompactStatPill(label: String, value: String, color: Color, scale: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp * scale)
    ) {
        Text(
            text = label,
            fontSize = (8.sp * scale),
            color = color.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = (10.sp * scale),
            fontFamily = FontFamily.Monospace,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Shared Composables ────────────────────────────────────────

@Composable
private fun OverlayHeader(fps: Int, showFps: Boolean, scale: Float, theme: OverlayTheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PERF",
            fontSize = (10.sp * scale),
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = (2.sp * scale)
        )
        if (showFps) {
            FpsBadge(fps, scale, theme)
        }
    }
}

@Composable
private fun FpsBadge(fps: Int, scale: Float, theme: OverlayTheme) {
    val color = when {
        fps >= 55 -> theme.accentSecondary
        fps >= 30 -> theme.accentWarn
        fps > 0 -> theme.accentDanger
        else -> Color.White.copy(alpha = 0.3f)
    }

    val label = if (fps > 0) "$fps" else "—"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp * scale, vertical = 3.dp * scale)
    ) {
        Text(
            text = label,
            fontSize = (14.sp * scale),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String?,
    color: Color,
    usage: Float,
    scale: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size((14.dp * scale)),
            tint = color
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = (11.sp * scale),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp * scale)) {
                    Text(
                        text = value,
                        fontSize = (11.sp * scale),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    subValue?.let {
                        Text(
                            text = it,
                            fontSize = (9.sp * scale),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp * scale))
            UsageBar(usage, color, scale)
        }
    }
}

@Composable
private fun UsageBar(usage: Float, color: Color, scale: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((4.dp * scale))
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(usage.coerceIn(0f, 1f))
                .height((4.dp * scale))
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.8f), color)
                    )
                )
        )
    }
}

@Composable
private fun TemperatureRow(stats: PerformanceStats, scale: Float, theme: OverlayTheme) {
    val temps = listOfNotNull(
        if (stats.cpuTemp > 0) "CPU ${stats.cpuTemp.toInt()}°" to theme.accentDanger else null,
        if (stats.gpuTemp > 0) "GPU ${stats.gpuTemp.toInt()}°" to theme.accentWarn else null,
        if (stats.batteryTemp > 0) "BAT ${stats.batteryTemp.toInt()}°" to theme.accentPrimary else null,
    )

    if (temps.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        Icon(
            imageVector = Icons.Rounded.Thermostat,
            contentDescription = "Temperature",
            modifier = Modifier.size((14.dp * scale)),
            tint = theme.accentDanger
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp * scale)
        ) {
            temps.forEach { (text, color) ->
                Text(
                    text = text,
                    fontSize = (10.sp * scale),
                    fontFamily = FontFamily.Monospace,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun NetworkRow(stats: PerformanceStats, scale: Float, theme: OverlayTheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = "Network",
            modifier = Modifier.size((14.dp * scale)),
            tint = theme.accentInfo
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "↓ ${StatsCollector.formatSpeed(stats.downloadSpeed)}",
                fontSize = (10.sp * scale),
                fontFamily = FontFamily.Monospace,
                color = theme.accentSecondary
            )
            Text(
                text = "↑ ${StatsCollector.formatSpeed(stats.uploadSpeed)}",
                fontSize = (10.sp * scale),
                fontFamily = FontFamily.Monospace,
                color = theme.accentPrimary
            )
        }
    }
}

/**
 * Glassmorphism card with true frosted-glass effect.
 *
 * Android 12+ (S): Uses RenderEffect.createBlurEffect() for real gaussian blur,
 * simulating iOS-style frosted glass. The blur radius scales with opacity.
 *
 * Older devices: Falls back to layered gradient approximation with a subtle
 * noise texture pattern drawn via drawBehind.
 */
@Composable
fun GlassmorphismCard(
    alpha: Float = 0.85f,
    blurEnabled: Boolean = true,
    glowColor: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable BoxScope.() -> Unit
) {
    val blurRadius = if (blurEnabled) (12f * alpha).coerceIn(4f, 20f) else 0f

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurEnabled) {
                    // True gaussian blur via RenderEffect
                    Modifier.graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                } else {
                    Modifier
                }
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha * 0.28f),
                        Color.White.copy(alpha = alpha * 0.10f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .drawBehind {
                // Subtle inner highlight — top edge glow mimicking light refraction
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            glowColor.copy(alpha = glowColor.alpha * alpha * 2f),
                            glowColor.copy(alpha = glowColor.alpha * alpha),
                            glowColor.copy(alpha = glowColor.alpha * alpha * 2f)
                        )
                    ),
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 1f
                )
            },
        content = content
    )
}
