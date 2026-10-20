# Native iOS development (paused)

iOS development is paused. The foreground-only prototype does not meet the
product requirement to keep the screen awake across other apps. No App Store
release is planned. The code and checks below are retained for reference.

The NeverSleep Xcode scheme targets iPhone and iPad, iOS 18.2+. Build and run it
directly in Xcode. The scheme builds the small `core/review` Kotlin framework
for review-prompt policy; it does not use `app/shared` or the KMP UI template.
The simulator workflow needs the repository's Gradle/JDK setup but does not
require an Apple signing team.

## Behavior and ownership

- `DisplaySessionModel` owns each scene's requested session and absolute deadline.
  Background time counts toward the deadline. Returning after expiry does not
  reactivate protection. Repeated external start actions do not extend a session.
- `IdleTimerCoordinator` aggregates active scenes before setting
  `UIApplication.shared.isIdleTimerDisabled`. The app releases the assertion when
  no active scene requests it. It never changes the user's Auto-Lock setting.
- `ContentView` provides only the main toggle, duration picker/countdown, and Help.
  New installs default to an unlimited session.
  Duration preference and review-prompt eligibility are stored in app-local
  UserDefaults; session state is not restored after termination.
  Device backups may retain preferences.
- `Shared/StartDisplaySessionIntent.swift` belongs to both the app and extension.
  Its `OpenIntent` opens the app and queues a request. An active scene consumes it
  once, including when the intent runs before the scene exists. The Control
  Center control, Home Screen widget, and App Shortcut use this same intent.
  There is no cross-app awake toggle and no shared app-group state.
- `neversleep://session/start` remains supported as a direct app deep link.
  System actions do not use `OpenURLIntent` with that custom scheme.
- `PrivacyInfo.xcprivacy` declares local UserDefaults access (`CA92.1`).
  No ads, tracking SDKs, or analytics are included in the native targets.

## Automated checks

```bash
bash scripts/check-ios.sh [SIMULATOR_UDID]
# Equivalent default selection:
./scripts/fastlane.sh ios check
```

The script prefers an already booted iPhone, otherwise boots the first available
iPhone. Passing an iPad UDID runs the same checks on iPad. Results and screenshot
attachments are saved under `dist/ios-check/<timestamp>/`.

Unit tests exercise active/inactive scenes, multiple scenes, disconnects,
deadline expiry, repeated starts, indefinite sessions, persistence, and intent
request handoff. UI tests exercise the toggle, duration selection/persistence, timed expiry,
and return from background.

Debug builds accept `NEVER_SLEEP_VALIDATION_DURATION_SECONDS` to shorten a
session during validation. Release builds ignore it.

## Manual device checks

1. Select a short system Auto-Lock interval. Start a session and verify the
   display remains awake beyond that interval.
2. End the session, or leave the app, and verify normal Auto-Lock returns.
3. Test expiry, return before/after expiry, Side-button locking, and Low Power Mode.
4. Add the Never Sleep Control Center control and Home Screen widget. Invoke
   each with the app terminated, backgrounded, and already active. Verify an
   existing session's deadline is preserved.
5. Run Start Display Session from Shortcuts. Verify cold and warm launches.
6. Check iPad multitasking, rotation, larger text, VoiceOver, and Reduce Motion.

Simulator checks cannot certify physical Auto-Lock or power-management behavior.
Signing, archive validation, App Store Connect configuration, and uploading are
intentionally deferred. Refresh store screenshots from the final UI before submission.

## Verification on 20 October 2026

- Xcode 26.6 with iOS 26.5 on iPhone 17 Pro: 16 unit tests and 3 UI tests
  passed, including the shared review-policy integration.
- The same 3 UI tests passed on iPad Pro 13-inch (M5). Its initial OS migration
  stalled; a simulator restart recovered it before testing.
- Added the Home Screen widget through the simulator's widget gallery. Tapping
  its button cold-launched the app; the system log recorded
  `StartDisplaySessionIntent.perform()` completing in NeverSleep and the idle
  timer becoming disabled.
- iPhone screenshots are in `dist/ios-preview/iphone/`. The UI test screenshots
  cover ready, active, and help.
- Equivalent iPad captures are in `dist/ios-preview/ipad/`.
- The privacy-page static build and project/privacy manifest validation passed.
- Control Center and Shortcuts use the same intent, but their individual UI
  launch paths still belong in the manual device checklist above.

Apple references:

- [Idle timer](https://developer.apple.com/documentation/uikit/uiapplication/isidletimerdisabled)
- [Controls and OpenIntent target membership](https://developer.apple.com/documentation/widgetkit/creating-controls-to-perform-actions-across-the-system)
- [OpenURLIntent requires universal links](https://developer.apple.com/documentation/appintents/openurlintent)
- [Required-reason privacy APIs](https://developer.apple.com/documentation/bundleresources/app-privacy-configuration/nsprivacyaccessedapitypes/nsprivacyaccessedapitype)

After the UI simplification, all 16 unit tests and 3 revised UI tests passed on
iPad Pro 13-inch (M5). Current captures are in `dist/ios-preview/simplified-ipad/`.
