package dev.perfoverlay.data

data class PerformanceStats(
    val fps: Int = 0,
    val cpuUsage: Float = 0f,
    val cpuFrequency: Long = 0L,
    val gpuUsage: Float = 0f,
    val cpuTemp: Float = 0f,
    val gpuTemp: Float = 0f,
    val batteryTemp: Float = 0f,
    val deviceTemp: Float = 0f,
    val ramUsed: Long = 0L,
    val ramTotal: Long = 0L,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

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
    val backgroundBlur: Boolean = true,
    val themeName: String = "OCEAN",
    val compactMode: Boolean = false
)

enum class OverlayPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_CENTER
}
