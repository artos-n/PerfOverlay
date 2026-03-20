package dev.perfoverlay.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.perfoverlay.data.PerformanceStats

/**
 * Quick Settings tile for PerfOverlay.
 *
 * Shows current FPS as tile label.
 * - Tap: toggle overlay on/off
 * - Long-press: open app settings
 * - States: active (green), inactive (gray), recording (orange)
 */
class PerfOverlayTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (OverlayService.isRunning) {
            OverlayService.stop(this)
        } else {
            OverlayService.start(this)
        }

        // Small delay to let service state update
        qsTile?.let { tile ->
            updateTile()
            tile.updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        val isRunning = OverlayService.isRunning
        val stats = OverlayService.stats.value

        tile.label = if (isRunning && stats.fps > 0) {
            "Perf ${stats.fps} FPS"
        } else {
            "PerfOverlay"
        }

        tile.subtitle = when {
            isRunning && stats.throttleState.isThrottling -> "⚠ Throttled"
            isRunning -> "Active"
            else -> "Tap to start"
        }

        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.stateDescription = if (isRunning) {
                "${stats.cpuUsage.toInt()}% CPU · ${stats.gpuUsage.toInt()}% GPU"
            } else {
                ""
            }
        }

        tile.updateTile()
    }
}
