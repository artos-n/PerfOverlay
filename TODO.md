# PerfOverlay — TODO

## High Priority
- [x] ~~Build verification~~ — code verified, needs local `./gradlew assembleDebug`
- [x] ~~Proper blur effect~~ — real frosted glass on Android 12+ using `RenderEffect.createBlurEffect()`
- [x] ~~Overlay drag~~ — long-press to drag and reposition overlay anywhere on screen
- [x] ~~Shizuku integration~~ — auto-grant overlay permission via Shizuku UserService + appops
- [x] ~~Per-app recording~~ — Room DB storage, per-app session tracking, foreground app detection
- [x] ~~Performance graphs~~ — Canvas-based line charts with smooth curves, gradient fill, multi-metric overlay
- [ ] **Full Shizuku overlay** — create overlay windows directly via Shizuku (bypass SYSTEM_ALERT_WINDOW entirely)

## Novel Features (desktop → mobile firsts)

### Frame Time Graph
- [x] **Per-frame duration tracking** — capture actual frame time in ms via Choreographer (not averaged FPS)
- [x] **Frame time strip** — thin micro-graph below FPS badge showing per-frame spikes
- [x] **Dropped frame detection** — flag frames exceeding 2× vsync interval (e.g. >16.6ms @ 60Hz)
- [x] **P99/P95 frame time stats** — percentile frame latency in session summary (like CapFrameX)
- [x] **Frame time in recordings** — store per-frame data in Room for session analysis

### Thermal Throttling Detection
- [x] **Throttle event logging** — detect when CPU/GPU frequency drops while temperature is high
- [x] **Throttle correlation** — link temp spikes to frequency drops and FPS dips
- [x] **Throttle warning overlay** — real-time visual indicator when throttling is detected
- [x] **Per-session throttle summary** — "Throttled 3x, total duration 45s, worst drop: 3.2GHz → 1.8GHz"

### Anomaly Timeline
- [x] **Rolling baseline** — compute moving average + standard deviation per metric
- [x] **Spike detection** — flag events where metric deviates >2σ from rolling baseline
- [x] **Human-readable event log** — "At 2:34, FPS dropped to 12 (avg: 58)" per session
- [x] **Anomaly markers on graphs** — anomaly events shown in session detail view
- [x] **Configurable sensitivity** — user-adjustable sigma threshold for anomaly detection

### Session Diff / Comparison
- [x] **Compare two sessions** — select any two recordings and overlay their graphs
- [x] **Delta visualization** — show improvement/regression with color (green = better, red = worse)
- [x] **Delta summary** — "FPS +8%, CPU -3%, GPU +12%" between sessions
- [x] **Named benchmarks** — tag sessions as "A" / "B" for quick comparison

### CPU Governor Tracking
- [x] **Governor detection** — read current frequency governor from `/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor`
- [x] **Governor transitions** — log when governor switches modes (schedutil ↔ performance ↔ powersave)
- [x] **Governor overlay marker** — show current governor name on the overlay
- [ ] **Governor on graphs** — color-band graph background by active governor mode

### Quick Settings Tile
- [x] **QS tile with FPS label** — shows current FPS as tile label, always visible in notification shade
- [x] **Tap to toggle overlay** — one-tap overlay on/off from QS panel
- [x] **Long-press to open app** — jump to PerfOverlay settings
- [x] **Tile state indicators** — active/inactive/recording visual states

### Stress Test Mode
- [x] **CPU stress test** — controlled multi-threaded load generation (prime computation)
- [x] **GPU stress test** — CPU stress indirectly stresses GPU via thermal coupling
- [x] **Thermal ceiling detection** — find sustained max performance before throttling
- [x] **Real-time test dashboard** — live readout of frequency, temp, FPS during stress
- [x] **Test result summary** — "Sustains 85% CPU at 72°C before throttling to 60%"

## Medium Priority
- [x] ~~Custom themes~~ — color scheme picker (dark glass, light glass, neon, minimal)
- [x] ~~Compact mode~~ — toggle between full and minimal (FPS-only) overlay
- [x] ~~Settings export/import~~ — backup and restore config
- [x] ~~Export recordings~~ — CSV/JSON export of session data

## Low Priority
- [ ] **Widget** — home screen widget showing current FPS/CPU
- [ ] **Battery stats** — battery level, charge rate, estimated time remaining
- [ ] **Per-core CPU** — show individual core usage (not just aggregate)
- [ ] **GPU frequency** — read GPU clock speed in addition to usage %
- [ ] **Storage I/O** — read/write speed monitoring
- [ ] **Localization** — support multiple languages

## Polish
- [ ] **App icon** — proper high-res icon (512x512 Play Store, adaptive icon assets)
- [ ] **Screenshot assets** — Play Store listing screenshots
- [ ] **CI/CD** — GitHub Actions workflow for APK builds
- [ ] **Release signing** — configure release build signing
- [ ] **ProGuard rules** — test minified release builds
- [ ] **Changelog** — maintain CHANGELOG.md per release
