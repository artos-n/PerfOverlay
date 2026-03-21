# PerfOverlay — TODO

## ✅ Completed

### Core (High Priority)
- [x] ~~Build verification~~ — code verified, needs local `./gradlew assembleDebug`
- [x] ~~Proper blur effect~~ — real frosted glass on Android 12+ using `RenderEffect.createBlurEffect()`
- [x] ~~Overlay drag~~ — long-press to drag and reposition overlay anywhere on screen
- [x] ~~Shizuku integration~~ — auto-grant overlay permission via Shizuku UserService + appops
- [x] ~~Per-app recording~~ — Room DB storage, per-app session tracking, foreground app detection
- [x] ~~Performance graphs~~ — Canvas-based line charts with smooth curves, gradient fill, multi-metric overlay
- [x] ~~Custom themes~~ — 6 built-in color themes (Ocean, Amethyst, Emerald, Sunset, Mono, Cyber)
- [x] ~~Compact mode~~ — minimal horizontal bar overlay toggle
- [x] ~~Export recordings~~ — CSV and JSON export to Downloads
- [x] ~~Auto-start on boot~~ — BootReceiver for persistent overlay

### Novel Features (desktop → mobile firsts)
- [x] ~~Frame time graph~~ — per-frame duration strip, dropped frame detection, P95/P99 stats
- [x] ~~Thermal throttling detection~~ — real-time warning overlay, temp-freq correlation
- [x] ~~Anomaly timeline~~ — rolling baseline (30-sample window), 2σ spike detection, Room storage
- [x] ~~Session diff / comparison~~ — side-by-side overlaid graphs, delta summary with color coding
- [x] ~~CPU governor tracking~~ — read `scaling_governor` from sysfs, display in overlay
- [x] ~~Quick Settings tile~~ — FPS label, tap toggle, throttle warning subtitle
- [x] ~~Stress test mode~~ — configurable CPU load generator, thermal ceiling detection, results dashboard

---

## 🔨 Remaining

### Novel Features
- [x] ~~Governor on graphs~~ — color-band graph background by active governor mode (schedutil=green, performance=blue, powersave=purple)

### Low Priority
- [ ] **Full Shizuku overlay** — create overlay windows directly via Shizuku (bypass SYSTEM_ALERT_WINDOW entirely)
- [ ] **Widget** — home screen widget showing current FPS/CPU
- [x] ~~Battery stats~~ — battery level, charge rate, estimated time remaining
- [x] ~~Per-core CPU~~ — show individual core usage (not just aggregate)
- [x] ~~GPU frequency~~ — read GPU clock speed in addition to usage %
- [ ] **Storage I/O** — read/write speed monitoring
- [ ] **Localization** — support multiple languages

### Polish
- [x] ~~App icon~~ — proper high-res icon (512x512 Play Store, adaptive icon assets)
- [ ] **Screenshot assets** — Play Store listing screenshots
- [x] ~~CI/CD~~ — GitHub Actions workflow for APK builds
- [ ] **Release signing** — configure release build signing
- [ ] **ProGuard rules** — test minified release builds
- [x] ~~Changelog~~ — maintain CHANGELOG.md per release
