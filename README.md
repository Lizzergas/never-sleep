# Never Sleep

Android utility that prevents your screen from turning off due to inactivity.

- One tap in the app, a **Quick Settings tile**, or a **1×1 home-screen widget**
- Uses **Modify system settings** to raise `SCREEN_OFF_TIMEOUT` system-wide
- Restores your previous timeout when you turn it off
- Optional **AdMob** banner (disable in Settings)

**Package:** `com.lizz.neversleep` · **Site:** https://neversleep.app · **Privacy:** https://neversleep.app/privacy

## Quick start (Android)

```bash
./gradlew :app:androidApp:assembleDebug
```

APK: `app/androidApp/build/outputs/apk/debug/androidApp-debug.apk`

Product code lives in `app/androidApp/src/main/kotlin/com/lizz/neversleep/` —
mainly `MainActivity.kt`, `NeverSleepController.kt`, tile/widget services.

## Release to Google Play

```bash
./scripts/release-android.sh              # qualityCheck + signed APK/AAB → dist/release/
./scripts/release-android.sh --fast       # skip qualityCheck
./scripts/release-android.sh --screenshots  # + polished screenshots on device
```

Setup (gitignored):

- `keystore.properties` ← `keystore.properties.example`
- `app/androidApp/admob.properties` ← `admob.properties.example`

Bump `versionCode` in `app/androidApp/build.gradle.kts` before every upload.

Play assets: `assets/play-store/` · preview locally: open `assets/play-store/preview.html`

## Web (marketing site)

Static landing at **neversleep.app**, built from `web/apps/landing` (Astro + Paper Shaders).

```bash
cd web && bun install
bun run dev:landing
```

Deploy: push `web/**` to `main` → `.github/workflows/deploy-landing.yml` (GitHub Pages).

Local Play listing reference (not on production): `bun run dev:landing:play`

## Repo structure

| Path | Role |
|------|------|
| `app/androidApp/` | **Shipping Android app** |
| `web/apps/landing/` | Public website (`/`, `/privacy`) |
| `assets/play-store/` | Play Console graphics |
| `scripts/` | Release + screenshot automation |
| `app/shared`, `feature/*`, `core/*`, `server/` | KMP template scaffold (not wired into the Android app today) |

Agents and contributors: read **[AGENTS.md](./AGENTS.md)**.

Template module map: [docs/MODULES.md](./docs/MODULES.md) · architecture: [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)

## Tests & quality

```bash
./gradlew jvmTest :server:test :app:androidApp:assembleDebug
./gradlew qualityCheck    # detekt + ktlint (CI quality job)
./gradlew ktlintFormat    # format Kotlin before commit
cd web && bun run lint && bun run typecheck && bun run test && bun run build
```

## Icons & Play assets

See [docs/ICON_GENERATION.md](./docs/ICON_GENERATION.md) for launcher, tile, widget, and store asset generation.