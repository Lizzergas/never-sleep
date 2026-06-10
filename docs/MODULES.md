# Module guide

## Layout

```
build-logic/        Gradle convention plugins (template.kmp.library, template.kmp.feature, template.compose)
core/
  model/            DTOs shared with the server, AppError, ApiResult — UI-free
  common/           coroutines + Kermit logging facade — UI-free
  network/          Ktor client factory, safeGet/safeApiCall error mapping, Koin-owned HttpClient
  database/         Room 3 KMP setup: driver, per-platform builders, sample Note entity
  datastore/        DataStore<Preferences> factory + per-platform storage location
  designsystem/     AppTheme, color schemes, typography, spacing tokens, ThemeModeProvider
  navigation/       FeatureRegistration contract, Navigator, FeatureCatalog
  ui/               UiState + Loading/Error/Empty components, AppError user messages
feature/
  settings/         Reference feature: theme mode persisted via core:datastore
  showcase/         Start destination: feature catalog + designsystem gallery + demos
app/
  shared/           App shell: AppTheme application, AppNavHost (registry), di/initKoin
  androidApp/       Android entry (MainApplication starts Koin with androidContext)
  desktopApp/       Desktop entry (main() starts Koin)
  iosApp/           Xcode project (iOSApp.init starts Koin via doInitKoin)
server/             Ktor server with /api/* sample routes using core:model DTOs
```

## Dependency rules

- `feature:*` modules depend on `core:*` only — never on other features or app modules.
- `core:model` and `core:common` are UI-free; the server may depend on them.
- `core:ui` may depend on `core:designsystem` (one-way, both are UI-foundation).
- `app:shared` is the only module that sees everything; it owns theme
  application, the navigation shell, and Koin startup.

## Adding a feature (3 wiring lines)

1. Create `feature/<name>` with `id("template.kmp.feature")` and implement a
   `FeatureRegistration` object (routes + entries + optional catalog
   descriptors) plus a Koin `Module`.
2. `settings.gradle.kts`: `include(":feature:<name>")`
3. `app/shared .../AppNavHost.kt`: add the registration to `featureRegistrations`
4. `app/shared .../di/Koin.kt`: add the feature's Koin module to `initKoin`

(Plus one `implementation(projects.feature.<name>)` line in app/shared's
build file.) The showcase home lists the feature automatically — its catalog
derives from the registrations.

## Removing a feature

Delete the same lines in reverse plus the module directory. Nothing else
references a feature; if a removal leaves a compile error, that error IS the
remaining reference.

`feature:settings` note: it provides `ThemeModeProvider`; removing it makes
the theme fall back to following the system — no other change needed.

## Core module removal notes

- `core:network` — also delete `NetworkConfig` from `appModule` and the
  network demo from `feature:showcase`.
- `core:database` — Room/KSP wiring lives entirely inside the module; deleting
  the module removes everything (don't forget its `schemas/` directory).
- `core:datastore` — `feature:settings` depends on it.
