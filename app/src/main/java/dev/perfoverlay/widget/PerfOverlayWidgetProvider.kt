package dev.perfoverlay.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.perfoverlay.R
import dev.perfoverlay.service.OverlayService
import dev.perfoverlay.ui.MainActivity

class PerfOverlayWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WIDGET = "dev.perfoverlay.UPDATE_WIDGET"
        private var lastFps = "—"
        private var lastCpu = "—"
        private var lastGpu = "—"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, PerfOverlayWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }

        fun pushStats(context: Context, fps: Int, cpuUsage: Float, gpuUsage: Float) {
            lastFps = if (fps > 0) "$fps" else "—"
            lastCpu = if (cpuUsage >= 0) "${cpuUsage.toInt()}%" else "—"
            lastGpu = if (gpuUsage >= 0) "${gpuUsage.toInt()}%" else "—"
            updateAllWidgets(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_UPDATE_WIDGET -> {
                val stats = OverlayService.stats.value
                lastFps = if (stats.fps > 0) "${stats.fps}" else "—"
                lastCpu = if (stats.cpuUsage >= 0) "${stats.cpuUsage.toInt()}%" else "—"
                lastGpu = if (stats.gpuUsage >= 0) "${stats.gpuUsage.toInt()}%" else "—"
            }
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, PerfOverlayWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)

        if (appWidgetIds.isNotEmpty()) {
            updateAppWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAppWidgets(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val refreshIntent = Intent(context, PerfOverlayWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val views = RemoteViews(context.packageName, R.layout.widget_perf_overlay).apply {
            setTextViewText(R.id.widget_fps, lastFps)
            setTextViewText(R.id.widget_cpu, lastCpu)
            setTextViewText(R.id.widget_gpu, lastGpu)
            setOnClickPendingIntent(R.id.widget_fps, openAppPendingIntent)
        }

        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}