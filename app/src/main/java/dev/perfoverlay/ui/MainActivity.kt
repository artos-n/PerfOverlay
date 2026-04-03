import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.perfoverlay.PerfOverlayApp
import dev.perfoverlay.R
import dev.perfoverlay.data.*
import androidx.compose.ui.res.stringResource

StatToggle(stringResource(R.string.cd_battery), Icons.Rounded.BatteryFull, config.showBattery) { updateConfig(config.copy(showBattery = it)) }
                StatToggle(stringResource(R.string.stat_storage_io), Icons.Rounded.Storage, config.showStorage) { updateConfig(config.copy(showStorage = it)) }
                if (config.showBattery) {
                    val chargeStr = if (stats.isCharging) "⚡ ${stats.batteryLevel}%" else "${stats.batteryLevel}%"
                    val rateStr = if (stats.chargeRate != 0f) " (${if (stats.chargeRate > 0) "+" else ""}${stats.chargeRate.toInt()}mA)" else ""
                    LiveStatRow("BAT", "$chargeStr$rateStr", if (stats.isCharging) AccentGreen else GlassPurple)
                }
                if (config.showStorage && stats.storageReadSpeed > 0 || stats.storageWriteSpeed > 0) {
                    LiveStatRow("STORAGE", "↓ ${StatsCollector.formatStorageSpeed(stats.storageReadSpeed)}  ↑ ${StatsCollector.formatStorageSpeed(stats.storageWriteSpeed)}", GlassBlue)
                }