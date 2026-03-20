# PerfOverlay 🐟

Real-time performance overlay for Android. FPS, CPU, GPU, temperatures, RAM, and network — all on screen, beautifully.

![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?logo=kotlin&logoColor=white)
![Compose UI](https://img.shields.io/badge/Compose-UI-blue)
![License MIT](https://img.shields.io/badge/License-MIT-green.svg)

## Features

| Stat | Details |
|------|---------|
| FPS | Real-time frame rate via Choreographer, color-coded badge (🟢🟡🔴) |
| Frame Time | Per-frame duration strip, dropped frame detection, P95/P99 stats |
| CPU | Usage % + frequency (MHz) + governor with animated bar, delta-based from `/proc/stat` |
| GPU | Usage % — Adreno & Mali support via sysfs |
| Temperature | CPU, GPU, battery temps (device-dependent, reads `/sys/class/thermal/`) |
| RAM | Used/total with usage bar |
| Network | Download/upload speeds in real-time via TrafficStats |
| Throttle Detection | Real-time warning when CPU/GPU frequency drops under thermal load |
| Anomaly Timeline | Rolling baseline with 2σ spike detection per metric |

## Modes

| Mode | Description |
|------|-------------|
| Overlay | Floating performance stats on any app |
| Record | Capture sessions with per-app tracking, graphs, and export |
| Stress Test | Controlled CPU load with thermal ceiling detection |
| Compare | Side-by-side session comparison with delta visualization |

## Themes

Six built-in color themes for the overlay:

| Theme | Vibe |
|-------|------|
| 🌊 Ocean | Default blue-green, calm and clean |
| 💜 Amethyst | Purple-pink, elegant and bold |
| 💚 Emerald | Green, nature-inspired freshness |
| 🌅 Sunset | Orange-warm, energetic and fiery |
| ⬜ Mono | Grayscale, minimal and focused |
| ⚡ Cyber | Neon cyan-magenta, cyberpunk intensity |

Themes affect all accent colors, FPS badge states, stat bars, and the glassmorphism glow.

## Compact Mode

Toggle compact mode for a minimal horizontal bar overlay — FPS badge + condensed stat pills side by side. Perfect for gaming or when you want maximum screen real estate.

## Glassmorphism UI

Frosted glass cards with real gaussian blur (Android 12+ via `RenderEffect`) and gradient accents. Inspired by iOS control center — floating, translucent, unobtrusive. Falls back to layered gradient approximation on older devices.

## How It Works

- **No root required** — uses Android's overlay permission (`SYSTEM_ALERT_WINDOW`)
- **Shizuku support** — grant overlay permission automatically via Shizuku (no manual Settings needed)
- **True blur** — Android 12+ uses `RenderEffect.createBlurEffect()` for real frosted glass
- **Drag to reposition** — long-press and drag the overlay anywhere on screen
- **Lightweight** — reads `/proc/stat`, `/sys/class/thermal/`, and `TrafficStats` directly
- **Configurable** — toggle individual stats, adjust opacity/scale, choose overlay position
- **Persistent** — settings saved via DataStore, survives reboots

## Shizuku Integration

PerfOverlay supports [Shizuku](https://shizuku.rikka.app/) for enhanced functionality:

- **Auto-grant overlay permission** — when Shizuku is running, PerfOverlay can grant `SYSTEM_ALERT_WINDOW` via `appops` without the user navigating to Settings
- **No root needed** — Shizuku works via ADB (wireless debugging on Android 11+, or USB on older)
- **Status indicator** — app shows Shizuku connection state (running / not running / permission denied)

### Setup
1. Install [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api)
2. Start Shizuku (via wireless debugging or ADB)
3. Open PerfOverlay — it will detect Shizuku and offer a "⚡ Shizuku" button for one-tap permission granting

## Data Sources

| Data | Source |
|------|--------|
| CPU usage | `/proc/stat` (delta-based) |
| CPU frequency | `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq` |
| GPU usage | `/sys/class/kgsl/` (Adreno) or `/sys/class/devfreq/` (Mali) |
| Temperatures | `/sys/class/thermal/thermal_zone*/temp` |
| RAM | `ActivityManager.MemoryInfo` |
| Network | `TrafficStats` |

## Build

```bash
git clone https://github.com/artos-n/PerfOverlay.git
cd PerfOverlay
./gradlew assembleDebug
```

Requires Android Studio Hedgehog+ and JDK 17.

## Roadmap

- [x] Real frosted-glass blur (Android 12+)
- [x] Drag-to-reposition overlay
- [x] Shizuku integration (auto-grant overlay permission)
- [x] Per-app performance recording & graphs
- [x] Custom themes & color schemes (6 themes)
- [x] Compact/minimal mode
- [x] Export performance logs (CSV & JSON)
- [x] Auto-start on boot
- [x] Frame time graph with dropped frame detection
- [x] Thermal throttling detection & warning overlay
- [x] Anomaly timeline with rolling baseline detection
- [x] CPU governor tracking & display
- [x] Session diff / comparison with delta visualization
- [x] Quick Settings tile with live FPS
- [x] Stress test mode with thermal ceiling detection
- [ ] Widget support
- [ ] CPU governor tracking
- [ ] Quick Settings tile
- [ ] Stress test mode
- [ ] Widget support

## Inspired By

[TakoStats](https://play.google.com/store/apps/details?id=rikka.fpsmonitor) by RikkaApps.

## License

[MIT](LICENSE) © 2026 artos
