package dev.perfoverlay.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * A recording session — one continuous monitoring period for a specific app.
 */
@Entity(
    tableName = "recording_sessions",
    indices = [Index("packageName"), Index("startTime")]
)
data class RecordingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Package name of the recorded app, or "all" for global recording */
    val packageName: String = "all",

    /** Human-readable app label */
    val appName: String = "All Apps",

    /** Session start timestamp (epoch millis) */
    val startTime: Long = System.currentTimeMillis(),

    /** Session end timestamp, null if still recording */
    val endTime: Long? = null,

    /** Total samples collected */
    val sampleCount: Int = 0,

    /** Average FPS during the session */
    val avgFps: Float = 0f,

    /** Average frame time in ms during the session */
    val avgFrameTimeMs: Float = 0f,

    /** P95 frame time in ms during the session */
    val p95FrameTimeMs: Float = 0f,

    /** Total dropped frames during the session */
    val totalDroppedFrames: Int = 0,

    /** Average CPU usage during the session */
    val avgCpu: Float = 0f,

    /** Average GPU usage during the session */
    val avgGpu: Float = 0f
)

/**
 * A single performance stat sample within a recording session.
 */
@Entity(
    tableName = "stat_samples",
    foreignKeys = [ForeignKey(
        entity = RecordingSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId"), Index("timestamp")]
)
data class StatSample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Parent session */
    val sessionId: Long,

    /** Timestamp relative to session start (millis) */
    val timestamp: Long,

    val fps: Int = 0,
    val avgFrameTimeMs: Float = 0f,
    val p95FrameTimeMs: Float = 0f,
    val p99FrameTimeMs: Float = 0f,
    val droppedFrames: Int = 0,
    val cpuUsage: Float = 0f,
    val cpuFrequency: Long = 0L,
    val gpuUsage: Float = 0f,
    val cpuTemp: Float = 0f,
    val gpuTemp: Float = 0f,
    val batteryTemp: Float = 0f,
    val ramUsed: Long = 0L,
    val ramTotal: Long = 0L,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L
)

@Dao
interface RecordingDao {

    // --- Sessions ---

    @Insert
    suspend fun insertSession(session: RecordingSession): Long

    @Update
    suspend fun updateSession(session: RecordingSession)

    @Query("UPDATE recording_sessions SET endTime = :endTime, sampleCount = :sampleCount, avgFps = :avgFps, avgFrameTimeMs = :avgFrameTimeMs, p95FrameTimeMs = :p95FrameTimeMs, totalDroppedFrames = :totalDroppedFrames, avgCpu = :avgCpu, avgGpu = :avgGpu WHERE id = :sessionId")
    suspend fun finishSession(sessionId: Long, endTime: Long, sampleCount: Int, avgFps: Float, avgFrameTimeMs: Float, p95FrameTimeMs: Float, totalDroppedFrames: Int, avgCpu: Float, avgGpu: Float)

    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<RecordingSession>>

    @Query("SELECT * FROM recording_sessions WHERE packageName = :packageName ORDER BY startTime DESC")
    fun getSessionsForApp(packageName: String): Flow<List<RecordingSession>>

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): RecordingSession?

    @Query("DELETE FROM recording_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM recording_sessions WHERE packageName = :packageName")
    suspend fun deleteSessionsForApp(packageName: String)

    @Query("DELETE FROM recording_sessions")
    suspend fun deleteAllSessions()

    // --- Samples ---

    @Insert
    suspend fun insertSample(sample: StatSample)

    @Insert
    suspend fun insertSamples(samples: List<StatSample>)

    @Query("SELECT * FROM stat_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSamplesForSession(sessionId: Long): Flow<List<StatSample>>

    @Query("SELECT * FROM stat_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSamplesForSessionOnce(sessionId: Long): List<StatSample>

    @Query("SELECT * FROM stat_samples WHERE sessionId = :sessionId AND timestamp >= :from ORDER BY timestamp ASC")
    fun getSamplesSince(sessionId: Long, from: Long): Flow<List<StatSample>>

    @Query("SELECT COUNT(*) FROM stat_samples WHERE sessionId = :sessionId")
    suspend fun getSampleCount(sessionId: Long): Int

    @Query("SELECT AVG(fps) FROM stat_samples WHERE sessionId = :sessionId")
    suspend fun getAvgFps(sessionId: Long): Float?

    @Query("SELECT AVG(cpuUsage) FROM stat_samples WHERE sessionId = :sessionId")
    suspend fun getAvgCpu(sessionId: Long): Float?

    @Query("SELECT AVG(gpuUsage) FROM stat_samples WHERE sessionId = :sessionId")
    suspend fun getAvgGpu(sessionId: Long): Float?
}
