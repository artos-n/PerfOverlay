package dev.perfoverlay.data

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import dev.perfoverlay.PerfOverlayApp
import dev.perfoverlay.util.AnomalyDetector
import dev.perfoverlay.util.ThrottleDetector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages performance recording sessions.
 * Collects stats at the configured interval and stores them in Room DB.
 * Detects the foreground app for per-app recording.
 */
class RecordingManager(private val context: Context) {

    private val dao = PerfOverlayDatabase.getInstance(context).recordingDao()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentSession = MutableStateFlow<RecordingSession?>(null)
    val currentSession: StateFlow<RecordingSession?> = _currentSession.asStateFlow()

    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Running averages for session summary
    private var fpsSum = 0L
    private var cpuSum = 0f
    private var gpuSum = 0f
    private var frameTimeSum = 0f
    private var p95FrameTimeMax = 0f
    private var totalDroppedFrames = 0
    private var sampleCount = 0

    /**
     * Start recording. If [packageName] is provided, records only for that app.
     * Otherwise records globally ("all").
     */
    suspend fun startRecording(
        packageName: String = "all",
        appName: String = "All Apps",
        intervalMs: Long = 1000L,
        statsProvider: () -> PerformanceStats
    ) {
        if (_isRecording.value) return

        val session = RecordingSession(
            packageName = packageName,
            appName = appName,
            startTime = System.currentTimeMillis()
        )
        val sessionId = dao.insertSession(session)
        _currentSession.value = session.copy(id = sessionId)
        _isRecording.value = true

        // Reset accumulators
        fpsSum = 0
        cpuSum = 0f
        gpuSum = 0f
        frameTimeSum = 0f
        p95FrameTimeMax = 0f
        totalDroppedFrames = 0
        sampleCount = 0

        // Start collecting samples
        recordingJob = scope.launch {
            val batchBuffer = mutableListOf<StatSample>()
            val batchInterval = 5000L // flush every 5 seconds
            var lastFlush = System.currentTimeMillis()

            while (isActive) {
                val stats = statsProvider()
                val now = System.currentTimeMillis()
                val relativeTime = now - session.startTime

                // Check if we should filter by app
                val shouldRecord = if (packageName == "all") {
                    true
                } else {
                    getForegroundPackage() == packageName
                }

                if (shouldRecord) {
                    val sample = StatSample(
                        sessionId = sessionId,
                        timestamp = relativeTime,
                        fps = stats.fps,
                        avgFrameTimeMs = stats.avgFrameTimeMs,
                        p95FrameTimeMs = stats.p95FrameTimeMs,
                        p99FrameTimeMs = stats.p99FrameTimeMs,
                        droppedFrames = stats.droppedFrames,
                        cpuUsage = stats.cpuUsage,
                        cpuFrequency = stats.cpuFrequency,
                        gpuUsage = stats.gpuUsage,
                        cpuTemp = stats.cpuTemp,
                        gpuTemp = stats.gpuTemp,
                        batteryTemp = stats.batteryTemp,
                        ramUsed = stats.ramUsed,
                        ramTotal = stats.ramTotal,
                        downloadSpeed = stats.downloadSpeed,
                        uploadSpeed = stats.uploadSpeed
                    )
                    batchBuffer.add(sample)

                    // Update running averages
                    fpsSum += stats.fps
                    cpuSum += stats.cpuUsage
                    gpuSum += stats.gpuUsage
                    frameTimeSum += stats.avgFrameTimeMs
                    if (stats.p95FrameTimeMs > p95FrameTimeMax) p95FrameTimeMax = stats.p95FrameTimeMs
                    totalDroppedFrames = stats.droppedFrames
                    sampleCount++
                }

                // Batch flush to DB
                if (now - lastFlush >= batchInterval && batchBuffer.isNotEmpty()) {
                    dao.insertSamples(batchBuffer.toList())
                    batchBuffer.clear()
                    lastFlush = now
                }

                delay(intervalMs)
            }

            // Final flush
            if (batchBuffer.isNotEmpty()) {
                dao.insertSamples(batchBuffer.toList())
            }
        }
    }

    /**
     * Stop the current recording session and finalize stats.
     */
    suspend fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null

        val session = _currentSession.value
        if (session != null && sampleCount > 0) {
            dao.finishSession(
                sessionId = session.id,
                endTime = System.currentTimeMillis(),
                sampleCount = sampleCount,
                avgFps = fpsSum.toFloat() / sampleCount,
                avgFrameTimeMs = frameTimeSum / sampleCount,
                p95FrameTimeMs = p95FrameTimeMax,
                totalDroppedFrames = totalDroppedFrames,
                avgCpu = cpuSum / sampleCount,
                avgGpu = gpuSum / sampleCount
            )
        }

        _isRecording.value = false
        _currentSession.value = null
        fpsSum = 0
        cpuSum = 0f
        gpuSum = 0f
        frameTimeSum = 0f
        p95FrameTimeMax = 0f
        totalDroppedFrames = 0
        sampleCount = 0
    }

    /**
     * Get the currently foregrounded app package name.
     * Uses UsageStatsManager (requires PACKAGE_USAGE_STATS permission).
     */
    fun getForegroundPackage(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val now = System.currentTimeMillis()
                val stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - 60_000,
                    now
                )
                stats?.maxByOrNull { it.lastTimeUsed }?.packageName ?: "unknown"
            } else {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                @Suppress("DEPRECATION")
                am.runningTasks?.firstOrNull()?.topActivity?.packageName ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get all recorded sessions.
     */
    fun getAllSessions() = dao.getAllSessions()

    /**
     * Get sessions for a specific app.
     */
    fun getSessionsForApp(packageName: String) = dao.getSessionsForApp(packageName)

    /**
     * Get samples for a session.
     */
    fun getSamplesForSession(sessionId: Long) = dao.getSamplesForSession(sessionId)

    /**
     * Get anomaly events for a session.
     */
    fun getAnomalyEvents(sessionId: Long) = dao.getAnomalyEvents(sessionId)

    /**
     * Delete a session and its samples.
     */
    suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

    /**
     * Delete all recordings.
     */
    suspend fun deleteAll() = dao.deleteAllSessions()

    fun destroy() {
        recordingJob?.cancel()
        scope.cancel()
    }
}
