package dev.perfoverlay.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.app.PendingIntent
import android.content.SharedPreferences
import dev.perfoverlay.R
import dev.perfoverlay.service.OverlayService

class PerfOverlayWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}

    companion object {
        private const val PREFS_NAME = "perf_overlay_widget_prefs"
        private const val PREF_FPS = "fps"
        private const val PREF_CPU = "cpu"
        private const val PREF_GPU = "gpu"
        private const val PREF_TEMP = "temp"

        fun pushStats(context: Context, fps: Int, cpuUsage: Float, gpuUsage: Float, temp: Float) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putInt(PREF_FPS, fps)
                putFloat(PREF_CPU, cpuUsage)
                putFloat(PREF_GPU, gpuUsage)
                putFloat(PREF_TEMP, temp)
                apply()
            }
            // Trigger widget update
            val intent = Intent(context, PerfOverlayWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, PerfOverlayWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fps = prefs.getInt(PREF_FPS, -1)
            val cpuUsage = prefs.getFloat(PREF_CPU, -1f)
            val gpuUsage = prefs.getFloat(PREF_GPU, -1f)
            val temp = prefs.getFloat(PREF_TEMP, -1f)

            val views = RemoteViews(context.packageName, R.layout.widget_perf_overlay)

            val fpsText = if (fps >= 0) "$fps" else "—"
            views.setTextViewText(R.id.widget_fps, fpsText)
            views.setTextColor(R.id.widget_fps, fpsColor(fps))

            views.setTextViewText(R.id.widget_cpu, if (cpuUsage >= 0) "${cpuUsage.toInt()}%" else "—")
            views.setTextViewText(R.id.widget_gpu, if (gpuUsage >= 0) "${gpuUsage.toInt()}%" else "—")

            val maxTemp = if (temp >= 0) "${temp.toInt()}°" else "—"
            views.setTextViewText(R.id.widget_temp, maxTemp)

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun fpsColor(fps: Int): Int = when {
            fps >= 55 -> 0xFF00E676.toInt()
            fps >= 30 -> 0xFFFFCA28.toInt()
            fps > 0 -> 0xFFFF5252.toInt()
            else -> 0xFFAAAAAA.toInt()
        }
    }
}
