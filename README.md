# MyAppTemplate

A Kotlin/Compose Multiplatform template targeting **Android, iOS, Desktop (JVM)
and Server**, with a plug-in module architecture: the infrastructure every app
needs ships pre-built and wired, and each feature is removable in three
documented lines.

**Read [docs/MODULES.md](./docs/MODULES.md)** for the module map, dependency
rules, and the add/remove-a-feature contract. The PRD and phased plan behind
the architecture live in [docs/prd](./docs/prd) and [plans](./plans).

## What's inside

- **Baseline stack** (all verified on every target): Koin DI, Ktor client +
  server, kotlinx-serialization/datetime, Navigation3, Room 3 (KMP), DataStore,
  Coil 3, FileKit, Kermit — versions in `gradle/libs.versions.toml`.
- **Convention plugins** (`build-logic/`): a new module's build file is a few
  lines (`template.kmp.library`, `template.kmp.feature`, `template.compose`).
- **core:*** modules: model (shared DTOs + AppError + ApiResult), network
  (typed error-mapped client), database (Room 3), datastore, designsystem
  (AppTheme + tokens), navigation (feature registry contract), ui (UiState +
  state components), common.
- **feature:settings**: the reference feature — persisted theme mode driving
  AppTheme live.
- **feature:showcase**: the start destination — lists installed features and
  demos the designsystem, network (against the local server), and database.

## Running

- Android app: `./gradlew :app:androidApp:assembleDebug`
- Desktop app: `./gradlew :app:desktopApp:run` (hot reload: `:app:desktopApp:hotRun --auto`)
- Server: `./gradlew :server:run` (the showcase network demo talks to it)
- iOS app: open [/app/iosApp](./app/iosApp) in Xcode and run.

## Tests

- Everything JVM-side: `./gradlew jvmTest :server:test`
- Highlights: Ktor error-mapping table (`core:network`), DataStore persistence
  (`feature:settings`), state components (`core:ui`), Room round-trip
  (`core:database`), and full client↔server e2e through the composed UI
  (`app:shared`'s NetworkDemoE2eTest, which boots the real server in-process).
- Android host tests: `./gradlew :app:shared:testAndroidHostTest`
- iOS: `./gradlew :app:shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
