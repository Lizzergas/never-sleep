# PRD 0001 — Wave 1: Plug-in Module Architecture (core split, convention plugins, infra features)

Status: Implemented / archived · Date: 2026-06-10 · Target: MyAppTemplate

Completion note: this PRD describes the original wave-1 scope. Later waves have
since delivered auth, connectivity, onboarding, notes, quality tooling, and the
MVVM feature anatomy; current contracts live in
[docs/MODULES.md](../MODULES.md) and [docs/ARCHITECTURE.md](../ARCHITECTURE.md).

## Problem Statement

Every new app rebuilds the same infrastructure from scratch: theming, a configured
HTTP client with sane error handling, loading/error/empty screen states, and a
settings screen. Today MyAppTemplate provides a verified dependency baseline
(Koin, Ktor, Room 3, DataStore, Navigation3, Coil, FileKit, Kermit on
Android/iOS/Desktop/Server) but no reusable structure on top of it: there is a
single `core` module with no real code, a temporary dependency-verification
screen, and every future module would have to copy ~40 lines of identical KMP
Gradle configuration. Starting a real app from the template still means weeks of
re-deriving architecture conventions (where DI modules live, how navigation
composes, how errors flow from Ktor to the UI) before writing the first feature.

## Solution

Restructure the template into a multi-module architecture where repeating app
infrastructure ships as small, deep, independently testable modules that are
**all wired by default and removable through documented touchpoints**
(settings.gradle include, app/shared dependency, Koin registry entry,
navigation registry entry).

Wave 1 delivers:

1. **Gradle convention plugins** (`build-logic`) so a new module's build file is
   ~5 lines (`template.kmp.library`, `template.kmp.feature`, `template.compose`).
2. **A split core**: `core:model`, `core:common`, `core:network`,
   `core:database`, `core:datastore`, `core:designsystem`, `core:ui` — each a
   deep module with a small stable interface.
3. **The first real feature**: `feature:settings` (theme mode persisted via
   DataStore), proving the feature-module conventions end to end.
4. **A permanent showcase** (`feature:showcase`): a gallery home screen that
   lists features, demonstrates designsystem tokens/components, and exercises
   the network stack against the template's own server — replacing the
   temporary DemoScreen as the living runtime verification.
5. **Server participation**: sample endpoints using DTOs from `core:model`,
   proving the shared-types client/server story that is this template's unique
   advantage.

Later waves, which are outside this PRD's original scope, add auth, connectivity, onboarding,
permissions, notifications, sync, etc., each following the conventions wave 1
establishes.

## User Stories

1. As an app developer starting from the template, I want infrastructure modules pre-built and
   wired, so that I can write my first feature on day one instead of building plumbing for weeks.
2. As an app developer, I want every feature module removable through documented wiring touchpoints,
   so that stripping the template down to my app's needs takes minutes and leaves no dead wiring.
3. As an app developer, I want a new module's build file to be ~5 lines via convention plugins, so
   that adding feature modules never involves copy-pasting 40 lines of KMP target configuration.
4. As an app developer, I want a single `AppTheme` entry point with light/dark color schemes,
   typography, and spacing tokens, so that I restyle the whole app by editing one module.
5. As an app developer, I want a preconfigured HTTP client that maps transport failures, HTTP error
   codes, and serialization failures into one typed error model, so that every screen handles
   failures the same way without per-call boilerplate.
6. As an app developer, I want network calls to return a typed result instead of throwing, so that
   the compiler forces me to handle the failure path.
7. As an app developer, I want a `UiState` type plus ready Loading/Error/Empty composables with
   retry hooks, so that every screen's three non-happy states look consistent and cost zero design
   time.
8. As an app developer, I want a typed preferences wrapper over DataStore, so that I read and write
   settings through named, typed accessors instead of raw keys scattered across the codebase.
9. As an app developer, I want a ready Room database module with per-platform builders, so that
   adding my first entity does not require re-learning the Room KMP setup (drivers, constructors,
   schema export).
