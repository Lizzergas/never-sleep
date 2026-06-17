# Plan: Desktop Deep Links PRD

Status: Draft / macOS-first implementation. Date: 2026-06-17.

## Problem Statement

Android and iOS can receive custom-scheme deep links and pass them into shared
Kotlin, but Desktop JVM currently cannot. Opening `myapptemplate://open/...` in
Chrome on desktop has no reliable app target because the packaged desktop app
does not register itself as a protocol handler and the desktop entry point does
not consume URL arguments or warm URL events.

This creates an architectural gap: deep link parsing is modular and shared, but
desktop host ownership is incomplete. A production template should treat desktop
as a first-class platform, not as a manual-only test surface.

## Solution

Add desktop/JVM deep link support so links like `myapptemplate://open/notes`
work from Chrome, terminal commands, and OS-level URL dispatch on macOS,
Windows, and Linux. Shared Kotlin already owns parsing and navigation; desktop
should only capture raw URLs and forward them to the existing shared deep link
entry point.

Desktop behavior:

- Cold start with a URL opens the resolved deep-linked destination as soon as
  Compose mounts.
- Warm links are delivered to the existing app instance instead of opening
  duplicate windows.
- Invalid or unsupported URLs are ignored and logged.
- Screens and ViewModels never receive raw URLs; they continue receiving typed
  routes through shared navigation.

Platform strategy:

- macOS registers `CFBundleURLTypes` through Compose Desktop
  `macOS.infoPlist.extraKeysRawXml`, then installs a JVM
  `Desktop.setOpenURIHandler` bridge for warm URL events.
- Windows registers the custom scheme for the installed app and passes the URL
  to the launcher as an argument. Because the current target is MSI, this uses
  installer or registry support rather than assuming Android/iOS-style manifest
  routing.
- Linux registers `x-scheme-handler/myapptemplate` through the desktop entry
  and passes `%u` to the launcher.
- The JVM entry point accepts `args`, extracts supported URL arguments, and
  forwards them after Koin init.
- A desktop-only warm-instance bridge forwards second-process URL launches to
  the running app and exits.

Implementation note: the first implementation pass is macOS-first for packaged
protocol registration. Desktop startup URL args are implemented generically for
manual launcher tests, but Windows/Linux installer registration and
cross-platform single-instance forwarding remain follow-up work.

## User Stories

1. As an app user, I want to click `myapptemplate://open/notes` from Chrome on
   desktop, so that I land on Notes without manually navigating.
2. As an app user, I want to open
   `myapptemplate://open/showcase/network` while the app is already running, so
   that the existing window navigates there instead of opening a duplicate app
   window.
3. As an app user, I want invalid or unknown desktop links to fail quietly, so
   that a bad link does not crash the app or leave it in a partial navigation
   state.
4. As a template user, I want desktop to use the same feature-owned deep link
   specs as Android and iOS, so that adding a new route does not require
   platform-specific parsing logic.
5. As a template user, I want desktop links to synthesize the same retained
   navigation stacks as mobile links, so that deep-linked detail screens have
   predictable back behavior.
6. As a maintainer, I want to test desktop deep links from terminal commands, so
   that I can verify routing without relying only on Chrome or OS UI flows.
7. As a maintainer, I want host layers to capture URLs only, so that parsing,
   validation, auth gating, and stack synthesis remain shared Kotlin behavior.
8. As a developer using the renamed template, I want the custom scheme to be
   renamed with the rest of the app identity, so that generated apps do not keep
   `myapptemplate` in protocol registration.

## Implementation Decisions

- Reuse the existing shared deep link system; do not create a desktop-only
  parser.
- Add a desktop-only bridge object responsible for installing platform URL
  handlers when supported, extracting startup URLs from `main(args)`, forwarding
  warm URLs to the existing process, and calling the shared deep link entry
  point exactly once per received URL.
- Keep the custom scheme as `myapptemplate` in the template so the rename flow
  can rewrite it with the rest of the app identity.
- Change desktop `main` to accept `args`; do not change shared route shapes,
  feature route declarations, or screen/ViewModel contracts.
- Keep raw URLs out of screens and ViewModels. Desktop passes raw URLs only to
  the shared app-level deep link API.
- Treat macOS as the first-class packaged path because Compose Desktop and the
  JDK provide direct support through `Info.plist` URL scheme registration and
  `Desktop.setOpenURIHandler`.
- Treat Windows and Linux as packaging-specific protocol registration work:
  Windows through MSI/registry command registration, Linux through
  `x-scheme-handler` desktop entry registration.
- Add a small desktop-only single-instance forwarding mechanism for warm links
  on platforms where URL activation starts a second process.
- Keep HTTPS App Links / Universal Links documented as the production-domain
  path, but do not enable them until a generated app has a real domain and
  platform signing/team configuration.
- Update active architecture docs during implementation to state that platform
  layers capture URLs only, while shared Kotlin owns parsing, validation, auth
  gating, and retained-stack navigation.

## Testing Decisions

- Good tests should assert external behavior: accepted URLs are forwarded once,
  invalid URLs are ignored, and warm links reach the existing app instance. They
  should not depend on the internal socket, lock, or platform API details.
- Desktop URL extraction tests should accept:
  - `myapptemplate://open/home`
  - `myapptemplate://open/notes`
  - `myapptemplate://open/settings`
  - `myapptemplate://open/account`
  - `myapptemplate://open/showcase/design-system`
  - `myapptemplate://open/showcase/network`
- Desktop URL extraction tests should reject unknown schemes, malformed URLs,
  oversized input, unknown paths, and unrelated command-line arguments.
- Warm-instance forwarding should be tested behind an interface so JVM tests do
  not depend on real OS protocol registration.
- Existing shared deep link parser and navigation tests remain the source of
  truth for route synthesis, auth gating, and retained-stack behavior.
- Manual macOS verification:
  - Package or run the distributable.
  - Run `open 'myapptemplate://open/notes'`.
  - Paste the same URL in Chrome.
  - Verify warm links navigate the existing window and do not create a second
    window.
- Manual Windows verification:
  - Install the MSI or a development protocol registration.
  - Run `start "" "myapptemplate://open/notes"`.
  - Verify warm links reuse the running app.
- Manual Linux verification:
  - Install the package or desktop entry.
  - Run `xdg-open 'myapptemplate://open/notes'`.
  - Verify warm links reuse the running app.

## Out of Scope

- Verified HTTPS desktop links.
- PWA or web `navigator.registerProtocolHandler` support.
- User-configurable schemes.
- Note detail links until a real `NoteDetailRoute` exists.
- Native installer signing, notarization, and update changes beyond the
  existing desktop packaging path.
- Replacing or redesigning the shared deep link registry.

## Further Notes

- Compose Multiplatform documents macOS desktop deep linking through
  `macOS.infoPlist.extraKeysRawXml` plus `Desktop.setOpenURIHandler`.
- Java's `Desktop.setOpenURIHandler` must be installed conditionally because
  support depends on desktop environment capabilities.
- Linux desktop protocol handlers use the freedesktop
  `x-scheme-handler/<scheme>` convention.
- Oracle `jpackage` supports custom packaging resources, which is the likely
  path for overriding generated desktop entries where needed.
- Windows URI activation is straightforward for packaged Windows apps, but the
  current desktop target is MSI, so classic protocol registration must be
  handled through installer or registry integration.
