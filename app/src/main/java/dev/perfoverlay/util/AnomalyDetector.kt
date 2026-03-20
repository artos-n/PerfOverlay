package dev.perfoverlay.util

/**
 * Detects performance anomalies by comparing current metrics against
 * a rolling baseline (moving average ± standard deviation).
 *
 * An anomaly is detected when a metric deviates more than N standard
 * deviations from its rolling mean (default: 2σ).
 *
 * Produces human-readable event logs like:
 * "At 2:34, FPS dropped to 12 (avg: 58)"
 */
class AnomalyDetector {

    data class AnomalyEvent(
        val timestamp: Long,       // relative to session start (ms)
        val metric: String,        // "FPS", "CPU", "GPU", "Frame Time"
        val value: Float,          // the anomalous value
        val baseline: Float,       // the rolling average
        val deviationSigma: Float, // how many σ away from baseline
        val isSpike: Boolean       // true = spike up, false = drop
    ) {
        fun toReadableString(): String {
            val timeStr = formatTimestamp(timestamp)
            val direction = if (isSpike) "spiked to" else "dropped to"
            return when (metric) {
                "FPS" -> "At $timeStr, FPS $direction ${value.toInt()} (avg: ${baseline.toInt()})"
                "CPU" -> "At $timeStr, CPU $direction ${value.toInt()}% (avg: ${baseline.toInt()}%)"
                "GPU" -> "At $timeStr, GPU $direction ${value.toInt()}% (avg: ${baseline.toInt()}%)"
                "Frame Time" -> "At $timeStr, frame time $direction ${String.format("%.1f", value)}ms (avg: ${String.format("%.1f", baseline)}ms)"
                else -> "At $timeStr, $metric $direction $value (avg: $baseline)"
            }
        }

        companion object {
            fun formatTimestamp(millis: Long): String {
                val totalSeconds = millis / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                return if (minutes > 0) "${minutes}:${seconds.toString().padStart(2, '0')}"
                else "${seconds}s"
            }
        }
    }

    /**
     * Rolling statistics tracker for a single metric.
     */
    private class RollingStats(private val windowSize: Int = 30) {
        private val values = FloatArray(windowSize)
        private var index = 0
        private var count = 0

        fun add(value: Float) {
            values[index] = value
            index = (index + 1) % windowSize
            if (count < windowSize) count++
        }

        fun mean(): Float {
            if (count == 0) return 0f
            return values.take(count).sum() / count
        }

        fun stdDev(): Float {
            if (count < 2) return 0f
            val m = mean()
            val variance = values.take(count).sumOf { ((it - m) * (it - m)).toDouble() }.toFloat() / count
            return kotlin.math.sqrt(variance)
        }

        fun reset() {
            index = 0
            count = 0
        }
    }

    // Rolling stats per metric
    private val fpsStats = RollingStats(30)
    private val cpuStats = RollingStats(30)
    private val gpuStats = RollingStats(30)
    private val frameTimeStats = RollingStats(30)

    // Detected anomalies
    private val anomalies = mutableListOf<AnomalyEvent>()

    // Config
    private var sigmaThreshold = 2.0f // flag events > 2σ from mean
    private var minSamplesForDetection = 10 // need baseline before detecting
    private var cooldownMs = 3000L // don't fire same metric within 3s
    private var lastFpsAnomaly = 0L
    private var lastCpuAnomaly = 0L
    private var lastGpuAnomaly = 0L
    private var lastFtAnomaly = 0L

