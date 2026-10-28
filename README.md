# Never Sleep

Android utility that prevents your screen from turning off due to inactivity.

- One tap in the app, a **Quick Settings tile**, or a **1×1 home-screen widget**
- Uses **Modify system settings** to raise `SCREEN_OFF_TIMEOUT` system-wide
- Restores your previous timeout when you turn it off
- Optional **AdMob** banner (disable in Settings)

**Package:** `com.lizz.neversleep` · **Site:** https://neversleep.app · **Privacy:** https://neversleep.app/privacy

## Quick start (Android)

The Gradle daemon requires Amazon Corretto JDK 21 (pinned in
`gradle/gradle-daemon-jvm.properties`). Before the first Android Studio sync, run
`./gradlew help` once to let the wrapper download the matching JDK. If the shell
cannot find Java, set `JAVA_HOME` to Android Studio's bundled JDK first; on macOS
with Android Studio Preview:

```bash
export JAVA_HOME="/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home"
./gradlew help
```

This also fixes a first-sync failure reporting `Service 'SystemInfo' is not
available (... enabled=false)` while provisioning the daemon JDK. After the
command succeeds, retry **Sync Project with Gradle Files** in Android Studio.

```bash
./gradlew :app:androidApp:assembleDebug
```

APK: `app/androidApp/build/outputs/apk/debug/androidApp-debug.apk`

The standalone app currently builds for **Android 7.0+ (API 24)**. See the
[compatibility experiment](docs/ANDROID_7_COMPATIBILITY.md) for verified behavior
and remaining physical-device checks. Android 7–12 uses the animated Canvas
fallback; Android 13+ uses the full shader. KMP template modules retain their
separate Android 12 minimum.

The product compiles and targets **API 37**, with emulator checks on Android 16
and Android 17 (including a 16 KB page-size image). The checks cover cold launch,
the main toggle, exact timeout restoration, operation after leaving/force-stopping
the app, permission revocation, and Quick Settings. Physical-device OEM behavior
still needs device-specific verification.

Product code lives in `app/androidApp/src/main/kotlin/com/lizz/neversleep/` —
mainly `MainActivity.kt`, `NeverSleepController.kt`, tile/widget services.

## iOS prototype (paused)

iOS development is paused: the prototype cannot provide the Android product’s
system-wide keep-awake behavior. No iOS release is planned.

`app/iosApp` is a standalone SwiftUI app for iPhone and iPad on iOS 18.2+.
Open `app/iosApp/iosApp.xcodeproj` and choose the **NeverSleep** scheme.
It does not use the KMP UI template. Its small review-prompt policy is shared
through the `core/review` Kotlin framework, built by the Xcode scheme.

iOS sessions keep **Never Sleep itself** awake with one tap and a duration picker.
Leaving the app restores normal Auto-Lock;
returning before the session expires resumes protection. The user's global
Auto-Lock setting and other apps are never changed. A Control Center control,
Home Screen widget, and App Shortcut open the app and start a session.

Build and run unit/UI tests with an installed iOS Simulator:

```bash
./scripts/fastlane.sh ios check
# Or without Fastlane; optionally supply an explicit Simulator UDID:
bash scripts/check-ios.sh
```

The check boots the selected simulator and writes results and screenshot
attachments to `dist/ios-check/`. See [iOS development](docs/IOS_DEVELOPMENT.md)
for architecture and manual verification. Signing and App Store delivery are
separate from these simulator checks.

## Release to Google Play

```bash
./scripts/fastlane.sh android release     # checked + signed APK/AAB + store package
./scripts/fastlane.sh android check_all   # former CI checks, including template/iOS/web
./scripts/fastlane.sh android assets      # validate Play copy and graphics
# ./scripts/release-android.sh remains an alias for the release lane.
```

Prerequisites: a managed Ruby (3.3+), Bundler, JDK 21+, Android SDK 37,
Python 3.9+, Bun 1.3.14 and a supported Node.js LTS for web checks. On macOS
the wrapper prefers Homebrew Ruby; gems install into ignored `vendor/bundle/`.
`Gemfile.lock` pins the release tooling. Set `ANDROID_HOME` for a non-default SDK.

Signing setup (once, only if no upload key already exists):

```bash
python3 scripts/setup-android-signing.py
```

