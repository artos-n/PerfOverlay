package dev.perfoverlay.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.perfoverlay.data.*
import dev.perfoverlay.ui.component.*
import dev.perfoverlay.ui.theme.*
import dev.perfoverlay.util.AnomalyDetector
import dev.perfoverlay.util.ExportManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Recording screen — session list, recording controls, and session detail graphs.
 */
@Composable
fun RecordingScreen(
    recordingManager: RecordingManager,
    statsProvider: () -> PerformanceStats,
    configRefreshInterval: Long
) {
    val isRecording by recordingManager.isRecording.collectAsState()
    val currentSession by recordingManager.currentSession.collectAsState()
    val sessions by recordingManager.getAllSessions().collectAsState(initial = emptyList())

    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var showCompare by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // If compare mode, show compare screen
    if (showCompare) {
        SessionCompareScreen(
            recordingManager = recordingManager,
            onBack = { showCompare = false }
        )
        return
    }

    // If a session is selected, show its detail
    if (selectedSessionId != null) {
        SessionDetailView(
            sessionId = selectedSessionId!!,
            recordingManager = recordingManager,
            onBack = { selectedSessionId = null }
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) AccentRed.copy(alpha = 0.2f)
                        else GlassPurple.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isRecording) AccentRed else GlassPurple
                )
            }
            Column {
                Text(
                    text = "Recordings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isRecording) "Recording in progress..." else "Capture performance data",
                    fontSize = 13.sp,
                    color = if (isRecording) AccentRed.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Record button
        GlassmorphismCard(alpha = 0.9f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isRecording) {
                    // Recording in progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Recording: ${currentSession?.appName ?: "All Apps"}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Started ${formatTime(currentSession?.startTime ?: 0)}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Button(
                            onClick = {
                                scope.launch { recordingManager.stopRecording() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }
                } else {
                    // Start recording
                    Button(
                        onClick = {
                            scope.launch {
                                recordingManager.startRecording(
                                    intervalMs = configRefreshInterval,
                                    statsProvider = statsProvider
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.FiberManualRecord, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Recording")
                    }
                }
            }
        }

        // Sessions list
        if (sessions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("PAST RECORDINGS")
                if (sessions.size >= 2) {
                    TextButton(onClick = { showCompare = true }) {
                        Icon(Icons.Rounded.Compare, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentBlue)
                        Spacer(Modifier.width(4.dp))
                        Text("Compare", color = AccentBlue, fontSize = 12.sp)
                    }
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        onClick = { selectedSessionId = session.id },
                        onDelete = {
                            scope.launch { recordingManager.deleteSession(session.id) }
                        }
                    )
                }
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Timeline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.15f)
                    )
                    Text(
                        "No recordings yet",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp
                    )
                    Text(
                        "Start recording to capture performance data",
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: RecordingSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    GlassmorphismCard(alpha = 0.8f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        formatTime(session.startTime),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        "${session.sampleCount} samples",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    val duration = (session.endTime ?: System.currentTimeMillis()) - session.startTime
                    Text(
                        formatDurationShort(duration),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
                // Mini stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    MiniStat("FPS", String.format("%.0f", session.avgFps), AccentGreen)
                    MiniStat("CPU", String.format("%.0f%%", session.avgCpu), AccentBlue)
                    MiniStat("GPU", String.format("%.0f%%", session.avgGpu), AccentYellow)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 9.sp, color = color.copy(alpha = 0.7f))
        Text(value, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

/**
 * Detail view for a recording session — shows performance graphs.
 */
@Composable
private fun SessionDetailView(
    sessionId: Long,
    recordingManager: RecordingManager,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val samples by recordingManager.getSamplesForSession(sessionId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var exportMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back button + title + export
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Session Detail",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (samples.isNotEmpty()) {
                    Text(
                        "${samples.size} samples · ${formatDurationShort(samples.last().timestamp)}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            // Export buttons
            if (samples.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val sessionName = "Session_$sessionId"
                                val result = ExportManager.exportCsv(context, sessionName, samples)
                                exportMessage = if (result != null) "CSV exported ✓" else "Export failed"
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = "Export CSV", tint = AccentGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val sessionName = "Session_$sessionId"
                                val result = ExportManager.exportJson(context, sessionName, samples)
                                exportMessage = if (result != null) "JSON exported ✓" else "Export failed"
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = "Export JSON", tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Export toast
        exportMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2000)
                exportMessage = null
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (msg.contains("✓")) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(msg, color = Color.White, fontSize = 13.sp)
            }
        }

        if (samples.size < 2) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Not enough data points for graphs",
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
            return
        }

        // Graphs
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // FPS
            item {
                PerformanceGraph(
                    samples = samples,
                    valueExtractor = { it.fps.toFloat() },
                    maxValue = 144f,
                    label = "FPS",
                    lineColor = AccentGreen
                )
            }

            // Frame Time (P95)
            item {
                PerformanceGraph(
                    samples = samples,
                    valueExtractor = { it.p95FrameTimeMs },
                    maxValue = 50f,
                    label = "Frame Time (P95)",
                    unit = "ms",
                    lineColor = AccentYellow
                )
            }

            // CPU + GPU overlay
            item {
                MultiMetricGraph(
                    samples = samples,
                    series = listOf(
                        GraphSeries("CPU", AccentBlue) { it.cpuUsage },
                        GraphSeries("GPU", AccentYellow) { it.gpuUsage }
                    )
                )
            }

            // RAM
            item {
                val maxRam = (samples.maxOfOrNull { it.ramTotal } ?: 8192L).toFloat()
                PerformanceGraph(
                    samples = samples,
                    valueExtractor = { it.ramUsed.toFloat() },
                    maxValue = maxRam,
                    label = "RAM",
                    unit = " MB",
                    lineColor = GlassPurple
                )
            }

            // Temperatures
            item {
                MultiMetricGraph(
                    samples = samples,
                    series = listOf(
                        GraphSeries("CPU Temp", AccentRed, 100f) { it.cpuTemp },
                        GraphSeries("GPU Temp", AccentYellow, 100f) { it.gpuTemp },
                        GraphSeries("Battery", AccentBlue, 50f) { it.batteryTemp }
                    )
                )
            }

            // Network
            item {
                val maxNet = samples.maxOfOrNull { maxOf(it.downloadSpeed, it.uploadSpeed).toFloat() } ?: 1024f
                PerformanceGraph(
                    samples = samples,
                    valueExtractor = { it.downloadSpeed.toFloat() },
                    maxValue = maxNet.coerceAtLeast(1024f),
                    label = "Download",
                    lineColor = AccentGreen
                )
            }

            item {
                val maxNet = samples.maxOfOrNull { maxOf(it.downloadSpeed, it.uploadSpeed).toFloat() } ?: 1024f
                PerformanceGraph(
                    samples = samples,
                    valueExtractor = { it.uploadSpeed.toFloat() },
                    maxValue = maxNet.coerceAtLeast(1024f),
                    label = "Upload",
                    lineColor = AccentBlue
                )
            }

            // Dropped frames
            item {
                PerformanceGraph(
                    samples = samples,
                    valueExtractor = { it.droppedFrames.toFloat() },
                    maxValue = (samples.maxOfOrNull { it.droppedFrames } ?: 10).toFloat().coerceAtLeast(10f),
                    label = "Dropped Frames",
                    lineColor = AccentRed
                )
            }

            // Anomaly events
            item {
                AnomalyEventsSection(sessionId, recordingManager)
            }

            // Time axis
            item {
                TimeAxisLabels(samples = samples)
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// --- Helpers ---

private fun formatTime(epochMillis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}

private fun formatDurationShort(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = Color.White.copy(alpha = 0.4f),
        modifier = Modifier.padding(top = 4.dp)
    )
}

/**
 * Shows detected anomaly events for a recording session.
 */
@Composable
private fun AnomalyEventsSection(
    sessionId: Long,
    recordingManager: RecordingManager
) {
    val anomalies by recordingManager.getAnomalyEvents(sessionId).collectAsState(initial = emptyList())

    if (anomalies.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(12.dp)
        ) {
            Text(
                text = "Anomalies",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "No anomalies detected",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Anomalies",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "${anomalies.size} detected",
                fontSize = 10.sp,
                color = AccentYellow.copy(alpha = 0.7f)
            )
        }

        Spacer(Modifier.height(8.dp))

        anomalies.forEach { event ->
            val timeStr = AnomalyDetector.AnomalyEvent.formatTimestamp(event.timestamp)
            val direction = if (event.isSpike) "↑" else "↓"
            val color = when (event.metric) {
                "FPS" -> AccentGreen
                "CPU" -> AccentBlue
                "GPU" -> AccentYellow
                "Frame Time" -> AccentRed
                else -> Color.White
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.width(36.dp)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = "${event.metric} $direction",
                    fontSize = 10.sp,
                    color = color.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format("%.0f", event.value)} (avg: ${String.format("%.0f", event.baseline)})",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
