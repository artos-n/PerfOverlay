package dev.perfoverlay.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.perfoverlay.data.*
import dev.perfoverlay.ui.component.GlassmorphismCard
import dev.perfoverlay.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Session comparison screen — select two sessions and see their deltas.
 */
@Composable
fun SessionCompareScreen(
    recordingManager: RecordingManager,
    onBack: () -> Unit
) {
    val sessions by recordingManager.getAllSessions().collectAsState(initial = emptyList())

    var sessionA by remember { mutableStateOf<RecordingSession?>(null) }
    var sessionB by remember { mutableStateOf<RecordingSession?>(null) }

    // If both selected, show comparison
    if (sessionA != null && sessionB != null) {
        SessionComparisonView(
            sessionA = sessionA!!,
            sessionB = sessionB!!,
            recordingManager = recordingManager,
            onChangeA = { sessionA = null },
            onChangeB = { sessionB = null },
            onBack = onBack
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column {
                Text(
                    text = "Compare Sessions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Select two sessions to compare",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Selection state
        GlassmorphismCard(alpha = 0.9f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SelectionSlot("A", sessionA, AccentBlue) { sessionA = null }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
                SelectionSlot("B", sessionB, AccentGreen) { sessionB = null }
            }
        }

        // Session list
        SectionLabel("RECORDINGS")
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(sessions) { session ->
                val isSelected = session.id == sessionA?.id || session.id == sessionB?.id
                val slotColor = when (session.id) {
                    sessionA?.id -> AccentBlue
                    sessionB?.id -> AccentGreen
                    else -> null
                }

                GlassmorphismCard(
                    alpha = if (isSelected) 0.95f else 0.7f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isSelected) {
                                    if (sessionA == null) sessionA = session
                                    else if (sessionB == null) sessionB = session
                                }
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                session.appName,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                "${formatTime(session.startTime)} · ${session.sampleCount} samples",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        if (slotColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(slotColor.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (session.id == sessionA?.id) "A" else "B",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slotColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionSlot(
    label: String,
    session: RecordingSession?,
    color: Color,
    onClear: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (session != null) color.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                .then(if (session != null) Modifier.clickable { onClear() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (session != null) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            } else {
                Text("?", fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f))
            }
        }
        Text(
            session?.let { truncateAppName(it.appName, 12) } ?: "Select",
            fontSize = 10.sp,
            color = if (session != null) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)
        )
    }
}

/**
 * Side-by-side comparison with delta visualization.
 */
@Composable
private fun SessionComparisonView(
    sessionA: RecordingSession,
    sessionB: RecordingSession,
    recordingManager: RecordingManager,
    onChangeA: () -> Unit,
    onChangeB: () -> Unit,
    onBack: () -> Unit
) {
    val samplesA by recordingManager.getSamplesForSession(sessionA.id).collectAsState(initial = emptyList())
    val samplesB by recordingManager.getSamplesForSession(sessionB.id).collectAsState(initial = emptyList())

    if (samplesA.isEmpty() || samplesB.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading samples...", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    // Compute deltas
    val avgFpsA = samplesA.map { it.fps }.average().toFloat()
    val avgFpsB = samplesB.map { it.fps }.average().toFloat()
    val fpsDelta = ((avgFpsB - avgFpsA) / avgFpsA.coerceAtLeast(1f) * 100f)

    val avgCpuA = samplesA.map { it.cpuUsage }.average().toFloat()
    val avgCpuB = samplesB.map { it.cpuUsage }.average().toFloat()
    val cpuDelta = ((avgCpuB - avgCpuA) / avgCpuA.coerceAtLeast(1f) * 100f)

    val avgGpuA = samplesA.map { it.gpuUsage }.average().toFloat()
    val avgGpuB = samplesB.map { it.gpuUsage }.average().toFloat()
    val gpuDelta = ((avgGpuB - avgGpuA) / avgGpuA.coerceAtLeast(1f) * 100f)

    val avgFtA = samplesA.map { it.avgFrameTimeMs }.average().toFloat()
    val avgFtB = samplesB.map { it.avgFrameTimeMs }.average().toFloat()
    val ftDelta = ((avgFtB - avgFtA) / avgFtA.coerceAtLeast(1f) * 100f)

    val dropsA = samplesA.maxOfOrNull { it.droppedFrames } ?: 0
    val dropsB = samplesB.maxOfOrNull { it.droppedFrames } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Comparison", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "${truncateAppName(sessionA.appName, 15)} vs ${truncateAppName(sessionB.appName, 15)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Delta summary
            item {
                GlassmorphismCard(alpha = 0.9f) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("DELTA SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.4f))
                        DeltaRow("FPS", avgFpsA, avgFpsB, fpsDelta, true)
                        DeltaRow("CPU", avgCpuA, avgCpuB, cpuDelta, false)
                        DeltaRow("GPU", avgGpuA, avgGpuB, gpuDelta, false)
                        DeltaRow("Frame Time", avgFtA, avgFtB, ftDelta, false)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dropped Frames", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("A: $dropsA", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AccentBlue)
                                Text("B: $dropsB", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AccentGreen)
                            }
                        }
                    }
                }
            }

            // FPS overlay graph
            item {
                Text("FPS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
                OverlayGraph(
                    samplesA = samplesA,
                    samplesB = samplesB,
                    valueExtractor = { it.fps.toFloat() },
                    maxValue = 144f,
                    colorA = AccentBlue,
                    colorB = AccentGreen
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("A · ${truncateAppName(sessionA.appName, 12)}", AccentBlue)
                    LegendItem("B · ${truncateAppName(sessionB.appName, 12)}", AccentGreen)
                }
            }

            // CPU overlay
            item {
                Text("CPU Usage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
                OverlayGraph(
                    samplesA = samplesA,
                    samplesB = samplesB,
                    valueExtractor = { it.cpuUsage },
                    maxValue = 100f,
                    colorA = AccentBlue,
                    colorB = AccentGreen
                )
            }

            // GPU overlay
            item {
                Text("GPU Usage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
                OverlayGraph(
                    samplesA = samplesA,
                    samplesB = samplesB,
                    valueExtractor = { it.gpuUsage },
                    maxValue = 100f,
                    colorA = AccentBlue,
                    colorB = AccentGreen
                )
            }

            // Frame Time overlay
            item {
                Text("Frame Time (P95)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
                OverlayGraph(
                    samplesA = samplesA,
                    samplesB = samplesB,
                    valueExtractor = { it.p95FrameTimeMs },
                    maxValue = 50f,
                    colorA = AccentBlue,
                    colorB = AccentGreen
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun DeltaRow(
    label: String,
    valueA: Float,
    valueB: Float,
    deltaPct: Float,
    higherIsBetter: Boolean
) {
    val isImproved = if (higherIsBetter) deltaPct > 0 else deltaPct < 0
    val deltaColor = when {
        kotlin.math.abs(deltaPct) < 2f -> Color.White.copy(alpha = 0.5f)
        isImproved -> AccentGreen
        else -> AccentRed
    }
    val arrow = when {
        kotlin.math.abs(deltaPct) < 2f → "="
        deltaPct > 0 → "↑"
        else → "↓"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${String.format("%.0f", valueA)}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = AccentBlue
            )
            Text("→", fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
            Text(
                "${String.format("%.0f", valueB)}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = AccentGreen
            )
            Text(
                "$arrow${String.format("%.1f", kotlin.math.abs(deltaPct))}%",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = deltaColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Overlaid line graph comparing two sessions.
 */
@Composable
private fun OverlayGraph(
    samplesA: List<StatSample>,
    samplesB: List<StatSample>,
    valueExtractor: (StatSample) -> Float,
    maxValue: Float,
    colorA: Color,
    colorB: Color
) {
    // Normalize both to same length (downsample longer one)
    val targetLen = minOf(samplesA.size, samplesB.size, 60)
    val valuesA = downsample(samplesA.map { valueExtractor(it).coerceIn(0f, maxValue) }, targetLen)
    val valuesB = downsample(samplesB.map { valueExtractor(it).coerceIn(0f, maxValue) }, targetLen)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (targetLen - 1).coerceAtLeast(1)

        // Grid
        for (i in 1 until 4) {
            val y = height * i / 4
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw series
        listOf(valuesA to colorA, valuesB to colorB).forEach { (values, color) ->
            if (values.size < 2) return@forEach
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value / maxValue * height)
                if (index == 0) path.moveTo(x, y)
                else {
                    val prevX = (index - 1) * stepX
                    val prevY = height - (values[index - 1] / maxValue * height)
                    val midX = (prevX + x) / 2f
                    path.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2f)
                }
            }
            val lastX = (values.size - 1) * stepX
            val lastY = height - (values.last() / maxValue * height)
            path.lineTo(lastX, lastY)

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(color = color, radius = 3f, center = Offset(lastX, lastY))
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

private fun downsample(values: List<Float>, targetSize: Int): List<Float> {
    if (values.size <= targetSize) return values
    val step = values.size.toFloat() / targetSize
    return (0 until targetSize).map { i ->
        val start = (i * step).toInt()
        val end = ((i + 1) * step).toInt().coerceAtMost(values.size)
        values.subList(start, end).average().toFloat()
    }
}

private fun formatTime(epochMillis: Long): String {
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(epochMillis))
}

private fun truncateAppName(name: String, maxLen: Int): String {
    return if (name.length <= maxLen) name else name.take(maxLen - 1) + "…"
}

private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = Color.White.copy(alpha = 0.4f)
    )
}
