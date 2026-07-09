# NeverSleep

**Never Sleep** — an Android utility that prevents the screen from turning off due to inactivity.

- Uses `WRITE_SETTINGS` → writes a very high value to `Settings.System.SCREEN_OFF_TIMEOUT`.
- Works after you leave the app (unlike `FLAG_KEEP_SCREEN_ON`).
- Saves the previous timeout so you can restore it.

The rest of the Compose Multiplatform template structure is kept for future expansion if needed, but the deliverable right now is the focused Android app in `app/androidApp`.

Run / install:
- `./gradlew :app:androidApp:assembleDebug`
- The APK is at `app/androidApp/build/outputs/apk/debug/androidApp-debug.apk`

See the implementation in `app/androidApp/src/main/kotlin/com/lizz/neversleep/MainActivity.kt`.

The optional **web workspace** under [web](./web) hosts separate Bun/Vite/Astro
surfaces for landing pages and admin panels without changing the Gradle module
graph.

**Read [docs/MODULES.md](./docs/MODULES.md)** for the module map, dependency
rules, and the add/remove-a-feature contract.

## What's inside

- **Baseline stack** (all verified on every target): Koin DI, Ktor client +
  server, kotlinx-serialization/datetime, Navigation3, Room 3 (KMP), DataStore,
  Coil 3, FileKit, Kermit — versions in `gradle/libs.versions.toml`.
- **Convention plugins** (`build-logic/`): a new module's build file is a few
  lines (`template.kmp.library`, `template.kmp.feature`, `template.compose`).
- **core:*** modules: model (shared DTOs + AppError + ApiResult), common,
  connectivity, network (typed error-mapped client + optional bearer auth),
  database (Room 3), datastore, designsystem (Material 3 Expressive AppTheme +
  tokens), navigation (feature registry + typed deep link contract), and ui
  (UiState + state components).
- **feature:notes**: the reference feature — full server <-> Room <-> domain
  <-> UI chain with offline reads, server-authoritative writes, and account
  cleanup.
- **feature:auth**: register/login/logout, KVault-backed token storage on
  mobile, desktop file fallback, and automatic 401 refresh.
- **feature:onboarding**: first-launch flow that overrides the start route
  until completed.
- **feature:settings**: the smallest exemplar — persisted theme mode driving
  AppTheme live.
- **feature:showcase**: the start destination — lists installed features and
  demos the designsystem and network stack against the local server.
- **web/**: optional Bun workspace with an Astro landing page, Vite React admin
  app, Oxlint/Oxfmt tooling, and a placeholder TypeScript API client.

## Running

- Android app: `./gradlew :app:androidApp:assembleDebug`
- Desktop app: `./gradlew :app:desktopApp:run` (hot reload: `:app:desktopApp:hotRun --auto`)
- Server: `./gradlew :server:run` (the showcase network demo talks to it)
- iOS app: open [/app/iosApp](./app/iosApp) in Xcode and run.
- Web landing: `cd web && bun run dev:landing`
- Web admin: `cd web && bun run dev:admin` (proxies `/api` to the Ktor server on `:8080`)

## Tests

- Everything JVM-side: `./gradlew jvmTest :server:test`
- Full verification used by CI/local release checks:
  `./gradlew qualityCheck jvmTest :server:test :app:androidApp:assembleDebug :app:shared:compileKotlinIosArm64 koverHtmlReport`
- Canonical Kotlin formatting is `./gradlew ktlintFormat`; canonical Kotlin
  checking is `./gradlew qualityCheck`. Android Studio's built-in Reformat Code
  is useful while editing, but Gradle is the final formatting authority.
- Highlights: Ktor error-mapping and bearer refresh (`core:network` /
  `feature:auth`), DataStore persistence (`feature:settings` and
  `feature:onboarding`), state components (`core:ui`), Room round-trip
  (`core:database`), notes server/cache behavior (`feature:notes`), and full
  client↔server e2e through the composed UI (`app:shared` tests boot the real
  server in-process).
- Android host tests: `./gradlew :app:shared:testAndroidHostTest`
- iOS: `./gradlew :app:shared:iosSimulatorArm64Test`
- Web workspace:
  `cd web && bun run lint && bun run format:check && bun run typecheck && bun run test && bun run build`

---

## Releasing to Google Play (Release Builds)

### Versioning
- `versionCode` and `versionName` live in `app/androidApp/build.gradle.kts` → `defaultConfig`.
- **Always increment `versionCode`** (by 1 or more) before every Play upload.
- Use semantic `versionName` (e.g. `1.0.0`, `1.1.0`, `2.0.0-beta`).

### Building a Release
```bash
# Unsigned (or signed if you configured keystore)
./gradlew :app:androidApp:assembleRelease

# Preferred for Play Store: Android App Bundle (AAB)
./gradlew :app:androidApp:bundleRelease
```

APKs / bundles will be in:
`app/androidApp/build/outputs/{apk,bundle}/release/`

### One-command release script
```bash
./scripts/release-android.sh              # qualityCheck + signed APK/AAB → dist/release/
./scripts/release-android.sh --fast       # skip qualityCheck
./scripts/release-android.sh --screenshots  # build + polished Play screenshots (2 states)
python3 scripts/generate-play-screenshots.py  # screenshots only (headline + UI, no ads/settings)
```

Configure once:
- `keystore.properties` (from `keystore.properties.example`) — signing
- `app/androidApp/admob.properties` (from `admob.properties.example`) — replace test IDs with your AdMob console values before monetizing

Play listing copy and privacy policy live in the web workspace:
- `web/apps/landing/src/pages/privacy.astro` → `https://neversleep.app/privacy`
- `web/apps/landing/src/pages/play.astro` → store listing fields + DNS setup
- Store assets: `assets/play-store/` (icon, feature graphic, screenshots)
- **Preview all Play uploads locally:** open `assets/play-store/preview.html` in a browser

**Host `neversleep.app` (free):** enable GitHub Pages → GitHub Actions in repo Settings, push to `main` (workflow `.github/workflows/deploy-landing.yml`), then point your domain DNS at GitHub (see `/play` on the site or `preview.html`).

### Signing for Release (one-time setup)
1. Generate a keystore (store it safely):
   ```bash
   mkdir -p keystores
   keytool -genkey -v -keystore keystores/never-sleep-release.keystore \
     -alias never-sleep -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Copy the example and fill real values:
   ```bash
   cp keystore.properties.example keystore.properties
   # edit keystore.properties (this file is gitignored)
   ```

3. Re-run the release task. The build will pick up the signing config.

**Recommendation**: Use **Google Play App Signing**. Upload the AAB and let Google manage the final signing key.

### ProGuard / R8
Release builds automatically:
- Minify code (`isMinifyEnabled = true`)
- Shrink resources
- Use `proguard-rules.pro` (Compose + app specific keeps already included)

### Other Play Store prep notes
- App icon: now uses custom generated crescent moon design (in `mipmap-*` + adaptive).
- The `WRITE_SETTINGS` permission requires a prominent disclosure + consent in the app (already present in the disclaimer UI).
- Test the release build on a real device before uploading.
- For the first upload you will need to create a Google Play Console entry, fill privacy policy, content rating, etc.

See `keystore.properties.example` and the signing block in `app/androidApp/build.gradle.kts` for details.


Learn more
about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
