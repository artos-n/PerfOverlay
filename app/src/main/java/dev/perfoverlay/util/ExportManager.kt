package dev.perfoverlay.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dev.perfoverlay.data.StatSample
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Exports recording session data to CSV or JSON format.
 * Uses MediaStore on Android 10+ for scoped storage compatibility.
 */
object ExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    /**
     * Export samples as CSV to the Downloads folder.
     * Returns the URI path on success, null on failure.
     */
    fun exportCsv(
        context: Context,
        sessionName: String,
        samples: List<StatSample>
    ): String? {
        if (samples.isEmpty()) return null

        val timestamp = dateFormat.format(Date())
        val fileName = "PerfOverlay_${sessionName.replace(" ", "_")}_$timestamp.csv"

        val csv = buildString {
            // Header
            appendLine("timestamp_ms,fps,cpu_usage_pct,cpu_freq_mhz,gpu_usage_pct,cpu_temp_c,gpu_temp_c,battery_temp_c,ram_used_mb,ram_total_mb,download_bps,upload_bps")

            // Data rows
            samples.forEach { s ->
                appendLine("${s.timestamp},${s.fps},${s.cpuUsage},${s.cpuFrequency},${s.gpuUsage},${s.cpuTemp},${s.gpuTemp},${s.batteryTemp},${s.ramUsed},${s.ramTotal},${s.downloadSpeed},${s.uploadSpeed}")
            }
        }

        return writeFile(context, fileName, "text/csv", csv.toByteArray())
    }

    /**
     * Export samples as JSON to the Downloads folder.
     */
    fun exportJson(
        context: Context,
        sessionName: String,
        samples: List<StatSample>
    ): String? {
        if (samples.isEmpty()) return null

        val timestamp = dateFormat.format(Date())
        val fileName = "PerfOverlay_${sessionName.replace(" ", "_")}_$timestamp.json"

        val json = buildString {
            append("{\n")
            append("  \"session\": \"$sessionName\",\n")
            append("  \"exportedAt\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())}\",\n")
            append("  \"sampleCount\": ${samples.size},\n")
            append("  \"samples\": [\n")

            samples.forEachIndexed { index, s ->
                append("    {")
                append("\"t\":${s.timestamp},")
                append("\"fps\":${s.fps},")
                append("\"cpu\":${s.cpuUsage},")
                append("\"cpuMhz\":${s.cpuFrequency},")
                append("\"gpu\":${s.gpuUsage},")
                append("\"cpuT\":${s.cpuTemp},")
                append("\"gpuT\":${s.gpuTemp},")
                append("\"batT\":${s.batteryTemp},")
                append("\"ram\":${s.ramUsed},")
                append("\"ramMax\":${s.ramTotal},")
                append("\"dl\":${s.downloadSpeed},")
                append("\"ul\":${s.uploadSpeed}")
                append("}")
                if (index < samples.size - 1) append(",")
                appendLine()
            }

            append("  ]\n")
            append("}")
        }

        return writeFile(context, fileName, "application/json", json.toByteArray())
    }

    private fun writeFile(
        context: Context,
        fileName: String,
        mimeType: String,
        data: ByteArray
    ): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped storage — use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PerfOverlay")
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return null

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(data)
                    os.flush()
                }

                uri.toString()
            } else {
                // Legacy — direct file write
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "PerfOverlay"
                )
                dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(data) }
                file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
