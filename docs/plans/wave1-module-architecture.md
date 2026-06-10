# Plan: Wave 1 — Plug-in Module Architecture

> Source PRD: docs/prd/0001-wave1-module-architecture.md

## Architectural decisions

Durable decisions that apply across all phases:

- **Module layout**: `core:model`, `core:common`, `core:network`, `core:database`,
  `core:datastore`, `core:designsystem`, `core:ui`; features under `feature:*`
  (wave 1: `feature:settings`, `feature:showcase`); existing `app:*` and
  `server` modules remain.
- **Dependency rules**: features → core only (never feature → feature);
  core modules may depend on `core:model`/`core:common` only; `core:model` and
  `core:common` are UI-free (server-consumable); `app:shared` is the sole
  aggregation point (theme, navigation shell, Koin startup).
- **Plug-in contract**: every feature removable via exactly three lines —
  settings.gradle include, Koin registry entry, navigation registry entry.
  The showcase derives its feature list from the registries.
- **Convention plugins**: `template.kmp.library` (KMP targets:
  android/iosArm64/iosSimulatorArm64/jvm + namespace/JVM conventions),
  `template.compose`, `template.kmp.feature` (library + compose +
  serialization + Koin defaults). Versions stay in the version catalog.
- **Error model**: sealed `AppError` taxonomy in `core:model`
  (network / timeout / unauthorized / server / validation / unknown);
  `core:network` returns typed `ApiResult` — no throwing API surface.
- **Server routes**: under `/api/*` (wave 1: a hello/health route and a typed
  list route) using DTOs from `core:model`; content negotiation + status-page
  error mapping installed.
- **Navigation**: Navigation3 with serializable route keys; the registry owns
  the polymorphic `NavKey` serializers module so feature authors never touch
  `SavedStateConfiguration` directly.
- **DI**: one Koin module value per module, aggregated by the existing
  `initKoin` at platform entry points.
- **Settings storage**: theme mode (light/dark/system) via the typed
  DataStore wrapper.
- **Testing conventions**: behavioral tests only; kotlin-test +
  coroutines-test + Turbine; Ktor MockEngine for network; JVM compose-ui tests
  for UI components.

---

## Phase 1: Build scaffolding tracer

**User stories**: 3, 18, 19

### What to build

Introduce `build-logic` with the three convention plugins, then dissolve the
current single `core` module into `core:model` and `core:common` as the first
consumers of `template.kmp.library`. Existing modules (`app:shared`, server)
switch their dependency from `core` to the new modules. The app must build and
run exactly as before on all four targets — this phase proves the build
machinery end to end without changing behavior.

### Acceptance criteria

- [ ] `build-logic` provides `template.kmp.library`, `template.compose`, `template.kmp.feature`; a module using them needs ≤ ~10 lines of build script
- [ ] Old `core` is gone; `core:model` and `core:common` exist, are UI-free, and the server depends on them
- [ ] All targets compile (android, iosArm64, iosSimulatorArm64, jvm) and the desktop app runs unchanged
- [ ] Dependency rules hold: no feature/app code in core modules; server has no UI dependencies in its graph

---

## Phase 2: Theme tracer

**User stories**: 4, 23, (16 partial)

### What to build

`core:designsystem` exposing `AppTheme` (light/dark/system via platform
darkness detection), color schemes, typography, and spacing tokens. The app
shell applies `AppTheme` instead of bare `MaterialTheme`. Demoable as a visible
restyle of the running app on desktop and mobile.

### Acceptance criteria

- [ ] `AppTheme` is the single theming entry point; app shell uses it on all targets
- [ ] Light and dark schemes resolve; system mode follows the platform setting
- [ ] Spacing/typography tokens are exposed and used by at least one screen
- [ ] Sanity tests: theme resolves for both modes, tokens exposed

---

## Phase 3: Registry + showcase shell

**User stories**: 2, 11, 12, 14, 15

### What to build

