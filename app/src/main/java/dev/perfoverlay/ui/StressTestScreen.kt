package dev.perfoverlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import dev.perfoverlay.data.PerformanceStats
import dev.perfoverlay.ui.component.GlassmorphismCard
import dev.perfoverlay.ui.theme.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Stress test mode — controlled CPU/GPU load with real-time monitoring
 * and thermal ceiling detection.
 */
@Composable
fun StressTestScreen(
    statsProvider: () -> PerformanceStats,
    onBack: () -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var cpuCores by remember { mutableStateOf(4) }
    var testDurationSec by remember { mutableStateOf(60) }

    // Real-time data during test
    var elapsedSec by remember { mutableStateOf(0) }
    var currentStats by remember { mutableStateOf(PerformanceStats()) }
    var peakCpuTemp by remember { mutableStateOf(0f) }
    var peakGpuTemp by remember { mutableStateOf(0f) }
    var peakCpuFreq by remember { mutableStateOf(0L) }
    var minCpuFreq by remember { mutableStateOf(Long.MAX_VALUE) }
    var throttleEvents by remember { mutableStateOf(0) }
    var thermalCeilingFound by remember { mutableStateOf(false) }
    var thermalCeilingTemp by remember { mutableStateOf(0f) }

    // Test history for graphs
    val testHistory = remember { mutableListOf<PerformanceStats>() }

    // Stress workers
    val stressWorkers = remember { mutableListOf<Job>() }
    val scope = rememberCoroutineScope()
    val keepRunning = remember { AtomicBoolean(false) }

    fun startTest() {
        isRunning = true
        elapsedSec = 0
        peakCpuTemp = 0f
        peakGpuTemp = 0f
        peakCpuFreq = 0L
        minCpuFreq = Long.MAX_VALUE
        throttleEvents = 0
        thermalCeilingFound = false
        thermalCeilingTemp = 0f
        testHistory.clear()
        keepRunning.set(true)

        // Start CPU stress workers
        val workerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        stressWorkers.clear()

        repeat(cpuCores) {
            val job = workerScope.launch {
                // CPU-intensive loop: compute primes
                var n = 2L
                while (keepRunning.get() && isActive) {
                    var isPrime = true
                    val limit = sqrt(n.toDouble()).toLong()
                    for (i in 2..limit) {
                        if (n % i == 0L) {
                            isPrime = false
                            break
                        }
                    }
                    n++
                    if (n > 1_000_000) n = 2
                }
            }
            stressWorkers.add(job)
        }

        // Monitor stats
        val monitorJob = workerScope.launch {
            var prevCpuFreq = 0L
            while (keepRunning.get() && isActive && elapsedSec < testDurationSec) {
                val stats = statsProvider()
                currentStats = stats
                testHistory.add(stats)

                if (stats.cpuTemp > peakCpuTemp) peakCpuTemp = stats.cpuTemp
                if (stats.gpuTemp > peakGpuTemp) peakGpuTemp = stats.gpuTemp
                if (stats.cpuFrequency > peakCpuFreq) peakCpuFreq = stats.cpuFrequency
                if (stats.cpuFrequency > 0 && stats.cpuFrequency < minCpuFreq) minCpuFreq = stats.cpuFrequency

                // Detect throttling (frequency drop while hot)
                if (prevCpuFreq > 0 && stats.cpuFrequency > 0 &&
                    stats.cpuTemp > 60f &&
                    stats.cpuFrequency < prevCpuFreq * 0.85f
                ) {
                    throttleEvents++
                    if (!thermalCeilingFound) {
                        thermalCeilingFound = true
                        thermalCeilingTemp = stats.cpuTemp
                    }
                }
                prevCpuFreq = stats.cpuFrequency

                elapsedSec++
                delay(1000)
            }

            // Auto-stop when duration reached
            if (elapsedSec >= testDurationSec) {
                keepRunning.set(false)
                stressWorkers.forEach { it.cancel() }
                stressWorkers.clear()
                isRunning = false
            }
        }
        stressWorkers.add(monitorJob)
    }

    fun stopTest() {
        keepRunning.set(false)
        stressWorkers.forEach { it.cancel() }
        stressWorkers.clear()
        isRunning = false
    }

    DisposableEffect(Unit) {
        onDispose {
            keepRunning.set(false)
            stressWorkers.forEach { it.cancel() }
        }
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
            IconButton(onClick = {
                if (isRunning) stopTest()
                onBack()
            }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) AccentRed.copy(alpha = 0.2f) else AccentYellow.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isRunning) Icons.Rounded.Stop else Icons.Rounded.Bolt,
                    null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isRunning) AccentRed else AccentYellow
                )
            }
            Column {
                Text("Stress Test", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    if (isRunning) "Running: ${elapsedSec}s / ${testDurationSec}s" else "Controlled load generation",
                    fontSize = 12.sp,
                    color = if (isRunning) AccentRed.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Configuration (when not running)
            if (!isRunning) {
                item {
                    GlassmorphismCard(alpha = 0.9f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("CONFIGURATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.4f))

                            // CPU cores slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CPU Cores", color = Color.White, fontSize = 14.sp)
                                    Text("$cpuCores", color = AccentBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = cpuCores.toFloat(),
                                    onValueChange = { cpuCores = it.toInt() },
                                    valueRange = 1f..8f,
                                    steps = 6,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentBlue,
                                        activeTrackColor = AccentBlue
                                    )
                                )
                            }

                            // Duration
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Duration", color = Color.White, fontSize = 14.sp)
                                    Text("${testDurationSec}s", color = AccentBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = testDurationSec.toFloat(),
                                    onValueChange = { testDurationSec = it.toInt() },
                                    valueRange = 10f..300f,
                                    steps = 28,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentBlue,
                                        activeTrackColor = AccentBlue
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { startTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Stress Test")
                    }
                }
            }

            // Live stats during test
            if (isRunning) {
                item {
                    GlassmorphismCard(alpha = 0.9f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = AccentRed.copy(alpha = 0.7f))
                            LiveStatLine("CPU", "${currentStats.cpuUsage.toInt()}% @ ${currentStats.cpuFrequency} MHz ${currentStats.cpuGovernor}", AccentBlue)
                            LiveStatLine("GPU", "${currentStats.gpuUsage.toInt()}%", AccentYellow)
                            LiveStatLine("FPS", "${currentStats.fps}", if (currentStats.fps >= 55) AccentGreen else AccentRed)
                            LiveStatLine("CPU Temp", "${currentStats.cpuTemp.toInt()}°C", tempColor(currentStats.cpuTemp))
                            LiveStatLine("GPU Temp", "${currentStats.gpuTemp.toInt()}°C", tempColor(currentStats.gpuTemp))
                            if (currentStats.throttleState.isThrottling) {
                                LiveStatLine("Throttle", "⚠ DETECTED — CPU ↓${String.format("%.0f", currentStats.throttleState.cpuFreqDropPct)}%", AccentRed)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { stopTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Stop Test", color = Color.Black)
                    }
                }
            }

            // Results after test
            if (!isRunning && testHistory.isNotEmpty()) {
                item {
                    GlassmorphismCard(alpha = 0.9f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("RESULTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = AccentYellow)

                            ResultLine("Duration", "${elapsedSec}s")
                            ResultLine("Avg CPU", "${String.format("%.0f", testHistory.map { it.cpuUsage }.average())}%")
                            ResultLine("Avg GPU", "${String.format("%.0f", testHistory.map { it.gpuUsage }.average())}%")
                            ResultLine("Peak CPU Temp", "${String.format("%.0f", peakCpuTemp)}°C")
                            ResultLine("Peak GPU Temp", "${String.format("%.0f", peakGpuTemp)}°C")
                            ResultLine("CPU Freq Range", "${if (minCpuFreq == Long.MAX_VALUE) 0 else minCpuFreq}–${peakCpuFreq} MHz")
                            ResultLine("Throttle Events", "$throttleEvents")

                            if (thermalCeilingFound) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentRed.copy(alpha = 0.1f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Warning, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text("Thermal Ceiling Found", color = AccentRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            "Throttling began at ${String.format("%.0f", thermalCeilingTemp)}°C after ${elapsedSec}s",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            // Summary text
                            val summary = buildString {
                                append("Sustained ${String.format("%.0f", testHistory.map { it.cpuUsage }.average())}% CPU")
                                append(" at ${String.format("%.0f", peakCpuTemp)}°C")
                                if (thermalCeilingFound) {
                                    append(" before throttling to ${minCpuFreq}MHz")
                                }
                            }
                            Text(summary, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveStatLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        Text(value, fontSize = 12.sp, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
        Text(value, fontSize = 13.sp, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

private fun tempColor(temp: Float): Color = when {
    temp >= 80f -> AccentRed
    temp >= 65f -> AccentYellow
    temp >= 45f -> AccentBlue
    else -> AccentGreen
}
