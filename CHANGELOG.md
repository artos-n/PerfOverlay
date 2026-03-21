# Changelog

All notable changes to PerfOverlay will be documented in this file.

## [1.0.0] - 2026-03-21

### Added
- Real-time FPS monitoring via Choreographer
- CPU usage, frequency, and governor tracking
- GPU usage (Adreno & Mali support) with frequency display
- Temperature monitoring (CPU, GPU, battery, device)
- RAM usage display
- Network speed (download/upload)
- Battery stats (level, charge rate, charging status)
- Per-core CPU usage display
- Floating overlay with drag-to-reposition
- Background blur (Android 12+ RenderEffect)
- Compact mode overlay
- 6 built-in color themes (Ocean, Amethyst, Emerald, Sunset, Mono, Cyber)
- Per-app performance recording with Room DB
- Performance graphs with smooth curves and governor color-bands
- Session comparison with delta visualization
- CSV and JSON export
- Auto-start on boot
- Shizuku integration for auto-grant overlay permission
- Frame time graph with dropped frame detection
- Thermal throttling detection with warning overlay
- Anomaly timeline with rolling baseline (2σ spike detection)
- Quick Settings tile with live FPS
- Stress test mode with thermal ceiling detection
- Adaptive app icon
- CI/CD via GitHub Actions
