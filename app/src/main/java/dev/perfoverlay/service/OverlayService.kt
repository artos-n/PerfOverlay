package dev.perfoverlay.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.perfoverlay.PerfOverlayApp
import dev.perfoverlay.R
import dev.perfoverlay.data.ConfigRepository
import dev.perfoverlay.data.OverlayConfig
import dev.perfoverlay.data.OverlayPosition
import dev.perfoverlay.data.PerformanceStats
import dev.perfoverlay.ui.component.OverlayView
import dev.perfoverlay.ui.theme.PerfOverlayTheme
import dev.perfoverlay.util.StatsCollector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

class OverlayService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "perf_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        var isRunning = false
            private set

        val stats = MutableStateFlow(PerformanceStats())
        val config = MutableStateFlow(OverlayConfig())

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var statsJob: Job? = null
    private val configRepo by lazy { ConfigRepository(PerfOverlayApp.instance) }

    override fun onBind(intent: android.content.Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Collect config changes
        lifecycleScope.launch {
            configRepo.config.collectLatest { newConfig ->
                config.value = newConfig
                recreateOverlay()
            }
        }

        startStatsCollection()
    }

    override fun onDestroy() {
        isRunning = false
        statsJob?.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private fun startStatsCollection() {
        statsJob = lifecycleScope.launch {
            while (isActive) {
                stats.value = StatsCollector.collect(applicationContext)
                delay(config.value.refreshIntervalMs)
            }
        }
    }

    private fun recreateOverlay() {
        removeOverlay()
        showOverlay()
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = getGravity(config.value.position)
        }

        overlayView = ComposeView(this).apply {
            setContent {
                PerfOverlayTheme {
                    OverlayView(stats = stats, config = config)
                }
            }
        }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Already removed
            }
        }
        overlayView = null
    }

    private fun getGravity(position: OverlayPosition): Int {
        return when (position) {
            OverlayPosition.TOP_LEFT -> Gravity.TOP or Gravity.START
            OverlayPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
            OverlayPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
            OverlayPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
            OverlayPosition.TOP_CENTER -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Performance Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows performance stats on screen"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PerfOverlay")
            .setContentText("Monitoring performance")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
