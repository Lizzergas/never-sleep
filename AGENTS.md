# Agent / developer guide

Compose Multiplatform template (Android, iOS, Desktop JVM, Ktor server) with a
plug-in module architecture. Full module map and the add/remove-a-feature
contract: **docs/MODULES.md**. Layer rules, UiState/Event contract, and the
anatomy of a feature: **docs/ARCHITECTURE.md** — copy `feature/notes` when
building a new feature.

## Renaming the template (do this first in a new app)

```
./rename.sh MyCoolApp                 # package becomes com.lizz.mycoolapp
./rename.sh MyCoolApp org.acme.cool   # custom package
```

Rewrites `com.lizz.myapptemplate` / `MyAppTemplate` / `myapptemplate` in all
tracked files and moves the source trees. Requires a clean working tree;
review with `git diff`, verify, commit, then delete `rename.sh`.

## Commands

- Desktop app: `./gradlew :app:desktopApp:run`
- Server: `./gradlew :server:run` (showcase network demo talks to it)
- Android: `./gradlew :app:androidApp:assembleDebug`
- iOS: open `app/iosApp` in Xcode
- All meaningful tests: `./gradlew jvmTest :server:test`
- Lint (detekt + ktlint): `./gradlew qualityCheck` — run whenever you want;
  enforced ONLY by the CI `quality` job, never by commit/push hooks.
  Auto-fix formatting: `./gradlew ktlintFormat`. Coverage: `./gradlew koverHtmlReport`.

## Conventions

- New modules use the `build-logic` convention plugins:
  `template.kmp.library`, `template.kmp.feature`, `template.compose`.
- A feature = one `FeatureRegistration` object + one Koin module, wired in
  `app/shared`'s `AppNavHost.kt` and `di/Koin.kt` plus a settings.gradle
  include (the 3-line contract).
- Networking returns `ApiResult`/`AppError` (never throws); screens render
  through `core:ui`'s `UiState` components.
- Versions live only in `gradle/libs.versions.toml`.