    /**
     * Feed a new sample and detect anomalies.
     * Returns any newly detected anomaly events.
     */
    fun process(
        fps: Int,
        cpuUsage: Float,
        gpuUsage: Float,
        frameTimeMs: Float,
        relativeTimestamp: Long
    ): List<AnomalyEvent> {
        val newAnomalies = mutableListOf<AnomalyEvent>()

        // FPS anomaly (drop is concerning)
        if (fpsStats.mean() > 0) {
            val mean = fpsStats.mean()
            val sd = fpsStats.stdDev()
            if (sd > 0 && fpsStats.count >= minSamplesForDetection) {
                val deviation = (mean - fps) / sd // positive = drop
                if (deviation > sigmaThreshold && relativeTimestamp - lastFpsAnomaly > cooldownMs) {
                    newAnomalies.add(AnomalyEvent(
                        timestamp = relativeTimestamp,
                        metric = "FPS",
                        value = fps.toFloat(),
                        baseline = mean,
                        deviationSigma = deviation,
                        isSpike = false
                    ))
                    lastFpsAnomaly = relativeTimestamp
                }
            }
        }
        fpsStats.add(fps.toFloat())

        // CPU anomaly
        if (cpuStats.mean() > 0) {
            val mean = cpuStats.mean()
            val sd = cpuStats.stdDev()
            if (sd > 0 && cpuStats.count >= minSamplesForDetection) {
                val deviation = kotlin.math.abs(cpuUsage - mean) / sd
                if (deviation > sigmaThreshold && relativeTimestamp - lastCpuAnomaly > cooldownMs) {
                    newAnomalies.add(AnomalyEvent(
                        timestamp = relativeTimestamp,
                        metric = "CPU",
                        value = cpuUsage,
                        baseline = mean,
                        deviationSigma = deviation,
                        isSpike = cpuUsage > mean
                    ))
                    lastCpuAnomaly = relativeTimestamp
                }
            }
        }
        cpuStats.add(cpuUsage)

        // GPU anomaly
        if (gpuStats.mean() > 0) {
            val mean = gpuStats.mean()
            val sd = gpuStats.stdDev()
            if (sd > 0 && gpuStats.count >= minSamplesForDetection) {
                val deviation = kotlin.math.abs(gpuUsage - mean) / sd
                if (deviation > sigmaThreshold && relativeTimestamp - lastGpuAnomaly > cooldownMs) {
                    newAnomalies.add(AnomalyEvent(
                        timestamp = relativeTimestamp,
                        metric = "GPU",
                        value = gpuUsage,
                        baseline = mean,
                        deviationSigma = deviation,
                        isSpike = gpuUsage > mean
                    ))
                    lastGpuAnomaly = relativeTimestamp
                }
            }
        }
        gpuStats.add(gpuUsage)

        // Frame time anomaly (spike is concerning)
        if (frameTimeMs > 0 && frameTimeStats.mean() > 0) {
            val mean = frameTimeStats.mean()
            val sd = frameTimeStats.stdDev()
            if (sd > 0 && frameTimeStats.count >= minSamplesForDetection) {
                val deviation = (frameTimeMs - mean) / sd // positive = spike
                if (deviation > sigmaThreshold && relativeTimestamp - lastFtAnomaly > cooldownMs) {
                    newAnomalies.add(AnomalyEvent(
                        timestamp = relativeTimestamp,
                        metric = "Frame Time",
                        value = frameTimeMs,
                        baseline = mean,
                        deviationSigma = deviation,
                        isSpike = true
                    ))
                    lastFtAnomaly = relativeTimestamp
                }
            }
        }
        if (frameTimeMs > 0) frameTimeStats.add(frameTimeMs)

        anomalies.addAll(newAnomalies)
        return newAnomalies
    }

    /**
     * Set the sigma threshold for anomaly detection.
     * Lower = more sensitive (more anomalies), higher = only flag severe events.
     */
    fun setSigmaThreshold(sigma: Float) {
        sigmaThreshold = sigma.coerceIn(1.0f, 5.0f)
    }

    fun getAnomalyCount(): Int = anomalies.size
    fun getAnomalies(): List<AnomalyEvent> = anomalies.toList()

    /**
     * Returns the last N anomaly messages for display.
     */
    fun getRecentMessages(n: Int = 5): List<String> {
        return anomalies.takeLast(n).map { it.toReadableString() }
    }

    fun reset() {
        fpsStats.reset()
        cpuStats.reset()
        gpuStats.reset()
        frameTimeStats.reset()
        anomalies.clear()
        lastFpsAnomaly = 0L
        lastCpuAnomaly = 0L
        lastGpuAnomaly = 0L
        lastFtAnomaly = 0L
    }
}
