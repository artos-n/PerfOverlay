package dev.perfoverlay.data

import kotlinx.serialization.Serializable

@Serializable
data class PerformanceStats(
    val fps: Int = 0,
    val cpuUsage: Float = 0f,        // percentage 0-100
    val cpuFrequency: Long = 0L,     // MHz
    val gpuUsage: Float = 0f,        // percentage 0-100
    val cpuTemp: Float = 0f,         // celsius
    val gpuTemp: Float = 0f,         // celsius
    val batteryTemp: Float = 0f,     // celsius
    val deviceTemp: Float = 0f,      // celsius (skin/case)
    val ramUsed: Long = 0L,          // MB
    val ramTotal: Long = 0L,         // MB
    val downloadSpeed: Long = 0L,    // bytes/s
    val uploadSpeed: Long = 0L,      // bytes/s
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class OverlayConfig(
    val position: OverlayPosition = OverlayPosition.TOP_LEFT,
    val opacity: Float = 0.85f,
    val showFps: Boolean = true,
    val showCpu: Boolean = true,
    val showGpu: Boolean = true,
    val showTemp: Boolean = true,
    val showNetwork: Boolean = true,
    val showRam: Boolean = false,
    val refreshIntervalMs: Long = 1000L,
    val scale: Float = 1.0f,
    val backgroundBlur: Boolean = true
)

enum class OverlayPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_CENTER
}
