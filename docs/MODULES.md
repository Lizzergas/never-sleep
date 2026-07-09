# Module guide

> Layer rules, the UiState/Event contract, and the file-by-file anatomy of a
> feature live in [ARCHITECTURE.md](./ARCHITECTURE.md). **Copy `feature/notes`
> when starting a new feature** — it demonstrates the full chain.

## Layout

```
build-logic/        Gradle convention plugins (template.kmp.library, template.kmp.feature, template.compose)
core/
  model/            DTOs shared with the server, AppError, ApiResult — UI-free
  common/           coroutines + Kermit logging facade — UI-free
  connectivity/     online/offline Flow (ConnectivityManager / NWPathMonitor / JVM polling)
  network/          Ktor client factory, safeGet/safeApiCall error mapping, Koin-owned HttpClient
  database/         Room 3 KMP setup: driver, per-platform builders, sample Note entity
  datastore/        DataStore<Preferences> factory + per-platform storage location
  designsystem/     Material 3 Expressive AppTheme, color schemes, typography, spacing, ThemeModeProvider
  navigation/       FeatureRegistration, destinations, Navigator, FeatureCatalog, deep links, route content registry
  ui/               UiState + Loading/Error/Empty components, status timing helpers, AppError user messages
feature/
  auth/             Account: register/login/logout, KVault token storage, 401 auto-refresh
  notes/            THE reference feature — full chain server<->Room<->domain<->UI (copy me)
  onboarding/       First-launch pager; overrides the start destination until seen
  settings/         Theme mode persisted via core:datastore (smallest exemplar)
  showcase/         Start destination: feature catalog + designsystem gallery + network demo
app/
  shared/           App shell: AppTheme, start/deep-link resolver, AppNavHost, native host bridge, di/initKoin
  androidApp/       PRODUCT: standalone Never Sleep Android app (Compose, tile, widget, ads)
  desktopApp/       Desktop entry: Koin startup + route resolution + macOS deep-link capture
  iosApp/           Xcode project: native LaunchScreen + iOS 26 SwiftUI shell + URL forwarding
server/             Ktor server with /api/* sample routes using core:model DTOs
web/                Optional Bun workspace: Astro landing, Vite admin, TS API client
```

## Dependency rules

- `feature:*` modules depend on `core:*` only — never on other features or app modules.
- `core:model` and `core:common` are UI-free; the server may depend on them.
- `core:ui` may depend on `core:designsystem` (one-way, both are UI-foundation).
- `app:shared` is the only module that sees everything; it owns theme
  application, the navigation shell, the start-route/deep-link resolver, and
  Koin startup.
- `web/` is not a Gradle module. Keep web tooling, package manifests, and
  generated artifacts inside `web/`; it talks to `server` over HTTP `/api/*`.

## Adding a feature (4 wiring touchpoints)

First create `feature/<name>` with `id("template.kmp.feature")` and implement a
`FeatureRegistration` object (routes + entries + optional catalog descriptors +
shared `destinations` for top bars, primary navigation, and full-screen routes +
optional explicit `DeepLinkSpec`s + shared route content for iOS 26 native
hosting) plus a Koin `Module`. Follow the package anatomy in ARCHITECTURE.md.

Then wire it in four places:

1. `settings.gradle.kts`: `include(":feature:<name>")`
2. `app/shared/build.gradle.kts`: add
   `implementation(projects.feature.<name>)`
3. `app/shared .../AppNavHost.kt`: add the registration to `featureRegistrations`
4. `app/shared .../di/Koin.kt`: add the feature's Koin module to `initKoin`

The showcase home lists the feature automatically when its registration
contributes catalog descriptors; `AppDestination(kind = TopLevel)` entries with
`PrimaryNavigationItem` appear in the shell's bottom bar / rail. Top-level
destinations are retained shell tabs. Screens should not render route titles or
Back buttons themselves; the Compose shell and native iOS shell render top bars
and primary navigation from `AppDestination`.

iOS 26+ uses a native SwiftUI `TabView` and per-tab `NavigationStack` so Liquid
Glass navigation UI is system-owned. Android, Desktop, and iOS < 26 keep the
Compose Navigation3 shell. Any route exposed to the native iOS shell needs a
stable `AppDestination` entry and a matching `registerRouteContent` entry in
the feature registration.

Deep links are aggregated from `FeatureRegistration.deepLinks`; Android, iOS,
and Desktop only register/capture the template custom scheme and forward URL
strings into shared Kotlin. On iOS 26+, shared Kotlin returns a native
selected-tab plus stack command instead of mutating the Compose shell. Desktop
support is macOS-first for packaged URL events, with JVM startup args available
for manual launcher tests. The template ships `neversleep://open/...` links.
Verified HTTPS App Links / Universal Links are intentionally left to generated
apps with a real domain and app association files.

## Removing a feature

Delete the same touchpoints in reverse plus the module directory. Code references
surface as compile errors; runtime wiring (Koin lookups, catalog entries)
degrades via documented fallbacks — run the app and tests after a removal.

`feature:settings` note: it provides `ThemeModeProvider`; removing it makes
the theme fall back to following the system — no other change needed.

`feature:onboarding` note: it provides `StartRouteOverride`; removing it makes
the app start directly at the showcase home.

`feature:auth` note: it provides `AuthTokenProvider`; removing it makes the
app HttpClient unauthenticated (no bearer/refresh). The server's /api/auth
routes are independent and can stay or go separately.

`feature:notes` note (the copy-me sample) spreads wider than the normal
feature contract; removing it also means deleting `NoteEntity`/`NoteDao` from
`core:database` (and bumping the Room schema), the note DTOs in `core:model`,
the `/api/notes` routes in `server`, and `NotesE2eTest`. It binds a
`UserDataCleaner`, which auth discovers via `getAll` — no unbinding needed.

## Core module removal notes

- `core:connectivity` — the shell looks it up with a fallback (no banner when
  absent); also remove the retry-on-reconnect dependency from
  `NetworkDemoViewModel`, its tests, and its Koin factory.
- `core:network` — also delete `NetworkConfig` from `appModule` and the
  network demo from `feature:showcase`.
- `core:database` — Room/KSP wiring lives entirely inside the module; deleting
  the module removes everything (don't forget its `schemas/` directory).
- `core:datastore` — `feature:settings` depends on it.
