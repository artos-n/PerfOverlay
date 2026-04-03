package dev.perfoverlay.ui.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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

            if (config.showFrameTime && config.showFps) {
                FrameTimeStrip(
                    frameTimeStrip = stats.frameTimeStrip,
                    avgFrameTimeMs = stats.avgFrameTimeMs,
                    p95FrameTimeMs = stats.p95FrameTimeMs,
                    droppedFrames = stats.droppedFrames,
                    scale = config.scale,
                    theme = theme
                )
            }

            if (config.showCpu) {
                StatRow(
                    icon = Icons.Rounded.Memory,
                    label = stringResource(R.string.stat_cpu),
                    value = "${stats.cpuUsage.toInt()}%",
                    subValue = if (stats.cpuFrequency > 0) "${stats.cpuFrequency} MHz${if (stats.cpuGovernor.isNotEmpty()) " ${stats.cpuGovernor}" else ""}" else null,
                    color = theme.accentPrimary,
                    usage = stats.cpuUsage / 100f,
                    scale = config.scale
                )
                // Per-core CPU bars
                val activeCores = stats.perCoreUsage.filter { it > 0f }
                if (activeCores.isNotEmpty()) {
                    PerCoreUsageBars(activeCores, config.scale, theme)
                }
            }

            if (config.showGpu) {
                val gpuSub = if (stats.gpuFrequency > 0) "@ ${stats.gpuFrequency} MHz" else null
                StatRow(
                    icon = Icons.Rounded.Games,
                    label = stringResource(R.string.stat_gpu),
                    value = "${stats.gpuUsage.toInt()}%",
                    subValue = gpuSub,
                    color = theme.accentSecondary,
                    usage = stats.gpuUsage / 100f,
                    scale = config.scale
                )
            }

            if (config.showRam) {
                StatRow(
                    icon = Icons.Rounded.Storage,
                    label = stringResource(R.string.stat_ram),
                    value = "${stats.ramUsed}MB",
                    subValue = "/ ${stats.ramTotal}MB",
                    color = theme.accentInfo,
                    usage = stats.ramUsed.toFloat() / stats.ramTotal.coerceAtLeast(1),
                    scale = config.scale
                )
            }

            if (config.showBattery) {
                BatteryRow(stats, config.scale, theme)
            }

            if (config.showTemp) {
                TemperatureRow(stats, config.scale, theme)
            }

            if (config.showNetwork) {
                NetworkRow(stats, config.scale, theme)
            }

            if (config.showStorage) {
                StorageRow(stats, config.scale, theme)
            }

            // Throttle warning
            if (stats.throttleState.isThrottling) {
                ThrottleWarningRow(stats.throttleState, config.scale, theme)
            }

            // Anomaly count
            if (stats.anomalyCount > 0) {
                AnomalyCountRow(stats.anomalyCount, config.scale, theme)
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

            // Throttle warning in compact mode
            if (stats.throttleState.isThrottling) {
                CompactStatPill("⚠", "THROT", theme.accentDanger, config.scale)
            }

            // Stat pills
            if (config.showCpu) {
                CompactStatPill(stringResource(R.string.stat_cpu), "${stats.cpuUsage.toInt()}%", theme.accentPrimary, config.scale)
            }
            if (config.showGpu) {
                CompactStatPill(stringResource(R.string.stat_gpu), "${stats.gpuUsage.toInt()}%", theme.accentSecondary, config.scale)
            }
            if (config.showRam) {
                CompactStatPill(stringResource(R.string.stat_ram), "${stats.ramUsed}MB", theme.accentInfo, config.scale)
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
            if (config.showBattery) {
                val chargeIcon = if (stats.isCharging) "⚡" else ""
                CompactStatPill("BAT", "${stats.batteryLevel}%$chargeIcon", theme.accentInfo, config.scale)
            }
            if (config.showStorage && (stats.storageReadSpeed > 0 || stats.storageWriteSpeed > 0)) {
                CompactStatPill("IO", "↓${StatsCollector.formatStorageSpeed(stats.storageReadSpeed)}", theme.accentSecondary, config.scale)
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
            text = stringResource(R.string.overlay_perf),
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
            contentDescription = stringResource(R.string.cd_temperature),
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
            contentDescription = stringResource(R.string.stat_network),
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

@Composable
private fun BatteryRow(stats: PerformanceStats, scale: Float, theme: OverlayTheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        Icon(
            imageVector = if (stats.isCharging) Icons.Rounded.Bolt else Icons.Rounded.BatteryFull,
            contentDescription = stringResource(R.string.cd_battery),
            modifier = Modifier.size((14.dp * scale)),
            tint = if (stats.isCharging) theme.accentSecondary else theme.accentInfo
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BAT",
                    fontSize = (11.sp * scale),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp * scale)) {
                    Text(
                        text = "${stats.batteryLevel}%",
                        fontSize = (11.sp * scale),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    if (stats.isCharging && stats.chargeRate != 0f) {
                        Text(
                            text = "${if (stats.chargeRate > 0) "+" else ""}${stats.chargeRate.toInt()}mA",
                            fontSize = (9.sp * scale),
                            color = if (stats.chargeRate > 0) theme.accentSecondary else theme.accentDanger,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp * scale))
            UsageBar(stats.batteryLevel / 100f, if (stats.isCharging) theme.accentSecondary else theme.accentInfo, scale)
        }
    }
}

@Composable
private fun PerCoreUsageBars(cores: List<Float>, scale: Float, theme: OverlayTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (22.dp * scale)), // align with stat row content
        horizontalArrangement = Arrangement.spacedBy(3.dp * scale)
    ) {
        cores.forEachIndexed { index, usage ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((16.dp * scale))
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((usage / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        theme.accentPrimary,
                                        theme.accentPrimary.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .align(Alignment.BottomCenter)
                    )
                }
                Text(
                    text = "C$index",
                    fontSize = (6.sp * scale),
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

/**
 * Thin micro-graph strip showing per-frame durations below the FPS badge.
 * Highlights dropped frames (red spikes) and shows P95 frame time.
 * Only visible when showFrameTime is enabled in config.
 */
@Composable
private fun FrameTimeStrip(
    frameTimeStrip: FloatArray,
    avgFrameTimeMs: Float,
    p95FrameTimeMs: Float,
    droppedFrames: Int,
    scale: Float,
    theme: OverlayTheme
) {
    if (frameTimeStrip.size < 2) return

    val stripHeight = 20.dp * scale
    val hasDrops = droppedFrames > 0

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp * scale)
    ) {
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.overlay_ft),
                fontSize = (7.sp * scale),
                color = Color.White.copy(alpha = 0.35f),
                fontWeight = FontWeight.Medium,
                letterSpacing = (1.sp * scale)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp * scale)) {
                Text(
                    text = "avg ${String.format("%.1f", avgFrameTimeMs)}ms",
                    fontSize = (7.sp * scale),
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = "p95 ${String.format("%.1f", p95FrameTimeMs)}ms",
                    fontSize = (7.sp * scale),
                    fontFamily = FontFamily.Monospace,
                    color = if (p95FrameTimeMs > 33f) theme.accentDanger else Color.White.copy(alpha = 0.4f)
                )
                if (hasDrops) {
                    Text(
                        text = "▼$droppedFrames",
                        fontSize = (7.sp * scale),
                        fontFamily = FontFamily.Monospace,
                        color = theme.accentDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Strip chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(stripHeight)
                .clip(RoundedCornerShape(4.dp * scale))
                .background(Color.White.copy(alpha = 0.04f))
        ) {
            val width = size.width
            val height = size.height
            val data = frameTimeStrip
            if (data.size < 2) return@Canvas

            // Y-axis: 0ms at bottom, cap at 50ms (3 frames at 60Hz)
            val maxDisplayMs = 50f
            val stepX = width / (data.size - 1).coerceAtLeast(1)

            // Vsync line at 16.67ms
            val vsyncY = height - (16.67f / maxDisplayMs * height)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, vsyncY),
                end = Offset(width, vsyncY),
                strokeWidth = 1f
            )

            // Draw the frame time line
            val path = Path()
            data.forEachIndexed { index, frameTimeMs ->
                val x = index * stepX
                val y = height - (frameTimeMs.coerceIn(0f, maxDisplayMs) / maxDisplayMs * height)
                if (index == 0) path.moveTo(x, y)
                else {
                    val prevX = (index - 1) * stepX
                    val prevY = height - (data[index - 1].coerceIn(0f, maxDisplayMs) / maxDisplayMs * height)
                    val midX = (prevX + x) / 2f
                    path.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2f)
                }
            }
            val lastX = (data.size - 1) * stepX
            val lastY = height - (data.last().coerceIn(0f, maxDisplayMs) / maxDisplayMs * height)
            path.lineTo(lastX, lastY)

            drawPath(
                path = path,
                color = theme.accentSecondary.copy(alpha = 0.8f),
                style = Stroke(
                    width = 1.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Highlight dropped frames (>33.3ms = 2× vsync) as red dots
            data.forEachIndexed { index, frameTimeMs ->
                if (frameTimeMs > 33.3f) {
                    val x = index * stepX
                    val y = height - (frameTimeMs.coerceIn(0f, maxDisplayMs) / maxDisplayMs * height)
                    drawCircle(
                        color = theme.accentDanger.copy(alpha = 0.9f),
                        radius = 2.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

/**
 * Throttle warning indicator — pulsing red alert when thermal throttling is detected.
 */
@Composable
private fun ThrottleWarningRow(
    throttleState: dev.perfoverlay.util.ThrottleState,
    scale: Float,
    theme: OverlayTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp * scale))
            .background(theme.accentDanger.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp * scale, vertical = 4.dp * scale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp * scale)
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = "Throttling",
            modifier = Modifier.size((12.dp * scale)),
            tint = theme.accentDanger
        )
        Text(
            text = stringResource(R.string.overlay_throttled),
            fontSize = (8.sp * scale),
            fontWeight = FontWeight.Bold,
            color = theme.accentDanger,
            letterSpacing = (1.sp * scale)
        )
        Text(
            text = "CPU ↓${String.format("%.0f", throttleState.cpuFreqDropPct)}%",
            fontSize = (8.sp * scale),
            fontFamily = FontFamily.Monospace,
            color = theme.accentDanger.copy(alpha = 0.8f)
        )
        if (throttleState.cpuTemp > 0) {
            Text(
                text = "${throttleState.cpuTemp.toInt()}°C",
                fontSize = (8.sp * scale),
                fontFamily = FontFamily.Monospace,
                color = theme.accentWarn
            )
        }
    }
}

/**
 * Anomaly count badge — shows detected anomaly events.
 */
@Composable
private fun AnomalyCountRow(
    count: Int,
    scale: Float,
    theme: OverlayTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = stringResource(R.string.anomalies),
            modifier = Modifier.size((10.dp * scale)),
            tint = theme.accentWarn.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(3.dp * scale))
        Text(
            text = "$count anomaly${if (count != 1) "s" else ""}",
            fontSize = (7.sp * scale),
            fontFamily = FontFamily.Monospace,
            color = theme.accentWarn.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StorageRow(stats: PerformanceStats, scale: Float, theme: OverlayTheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        Icon(
            imageVector = Icons.Rounded.Storage,
            contentDescription = stringResource(R.string.stat_storage_io),
            modifier = Modifier.size((14.dp * scale)),
            tint = theme.accentInfo
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STORAGE",
                    fontSize = (11.sp * scale),
                    color = Color.White.copy(alpha = 0.7f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp * scale)) {
                    Text(
                        text = "↓ ${StatsCollector.formatStorageSpeed(stats.storageReadSpeed)}",
                        fontSize = (10.sp * scale),
                        fontFamily = FontFamily.Monospace,
                        color = theme.accentSecondary
                    )
                    Text(
                        text = "↑ ${StatsCollector.formatStorageSpeed(stats.storageWriteSpeed)}",
                        fontSize = (10.sp * scale),
                        fontFamily = FontFamily.Monospace,
                        color = theme.accentPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp * scale))
            // Combined I/O bar
            val totalIO = (stats.storageReadSpeed + stats.storageWriteSpeed).coerceAtLeast(1)
            val readFraction = stats.storageReadSpeed.toFloat() / totalIO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((4.dp * scale))
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(readFraction.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(theme.accentSecondary.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(stats.storageWriteSpeed.toFloat() / totalIO.coerceAtLeast(1))
                        .clip(RoundedCornerShape(2.dp))
                        .background(theme.accentPrimary.copy(alpha = 0.7f))
                )
            }
        }
    }
}

/** *
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
