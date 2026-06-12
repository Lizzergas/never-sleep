# Plan: Wave 2 — Quality Tooling, Connectivity, Onboarding, Auth

> Repo: Lizzergas/compose-multiplatform-template (local: MyAppTemplate). Wave 1
> (core split, convention plugins, registry/showcase, network, UiState,
> settings, database) is complete and on CI. This plan adds the next tracer
> features + the quality infrastructure, reusing wave-1 patterns throughout.

## Context

The template has the plug-in architecture but lacks: lint/coverage tooling
(the one thing lizz-yt-downloader had that this doesn't), connectivity
awareness, a first-launch flow, and the single biggest time-saver — auth.
Auth also forces two patterns the template doesn't demonstrate yet:
conditional navigation and an authenticated 401-refresh flow.

## User decisions

- **Tokens**: KVault (`com.liftric:kvault`, Keychain/Keystore) on mobile;
  desktop gets a documented plain-file fallback behind the same interface.
- **Connectivity**: hand-rolled expect/actual (no new dependency).
- **Lint policy**: NO commit/push hooks — CI-only enforcement on PRs/main,
  plus one easy local command (`./gradlew qualityCheck`). Existing violations
  fixed once during rollout; no baselines.
- **Server users**: in-memory `UserRepository` behind an interface (bcrypt
  hashes); interface is the documented swap-point for a real DB.

## Durable decisions

- New modules follow wave-1 conventions: `template.kmp.library` /
  `template.kmp.feature` plugins; features implement `FeatureRegistration`
  (core:navigation) + one Koin module; the 3-line wiring contract
  (settings.gradle include, `AppNavHost.featureRegistrations`, `di/Koin.kt`).
- Optional cross-feature contracts use the ThemeModeProvider pattern: a small
  interface in a core module, bound by the owning feature's Koin module,
  looked up with `getOrNull` + fallback so features stay removable.
- Auth routes under `/api/auth/*`; protected sample route `GET /api/me`.
  DTOs in `core:model` (AuthRequest, TokenPair, UserDto).
- `core:network` stays auth-agnostic: it defines an `AuthTokenProvider`
  interface and installs Ktor's Bearer auth (load + refresh) only when one is
  bound in Koin; `feature:auth` provides the implementation.
- Versions: pin at execution time via repo1.maven.org maven-metadata.xml
  (detekt, ktlint-gradle, kover, kvault, bcrypt; ktor-* stay 3.5.0).
- Tests follow wave-1 conventions: behavioral, Turbine + coroutines-test,
  MockEngine for client HTTP, ktor-server-test-host for routes, and the
  in-process-server e2e pattern from `NetworkDemoE2eTest` for full slices.

---

## Phase 1: Quality tooling (Detekt + Ktlint + Kover)

### What to build

A `template.quality` convention plugin in `build-logic` applying detekt +
ktlint (jlleitschuh) to every module via the existing `template.kmp.library`
(and the root/server/app modules), shared config under `config/detekt/`,
Kover applied with exclusion filters (generated code, `@Composable` previews,
Room/Koin generated classes — port the battle-tested filters from
lizz-yt-downloader). A root aggregate task **`qualityCheck`** (detekt +
ktlintCheck everywhere) and `koverHtmlReport` aggregation. Run `ktlintFormat`
once and fix remaining detekt findings so the codebase starts clean. Add a
parallel `quality` job to `.github/workflows/ci.yml`. Explicitly NO git hooks.

### Acceptance criteria

- [ ] `./gradlew qualityCheck` runs detekt + ktlint across all modules locally, currently green
- [ ] CI gains a `quality` job on PRs/main; new violations fail it
- [ ] `./gradlew koverHtmlReport` produces a merged report with sensible exclusions
- [ ] No pre-commit/pre-push hooks anywhere; AGENTS.md documents the one local command

## Phase 2: Connectivity monitor

### What to build

New `core:connectivity` module: `ConnectivityMonitor { val isOnline: Flow<Boolean> }`
with expect/actual platform Koin modules (wave-1 datastore pattern) —
Android `ConnectivityManager` network callbacks (+ `ACCESS_NETWORK_STATE`
permission in androidApp manifest), iOS `NWPathMonitor`, desktop lightweight
socket-reachability polling. `OfflineBanner` composable in `core:ui`; the app
shell (`App.kt`) shows it above `AppNavHost` via Koin `getOrNull` fallback
(absent module = no banner, stays removable). Demonstrate retry-on-reconnect
in the showcase network demo: when state is `Error(Network)` and connectivity
returns, re-issue the load.

### Acceptance criteria

- [ ] All targets compile; banner appears/disappears with a fake monitor in a compose test
- [ ] Network demo auto-retries on reconnect (test with fake monitor + in-process server)
- [ ] Removing `core:connectivity` follows the documented contract (shell falls back to no banner)

## Phase 3: Onboarding

### What to build

`feature:onboarding`: a HorizontalPager intro flow (2–3 template pages) with a
"Get started" button persisting a seen-flag through `core:datastore`
(`OnboardingRepository`, second consumer proving the preferences pattern).
Conditional start destination: a `StartRouteOverride` contract in
`core:navigation` (ThemeModeProvider pattern) — `AppNavHost` consults it
(suspend, with loading gate) before building the back stack; onboarding binds
it to return its route until the flag is set, then replaces the stack with the
showcase home. Listed in the catalog so it can be re-run from the showcase.

### Acceptance criteria

- [ ] First launch shows onboarding; "Get started" lands on showcase home; relaunch skips straight to home (UI test with temp DataStore, both paths)
- [ ] Repository flag round-trip test
- [ ] 3-line removal contract holds (no override bound → straight to showcase)

## Phase 4: Auth — server slice

### What to build

Server JWT auth with `ktor-server-auth` + `ktor-server-auth-jwt` + bcrypt
(`at.favre.lib:bcrypt`). DTOs in `core:model`. Routes: `POST /api/auth/register`,
`POST /api/auth/login`, `POST /api/auth/refresh` (refresh-token rotation),
protected `GET /api/me`. `UserRepository` interface + in-memory impl (bcrypt
hashes). JWT config (secret/issuer/audience/expiries) from environment with
dev defaults; short access TTL, long refresh TTL. StatusPages maps auth
failures to 401/409 consistently with the existing error contract.

### Acceptance criteria

- [ ] Route tests: register, login, wrong-password 401, duplicate-register 409, me-with-token 200, me-without 401, refresh rotates tokens, stale refresh 401
- [ ] `curl` happy path works against `./gradlew :server:run`
- [ ] UserRepository swap-point documented

## Phase 5: Auth — client slice

### What to build

`feature:auth` + `core:network` extension, the wave-2 flagship tracer:

- **TokenStorage** expect/actual: KVault on Android/iOS; desktop file fallback
  (documented as such). Koin platform modules per the datastore pattern.
- **AuthTokenProvider** interface in `core:network`; `createHttpClient`
  installs Ktor's `Auth`/Bearer plugin (loadTokens + refreshTokens against
  `/api/auth/refresh` via a bare client) only when a provider is bound —
  automatic 401-refresh for every API call in the app.
