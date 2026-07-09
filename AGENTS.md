# Agent / developer guide

## Product

**Never Sleep** (`com.lizz.neversleep`) is an Android utility that prevents the
screen from turning off due to inactivity.

- Writes a very high value to `Settings.System.SCREEN_OFF_TIMEOUT` (requires
  **Modify system settings** / `WRITE_SETTINGS`).
- Persists the previous timeout locally and restores it when turned off.
- Works after leaving the app (unlike `FLAG_KEEP_SCREEN_ON`).

**Surfaces users care about**

| Surface | Where |
|--------|--------|
| Main toggle UI | `app/androidApp` → `MainActivity.kt` |
| Quick Settings tile | `NeverSleepTileService.kt` |
| 1×1 home-screen widget | `NeverSleepWidgetProvider.kt`, `NeverSleepWidgetReceiver.kt` |
| Shared toggle logic | `NeverSleepController.kt` |
| Optional banner ads (AdMob) | `AdBanner.kt`, `NeverSleepApplication.kt` |
| Privacy policy (Play requirement) | https://neversleep.app/privacy |
| Marketing site | https://neversleep.app/ (`web/apps/landing`) |

**Not the product (template scaffold, kept for future expansion)**

- `app/shared`, `app/desktopApp`, `app/iosApp`, `feature/*`, `core/*`, `server/`
- The shipping Android app in `app/androidApp` does **not** depend on
  `app:shared`; it is a standalone Compose app.

When changing Never Sleep behavior, work in **`app/androidApp`** unless explicitly
asked to extend the KMP template.

---

## Repository layout

```
app/androidApp/          ← PRODUCT: shipping Android app
assets/play-store/       ← Play Console graphics + preview.html
scripts/                 ← release-android.sh, generate-play-screenshots.py
web/                     ← Bun workspace: landing site (+ local-only dev tools)
docs/                    ← ICON_GENERATION, MODULES, ARCHITECTURE (template depth)
build-logic/             ← Gradle convention plugins (template)
feature/, core/, server/ ← KMP template modules (CI still compiles them)
```

---

## Commands

### Android (primary)

```bash
./gradlew :app:androidApp:assembleDebug
# APK: app/androidApp/build/outputs/apk/debug/androidApp-debug.apk

./gradlew :app:androidApp:bundleRelease   # Play Store AAB
./scripts/release-android.sh              # signed APK/AAB → dist/release/
./scripts/release-android.sh --screenshots  # + Play screenshots on device
```

Configure once (gitignored): `keystore.properties`, `app/androidApp/admob.properties`
(examples: `keystore.properties.example`, `admob.properties.example`).

### Kotlin quality (CI `quality` job only)

```bash
./gradlew qualityCheck      # detekt + ktlint — canonical check
./gradlew ktlintFormat      # canonical format before commit
```

### Web landing (marketing site)

```bash
cd web && bun install
bun run dev:landing         # http://127.0.0.1:5173
bun run lint && bun run typecheck && bun run test && bun run build
```

Local-only Play listing reference (not deployed):

```bash
cd web && bun run dev:landing:play   # copies dev/play-listing.astro → /play
```

Deploy: push `web/**` to `main` → GitHub Actions `deploy-landing.yml` →
GitHub Pages at `neversleep.app`.

### Template / CI (secondary)

```bash
./gradlew jvmTest :server:test
./gradlew :app:shared:compileKotlinIosArm64   # macOS CI job
```

---

## Android app conventions

- **Package:** `com.lizz.neversleep` — all product Kotlin lives under
  `app/androidApp/src/main/kotlin/com/lizz/neversleep/`.
- **State:** `NeverSleepController` is the single source of truth for
  enabled/disabled, previous timeout, and ads preference (`SharedPreferences`).
- **Permissions:** `WRITE_SETTINGS` is special — user must grant via system
  Settings; UI must explain before requesting.
- **Screenshot mode:** `MainActivity` extras `screenshot_mode` /
  `capture_never` — used by `scripts/generate-play-screenshots.py` for Play
  assets (headline band + cropped UI; not used on the public website).
- **Versions:** `versionCode` / `versionName` in
  `app/androidApp/build.gradle.kts` — bump `versionCode` every Play upload.
- **Privacy URL:** baked into `BuildConfig.PRIVACY_POLICY_URL` and
  `res/values/strings.xml` → must stay `https://neversleep.app/privacy`.

Do not wire `app/androidApp` into `app:shared` or feature modules unless the
user explicitly asks to merge the product into the KMP shell.

---

## Web conventions

Read **web/AGENTS.md** when editing `web/`.

- `apps/landing` — static Astro site; React islands only where needed (Paper
  Shaders hero). **Do not** use Play Store screenshot PNGs on the landing hero;
  use the CSS app mock in `index.astro`.
- `apps/admin` — Vite React scaffold; proxies `/api` to Ktor `:8080` (template).
- Keep Bun/Vite/Astro tooling inside `web/`; do not add to the Gradle graph.
- Official **Get it on Google Play** badge: `public/badges/en_badge_web_generic.png`
  via `GooglePlayBadge.astro` — do not recolor or crop per Google guidelines.
- Public pages: `/`, `/privacy`, `404` only. `/play` is dev-local.

---

## Play Store & assets

- Listing copy / AdMob IDs / checklists: `web/apps/landing/dev/play-listing.astro`
  (local dev) and `assets/play-store/preview.html`.
- Graphics: `assets/play-store/` (icon, feature graphic, screenshots).
- Regenerate screenshots: `python3 scripts/generate-play-screenshots.py`
  (requires adb + device + Pillow).
- Icon/tile/widget generation prompts: **docs/ICON_GENERATION.md**.

---

## CI overview

| Workflow | What it checks |
|----------|----------------|
| `ci.yml` | Android debug + release bundle, JVM tests, ktlint/detekt, web checks, iOS compile |
| `deploy-landing.yml` | Build & publish `web/apps/landing/dist` to GitHub Pages |

---

## Deeper reference (template / expansion)

- Module map & add/remove feature contract: **docs/MODULES.md**
- Layer rules & feature anatomy: **docs/ARCHITECTURE.md** (copy `feature/notes`)
- New Gradle modules: `template.kmp.library`, `template.kmp.feature`,
  `template.compose` in `build-logic/`
- Versions: Kotlin/Gradle → `gradle/libs.versions.toml`; web → `web/package.json`

---

## Agent guardrails

- Prefer **small, product-focused diffs** in `app/androidApp` and `web/apps/landing`.
- Do not refactor or delete template modules unless asked.
- Do not commit secrets (`keystore.properties`, `admob.properties`, keystores).
- Run `./gradlew ktlintFormat` before Kotlin commits; `cd web && bun run format`
  before web commits.
- Human-facing README: **README.md**; web setup: **web/README.md**.