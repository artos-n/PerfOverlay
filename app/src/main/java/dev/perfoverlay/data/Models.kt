package dev.perfoverlay.data

data class PerformanceStats(
    val fps: Int = 0,
    val avgFrameTimeMs: Float = 0f,
    val p95FrameTimeMs: Float = 0f,
    val p99FrameTimeMs: Float = 0f,
    val droppedFrames: Int = 0,
    val totalFrames: Int = 0,
    val frameTimeStrip: FloatArray = FloatArray(0),
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerformanceStats) return false
        return fps == other.fps &&
            avgFrameTimeMs == other.avgFrameTimeMs &&
            p95FrameTimeMs == other.p95FrameTimeMs &&
            p99FrameTimeMs == other.p99FrameTimeMs &&
            droppedFrames == other.droppedFrames &&
            totalFrames == other.totalFrames &&
            frameTimeStrip.contentEquals(other.frameTimeStrip) &&
            cpuUsage == other.cpuUsage &&
            cpuFrequency == other.cpuFrequency &&
            gpuUsage == other.gpuUsage &&
            cpuTemp == other.cpuTemp &&
            gpuTemp == other.gpuTemp &&
            batteryTemp == other.batteryTemp &&
            deviceTemp == other.deviceTemp &&
            ramUsed == other.ramUsed &&
            ramTotal == other.ramTotal &&
            downloadSpeed == other.downloadSpeed &&
            uploadSpeed == other.uploadSpeed &&
            timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = fps
        result = 31 * result + avgFrameTimeMs.hashCode()
        result = 31 * result + p95FrameTimeMs.hashCode()
        result = 31 * result + p99FrameTimeMs.hashCode()
        result = 31 * result + droppedFrames
        result = 31 * result + totalFrames
        result = 31 * result + frameTimeStrip.contentHashCode()
        result = 31 * result + cpuUsage.hashCode()
        result = 31 * result + cpuFrequency.hashCode()
        result = 31 * result + gpuUsage.hashCode()
        result = 31 * result + cpuTemp.hashCode()
        result = 31 * result + gpuTemp.hashCode()
        result = 31 * result + batteryTemp.hashCode()
        result = 31 * result + deviceTemp.hashCode()
        result = 31 * result + ramUsed.hashCode()
        result = 31 * result + ramTotal.hashCode()
        result = 31 * result + downloadSpeed.hashCode()
        result = 31 * result + uploadSpeed.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

data class OverlayConfig(
    val position: OverlayPosition = OverlayPosition.TOP_LEFT,
    val opacity: Float = 0.85f,
    val showFps: Boolean = true,
    val showFrameTime: Boolean = true,
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
