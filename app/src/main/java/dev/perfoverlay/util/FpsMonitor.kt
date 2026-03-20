package dev.perfoverlay.util

import android.view.Choreographer

/**
 * Measures real FPS using Choreographer frame callbacks.
 * Tracks per-frame durations for frame time analysis,
 * dropped frame detection, and percentile stats.
 */
class FpsMonitor {

    private val frameTimes = LongArray(64)
    private var frameIndex = 0
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var running = false

    // Frame time strip data — recent frame durations in ms (for the micro-graph)
    private val stripData = FloatArray(64)
    private var stripIndex = 0
    private var stripCount = 0

    // Dropped frame tracking (frames exceeding 2× vsync interval)
    private var droppedFrames = 0
    private var totalFrames = 0

    // Vsync interval for dropped frame detection (default 60Hz = 16.67ms)
    private val vsyncIntervalMs = 16.67f

    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        if (!running) return@FrameCallback

        if (lastFrameTime > 0) {
            val deltaNs = frameTimeNanos - lastFrameTime
            val deltaMs = deltaNs / 1_000_000f

            // Ring buffer for FPS averaging
            frameTimes[frameIndex] = deltaNs
            frameIndex = (frameIndex + 1) % frameTimes.size
            if (frameCount < frameTimes.size) frameCount++

            // Strip data for the micro-graph
            stripData[stripIndex] = deltaMs
            stripIndex = (stripIndex + 1) % stripData.size
            if (stripCount < stripData.size) stripCount++

            // Dropped frame detection (exceeds 2× vsync)
            totalFrames++
            if (deltaMs > vsyncIntervalMs * 2f) {
                droppedFrames++
            }
        }
        lastFrameTime = frameTimeNanos

        Choreographer.getInstance().postFrameCallback(this)
    }

    fun start() {
        if (running) return
        running = true
        lastFrameTime = 0L
        frameCount = 0
        frameIndex = 0
        stripCount = 0
        stripIndex = 0
        droppedFrames = 0
        totalFrames = 0
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun getFps(): Int {
        if (frameCount < 2) return 0

        val count = frameCount.coerceAtMost(frameTimes.size)
        var totalNs = 0L
        for (i in 0 until count) {
            totalNs += frameTimes[i]
        }

        if (totalNs <= 0) return 0
        val avgFrameTimeSec = totalNs.toDouble() / count / 1_000_000_000.0
        return (1.0 / avgFrameTimeSec).toInt().coerceIn(0, 240)
    }

    /**
     * Returns average frame time in milliseconds.
     */
    fun getAvgFrameTimeMs(): Float {
        if (frameCount < 2) return 0f
        val count = frameCount.coerceAtMost(frameTimes.size)
        var totalNs = 0L
        for (i in 0 until count) {
            totalNs += frameTimes[i]
        }
        return (totalNs / count / 1_000_000f)
    }

    /**
     * Returns a copy of recent frame durations in milliseconds.
     * Ordered chronologically (oldest first) for the strip micro-graph.
     */
    fun getFrameTimeStrip(): FloatArray {
        if (stripCount == 0) return FloatArray(0)
        val result = FloatArray(stripCount)
        val count = stripCount.coerceAtMost(stripData.size)
        for (i in 0 until count) {
            val idx = (stripIndex - count + i + stripData.size) % stripData.size
            result[i] = stripData[idx]
        }
        return result
    }

    /**
     * Returns the number of dropped frames since start.
     * A dropped frame exceeds 2× the vsync interval (e.g., >33.3ms at 60Hz).
     */
    fun getDroppedFrames(): Int = droppedFrames

    /**
     * Returns total frames sampled since start.
     */
    fun getTotalFrames(): Int = totalFrames

    /**
     * Computes percentile frame time from recent data.
     * @param percentile 0.0 to 1.0 (e.g., 0.95 for P95, 0.99 for P99)
     * @return frame time in ms at the given percentile
     */
    fun getPercentileFrameTime(percentile: Float): Float {
        if (stripCount < 2) return 0f
        val sorted = getFrameTimeStrip().sorted()
        val index = (percentile * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }

    /**
     * Returns P95 frame time in milliseconds.
     */
    fun getP95FrameTimeMs(): Float = getPercentileFrameTime(0.95f)

    /**
     * Returns P99 frame time in milliseconds.
     */
    fun getP99FrameTimeMs(): Float = getPercentileFrameTime(0.99f)
}
