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
    val scope = rememberCoroutineScope()

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
            SectionLabel("PAST RECORDINGS")
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
    val samples by recordingManager.getSamplesForSession(sessionId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back button + title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column {
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