- **AuthRepository/SessionManager**: `StateFlow<SessionState>`
  (Unknown/LoggedOut/LoggedIn(user)) restored from storage at startup;
  login/register/logout.
- **UI + conditional navigation**: "Account" catalog entry whose entry point
  switches on session state — Login/Register screens (forms with `core:ui`
  error rendering) when logged out, Profile screen (`GET /api/me` data +
  logout) when logged in.
- Android manifest/iOS notes if KVault needs any (none expected).

### Acceptance criteria

- [ ] Unit tests: SessionManager state transitions (Turbine, fake TokenStorage); refresh flow with MockEngine (expired access -> refresh -> retry succeeds)
- [ ] E2e (in-process server, temp storage): register -> profile shows email -> logout -> login again; and 401-refresh path with a short-TTL access token
- [ ] Tokens survive app restart (storage round-trip test); desktop fallback documented
- [ ] All targets compile; `qualityCheck` green; full 3-line removal contract documented for feature:auth

---

## Execution notes

- Implementation order = phase order (quality first so every later phase is
  lint-checked from birth).
- Per phase: implement → all-target compile + tests → desktop run when
  user-visible → commit → push (CI validates).
- Update `docs/MODULES.md` (new modules + contracts) and `AGENTS.md`
  (qualityCheck command) as phases land; copy this plan into the repo's plans
  directory alongside the wave-1 plan.
- Watch-outs from wave 1: Navigation3 serializer registration is handled by
  the registry (new routes just implement `registerRoutes`); DataStore
  process-wide memoization pattern applies to TokenStorage's desktop file
  impl; e2e tests must override storage/config via `loadKoinModules` like
  `TestSupport.kt` does.

## Verification (end-to-end)

1. `./gradlew qualityCheck jvmTest :server:test :app:androidApp:assembleDebug :app:shared:compileKotlinIosArm64`
2. Desktop run: onboarding on first launch → showcase → Account: register,
   see profile, logout, login → kill server mid-session, see offline
   banner + network demo auto-retry on restart
3. CI green on push (build-and-test, ios-compile, quality)
