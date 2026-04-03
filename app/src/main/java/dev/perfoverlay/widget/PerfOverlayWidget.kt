package dev.perfoverlay.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.app.PendingIntent
import android.view.View
import dev.perfoverlay.R
import dev.perfoverlay.data.PerformanceStats
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

    override fun onEnabled(context: Context) {
        // Called when the first widget is placed
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
    }

    companion object {
        private const val ACTION_UPDATE_WIDGET = "dev.perfoverlay.UPDATE_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val stats = try { OverlayService.stats.value } catch (_: Exception) { null }
            val views = RemoteViews(context.packageName, R.layout.widget_perf_overlay)

            if (stats != null) {
                // FPS
                val fpsText = if (stats.fps > 0) "${stats.fps}" else "—"
                views.setTextViewText(R.id.widget_fps, fpsText)
                views.setTextColor(R.id.widget_fps, fpsColor(stats.fps))

                // CPU
                val cpuText = if (stats.cpuUsage > 0) "${stats.cpuUsage.toInt()}%" else "—"
                views.setTextViewText(R.id.widget_cpu, cpuText)

                // GPU
                val gpuText = if (stats.gpuUsage > 0) "${stats.gpuUsage.toInt()}%" else "—"
                views.setTextViewText(R.id.widget_gpu, gpuText)

                // Temp — show max of CPU/GPU
                val maxTemp = maxOf(stats.cpuTemp, stats.gpuTemp)
                val tempText = if (maxTemp > 0) "${maxTemp.toInt()}°" else "—"
                views.setTextViewText(R.id.widget_temp, tempText)
            } else {
                views.setTextViewText(R.id.widget_fps, "—")
                views.setTextViewText(R.id.widget_cpu, "—")
                views.setTextViewText(R.id.widget_gpu, "—")
                views.setTextViewText(R.id.widget_temp, "—")
            }

            // Tap to open app
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, PerfOverlayWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun fpsColor(fps: Int): Int {
            return when {
                fps >= 55 -> 0xFF00E676.toInt() // green
                fps >= 30 -> 0xFFFFCA28.toInt() // yellow
                fps > 0 -> 0xFFFF5252.toInt()   // red
                else -> 0xFFAAAAAA.toInt()       // gray
            }
        }
    }
}
