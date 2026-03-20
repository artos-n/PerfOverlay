package dev.perfoverlay.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "perf_overlay_config")

class ConfigRepository(private val context: Context) {

    companion object {
        private val POSITION = stringPreferencesKey("position")
        private val OPACITY = floatPreferencesKey("opacity")
        private val SHOW_FPS = booleanPreferencesKey("show_fps")
        private val SHOW_CPU = booleanPreferencesKey("show_cpu")
        private val SHOW_GPU = booleanPreferencesKey("show_gpu")
        private val SHOW_TEMP = booleanPreferencesKey("show_temp")
        private val SHOW_NETWORK = booleanPreferencesKey("show_network")
        private val SHOW_RAM = booleanPreferencesKey("show_ram")
        private val REFRESH_INTERVAL = longPreferencesKey("refresh_interval")
        private val SCALE = floatPreferencesKey("scale")
        private val BACKGROUND_BLUR = booleanPreferencesKey("background_blur")
        private val THEME_NAME = stringPreferencesKey("theme_name")
        private val COMPACT_MODE = booleanPreferencesKey("compact_mode")
    }

    val config: Flow<OverlayConfig> = context.dataStore.data.map { prefs ->
        OverlayConfig(
            position = prefs[POSITION]?.let { OverlayPosition.valueOf(it) }
                ?: OverlayPosition.TOP_LEFT,
            opacity = prefs[OPACITY] ?: 0.85f,
            showFps = prefs[SHOW_FPS] ?: true,
            showCpu = prefs[SHOW_CPU] ?: true,
            showGpu = prefs[SHOW_GPU] ?: true,
            showTemp = prefs[SHOW_TEMP] ?: true,
            showNetwork = prefs[SHOW_NETWORK] ?: true,
            showRam = prefs[SHOW_RAM] ?: false,
            refreshIntervalMs = prefs[REFRESH_INTERVAL] ?: 1000L,
            scale = prefs[SCALE] ?: 1.0f,
            backgroundBlur = prefs[BACKGROUND_BLUR] ?: true,
            themeName = prefs[THEME_NAME] ?: "OCEAN",
            compactMode = prefs[COMPACT_MODE] ?: false
        )
    }

    suspend fun updateConfig(config: OverlayConfig) {
        context.dataStore.edit { prefs ->
            prefs[POSITION] = config.position.name
            prefs[OPACITY] = config.opacity
            prefs[SHOW_FPS] = config.showFps
            prefs[SHOW_CPU] = config.showCpu
            prefs[SHOW_GPU] = config.showGpu
            prefs[SHOW_TEMP] = config.showTemp
            prefs[SHOW_NETWORK] = config.showNetwork
            prefs[SHOW_RAM] = config.showRam
            prefs[REFRESH_INTERVAL] = config.refreshIntervalMs
            prefs[SCALE] = config.scale
            prefs[BACKGROUND_BLUR] = config.backgroundBlur
            prefs[THEME_NAME] = config.themeName
            prefs[COMPACT_MODE] = config.compactMode
        }
    }
}
