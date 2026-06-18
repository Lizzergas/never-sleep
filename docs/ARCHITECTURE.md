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
  <Name>Feature.kt             FeatureRegistration + route chrome + deep links + Koin module
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
- Offline-read shape: the local store is the source of truth for `observe*()`;
  `refresh()` pulls remote and upserts, while server-authoritative writes update
  the cache only after the server accepts them.
- Map at every boundary: DTOs and Room entities never escape `data/`.

### presentation/

- One `XxxUiState` data class per screen; one `StateFlow<XxxUiState>` per
  ViewModel; never multiple public flows of screen state.
- The ViewModel owns one private `MutableStateFlow<XxxUiState>` and exposes it
  with `asStateFlow()`. If repository/domain flows feed the screen, collect
  them in `viewModelScope` and copy their values into that same `_state`.
  Do not introduce a parallel private `LocalState` plus derived public
  `UiState` for new features.
- One `sealed interface XxxEvent`; the ViewModel exposes `fun onEvent(XxxEvent)`.
- One-off effects (navigate, snackbar) go through a `Channel`-backed Flow,
  collected in the Screen wrapper — never modeled as sticky state.
- **Screen/Content split**: `XxxScreen` is the only place that touches
  `koinViewModel()`/effects; `XxxContent(state, onEvent)` is stateless,
  has `@Preview`s, and is what UI tests target.
- Collect with `collectAsStateWithLifecycle()`.
- Prefer stable screen content with inline status over full-screen loading
  surfaces once a route is known. App startup route resolution is host-owned:
  Android/iOS keep their native splash/launch screen up, Desktop resolves
  before opening the window, and shared Compose receives the resolved route.
  Feature screens should keep headings, cached content, and current errors
  visible during refresh/restore. Use core:ui timing helpers for delayed
  indicators and status fades instead of ad hoc per-screen delays.

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
    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeNotes().collect { notes ->
                _state.update { it.copy(notes = notes) }
            }
        }
    }

    fun onEvent(event: NotesEvent) { ... }
}
```

## Cross-feature contracts

Features never depend on each other. When a feature offers something app-wide,
it implements a small interface from a core module and binds it in its Koin
module; the shell looks it up with a fallback (`getOrNull`):

| Contract             | Core home         | Bound by                                         | Fallback            |
|----------------------|-------------------|--------------------------------------------------|---------------------|
| `ThemeModeProvider`  | core:designsystem | feature:settings                                 | follow system       |
| `StartRouteOverride` | core:navigation   | feature:onboarding                               | default start route |
| `AuthTokenProvider`  | core:network      | feature:auth                                     | unauthenticated     |
| `UserDataCleaner`    | core:common       | any feature with per-user caches (feature:notes) | nothing to clear    |

`UserDataCleaner` is collected with `getAll` (not `getOrNull`): every feature
that caches per-user data binds one, and feature:auth invokes them all on
login/logout so nothing leaks across accounts.

In composables, use `rememberOptionalKoin<T>()` (core:ui) for optional
lookups. In `FeatureRegistration`, declare `destinations` for every
host-rendered route. `AppDestination(kind = TopLevel)` plus
`PrimaryNavigationItem` creates shell tabs, `Detail` routes get host-owned
Back/Up top bars when pushed, and `FullScreen` routes render without the shell
— onboarding does this for its pager.

Top-level destinations are shell tabs, not detail pages. The app shell retains
one Navigation3 back stack per top-level destination; switching bottom-bar/rail
items restores that destination's stack, and reselecting the current item pops
that stack to its root. Feature screens are content-only for navigation UI:
do not render route titles or Back buttons inside screen content. Compose and
native hosts render top bars and Back/Up from `AppDestination.topBar`.

Android, Desktop, and iOS < 26 use the full Compose Navigation3 shell. iOS 26+
uses a native SwiftUI shell for platform chrome: `TabView` for top-level
destinations and one native `NavigationStack` per tab, so Liquid Glass tab bars,
titles, and back gestures are real system UI. Compose still renders the screen
content and owns ViewModels/data flow. Feature registrations provide the shared
`AppDestination` metadata for top bars, stable IDs, primary navigation labels,
Material icons, SF Symbols, and top-level/detail/full-screen kind, plus
`registerRouteContent` entries that render those same typed `NavKey`s inside
the native host.

Deep links are feature-owned public API. Each feature declares explicit
`DeepLinkSpec`s in its `FeatureRegistration`; never derive URL paths from route
class names. Platform hosts only capture URLs (`myapptemplate://open/...` in the
template default) and forward the raw string to shared Kotlin: Android uses
intents, iOS < 26 forwards into the Compose shell, iOS 26+ converts the shared
resolution into a native selected-tab plus stack command, and Desktop currently
supports JVM startup args plus macOS LaunchServices URL events. Shared
navigation parses, validates, maps to typed `NavKey` stacks, applies auth
gating, and then mutates either the retained Compose stack, transient
full-screen stack, or native iOS tab stack. Route arguments remain typed route
fields; screens and ViewModels never receive raw URLs.

