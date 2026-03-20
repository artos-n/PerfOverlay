package dev.perfoverlay.util

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.math.roundToInt

object StatsCollector {

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L
    private var prevIdle = 0L
    private var prevTotal = 0L

    fun collect(context: Context): dev.perfoverlay.data.PerformanceStats {
        val now = System.currentTimeMillis()

        // Network
        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()
        val elapsed = if (lastTimestamp > 0) (now - lastTimestamp) / 1000f else 1f
        val dlSpeed = if (elapsed > 0) ((totalRx - lastRxBytes) / elapsed).toLong() else 0L
        val ulSpeed = if (elapsed > 0) ((totalTx - lastTxBytes) / elapsed).toLong() else 0L
        lastRxBytes = totalRx
        lastTxBytes = totalTx
        lastTimestamp = now

        // RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val ramTotal = memInfo.totalMem / (1024 * 1024)
        val ramUsed = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024)

        // CPU (delta-based for accuracy)
        val cpuUsage = getCpuUsageDelta()
        val cpuFreq = getCpuFrequency()

        // GPU
        val gpuUsage = getGpuUsage()

        // Temperatures
        val temps = getTemperatures()

        return dev.perfoverlay.data.PerformanceStats(
            cpuUsage = cpuUsage,
            cpuFrequency = cpuFreq,
            gpuUsage = gpuUsage,
            cpuTemp = temps.getOrElse(0) { 0f },
            gpuTemp = temps.getOrElse(1) { 0f },
            batteryTemp = temps.getOrElse(2) { 0f },
            deviceTemp = temps.getOrElse(3) { 0f },
            ramUsed = ramUsed,
            ramTotal = ramTotal,
            downloadSpeed = dlSpeed.coerceAtLeast(0),
            uploadSpeed = ulSpeed.coerceAtLeast(0),
            timestamp = now
        )
    }

    /**
     * Delta-based CPU usage from /proc/stat.
     * Tracks previous idle/total and computes the difference.
     */
    private fun getCpuUsageDelta(): Float {
        return try {
            val reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine()
            reader.close()
            val parts = line.split("\\s+".toRegex())
            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = parts[5].toLongOrNull() ?: 0L
            val irq = parts[6].toLongOrNull() ?: 0L
            val softirq = parts[7].toLongOrNull() ?: 0L

            val total = user + nice + system + idle + iowait + irq + softirq
            val idleTotal = idle + iowait

            val usage = if (prevTotal > 0) {
                val totalDelta = (total - prevTotal).toFloat()
                val idleDelta = (idleTotal - prevIdle).toFloat()
                if (totalDelta > 0) ((totalDelta - idleDelta) / totalDelta * 100f).coerceIn(0f, 100f)
                else 0f
            } else {
                0f
            }

            prevTotal = total
            prevIdle = idleTotal
            usage
        } catch (e: Exception) {
            0f
        }
    }

    private fun getCpuFrequency(): Long {
        return try {
            val reader = BufferedReader(FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"))
            val freq = reader.readLine().trim().toLong() / 1000 // kHz to MHz
            reader.close()
            freq
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Best-effort GPU usage reading.
     * Adreno (Qualcomm): /sys/class/kgsl/kgsl-3d0/gpubusy or /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage
     * Mali (Samsung/MediaTek): /sys/class/devfreq/*/load (varies by device)
     */
    private fun getGpuUsage(): Float {
        // Adreno — gpu_busy_percentage (0-100)
        try {
            val file = File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
            if (file.exists()) {
                val reader = BufferedReader(FileReader(file))
                val raw = reader.readLine().trim()
                reader.close()
                return raw.replace("%", "").toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
            }
        } catch (_: Exception) {}

        // Adreno — gpubusy (two numbers: busy / total)
        try {
            val file = File("/sys/class/kgsl/kgsl-3d0/gpubusy")
            if (file.exists()) {
                val reader = BufferedReader(FileReader(file))
                val parts = reader.readLine().trim().split("\\s+".toRegex())
                reader.close()
                if (parts.size >= 2) {
                    val busy = parts[0].toLongOrNull() ?: 0L
                    val total = parts[1].toLongOrNull() ?: 1L
                    if (total > 0) return (busy.toFloat() / total * 100f).coerceIn(0f, 100f)
                }
            }
        } catch (_: Exception) {}

        // Mali — try devfreq load files
        try {
            val devfreqDir = File("/sys/class/devfreq/")
            if (devfreqDir.exists()) {
                devfreqDir.listFiles()?.forEach { dir ->
                    val name = dir.name.lowercase()
                    if (name.contains("gpu") || name.contains("mali") || name.contains("kgsl")) {
                        val loadFile = File(dir, "load")
                        if (loadFile.exists()) {
                            val reader = BufferedReader(FileReader(loadFile))
                            val raw = reader.readLine().trim()
                            reader.close()
                            // Format is typically "load@freq" e.g. "85@600000000"
                            val load = raw.split("@")[0].toIntOrNull()
                            if (load != null) return load.toFloat().coerceIn(0f, 100f)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return 0f
    }

    /**
     * Reads temperatures from /sys/class/thermal/thermal_zone*/
    */
    private fun getTemperatures(): List<Float> {
        val temps = mutableListOf<Float>()
        for (i in 0..20) {
            try {
                val path = "/sys/class/thermal/thermal_zone$i/type"
                val typeReader = BufferedReader(FileReader(path))
                val type = typeReader.readLine().trim()
                typeReader.close()

                val tempPath = "/sys/class/thermal/thermal_zone$i/temp"
                val tempReader = BufferedReader(FileReader(tempPath))
                val raw = tempReader.readLine().trim().toFloat()
                tempReader.close()

                val temp = if (raw > 1000) raw / 1000f else raw

                when {
                    type.contains("cpu", ignoreCase = true) -> temps.add(0, temp)
                    type.contains("gpu", ignoreCase = true) -> temps.add(1, temp)
                    type.contains("battery", ignoreCase = true) -> temps.add(2, temp)
                    type.contains("skin", ignoreCase = true) || type.contains("back", ignoreCase = true) ->
                        temps.add(3, temp)
                }
            } catch (e: Exception) {
                break
            }
        }
        while (temps.size < 4) temps.add(0f)
        return temps
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1_048_576 -> "${(bytesPerSec / 1_048_576f * 10).roundToInt() / 10f} MB/s"
            bytesPerSec >= 1024 -> "${(bytesPerSec / 1024f * 10).roundToInt() / 10f} KB/s"
            else -> "$bytesPerSec B/s"
        }
    }
}
