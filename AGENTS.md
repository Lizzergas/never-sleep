# Agent / developer guide

Compose Multiplatform template (Android, iOS, Desktop JVM, Ktor server) with a
plug-in module architecture. Full module map and the add/remove-a-feature
contract: **docs/MODULES.md**. Layer rules, UiState/Event contract, and the
anatomy of a feature: **docs/ARCHITECTURE.md** — copy `feature/notes` when
building a new feature.

Optional web surfaces live under **web/** as a separate Bun workspace. When
working there, read **web/AGENTS.md** first and keep Node/Vite/Astro/Oxc
tooling out of the Gradle module graph unless explicitly requested.

<!-- TEMPLATE_ONLY_RENAME_START -->
## Renaming the template (do this first in a new app)

```
./rename.sh MyCoolApp                 # package becomes com.lizz.mycoolapp
./rename.sh MyCoolApp org.acme.cool   # custom package
```

Rewrites `com.lizz.myapptemplate` / `com/lizz/myapptemplate` /
`MyAppTemplate` / `myapptemplate` in all tracked files and moves the source
trees. Requires a clean working tree; review with `git diff`, verify, and
commit. The script removes itself after a successful rename.
<!-- TEMPLATE_ONLY_RENAME_END -->

## Commands

- Desktop app: `./gradlew :app:desktopApp:run`
- Server: `./gradlew :server:run` (showcase network demo talks to it)
- Android: `./gradlew :app:androidApp:assembleDebug`
- iOS: open `app/iosApp` in Xcode
- All meaningful tests: `./gradlew jvmTest :server:test`
- Web workspace: `cd web && bun install`; run landing with
  `bun run dev:landing`, admin with `bun run dev:admin`, and checks with
  `bun run lint && bun run typecheck && bun run test && bun run build`.
- Canonical Kotlin check: `./gradlew qualityCheck` (detekt + ktlint + the
  template assignment-wrapping guard). Enforced ONLY by the CI `quality` job,
  never by commit/push hooks.
- Canonical Kotlin format: `./gradlew ktlintFormat`. Use this before commit
  and from Android Studio/IDE external tools; built-in Reformat Code is not the
  final authority for this template. Coverage: `./gradlew koverHtmlReport`.

## Conventions

- New modules use the `build-logic` convention plugins:
  `template.kmp.library`, `template.kmp.feature`, `template.compose`.
- A feature = one `FeatureRegistration` object (routes, entries, optional
  deep links) + one Koin module, wired through `settings.gradle.kts`,
  `app/shared/build.gradle.kts`, `app/shared`'s `AppNavHost.kt`, and
  `di/Koin.kt` (the 4-touchpoint contract).
- Networking returns `ApiResult`/`AppError` (never throws); screens render
  through `core:ui`'s `UiState` components.
- Kotlin/Gradle versions live only in `gradle/libs.versions.toml`; web
  dependency versions live in `web/package.json` and package manifests under
  `web/apps/*` / `web/packages/*`.
