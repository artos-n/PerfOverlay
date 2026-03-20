# PerfOverlay — TODO

## High Priority
- [x] ~~Build verification~~ — code verified, needs local `./gradlew assembleDebug`
- [x] ~~Proper blur effect~~ — real frosted glass on Android 12+ using `RenderEffect.createBlurEffect()`
- [x] ~~Overlay drag~~ — long-press to drag and reposition overlay anywhere on screen
- [x] ~~Shizuku integration~~ — auto-grant overlay permission via Shizuku UserService + appops
- [x] ~~Per-app recording~~ — Room DB storage, per-app session tracking, foreground app detection
- [x] ~~Performance graphs~~ — Canvas-based line charts with smooth curves, gradient fill, multi-metric overlay
- [ ] **Full Shizuku overlay** — create overlay windows directly via Shizuku (bypass SYSTEM_ALERT_WINDOW entirely)

## Medium Priority
- [ ] **Custom themes** — color scheme picker (dark glass, light glass, neon, minimal)
- [ ] **Compact mode** — toggle between full and minimal (FPS-only) overlay
- [ ] **Settings export/import** — backup and restore config
- [ ] **Export recordings** — CSV/JSON export of session data

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