10. As an app developer, I want a working settings screen with a theme switcher, so that I have a
    real, end-to-end example of the feature conventions (UI → ViewModel → datastore → theme
    recomposition) to copy for my own features.
11. As an app developer, I want a showcase home screen listing every installed feature, so that I
    can see what the template provides and verify at runtime that everything still works after I
    modify it.
12. As an app developer, I want a designsystem gallery screen, so that I can see all tokens and
    shared components rendered live while restyling.
13. As an app developer, I want the showcase's network demo to call the template's own server with
    DTOs shared from a common module, so that I can see the client/server type-sharing story working
    before building my own API.
14. As an app developer, I want each feature to register its own Koin module and navigation entries
    through one aggregation point, so that adding or removing features never means hunting through
    app wiring.
15. As an app developer, I want the navigation route registry to be type-safe (serializable route
    objects), so that renaming or removing a screen is a compile error, not a runtime crash.
16. As an end user of an app built from the template, I want my theme choice (light/dark/system)
    remembered across launches on every platform, so that the app respects my preference.
17. As an end user, I want failures shown as friendly, actionable states with a retry button instead
    of blank screens or crashes, so that temporary problems don't dead-end me.
18. As a template maintainer, I want every module compiled and exercised by the default build and
    showcase, so that unused-module rot is impossible and CI catches breakage in any of them.
19. As a template maintainer, I want module boundaries enforced by the dependency graph (features
    cannot depend on each other; core modules cannot depend on features), so that the architecture
    survives contributions.
20. As a template maintainer, I want the network error-mapping and settings logic covered by
    behavioral tests, so that dependency bumps (Ktor, DataStore, Kotlin) are validated by CI rather
    than manual clicking.
21. As a server developer, I want request/response DTOs defined once in a shared module, so that
    client and server can never drift apart silently.
22. As a server developer, I want content negotiation and error-status mapping pre-wired with a
    sample endpoint, so that adding my first real route is copy-paste of a working pattern.
23. As an iOS developer on the team, I want all of the above to work identically from the Xcode
    entry point, so that the iOS app is a first-class consumer rather than an afterthought.

## Implementation Decisions

- **Module layout**: multi-module core (`model`, `common`, `network`,
  `database`, `datastore`, `designsystem`, `ui`) plus `feature:*` modules and a
  `feature:showcase`. The existing single `core` module is dissolved into the
  new core modules; the temporary DemoScreen is deleted, its runtime-verification
  role inherited by the showcase. The Room demo entity moves into the showcase
  (or `core:database` sample) so the Room/KSP wiring stays exercised.
- **Dependency rules**: features depend on core modules only (never on other
  features); core modules may depend on `core:model`/`core:common` but not on
  features or app modules; `core:model` and `core:common` are UI-free so the
  server can depend on them. `app:shared` is the only module that sees
  everything; it owns theme application, the navigation shell, and Koin startup.
- **Plug-in mechanism ("all-in + delete")**: every module ships wired. Each
  feature's README section documents its exact removal touchpoints:
  settings.gradle include, app/shared dependency, the entry in the Koin module
  registry, and the entry in the navigation registry. The showcase feature list
  derives from the registries, so removal automatically updates it.
- **Convention plugins**: a `build-logic` included build provides
  `template.kmp.library` (KMP targets android/iosArm64/iosSimulatorArm64/jvm,
  namespace/JVM-target conventions), `template.compose` (compose plugins +
  common compose deps), and `template.kmp.feature` (library + compose +
  serialization + Koin defaults). Versions stay in the existing version catalog.
- **Error model**: a sealed `AppError` taxonomy lives in `core:model`
  (network/timeout/unauthorized/server/validation/unknown at minimum).
  `core:network` maps Ktor exceptions and non-2xx responses into it and returns
  a typed result (`ApiResult`); UI layers convert `AppError` to user-facing
  state via `core:ui`.
- **DI and navigation composition**: each module exposes one Koin module value;
  `app:shared`'s existing `initKoin` aggregates them (this generalizes the
  pattern already in place). Each feature exposes its serializable route keys
  and an entry-provider registration; `app:shared` composes them into the
  Navigation3 display, including the polymorphic NavKey serializers module
  (a wave-1 hardening of what DemoScreen discovered).
