package dev.perfoverlay.ui.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.perfoverlay.data.OverlayConfig
import dev.perfoverlay.data.PerformanceStats
import dev.perfoverlay.ui.theme.*
import dev.perfoverlay.util.StatsCollector
import kotlinx.coroutines.flow.StateFlow

@Composable
fun OverlayView(
    stats: StateFlow<PerformanceStats>,
    config: StateFlow<OverlayConfig>
) {
    val currentStats by stats.collectAsState()
    val currentConfig by config.collectAsState()

    GlassmorphismCard(alpha = currentConfig.opacity) {
        Column(
            modifier = Modifier
                .widthIn(min = 160.dp)
                .padding(10.dp * currentConfig.scale),
            verticalArrangement = Arrangement.spacedBy(6.dp * currentConfig.scale)
        ) {
            // Header
            OverlayHeader(currentStats.fps, currentConfig.showFps, currentConfig.scale)

            // Stats rows
            if (currentConfig.showCpu) {
                StatRow(
                    icon = Icons.Rounded.Memory,
                    label = "CPU",
                    value = "${currentStats.cpuUsage.toInt()}%",
                    subValue = "${currentStats.cpuFrequency} MHz",
                    color = AccentBlue,
                    usage = currentStats.cpuUsage / 100f,
                    scale = currentConfig.scale
                )
            }

            if (currentConfig.showGpu) {
                StatRow(
                    icon = Icons.Rounded.Games,
                    label = "GPU",
                    value = "${currentStats.gpuUsage.toInt()}%",
                    subValue = null,
                    color = AccentGreen,
                    usage = currentStats.gpuUsage / 100f,
                    scale = currentConfig.scale
                )
            }

            if (currentConfig.showRam) {
                StatRow(
                    icon = Icons.Rounded.Storage,
                    label = "RAM",
                    value = "${currentStats.ramUsed}MB",
                    subValue = "/ ${currentStats.ramTotal}MB",
                    color = GlassPurple,
                    usage = currentStats.ramUsed.toFloat() / currentStats.ramTotal.coerceAtLeast(1),
                    scale = currentConfig.scale
                )
            }

            if (currentConfig.showTemp) {
                TemperatureRow(currentStats, currentConfig.scale)
            }

            if (currentConfig.showNetwork) {
                NetworkRow(currentStats, currentConfig.scale)
            }
        }
    }
}

@Composable
private fun OverlayHeader(fps: Int, showFps: Boolean, scale: Float) {
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
            FpsBadge(fps, scale)
        }
    }
}

@Composable
private fun FpsBadge(fps: Int, scale: Float) {
    val color = when {
        fps >= 55 -> AccentGreen
        fps >= 30 -> AccentYellow
        else -> AccentRed
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp * scale, vertical = 3.dp * scale)
    ) {
        Text(
            text = "$fps",
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

            // Usage bar
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
private fun TemperatureRow(stats: PerformanceStats, scale: Float) {
    val temps = listOfNotNull(
        if (stats.cpuTemp > 0) "CPU ${stats.cpuTemp.toInt()}°" to AccentRed else null,
        if (stats.gpuTemp > 0) "GPU ${stats.gpuTemp.toInt()}°" to AccentYellow else null,
        if (stats.batteryTemp > 0) "BAT ${stats.batteryTemp.toInt()}°" to AccentBlue else null,
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
            tint = AccentRed
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
private fun NetworkRow(stats: PerformanceStats, scale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = "Network",
            modifier = Modifier.size((14.dp * scale)),
            tint = GlassBlue
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "↓ ${StatsCollector.formatSpeed(stats.downloadSpeed)}",
                fontSize = (10.sp * scale),
                fontFamily = FontFamily.Monospace,
                color = AccentGreen
            )
            Text(
                text = "↑ ${StatsCollector.formatSpeed(stats.uploadSpeed)}",
                fontSize = (10.sp * scale),
                fontFamily = FontFamily.Monospace,
                color = AccentBlue
            )
        }
    }
}

@Composable
fun GlassmorphismCard(
    alpha: Float = 0.85f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha * 0.25f),
                        Color.White.copy(alpha = alpha * 0.08f)
                    )
                )
            )
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier
                } else {
                    Modifier
                }
            )
            .padding(1.dp),
        content = content
    )
}