The aggregation backbone: a Koin module registry and a navigation registry in
`app:shared` (the nav registry owns serializable route keys and the polymorphic
NavKey serializer setup). `feature:showcase` becomes the start destination — a
home screen listing registered features plus a designsystem gallery screen
rendering the tokens/components from Phase 2. The temporary DemoScreen remains
for now (deleted in Phase 7).

### Acceptance criteria

- [ ] Adding a feature = settings.gradle include + one Koin registry line + one nav registry line; removal = deleting those three
- [ ] Showcase home lists features derived from the registry (not hardcoded)
- [ ] Designsystem gallery shows colors, typography, spacing live
- [ ] Navigating showcase → gallery → back works on desktop and Android; route keys are serializable objects (compile-checked)

---

## Phase 4: Network tracer (client ↔ server)

**User stories**: 5, 6, 13, 20, 21, 22

### What to build

The full client/server slice: `AppError` taxonomy and shared DTOs in
`core:model`; `core:network` providing the configured Ktor client factory and
typed `ApiResult` with the complete error-mapping table; server gains
`/api/hello` and a typed `/api/items` route with content negotiation and
status-page mapping, consuming the same DTOs. A showcase network demo screen
calls the server and displays the typed result, including a clear state when
the local server isn't running.

### Acceptance criteria

- [ ] One DTO set in `core:model` is used by both server responses and client decoding
- [ ] `core:network` maps timeouts, IO failures, 401/404/422/500, and malformed bodies to the documented `AppError` variants (MockEngine tests cover the full table)
- [ ] Showcase network demo shows success data from the running server and a distinct, friendly failure state when the server is down
- [ ] Server tests cover both routes via ktor-server-test-host

---

## Phase 5: UiState slice

**User stories**: 7, 17

### What to build

`core:ui` with the `UiState` type and Loading/Error/Empty composables
(error carries a retry hook fed from `AppError`). Refactor the showcase network
demo onto these components so the slice is visibly exercised: loading spinner →
data, or error component with working retry.

### Acceptance criteria

- [ ] `UiState` covers Loading/Success/Error/Empty; mapping from `ApiResult`/`AppError` is provided
- [ ] Network demo uses the shared components; retry re-issues the call
- [ ] JVM compose-ui tests: each state composable renders; retry callback fires
- [ ] Unit tests for state transitions

---

## Phase 6: Settings tracer

**User stories**: 1, 8, 10, 16

### What to build

`core:datastore` (per-platform DataStore factory + typed preferences wrapper)
and `feature:settings` — the conventions reference feature: a settings screen
with a theme-mode switcher (light/dark/system) whose choice persists across
restarts and drives `AppTheme` live. Registered through the Phase 3 registries;
reachable from the showcase.

### Acceptance criteria

- [ ] Theme change applies immediately and survives app restart on desktop and Android (iOS compiles; manual check optional)
- [ ] Preferences are accessed only through typed accessors — no raw keys outside `core:datastore`
- [ ] DataStore round-trip tests (in-memory/temp file) and ViewModel Turbine tests pass
- [ ] feature:settings demonstrates the full feature anatomy: UI → ViewModel → core module → DI + nav registration

---

## Phase 7: Database slice + demo retirement

**User stories**: 9, 2, 18

### What to build

Move Room into `core:database` (per-platform builders, bundled driver, schema
export location, migration scaffold) with a small sample entity/DAO surfaced in
the showcase so the KSP wiring stays exercised. Delete DemoScreen per its
checklist (file, App.kt hook, demo Koin module, old schemas) — its verification
role is now fully covered by the showcase. Write the per-feature removal
documentation (the 3-line contract) into the README/feature docs. Final
verification across all targets.

### Acceptance criteria

- [ ] `core:database` owns all Room setup; sample entity round-trips (insert + observe) via the showcase on desktop
- [ ] DemoScreen and its checklist items are fully removed; no orphaned schemas or Koin entries remain
- [ ] Each wave-1 feature/core module has removal documentation following the 3-line contract
- [ ] All targets compile; desktop showcase runs through every screen; all wave-1 tests green
