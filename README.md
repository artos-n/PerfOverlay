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
| CPU | Usage % + frequency (MHz) with animated bar, delta-based from `/proc/stat` |
| GPU | Usage % — Adreno & Mali support via sysfs |
| Temperature | CPU, GPU, battery temps (device-dependent, reads `/sys/class/thermal/`) |
| RAM | Used/total with usage bar |
| Network | Download/upload speeds in real-time via TrafficStats |

## Glassmorphism UI

Frosted glass cards with gradient accents and clean monospace data layout. Inspired by iOS control center aesthetics — floating, translucent, unobtrusive.

```
 ┌─────────────────────────────┐
 │ PERF 144 ▓▓▓ │
 │ │
 │ 🧠 CPU 45% 2.4 GHz │
 │ ▓▓▓▓▓▓▓▓░░░░░░░░░░░░ │
 │ │
 │ 🎮 GPU 62% │
 │ ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░ │
 │ │
 │ 🌡 CPU 72° GPU 65° │
 │ │
 │ 📶 ↓ 12.3 MB/s ↑ 2.1 │
 └─────────────────────────────┘
```

## How It Works

- **No root required** — uses Android's overlay permission (`SYSTEM_ALERT_WINDOW`)
- **Lightweight** — reads `/proc/stat`, `/sys/class/thermal/`, and `TrafficStats` directly
- **Configurable** — toggle individual stats, adjust opacity/scale, choose overlay position
- **Persistent** — settings saved via DataStore, survives reboots

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

- [ ] Per-app performance recording & graphs
- [ ] Shizuku integration (no overlay permission needed)
- [ ] Custom themes & color schemes
- [ ] Widget support
- [ ] Export performance logs

## Inspired By

[TakoStats](https://play.google.com/store/apps/details?id=rikka.fpsmonitor) by RikkaApps.

## License

[MIT](LICENSE) © 2026 artos