- **Settings feature scope (wave 1)**: theme mode (light/dark/system) persisted
  through `core:datastore`; the screen is the conventions reference
  implementation. Language and other preferences are later waves.
- **Server (wave 1)**: two sample endpoints (a hello/health route and a small
  typed list route) using `core:model` DTOs, with content negotiation and
  status-page error mapping installed. The showcase network demo consumes them;
  a fallback public URL is not used — the demo states clearly when the local
  server isn't running.
- **Showcase**: a permanent feature module acting as start destination —
  feature list, designsystem gallery, network demo. It is itself removable by
  the same app-shell wiring contract (an app replaces it with its own start
  screen).

## Testing Decisions

- A good test exercises a module's external behavior through its public
  interface and would survive an internal rewrite; no asserting on internals,
  no mocking of types the module owns.
- **core:network** (highest value): Ktor MockEngine-driven tests asserting the
  full error-mapping table (timeouts, IO failures, 401/404/422/500, malformed
  body → the corresponding `AppError`), success decoding, and the auth-header
  hook. Runs on JVM in CI.
- **feature:settings + core:datastore**: behavioral tests that setting a theme
  mode persists and re-emits across a simulated restart (in-memory/temp-file
  DataStore), and ViewModel state-flow tests using coroutines-test + Turbine.
- **core:ui**: unit tests for `UiState` transitions plus JVM compose-ui tests
  asserting Loading/Error/Empty composables render and the retry callback
  fires.
- **core:designsystem**: light sanity tests only in wave 1 (theme resolves for
  both modes; tokens are exposed); screenshot testing is noted as a later-wave
  addition rather than blocking this PRD.
- Prior art: the repo currently has only placeholder tests; these wave-1 tests
  establish the testing conventions (kotlin-test + coroutines-test + Turbine in
  commonTest/jvmTest, MockEngine for network) that later waves follow.

## Original Out of Scope

These items were out of scope for wave 1. Some have since shipped in later
waves; keep this list as historical PRD context, not current project inventory.

- Wave 2+ features: auth (client+server JWT), connectivity monitoring,
  onboarding, permissions, notifications, sync/outbox, paging, forms,
  analytics, localization switcher, in-app update gate, monetization.
- Snackbar/dialog message bus, deep links, and adaptive navigation scaffolds
  (bottom bar / rail) — deferred until a feature needs them.
- Screenshot/golden tests and iOS-simulator UI test automation.
- CI pipeline definition (worth its own PRD; wave-1 tests are runnable locally
  via Gradle regardless).
- Publishing the template (GitHub template repo, versioning/changelog policy).
- Replacing the temporary KSP/Kotlin-version risk note — tracked in the
  version catalog comment, unrelated to this restructuring.

## Further Notes

- The dependency baseline (Koin 4.2.1, Ktor 3.5.0, Room 3.0.0-alpha06,
  Navigation3 1.1.1, Coil 3.5.0-beta01, DataStore 1.2.1, FileKit 0.14.1,
  Kermit 2.1.0 on Kotlin 2.4.0 / CMP 1.11.1 / AGP 9.2.1) is already verified on
  all four targets in this repo; wave 1 is purely structural on top of it.
- Navigation3 findings already learned the hard way (entry as scope member,
  explicit `SavedStateConfiguration`, polymorphic NavKey serializer
  registration) must be encoded in the navigation registry so feature authors
  never hit them.
- The all-in + delete model makes the showcase the de-facto integration test:
  if the template builds and the showcase runs on desktop, the wiring is sound.
- Suggested implementation order: build-logic → core:model/common →
  core:designsystem/ui → core:network (+ server endpoints) →
  core:datastore/database → feature:settings → feature:showcase → delete
  DemoScreen → migrate app:shared wiring.
- When a GitHub remote exists, this document should be filed as the repo's
  first issue and broken into implementation tickets (the prd-to-issues flow).
