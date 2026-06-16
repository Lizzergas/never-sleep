# MyAppTemplate

A Kotlin/Compose Multiplatform template targeting **Android, iOS, Desktop (JVM)
and Server**, with a plug-in module architecture: the infrastructure every app
needs ships pre-built and wired, and each feature is removable through four
documented wiring touchpoints.

The optional **web workspace** under [web](./web) hosts separate Bun/Vite/Astro
surfaces for landing pages and admin panels without changing the Gradle module
graph.

**Read [docs/MODULES.md](./docs/MODULES.md)** for the module map, dependency
rules, and the add/remove-a-feature contract. The implemented PRD and archived
rollout plans behind the architecture live in [docs/prd](./docs/prd) and
[docs/plans](docs/plans).

## Make it yours

```
./rename.sh MyCoolApp                 # package becomes com.lizz.mycoolapp
./rename.sh MyCoolApp org.acme.cool   # custom package id
```

One script renames everything — packages, namespaces, bundle ids, app display
name, source directories. Run it on a clean working tree, review `git diff`,
run the tests, commit, then delete `rename.sh`.

## What's inside

- **Baseline stack** (all verified on every target): Koin DI, Ktor client +
  server, kotlinx-serialization/datetime, Navigation3, Room 3 (KMP), DataStore,
  Coil 3, FileKit, Kermit — versions in `gradle/libs.versions.toml`.
- **Convention plugins** (`build-logic/`): a new module's build file is a few
  lines (`template.kmp.library`, `template.kmp.feature`, `template.compose`).
- **core:*** modules: model (shared DTOs + AppError + ApiResult), common,
  connectivity, network (typed error-mapped client + optional bearer auth),
  database (Room 3), datastore, designsystem (Material 3 Expressive AppTheme +
  tokens), navigation (feature registry contract), and ui (UiState + state
  components).
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

Learn more
about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
