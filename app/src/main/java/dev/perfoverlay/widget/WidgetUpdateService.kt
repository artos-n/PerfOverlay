package dev.perfoverlay.widget

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import dev.perfoverlay.service.OverlayService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Foreground service that pushes widget updates whenever OverlayService stats change.
 * More efficient than relying on updatePeriodMillis in the widget provider.
 */
class WidgetUpdateService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) return START_NOT_STICKY

        serviceScope.launch {
            try {
                OverlayService.stats.collectLatest { stats ->
                    withContext(Dispatchers.Main) {
                        PerfOverlayWidget.updateAllWidgets(this@WidgetUpdateService)
                    }
                }
            } catch (_: Exception) {
                // Service may not be running, ignore
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        serviceJob.cancel()
        super.onDestroy()
    }
}
