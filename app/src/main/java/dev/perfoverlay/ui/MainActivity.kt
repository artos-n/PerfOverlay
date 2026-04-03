StatToggle("Battery", Icons.Rounded.BatteryFull, config.showBattery) { updateConfig(config.copy(showBattery = it)) }
                StatToggle("Storage I/O", Icons.Rounded.Storage, config.showStorage) { updateConfig(config.copy(showStorage = it)) }
                if (config.showBattery) {
                    val chargeStr = if (stats.isCharging) "⚡ ${stats.batteryLevel}%" else "${stats.batteryLevel}%"
                    val rateStr = if (stats.chargeRate != 0f) " (${if (stats.chargeRate > 0) "+" else ""}${stats.chargeRate.toInt()}mA)" else ""
                    LiveStatRow("BAT", "$chargeStr$rateStr", if (stats.isCharging) AccentGreen else GlassPurple)
                }
                if (config.showStorage && stats.storageReadSpeed > 0 || stats.storageWriteSpeed > 0) {
                    LiveStatRow("STORAGE", "↓ ${StatsCollector.formatStorageSpeed(stats.storageReadSpeed)}  ↑ ${StatsCollector.formatStorageSpeed(stats.storageWriteSpeed)}", GlassBlue)
                }