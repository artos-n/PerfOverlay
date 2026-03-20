# PerfOverlay

Real-time performance overlay for Android. FPS, CPU, GPU, temperatures, RAM, and network — all on screen, beautifully.

<div align="center">

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-UI-blue)
![License](https://img.shields.io/badge/License-MIT-green.svg)

</div>

---

## Features

| Stat | Details |
|------|---------|
| **FPS** | Real-time frame rate with color-coded badges (green/yellow/red) |
| **CPU** | Usage percentage + frequency (MHz) with animated bar |
| **GPU** | Usage percentage with gradient bar |
| **Temperature** | CPU, GPU, battery temps (device-dependent) |
| **RAM** | Used/total with usage bar |
| **Network** | Download/upload speeds in real-time |

## Design

Glassmorphism UI with frosted glass cards, gradient accents, and a clean monospace data layout. Inspired by iOS control center aesthetics — floating, translucent, unobtrusive.

## How It Works

- **No root required** — works via Android's overlay permission (`SYSTEM_ALERT_WINDOW`)
- **Lightweight** — reads `/proc/stat`, `/sys/class/thermal/`, and `TrafficStats` directly
- **Configurable** — toggle individual stats, adjust opacity/scale, choose overlay position
- **Persistent** — settings saved via DataStore, survives reboots

## Stats Source

| Data | Source |
|------|--------|
| CPU usage | `/proc/stat` |
| CPU frequency | `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq` |
| Temperatures | `/sys/class/thermal/thermal_zone*/temp` |
| RAM | `ActivityManager.MemoryInfo` |
| Network | `TrafficStats` |

## Building

```bash
git clone https://github.com/artos-n/PerfOverlay.git
cd PerfOverlay
./gradlew assembleDebug
```

Requires Android Studio Hedgehog+ and JDK 17.

## Screenshots

```
 ┌─────────────────────────────┐
 │  PERF              144 ▓▓▓ │
 │                             │
 │  🧠 CPU   45%  2.4 GHz     │
 │  ▓▓▓▓▓▓▓▓░░░░░░░░░░░░     │
 │                             │
 │  🎮 GPU   62%              │
 │  ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░     │
 │                             │
 │  🌡 CPU 72°  GPU 65°       │
 │                             │
 │  📶 ↓ 12.3 MB/s  ↑ 2.1    │
 └─────────────────────────────┘
```

## Roadmap

- [ ] Per-app performance recording & graphs
- [ ] Shizuku integration (no overlay permission needed)
- [ ] Custom themes & color schemes
- [ ] Widget support
- [ ] Export performance logs

## Credits

Inspired by [TakoStats](https://play.google.com/store/apps/details?id=rikka.fpsmonitor) by RikkaApps.

## License

[MIT](LICENSE) © 2026 artos
