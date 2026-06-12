# Architecture

Pragmatic MVVM on a plug-in module system. This document is the contract every
feature follows — copy `feature/notes` when starting a new one.

## The anatomy of a feature

```
feature/<name>/src/commonMain/kotlin/.../<name>/
  domain/
    <Name>Repository.kt        interface — what the feature needs, not how
    <Model>.kt                 pure Kotlin domain models
    <Verb><Noun>UseCase.kt     ONLY for multi-step business logic (operator invoke)
  data/
    <Name>RepositoryImpl.kt    coordinates DataSources; owns threading
    Mappers.kt                 Dto.toDomain() / Entity.toDomain() / Domain.toEntity()
  presentation/
    <Name>UiState.kt           ONE data class + sealed <Name>Event
    <Name>ViewModel.kt         ONE StateFlow<UiState> + onEvent(Event)
    <Name>Screen.kt            Screen (stateful) + Content (stateless, previewed)
  <Name>Feature.kt             FeatureRegistration + Koin module
```

Dependency rule inside a feature: `presentation → domain ← data`.
The `domain` package imports no Ktor, Room, Compose, or Koin types.

## Layer rules

### domain/
- Repository **interfaces** describe what the feature needs. Implementations
  live in `data/` and are bound in the feature's Koin module:
  `single<NotesRepository> { NotesRepositoryImpl(get(), get()) }`
- Domain models are plain data classes. Annotate with `@Immutable` when they
  flow into composables.
- A UseCase earns its keep only when logic spans multiple steps or sources
  (e.g. login = authenticate → persist tokens → load profile). A one-line
  delegation to a repository is NOT a UseCase — call the repository directly.
- Functions return `ApiResult<T>` (or domain types); never throw for expected
  failures.

### data/
- Repository implementations coordinate DataSources (network, Room, DataStore)
  and own threading (`Dispatchers.IO` etc. — callers never switch contexts).
- Offline-first shape: the local store is the source of truth; `observe*()`
  reads the DB, `refresh()` pulls remote and upserts, writes go local-first
  then sync.
- Map at every boundary: DTOs and Room entities never escape `data/`.

### presentation/
- One `XxxUiState` data class per screen; one `StateFlow<XxxUiState>` per
  ViewModel (combine sources with `combine`/`stateIn`); never multiple public
  flows of screen state.
- One `sealed interface XxxEvent`; the ViewModel exposes `fun onEvent(XxxEvent)`.
- One-off effects (navigate, snackbar) go through a `Channel`-backed Flow,
  collected in the Screen wrapper — never modeled as sticky state.
- **Screen/Content split**: `XxxScreen` is the only place that touches
  `koinViewModel()`/effects; `XxxContent(state, onEvent)` is stateless,
  has `@Preview`s, and is what UI tests target.
- Collect with `collectAsStateWithLifecycle()`.

```kotlin
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
)

sealed interface NotesEvent {
    data class Add(val text: String) : NotesEvent
    data class Delete(val id: Long) : NotesEvent
    data object Refresh : NotesEvent
}

class NotesViewModel(...) : ViewModel() {
    val state: StateFlow<NotesUiState> = ...
    fun onEvent(event: NotesEvent) { ... }
}
```

## Cross-feature contracts

Features never depend on each other. When a feature offers something app-wide,
it implements a small interface from a core module and binds it in its Koin
module; the shell looks it up with a fallback (`getOrNull`):

| Contract             | Core home          | Bound by          | Fallback           |
|----------------------|--------------------|-------------------|--------------------|
| `ThemeModeProvider`  | core:designsystem  | feature:settings  | follow system      |
| `StartRouteOverride` | core:navigation    | feature:onboarding| default start route|
| `AuthTokenProvider`  | core:network       | feature:auth      | unauthenticated    |

## Wiring (the 3-line plug-in contract)

1. `settings.gradle.kts` — `include(":feature:<name>")`
2. `AppNavHost.kt` — add the `FeatureRegistration` to `featureRegistrations`
3. `di/Koin.kt` — add the feature's Koin module to `initKoin`

Removal is the same lines in reverse; any leftover reference is a compile
error pointing at it.

## Testing conventions

- Behavioral tests only, through public interfaces.
- domain/data: fake the repository interface / DataSources; coroutines-test +
  Turbine. Use `runBlocking` (not `runTest`) when a real Ktor client with
  `HttpTimeout` is involved — virtual time trips the timeout.
- presentation: compose-ui tests against `Content` (no Koin needed) and
  app-level e2e through `App()` with `loadKoinModules` overrides
  (see `app/shared/src/jvmTest/.../TestSupport.kt`).
- Server: ktor-server-test-host route tests.

## Known sharp edges (learned the hard way)

- Kotlin block comments NEST: writing `/api/auth/*` inside a KDoc opens a
  nested comment and swallows the file.
- DataStore forbids two instances per file in one process — platform modules
  memoize; tests override with temp files.
- ktlint filter lambdas must live in plain Kotlin (not .gradle.kts) or they
  break the configuration cache.
