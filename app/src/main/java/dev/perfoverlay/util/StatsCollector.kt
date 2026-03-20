package dev.perfoverlay.util

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader
import kotlin.math.roundToInt

object StatsCollector {

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L

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

        // CPU
        val cpuUsage = getCpuUsage()
        val cpuFreq = getCpuFrequency()

        // Temperatures
        val temps = getTemperatures()

        return dev.perfoverlay.data.PerformanceStats(
            cpuUsage = cpuUsage,
            cpuFrequency = cpuFreq,
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

    private fun getCpuUsage(): Float {
        return try {
            val reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine()
            reader.close()
            val parts = line.split("\\s+".toRegex())
            val idle = parts[4].toLong()
            val total = parts.drop(1).take(7).sumOf { it.toLong() }
            // Simple instant reading (proper impl would track delta)
            ((total - idle).toFloat() / total * 100).coerceIn(0f, 100f)
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

    private fun getTemperatures(): List<Float> {
        val temps = mutableListOf<Float>()
        // Try common thermal zone paths
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