V1 template links:

| URL                                              | Stack                                      |
|--------------------------------------------------|--------------------------------------------|
| `myapptemplate://open/home`                      | Home root                                  |
| `myapptemplate://open/notes`                     | Notes root                                 |
| `myapptemplate://open/settings`                  | Settings root                              |
| `myapptemplate://open/account`                   | Account root                               |
| `myapptemplate://open/showcase/design-system`    | Home -> Design System Gallery              |
| `myapptemplate://open/showcase/network`          | Home -> Network Demo                       |

Verified HTTPS Android App Links / iOS Universal Links should be enabled only
in generated apps with a real domain, signing identity, and hosted association
files. Keep the shared parser shape explicit so those URLs can map to the same
typed resolutions without changing screens.

## Wiring (the 4-touchpoint plug-in contract)

1. `settings.gradle.kts` — `include(":feature:<name>")`
2. `app/shared/build.gradle.kts` — add `implementation(projects.feature.<name>)`
3. `AppNavHost.kt` — add the `FeatureRegistration` to `featureRegistrations`
4. `di/Koin.kt` — add the feature's Koin module to `initKoin`

If the route should render inside the iOS 26 native shell, also add a matching
`registerRouteContent` entry in the feature registration. The same
`AppDestination` entry feeds both the Compose shell and native iOS shell, so
avoid screen-local route titles or Back buttons.

Removal is the same touchpoints in reverse. Leftover *code* references become
compile errors pointing at the spot; leftover *runtime* wiring (Koin lookups,
catalog entries) is designed to degrade silently via the fallbacks above —
after removing a feature, run the app and the tests, not just the compiler.

## Testing conventions

- Behavioral tests only, through public interfaces.
- domain/data: fake the repository interface / DataSources; coroutines-test +
  Turbine. Use `runBlocking` (not `runTest`) when a real Ktor client with
  `HttpTimeout` is involved — virtual time trips the timeout.
- presentation: compose-ui tests against `Content` (no Koin needed) and
  app-level e2e through `App(startRoute = ...)` with `loadKoinModules` overrides
  (see `app/shared/src/jvmTest/.../TestSupport.kt`).
- Server: ktor-server-test-host route tests.

## Known sharp edges (learned the hard way)

- Kotlin block comments NEST: writing `/api/auth/*` inside a KDoc opens a
  nested comment and swallows the file.
- DataStore forbids two instances per file in one process — platform modules
  memoize; tests override with temp files.
- ktlint filter lambdas must live in plain Kotlin (not .gradle.kts) or they
  break the configuration cache.
- ktlint's official multiline-expression-wrapping rule conflicts with the
  template's preferred `val x = listOf(` style; keep that rule disabled and use
  the Gradle `kotlinAssignmentWrapping*` tasks wired through `template.quality`.
