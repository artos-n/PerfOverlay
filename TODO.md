# PerfOverlay — TODO

## High Priority
- [x] ~~Build verification~~ — code verified, needs local `./gradlew assembleDebug`
- [x] ~~Proper blur effect~~ — real frosted glass on Android 12+ using `RenderEffect.createBlurEffect()`
- [x] ~~Overlay drag~~ — long-press to drag and reposition overlay anywhere on screen
- [x] ~~Shizuku integration~~ — auto-grant overlay permission via Shizuku UserService + appops
- [ ] **Per-app FPS** — detect which app is in foreground and track its frame rate separately
- [ ] **Full Shizuku overlay** — create overlay windows directly via Shizuku (bypass SYSTEM_ALERT_WINDOW entirely)

## Medium Priority
- [ ] **Per-app recording** — record stats per application, store in Room DB
- [ ] **Performance graphs** — show recorded data as line charts (CPU/FPS/RAM over time)
- [ ] **Custom themes** — color scheme picker (dark glass, light glass, neon, minimal)
- [ ] **Compact mode** — toggle between full and minimal (FPS-only) overlay
- [ ] **Settings export/import** — backup and restore config

## Low Priority
- [ ] **Widget** — home screen widget showing current FPS/CPU
- [ ] **Export logs** — CSV/JSON export of recorded performance data
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
