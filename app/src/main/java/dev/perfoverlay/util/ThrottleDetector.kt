package dev.perfoverlay.util

/**
 * Detects thermal throttling by correlating temperature spikes with frequency drops.
 *
 * A throttle event is detected when:
 * 1. Temperature is above a threshold (default 60°C)
 * 2. CPU or GPU frequency drops significantly (default >15% drop)
 *
 * Tracks throttle events with timestamps, magnitude, and duration.
 */
class ThrottleDetector {

    data class ThrottleEvent(
        val startTime: Long,
        val endTime: Long? = null,
        val cpuFreqBefore: Long,
        val cpuFreqAfter: Long,
        val gpuUsageBefore: Float,
        val gpuUsageAfter: Float,
        val cpuTemp: Float,
        val gpuTemp: Float,
        val fpsDuring: Int = 0
    ) {
        val durationMs: Long get() = (endTime ?: startTime) - startTime
        val cpuFreqDrop: Long get() = cpuFreqBefore - cpuFreqAfter
        val cpuFreqDropPct: Float get() = if (cpuFreqBefore > 0) cpuFreqDrop.toFloat() / cpuFreqBefore * 100f else 0f
        val isSevere: Boolean get() = cpuFreqDropPct > 30f || (cpuFreqAfter > 0 && cpuFreqAfter < cpuFreqBefore * 0.5f)
    }

    // State tracking
    private var prevCpuFreq = 0L
    private var prevGpuUsage = 0f
    private var currentEvent: ThrottleEvent? = null

    // Session tracking
    private val events = mutableListOf<ThrottleEvent>()
    private var worstCpuDrop = 0L
    private var worstCpuFreqFrom = 0L
    private var worstCpuFreqTo = 0L
    private var totalThrottleDurationMs = 0L

    // Thresholds
    private val tempThresholdCpu = 60f
    private val tempThresholdGpu = 55f
    private val freqDropThreshold = 0.15f // 15% frequency drop

    /**
     * Feed a new sample and detect throttle events.
     * Returns the current throttle state for overlay display.
     */
    fun process(
        cpuFreq: Long,
        gpuUsage: Float,
        cpuTemp: Float,
        gpuTemp: Float,
        fps: Int,
        timestamp: Long
    ): ThrottleState {
        val isHot = cpuTemp >= tempThresholdCpu || gpuTemp >= tempThresholdGpu
        val freqDropped = prevCpuFreq > 0 && cpuFreq > 0 &&
            cpuFreq < prevCpuFreq * (1f - freqDropThreshold)
        val gpuDropped = prevGpuUsage > 10f && gpuUsage < prevGpuUsage * 0.7f

        val isThrottling = isHot && (freqDropped || gpuDropped)

        if (isThrottling) {
            if (currentEvent == null) {
                // New throttle event
                currentEvent = ThrottleEvent(
                    startTime = timestamp,
                    cpuFreqBefore = prevCpuFreq,
                    cpuFreqAfter = cpuFreq,
                    gpuUsageBefore = prevGpuUsage,
                    gpuUsageAfter = gpuUsage,
                    cpuTemp = cpuTemp,
                    gpuTemp = gpuTemp,
                    fpsDuring = fps
                )
            } else {
                // Update ongoing event
                currentEvent = currentEvent!!.copy(
                    cpuFreqAfter = cpuFreq,
                    gpuUsageAfter = gpuUsage,
                    cpuTemp = cpuTemp,
                    gpuTemp = gpuTemp,
                    fpsDuring = fps
                )
            }
        } else if (currentEvent != null) {
            // Throttle ended
            val ended = currentEvent!!.copy(endTime = timestamp)
            events.add(ended)
            totalThrottleDurationMs += ended.durationMs

            if (ended.cpuFreqDrop > worstCpuDrop) {
                worstCpuDrop = ended.cpuFreqDrop
                worstCpuFreqFrom = ended.cpuFreqBefore
                worstCpuFreqTo = ended.cpuFreqAfter
            }
            currentEvent = null
        }

        prevCpuFreq = cpuFreq
        prevGpuUsage = gpuUsage

        return ThrottleState(
            isThrottling = isThrottling,
            isHot = isHot,
            cpuFreqDropPct = if (currentEvent != null) currentEvent!!.cpuFreqDropPct else 0f,
            cpuTemp = cpuTemp,
            gpuTemp = gpuTemp,
            eventCount = events.size + if (currentEvent != null) 1 else 0
        )
    }

    /**
     * Returns a human-readable summary of throttle events for the session.
     */
    fun getSessionSummary(): String {
        val count = events.size + if (currentEvent != null) 1 else 0
        if (count == 0) return "No throttling detected"

        val totalSec = totalThrottleDurationMs / 1000
        val totalMin = totalSec / 60
        val remSec = totalSec % 60

        val durationStr = if (totalMin > 0) "${totalMin}m${remSec}s" else "${totalSec}s"
        val worstStr = if (worstCpuDrop > 0) {
            ", worst: ${worstCpuFreqFrom}→${worstCpuFreqTo} MHz"
        } else ""

        return "Throttled ${count}x, total ${durationStr}${worstStr}"
    }

    fun getEventCount(): Int = events.size + if (currentEvent != null) 1 else 0
    fun getTotalDurationMs(): Long = totalThrottleDurationMs
    fun getWorstCpuDropMhz(): Long = worstCpuDrop
    fun getEvents(): List<ThrottleEvent> = events.toList()

    fun reset() {
        prevCpuFreq = 0L
        prevGpuUsage = 0f
        currentEvent = null
        events.clear()
        worstCpuDrop = 0L
        worstCpuFreqFrom = 0L
        worstCpuFreqTo = 0L
        totalThrottleDurationMs = 0L
    }
}

/**
 * Current throttle state for overlay display.
 */
data class ThrottleState(
    val isThrottling: Boolean = false,
    val isHot: Boolean = false,
    val cpuFreqDropPct: Float = 0f,
    val cpuTemp: Float = 0f,
    val gpuTemp: Float = 0f,
    val eventCount: Int = 0
)