This creates `keystores/never-sleep-upload.keystore`, a public upload certificate,
and `keystore.properties` with a generated password. Private files are mode 600 and
gitignored. Back up the keystore **and** properties together in secure storage;
never regenerate them for an app already uploaded to Play.

Configure `app/androidApp/admob.properties` from its example. Production IDs must
match the AdMob app/banner and `web/apps/landing/public/app-ads.txt`. Release builds
reject missing signing and test/placeholder ad IDs. Debug builds always use test ads.
Publish consent messages and complete app verification in AdMob before expecting
production ad serving.

Outputs in `dist/release/`:

- `androidApp-release.aab` — signed Play upload
- `androidApp-release.apk` — signed device testing
- `play-store-assets.zip` — English listing, icon, feature graphic, screenshots
- `upload-certificate.pem` — public certificate (safe to give to Play)
- `release-manifest.json` — version, certificate fingerprint and SHA-256 checksums

The release lane verifies APK/AAB signatures against the configured upload key.
It builds locally and does **not** upload or submit an app for review.
Use a new `versionCode` for each new bundle uploaded to Play; promote an existing
release between tracks without rebuilding it.

Release R8 rules rely on dependency consumer rules, with a targeted constructor
rule for WorkManager's Room database. Avoid package-wide keep rules: they disable
most shrinking and optimization, and debug builds cannot verify these rules.

The app does not use AndroidX `graphics-path` iteration. Release builds omit its
stripped native binary and ignore its JNI keep rule. A `-checkdiscard` guard,
combined with disabled optimization for that package, requires all of its code
to be removed. If a dependency update or UI change needs path iteration, the
release build fails; restore the library and its consumer rules before proceeding.
Debug builds retain the native library.

Store copy: `fastlane/metadata/android/en-US/`. Graphics are symlinked there from
`assets/play-store/`; the packaged ZIP contains ordinary files.
Preview: `assets/play-store/preview.html`.

```bash
# Pillow is needed only for screenshot rendering.
uv run --with Pillow scripts/generate-play-screenshots.py --device SERIAL
# Re-render existing raw captures without using a device:
uv run --with Pillow scripts/generate-play-screenshots.py --from-raw
```

Screenshots are 1080×1920, opaque RGB PNGs. Capture selects an explicit device when
several are connected and restores its screen timeout and special permission.
The two main states are captured automatically. Settings and permission images
are rendered from `screenshots/raw/settings.png` and `permission.png`; refresh
those device captures when their UI changes.

Before the first Play submission:

- Back up the upload key and signing properties securely.
- Complete the Play listing, app signing enrollment, Data safety, advertising,
  content rating, target audience, and any account-specific testing requirements.
  Public identity: **Lizz Studio**, **home@lizz.com**.
- In AdMob, publish applicable consent messages, verify the app, and link its
  eventual Play listing. The production IDs and public `app-ads.txt` are configured.
- Test the app, tile, and widget together on a physical device, including permission
  revocation. All use sun for ON and moon for OFF; changing the mode also refreshes
  placed widgets.
- Smoke-test the minified release APK on API 24 and a modern Android version.
  Debug builds alone cannot catch the older-platform shader resolution regression
  described in [the compatibility notes](docs/ANDROID_7_COMPATIBILITY.md).

No Play Console upload or submission is performed by these scripts.

## Web (marketing site)

Static landing at **neversleep.app**, built from `web/apps/landing` (Astro + Paper Shaders).

```bash
cd web && bun install
bun run dev:landing
```

Publish the website from your machine:

```bash
./scripts/fastlane.sh website
```

This checks and builds locally, then publishes only static output to `gh-pages`.
GitHub Pages serves it at `neversleep.app`. CI and the old landing build workflow
are disabled in GitHub and manual-only in source. No app releases run on GitHub.

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

## Launch marketing

The Play listing emphasizes “Keep Screen On”. The website and Remotion promos
use ON with the sun and OFF with the night scene.

```bash
./scripts/marketing.sh render    # one silent 14-second app demo
./scripts/marketing.sh prepare   # tagged campaign links and post captions
./scripts/marketing.sh report marketing/reports/first-week.json
```

See [marketing/README.md](marketing/README.md) for publication and measurement.
Output: `dist/marketing/`. Attribution uses public campaign labels in Play URLs;
there is no additional app analytics SDK. Actual metrics begin after launch.
