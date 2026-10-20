# iOS platform feasibility

Implementation follow-up: the foreground one-tap toggle, shared `OpenIntent`,
Home Screen widget, native UI tests, and local privacy documentation have since
been added. See [iOS development](IOS_DEVELOPMENT.md) for the current architecture
and checks. The gaps below record the original investigation; the platform
boundary still applies to the current implementation.

## Media workaround investigation

The product requirement is one tap followed by unrestricted use of other apps,
with the display remaining awake until the selected deadline. A foreground-only
utility does not meet that requirement.

Picture in Picture is a candidate for a separate feasibility experiment:
[AVPlayer preventsDisplaySleepDuringVideoPlayback](https://developer.apple.com/documentation/avfoundation/avplayer/preventsdisplaysleepduringvideoplayback)
controls video-driven sleep prevention, and
[AVPictureInPictureController](https://developer.apple.com/documentation/avkit/avpictureinpicturecontroller)
allows video playback in a floating window while the app is backgrounded.
Their combination suggests a possible workaround; these documents do not prove
that a minimal looping video will reliably prevent Auto-Lock across other apps.
No PiP workaround has been implemented or physically verified.

Test on an actual iPhone with a short Auto-Lock interval: visible PiP, PiP tucked
offscreen, pause/close, switching apps, another app starting media, timed expiry,
and power-saving modes. Measure battery impact. Stop playback at expiry and
verify normal Auto-Lock resumes. Simulator playback alone is insufficient proof.

Silent background audio is not equivalent to preventing display sleep.
Guided Access restricts the device to one app, so it fails the switching-apps
requirement. Manual Auto-Lock changes remain available in Settings, but there
is no documented public API here for our app to set and restore that preference.

Technical feasibility and App Store eligibility are separate questions.
[Review guidelines 2.5.1 and 2.5.4](https://developer.apple.com/app-store/review/guidelines/#software-requirements)
require intended use of APIs and background services. A dummy-media keep-awake
workaround cannot be assumed eligible merely because it uses public AVKit APIs.

Researched 20 October 2026 against Apple documentation. This is a platform and
source-code assessment, not a physical-device certification or App Review result.

## The product boundary

Android Never Sleep changes the persistent system screen timeout and therefore
continues to affect other apps. Apple's documented
[`UIApplication.isIdleTimerDisabled`](https://developer.apple.com/documentation/uikit/uiapplication/isidletimerdisabled)
controls the idle timer **for the app**. Apple recommends using it only while
the app needs to continue displaying content and resetting it when that need ends.
It does not document reading, saving, changing, or restoring the user's global
Auto-Lock preference.

**Engineering conclusion:** a foreground display-session app is feasible;
Android's cross-app promise has no supported equivalent in the public APIs
reviewed. Treat this as a product limitation, not a missing Kotlin implementation
or permission screen. This research does not claim to prove the absence of every
private or managed-device mechanism.

Background execution does not supply a documented global display assertion.
Apple's [background strategies](https://developer.apple.com/documentation/backgroundtasks/choosing-background-strategies-for-your-app)
cover bounded work, content refresh, transfers, and system-scheduled processing.
Newer [continuous background tasks](https://developer.apple.com/documentation/backgroundtasks/performing-long-running-tasks-on-ios-and-ipados)
support user-started work such as exports, with progress reporting and cancellation.
Neither is documented as a mechanism for keeping another app's display awake.

## A concrete defect in the existing launch integrations

The checkout already has a native SwiftUI implementation, despite `AGENTS.md`
describing `app/iosApp` as template scaffold. Its
[`IdleTimerCoordinator`](../app/iosApp/iosApp/IdleTimerCoordinator.swift) uses the
public idle-timer API. However, both
[`NeverSleepControl`](../app/iosApp/NeverSleepControl/NeverSleepControl.swift) and
[`NeverSleepAppShortcuts`](../app/iosApp/iosApp/NeverSleepAppShortcuts.swift) pass
`neversleep://session/start` to `OpenURLIntent`.

Apple's [`OpenURLIntent` documentation](https://developer.apple.com/documentation/appintents/openurlintent)
explicitly requires a universal link and disallows custom URL schemes. An
[Apple DTS response](https://developer.apple.com/forums/thread/762586) confirms
universal links as the supported URL mechanism for this use.
The existing implementation violates that documented contract; compilation and
direct custom-scheme launch tests do not establish that these integrations work.

Fix options are to implement a proper foreground App Intent that routes inside
the app, or configure HTTPS universal links and use those with `OpenURLIntent`.
If retaining iOS 18 support, account for API availability when choosing the
foreground-intent implementation. Current
[`openAppWhenRun` documentation](https://developer.apple.com/documentation/appintents/appintent/openappwhenrun)
recommends `supportedModes` for newer systems and notes restrictions for extension
execution. Test the actual Control Center and Shortcuts actions with the app
terminated, backgrounded, and already foregrounded.

WidgetKit [controls](https://developer.apple.com/documentation/widgetkit/controls-collection)
can expose app actions or open the app from Control Center, the Lock Screen, and
the Action button. Ordinary [interactive widgets](https://developer.apple.com/documentation/widgetkit/adding-interactivity-to-widgets-and-live-activities)
also use App Intents. These are entry points to functionality the app can perform;
their availability does not imply authority to change global Auto-Lock.
For Never Sleep, communicate “open/start a display session” rather than a
system-wide awake toggle.

## Product and distribution implications

[App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)
require public APIs used for their intended purposes (2.5.1), appropriate use of
background services (2.5.4), accurate metadata (2.3), and adequate lasting utility
(4.2). Silent audio or unrelated location/background activity is not a supported
way to recover Android parity. These are design constraints, not a prediction
that this particular app will be accepted or rejected.

The app must provide a useful experience on its own visible screen. Recipes or
reference material in another app would not receive Never Sleep's protection.
If those are advertised use cases, provide an appropriate in-app content
experience or revise the claims. A useful ambient display or clock is another
possible product direction. Choose the purpose before adding more entry points.

Physical-device validation should cover foreground inactivity, timed-session
expiry, background/foreground transitions, manual locking, Control Center,
Shortcuts, and power-saving modes. An
[Apple DTS discussion of idle-timer behavior](https://developer.apple.com/forums/thread/815962)
reports that power management can override keep-awake behavior; this is a reason
to test supported devices, not evidence that every device behaves identically.

## User-managed alternatives

Apple documents changing Auto-Lock manually in
[Settings → Display & Brightness → Auto-Lock](https://support.apple.com/guide/iphone/keep-the-iphone-display-on-longer-iph7117338a8/ios).
That is a user-controlled system setting, not an implementation API for this app.
Always-On display shows a dimmed Lock Screen on supported hardware and is a
different behavior.

[Guided Access](https://support.apple.com/en-us/111795) has a separate Display
Auto-Lock setting and restricts the device to a single app during the session.
It can help a person keep a chosen task on screen, but it is user-managed and
does not provide Never Sleep with an ordinary cross-app toggle.

## Repository readiness checked during this investigation

The native app and Control Center extension target iOS 18.2. The app has six
session-model unit tests and no KMP build phase. This command completed with
`TEST BUILD SUCCEEDED`; it compiles the app, extension, and tests but **does not
execute the tests**:

```sh
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme NeverSleep \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/never-sleep-ios-investigation \
  build-for-testing CODE_SIGNING_ALLOWED=NO
```

The store-asset validator also passed:
`uv run --with Pillow python scripts/validate-ios-store-assets.py`
(four icons, ten screenshots). Remaining readiness gaps found in the checkout:

- `TEAM_ID` is blank in
  [`Config.xcconfig`](../app/iosApp/Configuration/Config.xcconfig). Configure
  signing and verify a device/archive build; Apple account availability was not
  checked.
- The existing Fastlane checks compile the KMP iOS template, not this native
  product. Add native build/test and release coverage.
- [iOS listing copy](../assets/app-store/ios/metadata/en-US.md) mentions recipes,
  reference material, and dashboards without an in-app content viewer. It also
  says sessions stop on backgrounding, while the model retains the request and
  resumes protection if the app returns before expiry. Align claims and behavior.
- The website's privacy copy describes Android. Review it against the iOS
  implementation and prepare accurate App Store privacy disclosures.
- Device testing and actual Shortcuts/Control Center execution remain unverified.
