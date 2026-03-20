package dev.perfoverlay.util

import android.view.Choreographer

/**
 * Measures real FPS using Choreographer frame callbacks.
 * Tracks frame times over a rolling window and computes
 * the average frames per second.
 */
class FpsMonitor {

    private val frameTimes = LongArray(64)
    private var frameIndex = 0
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var running = false

    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        if (!running) return@FrameCallback

        if (lastFrameTime > 0) {
            val delta = frameTimeNanos - lastFrameTime
            frameTimes[frameIndex] = delta
            frameIndex = (frameIndex + 1) % frameTimes.size
            if (frameCount < frameTimes.size) frameCount++
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
}
