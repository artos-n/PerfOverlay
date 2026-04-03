package dev.perfoverlay.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import dev.perfoverlay.PerfOverlayApp
import dev.perfoverlay.data.*
import dev.perfoverlay.service.OverlayService
import dev.perfoverlay.service.ShizukuHelper
import dev.perfoverlay.ui.component.GlassmorphismCard
import dev.perfoverlay.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private lateinit var configRepo: ConfigRepository
    private lateinit var recordingManager: RecordingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configRepo = ConfigRepository(applicationContext)
        recordingManager = RecordingManager(applicationContext)
        ShizukuHelper.init(this)

        setContent {
            PerfOverlayTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000)
                ) {
                    MainApp()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuHelper.destroy()
        recordingManager.destroy()
    }

    enum class Tab(val icon: ImageVector, val label: String) {
        OVERLAY(Icons.Rounded.Speed, "Overlay"),
        RECORD(Icons.Rounded.Timeline, "Record"),
        TEST(Icons.Rounded.Bolt, "Stress")
    }

    @Composable
    fun MainApp() {
        var selectedTab by remember { mutableStateOf(Tab.OVERLAY) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    Tab.OVERLAY -> OverlaySettingsScreen()
                    Tab.RECORD -> RecordingScreen(
                        recordingManager = recordingManager,
                        statsProvider = { OverlayService.stats.value },
                        configRefreshInterval = configRepo.config
                            .collectAsState(initial = OverlayConfig()).value
                            .refreshIntervalMs
                    )
                    Tab.TEST -> StressTestScreen(
                        statsProvider = { OverlayService.stats.value },
                        onBack = { selectedTab = Tab.OVERLAY }
                    )
                }
            }

            // Bottom tab bar
            BottomTabBar(selectedTab) { selectedTab = it }
        }
    }

    @Composable
    private fun BottomTabBar(selected: Tab, onSelect: (Tab) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .padding(vertical = 8.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Tab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) AccentBlue else Color.White.copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        color = if (isSelected) AccentBlue else Color.White.copy(alpha = 0.35f),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }

    // ─── Overlay Settings Screen ───────────────────────────────

    @Composable
    fun OverlaySettingsScreen() {
        var hasOverlayPermission by remember {
            mutableStateOf(Settings.canDrawOverlays(this@MainActivity))
        }
        var isServiceRunning by remember { mutableStateOf(OverlayService.isRunning) }
        val config by configRepo.config.collectAsState(initial = OverlayConfig())
        val shizukuState by ShizukuHelper.state.collectAsState()
        val liveStats by OverlayService.stats.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderSection()

            if (!hasOverlayPermission) {
                PermissionCard(
                    shizukuState = shizukuState,
                    onGrantDirect = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${packageName}")
                        )
                        overlayPermissionLauncher.launch(intent)
                        hasOverlayPermission = Settings.canDrawOverlays(this@MainActivity)
                    },
                    onGrantShizuku = {
                        ShizukuHelper.grantOverlayPermission { granted ->
                            if (granted) {
                                hasOverlayPermission = Settings.canDrawOverlays(this@MainActivity)
                            }
                        }
                    }
                )
            }

            MasterToggle(
                isRunning = isServiceRunning,
                hasPermission = hasOverlayPermission
            ) { running ->
                if (running) OverlayService.start(this@MainActivity)
                else OverlayService.stop(this@MainActivity)
                isServiceRunning = running
            }

            if (isServiceRunning) {
                LivePreviewCard(liveStats, config)
            }

            StatsTogglesCard(config)
            AppearanceCard(config)
            ThemePickerCard(config)
            PositionCard(config)
            RefreshRateCard(config)

            if (shizukuState != ShizukuHelper.State.NOT_INSTALLED) {
                ShizukuStatusCard(shizukuState)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // ─── Composable Sections ───────────────────────────────────

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
                    .background(Brush.linearGradient(colors = listOf(AccentBlue, AccentGreen))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(28.dp), tint = Color.White)
            }
            Column {
                Text("PerfOverlay", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Real-time performance stats", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }

    @Composable
    private fun PermissionCard(
        shizukuState: ShizukuHelper.State,
        onGrantDirect: () -> Unit,
        onGrantShizuku: () -> Unit
    ) {
        GlassmorphismCard(alpha = 0.9f) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, null, tint = AccentYellow)
                    Column {
                        Text("Overlay permission required", color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Needed to show stats on top of other apps", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onGrantDirect, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Settings")
                    }
                    if (shizukuState == ShizukuHelper.State.RUNNING) {
                        Button(onClick = onGrantShizuku, colors = ButtonDefaults.buttonColors(containerColor = GlassPurple), modifier = Modifier.weight(1f)) {
                            Text("⚡ Shizuku")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MasterToggle(isRunning: Boolean, hasPermission: Boolean, onToggle: (Boolean) -> Unit) {
        GlassmorphismCard(alpha = 0.9f) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Performance Overlay", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        if (isRunning) "Active — monitoring stats" else "Inactive",
                        fontSize = 12.sp,
                        color = if (isRunning) AccentGreen else Color.White.copy(alpha = 0.5f)
                    )
                }
                Switch(
                    checked = isRunning, onCheckedChange = { onToggle(it) }, enabled = hasPermission,
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen, checkedTrackColor = AccentGreen.copy(alpha = 0.3f))
                )
            }
        }
    }

    @Composable
    private fun LivePreviewCard(stats: PerformanceStats, config: OverlayConfig) {
        Text("LIVE STATS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.4f))
        GlassmorphismCard(alpha = 0.85f) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (config.showFps) LiveStatRow("FPS", "${stats.fps}", AccentGreen)
                if (config.showFrameTime && stats.avgFrameTimeMs > 0) {
                    val ftStr = "${String.format("%.1f", stats.avgFrameTimeMs)}ms" +
                        if (stats.droppedFrames > 0) " (▼${stats.droppedFrames})" else ""
                    LiveStatRow("FRAME", ftStr, if (stats.p95FrameTimeMs > 33f) AccentRed else AccentGreen)
                }
                if (config.showCpu) LiveStatRow("CPU", "${stats.cpuUsage.toInt()}% @ ${stats.cpuFrequency} MHz", AccentBlue)
                if (config.showGpu) {
                    val gpuSub = if (stats.gpuFrequency > 0) " @ ${stats.gpuFrequency} MHz" else ""
                    LiveStatRow("GPU", "${stats.gpuUsage.toInt()}%$gpuSub", AccentGreen)
                }
                if (config.showRam) LiveStatRow("RAM", "${stats.ramUsed} / ${stats.ramTotal} MB", GlassPurple)
                if (config.showTemp) {
                    val temps = listOfNotNull(
                        if (stats.cpuTemp > 0) "CPU ${stats.cpuTemp.toInt()}°" else null,
                        if (stats.gpuTemp > 0) "GPU ${stats.gpuTemp.toInt()}°" else null,
                        if (stats.batteryTemp > 0) "BAT ${stats.batteryTemp.toInt()}°" else null
                    ).joinToString("  ")
                    if (temps.isNotEmpty()) LiveStatRow("TEMP", temps, AccentRed)
                }
                if (config.showNetwork) {
                    LiveStatRow("NET", "↓ ${StatsCollector.formatSpeed(stats.downloadSpeed)}  ↑ ${StatsCollector.formatSpeed(stats.uploadSpeed)}", GlassBlue)
                }
                if (config.showBattery) {
                    val chargeStr = if (stats.isCharging) "⚡ ${stats.batteryLevel}%" else "${stats.batteryLevel}%"
                    val rateStr = if (stats.chargeRate != 0f) " (${if (stats.chargeRate > 0) "+" else ""}${stats.chargeRate.toInt()}mA)" else ""
                    LiveStatRow("BAT", "$chargeStr$rateStr", if (stats.isCharging) AccentGreen else GlassPurple)
                }
            }
        }
    }

    @Composable
    private fun LiveStatRow(label: String, value: String, color: Color) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 12.sp, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }

    @Composable
    private fun StatsTogglesCard(config: OverlayConfig) {
        SectionLabel("STATS")
        GlassmorphismCard(alpha = 0.8f) {
            Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StatToggle("FPS", Icons.Rounded.PlayArrow, config.showFps) { updateConfig(config.copy(showFps = it)) }
                StatToggle("Frame Time", Icons.Rounded.Timeline, config.showFrameTime) { updateConfig(config.copy(showFrameTime = it)) }
                StatToggle("CPU", Icons.Rounded.Memory, config.showCpu) { updateConfig(config.copy(showCpu = it)) }
                StatToggle("GPU", Icons.Rounded.Games, config.showGpu) { updateConfig(config.copy(showGpu = it)) }
                StatToggle("RAM", Icons.Rounded.Storage, config.showRam) { updateConfig(config.copy(showRam = it)) }
                StatToggle("Temperature", Icons.Rounded.Thermostat, config.showTemp) { updateConfig(config.copy(showTemp = it)) }
                StatToggle("Network", Icons.Rounded.Wifi, config.showNetwork) { updateConfig(config.copy(showNetwork = it)) }
                StatToggle("Battery", Icons.Rounded.BatteryFull, config.showBattery) { updateConfig(config.copy(showBattery = it)) }
            }
        }
    }

    @Composable
    private fun StatToggle(label: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onChange(!checked) }.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                Text(label, color = Color.White, fontSize = 14.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.3f)))
        }
    }

    @Composable
    private fun AppearanceCard(config: OverlayConfig) {
        SectionLabel("APPEARANCE")
        GlassmorphismCard(alpha = 0.8f) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Compact mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CropFree, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        Column {
                            Text("Compact mode", color = Color.White, fontSize = 14.sp)
                            Text("Minimal horizontal bar", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                    Switch(
                        checked = config.compactMode,
                        onCheckedChange = { updateConfig(config.copy(compactMode = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.3f))
                    )
                }

                // Background blur toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BlurOn, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        Column {
                            Text("Background blur", color = Color.White, fontSize = 14.sp)
                            Text("Frosted glass effect (Android 12+)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                    Switch(
                        checked = config.backgroundBlur,
                        onCheckedChange = { updateConfig(config.copy(backgroundBlur = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.3f))
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Opacity", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text("${(config.opacity * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                }
                Slider(value = config.opacity, onValueChange = { updateConfig(config.copy(opacity = it)) }, valueRange = 0.3f..1f, colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Scale", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text("${(config.scale * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                }
                Slider(value = config.scale, onValueChange = { updateConfig(config.copy(scale = it)) }, valueRange = 0.5f..2f, colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen))
            }
        }
    }

    @Composable
    private fun ThemePickerCard(config: OverlayConfig) {
        SectionLabel("THEME")
        GlassmorphismCard(alpha = 0.8f) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OverlayTheme.entries.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { theme ->
                            val isSelected = config.themeName == theme.name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) theme.accentPrimary.copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .clickable { updateConfig(config.copy(themeName = theme.name)) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Color preview dots
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        listOf(theme.accentPrimary, theme.accentSecondary, theme.accentDanger).forEach { c ->
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(c)
                                            )
                                        }
                                    }
                                    Text(
                                        theme.displayName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) theme.accentPrimary else Color.White.copy(alpha = 0.6f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        // Fill remaining slots in the row
                        if (row.size < 3) {
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PositionCard(config: OverlayConfig) {
        SectionLabel("POSITION")
        GlassmorphismCard(alpha = 0.8f) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OverlayPosition.entries.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { pos ->
                            val isSelected = config.position == pos
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentBlue.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                    .clickable { updateConfig(config.copy(position = pos)) }.padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    pos.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val rates = listOf(500L to "0.5s", 1000L to "1s", 2000L to "2s", 3000L to "3s")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rates.forEach { (ms, label) ->
                        val isSelected = config.refreshIntervalMs == ms
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentBlue.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                .clickable { updateConfig(config.copy(refreshIntervalMs = ms)) }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 13.sp, color = if (isSelected) AccentBlue else Color.White.copy(alpha = 0.6f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ShizukuStatusCard(state: ShizukuHelper.State) {
        SectionLabel("SHIZUKU")
        GlassmorphismCard(alpha = 0.8f) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bolt, null, tint = when (state) {
                        ShizukuHelper.State.RUNNING -> AccentGreen
                        ShizukuHelper.State.PERMISSION_DENIED -> AccentYellow
                        ShizukuHelper.State.NOT_RUNNING -> Color.White.copy(alpha = 0.4f)
                        else -> Color.White.copy(alpha = 0.3f)
                    })
                    Column {
                        Text("Shizuku", color = Color.White, fontWeight = FontWeight.Medium)
                        Text(when (state) {
                            ShizukuHelper.State.RUNNING -> "Connected — ready to grant permissions"
                            ShizukuHelper.State.PERMISSION_DENIED -> "Permission denied — open Shizuku app"
                            ShizukuHelper.State.NOT_RUNNING -> "Not running — start Shizuku first"
                            ShizukuHelper.State.NOT_INSTALLED -> "Not installed"
                        }, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(when (state) {
                    ShizukuHelper.State.RUNNING -> AccentGreen
                    ShizukuHelper.State.PERMISSION_DENIED -> AccentYellow
                    else -> Color.White.copy(alpha = 0.3f)
                }))
            }
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(top = 8.dp))
    }

    private fun updateConfig(newConfig: OverlayConfig) {
        lifecycleScope.launch { configRepo.updateConfig(newConfig) }
    }
}
