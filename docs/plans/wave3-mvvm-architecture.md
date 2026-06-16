# Plan: Wave 3 — MVVM Architecture Foundation

> Designed against the android-clean-architecture, compose-multiplatform-patterns,
> and mobile-android-design skills. Goal: a prescriptive, copyable "anatomy of a
> feature" so new projects spawn fast with consistent structure.
> Status: Implemented / archived. The resulting contract is maintained in
> docs/ARCHITECTURE.md, with the module map in docs/MODULES.md.

## Decisions (made together, 2026-06-14)

1. **Layering: Pragmatic MVVM.** Every feature module gets three PACKAGES —
   `domain/` (repository interface, domain models, UseCases only where logic is
   multi-step), `data/` (repository impl, DataSources, mappers), `presentation/`
   (ViewModel + UiState + Event, Screen/Content split). Features stay
   self-contained Gradle modules; the app-shell plug-in contract is unchanged.
   No cross-cutting core:domain/core:data modules.
2. **Presentation contract: UiState + Event sink, convention only.** One
   `StateFlow<XxxUiState>` (single data class) + `fun onEvent(XxxEvent)` per
   ViewModel; one-off effects (navigation, snackbars) via Channel-backed Flow.
   No base class — the convention is documented and demonstrated, not enforced.
3. **Canonical sample: `feature:notes`** demonstrating the FULL chain:
   server CRUD ↔ NoteDto ↔ NoteEntity (Room) ↔ Note (domain) ↔ NoteUi,
   offline reads with server-authoritative writes, a real UseCase,
   UiState/Event VM, previews.
4. **Design system upgrades: all four** — Screen/Content + @Previews,
   collectAsStateWithLifecycle, dynamic color (Android 12+), WindowSizeClass
   adaptive shell.

## The anatomy of a feature (the artifact this wave produces)

```
feature/<name>/src/commonMain/kotlin/.../<name>/
  domain/
    <Name>Repository.kt        interface; what the feature needs, not how
    <Model>.kt                 pure Kotlin domain models (@Immutable where UI-facing)
    <Verb><Noun>UseCase.kt     only for multi-step business logic (operator invoke)
  data/
    <Name>RepositoryImpl.kt    coordinates DataSources; owns threading
    Mappers.kt                 Dto.toDomain(), Entity.toDomain(), Domain.toEntity()
  presentation/
    <Name>UiState.kt           single data class + sealed <Name>Event (+ Effect)
    <Name>ViewModel.kt         StateFlow<UiState>, onEvent(), effects Channel
    <Name>Screen.kt            Screen (stateful, owns VM) + Content (stateless, previewed)
  <Name>Feature.kt             FeatureRegistration + Koin module (interface→impl binding)
```

Dependency rule inside a feature: presentation → domain ← data. The domain
package imports nothing from data/presentation, no Ktor/Room/Compose types.

---

## Phase 1: Conventions + first exemplar (Settings)

Write `docs/ARCHITECTURE.md` (the anatomy above, the UiState/Event/Effect
convention, Screen/Content rules, mapper rules, when a UseCase earns its keep).
Retrofit feature:settings as the smallest exemplar: SettingsRepository
interface + impl, SettingsUiState/Event, Screen/Content split with @Preview.
Switch all screens to collectAsStateWithLifecycle.

**Accept**: settings tests green against the interface; preview renders
Content without Koin; ARCHITECTURE.md reviewed.

## Phase 2: Retrofit auth + onboarding

Auth: `domain/` SessionRepository interface + SessionState + LoginUseCase /
RegisterUseCase (multi-step: authenticate → persist tokens → load profile);
`data/` SessionRepositoryImpl + TokenStorage; `presentation/` AccountUiState
(replaces the 3-flow ViewModel) + AccountEvent + Screen/Content.
Onboarding: same treatment (tiny). All existing e2e tests keep passing.

**Accept**: SessionViewModel exposes exactly one state flow + onEvent;
AuthTokenProvider still bound via the interface; e2e suite green.

## Phase 3: feature:notes — the canonical full-chain sample

Server: /api/notes GET/POST/DELETE (auth-protected, per-user, in-memory store).
Client: NoteDto ↔ NoteEntity ↔ Note mappers; NotesRepository with offline
reads and server-authoritative writes (Room as source of truth for observeAll,
refresh pulls server → upserts, add/delete update the cache after server
success); AddNoteUseCase (validate + persist);
NotesUiState/Event; Screen/Content with previews; replaces the showcase
Database demo (Network demo stays — it demonstrates error states).

**Accept**: cached notes remain readable during an outage; server round-trip when online;
repository tests (fake DataSources) + UI e2e; documented as THE copy recipe.

## Phase 4: Design system — dynamic color + adaptive shell

expect/actual `platformColorScheme(dark)`: Material You dynamic color on
Android 12+, brand palette elsewhere. WindowSizeClass-driven shell: bottom
bar (compact) / nav rail (expanded) for top-level destinations; registry
extended so features declare an optional top-level destination (icon + label).
Showcase home remains the catalog; previews for both form factors.

**Accept**: desktop shows rail, phone shows bottom bar; dynamic color verified
on Android emulator; registry contract remains removable through the documented
app-shell touchpoints.

## Phase 5: Sweep + docs

@Previews across remaining screens (showcase, onboarding), MODULES.md +
AGENTS.md updated to point at ARCHITECTURE.md, qualityCheck/jvmTest/iOS/
Android/CI all green, wave-3 plan archived.

---

## Watch-outs

- Koin bindings move to interfaces: `single<SettingsRepository> { SettingsRepositoryImpl(...) }`
  — test overrides keep working (they bind the interface).
- ThemeModeProvider/StartRouteOverride/AuthTokenProvider stay as the optional
  cross-feature contracts; their binders become the repository impls.
- DataStore process-wide memoization + loadKoinModules test-override patterns
  from wave 2 still apply.
- collectAsStateWithLifecycle: multiplatform via lifecycle-runtime-compose ≥2.8 (already on 2.11).
- Adaptive shell must not break Navigation3 saved-state (serializers registry untouched).
