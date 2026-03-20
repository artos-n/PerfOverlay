package dev.perfoverlay.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dev.perfoverlay.data.*
import dev.perfoverlay.service.OverlayService
import dev.perfoverlay.ui.component.GlassmorphismCard
import dev.perfoverlay.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Permission result handled in compose
    }

    private lateinit var configRepo: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configRepo = ConfigRepository(applicationContext)

        setContent {
            PerfOverlayTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000)
                ) {
                    MainScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        var hasOverlayPermission by remember {
            mutableStateOf(Settings.canDrawOverlays(this@MainActivity))
        }
        var isServiceRunning by remember { mutableStateOf(OverlayService.isRunning) }
        val config by configRepo.config.collectAsState(initial = OverlayConfig())

        // Live stats from service
        val liveStats by OverlayService.stats.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            HeaderSection()

            // Permission card
            if (!hasOverlayPermission) {
                PermissionCard {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                    hasOverlayPermission = Settings.canDrawOverlays(this@MainActivity)
                }
            }

            // Master toggle
            MasterToggle(
                isRunning = isServiceRunning,
                hasPermission = hasOverlayPermission
            ) { running ->
                if (running) {
                    OverlayService.start(this@MainActivity)
                } else {
                    OverlayService.stop(this@MainActivity)
                }
                isServiceRunning = running
            }

            // Live preview
            if (isServiceRunning) {
                LivePreviewCard(liveStats, config)
            }

            // Stats toggles
            StatsTogglesCard(config)

            // Appearance
            AppearanceCard(config)

            // Position
            PositionCard(config)

            // Refresh rate
            RefreshRateCard(config)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    @Composable
    private fun HeaderSection() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentBlue, AccentGreen)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
            Column {
                Text(
                    text = "PerfOverlay",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Real-time performance stats",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }

    @Composable
    private fun PermissionCard(onRequest: () -> Unit) {
        GlassmorphismCard(alpha = 0.9f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = AccentYellow
                    )
                    Column {
                        Text(
                            "Overlay permission required",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Tap to grant in Settings",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Grant")
                }
            }
        }
    }

    @Composable
    private fun MasterToggle(
        isRunning: Boolean,
        hasPermission: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        GlassmorphismCard(alpha = 0.9f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Performance Overlay",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (isRunning) "Active — monitoring stats" else "Inactive",
                        fontSize = 12.sp,
                        color = if (isRunning) AccentGreen else Color.White.copy(alpha = 0.5f)
                    )
                }
                Switch(
                    checked = isRunning,
                    onCheckedChange = { onToggle(it) },
                    enabled = hasPermission,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentGreen,
                        checkedTrackColor = AccentGreen.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }

    @Composable
    private fun LivePreviewCard(stats: PerformanceStats, config: OverlayConfig) {
        Text(
            "LIVE STATS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
        GlassmorphismCard(alpha = 0.85f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (config.showFps) {
                    LiveStatRow("FPS", "${stats.fps}", AccentGreen)
                }
                if (config.showCpu) {
                    LiveStatRow("CPU", "${stats.cpuUsage.toInt()}% @ ${stats.cpuFrequency} MHz", AccentBlue)
                }
                if (config.showGpu) {
                    LiveStatRow("GPU", "${stats.gpuUsage.toInt()}%", AccentGreen)
                }
                if (config.showRam) {
                    LiveStatRow("RAM", "${stats.ramUsed} / ${stats.ramTotal} MB", GlassPurple)
                }
                if (config.showTemp) {
                    val temps = listOfNotNull(
                        if (stats.cpuTemp > 0) "CPU ${stats.cpuTemp.toInt()}°" else null,
                        if (stats.gpuTemp > 0) "GPU ${stats.gpuTemp.toInt()}°" else null,
                        if (stats.batteryTemp > 0) "BAT ${stats.batteryTemp.toInt()}°" else null
                    ).joinToString("  ")
                    if (temps.isNotEmpty()) LiveStatRow("TEMP", temps, AccentRed)
                }
                if (config.showNetwork) {
                    LiveStatRow(
                        "NET",
                        "↓ ${dev.perfoverlay.util.StatsCollector.formatSpeed(stats.downloadSpeed)}  ↑ ${dev.perfoverlay.util.StatsCollector.formatSpeed(stats.uploadSpeed)}",
                        GlassBlue
                    )
                }
            }
        }
    }

    @Composable
    private fun LiveStatRow(label: String, value: String, color: Color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 12.sp, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }

    @Composable
    private fun StatsTogglesCard(config: OverlayConfig) {
        SectionLabel("STATS")
        GlassmorphismCard(alpha = 0.8f) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                StatToggle("FPS", Icons.Rounded.PlayArrow, config.showFps) {
                    updateConfig(config.copy(showFps = it))
                }
                StatToggle("CPU", Icons.Rounded.Memory, config.showCpu) {
                    updateConfig(config.copy(showCpu = it))
                }
                StatToggle("GPU", Icons.Rounded.Games, config.showGpu) {
                    updateConfig(config.copy(showGpu = it))
                }
                StatToggle("RAM", Icons.Rounded.Storage, config.showRam) {
                    updateConfig(config.copy(showRam = it))
                }
                StatToggle("Temperature", Icons.Rounded.Thermostat, config.showTemp) {
                    updateConfig(config.copy(showTemp = it))
                }
                StatToggle("Network", Icons.Rounded.Wifi, config.showNetwork) {
                    updateConfig(config.copy(showNetwork = it))
                }
            }
        }
    }

    @Composable
    private fun StatToggle(label: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                Text(label, color = Color.White, fontSize = 14.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentBlue,
                    checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                )
            )
        }
    }

    @Composable
    private fun AppearanceCard(config: OverlayConfig) {
        SectionLabel("APPEARANCE")
        GlassmorphismCard(alpha = 0.8f) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Opacity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Opacity", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text("${(config.opacity * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                }
                Slider(
                    value = config.opacity,
                    onValueChange = { updateConfig(config.copy(opacity = it)) },
                    valueRange = 0.3f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentBlue,
                        activeTrackColor = AccentBlue
                    )
                )

                // Scale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scale", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text("${(config.scale * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                }
                Slider(
                    value = config.scale,
                    onValueChange = { updateConfig(config.copy(scale = it)) },
                    valueRange = 0.5f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGreen,
                        activeTrackColor = AccentGreen
                    )
                )
            }
        }
    }

    @Composable
    private fun PositionCard(config: OverlayConfig) {
        SectionLabel("POSITION")
        GlassmorphismCard(alpha = 0.8f) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val positions = OverlayPosition.entries
                positions.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { pos ->
                            val isSelected = config.position == pos
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) AccentBlue.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .clickable { updateConfig(config.copy(position = pos)) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    pos.name.replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    color = if (isSelected) AccentBlue else Color.White.copy(alpha = 0.6f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    @Composable
    private fun RefreshRateCard(config: OverlayConfig) {
        SectionLabel("REFRESH RATE")
        GlassmorphismCard(alpha = 0.8f) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rates = listOf(500L to "0.5s", 1000L to "1s", 2000L to "2s", 3000L to "3s")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rates.forEach { (ms, label) ->
                        val isSelected = config.refreshIntervalMs == ms
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) AccentBlue.copy(alpha = 0.3f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable { updateConfig(config.copy(refreshIntervalMs = ms)) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                color = if (isSelected) AccentBlue else Color.White.copy(alpha = 0.6f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    private fun updateConfig(newConfig: OverlayConfig) {
        lifecycleScope.launch {
            configRepo.updateConfig(newConfig)
        }
    }
}
