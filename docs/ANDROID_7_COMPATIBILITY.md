# Android 7 compatibility experiment

Date: 2026-10-20

## Scope and changes

The standalone Android product now builds with `minSdk = 24` (Android 7.0).
The KMP template keeps its existing API 31 minimum. Compile/target SDK, app
identity, dependencies, signing, and timeout behavior are unchanged.

Two runtime fixes were necessary:

- Guard `Tile.setSubtitle` behind API 29. Older Quick Settings panels still
  receive the label, sun/moon icon, active state, and accessible description.
- Move the button's shader into a separate API 33 composable. The original
  shared drawing lambda captured a nullable `RuntimeShader`, even on API 24.
  Debug worked, but R8 merged that lambda and the optimized APK crashed with
  `NoClassDefFoundError: android.graphics.RuntimeShader` before reaching the
  SDK guard. The fallback now captures no `RuntimeShader` at all.

The existing Canvas renderer covers API 24–32. Android 13+ retains the AGSL
shader. Older devices have animated sun/moon transitions, but not the identical
shader effect.

## Evidence

Test environment: Android 7.0 / API 24 Google APIs ARM64 image, revision 29,
Android Emulator 37.3.3 on Apple Silicon, Pixel 2 profile, 1080×1920.

- Debug APK installs and launches without an app crash.
- Main screen renders both OFF/moon and ON/sun states.
- Permission rationale opens Android's Modify system settings page; granting
  access and returning applies the requested ON state.
- ON writes `1000000000` to `screen_off_timeout`.
- The timeout remains applied after Home and force-stopping the app.
- Reopening and switching OFF restores the exact previous value (`120000`
  during this test).
- Quick Settings tile toggles both ways with the same timeout results.
- A 1×1 widget can be placed using the Android 7 launcher. Its taps toggle the
  timeout and update its icon/accessibility action.
- With the charging stay-awake setting disabled, the screen remains awake on
  the launcher after waiting beyond the original 15-second timeout. The widget
  then restores that 15-second value when switched OFF; the display subsequently
  sleeps normally.
- Revoking Modify system settings and tapping the tile opens the in-app
  rationale without changing the timeout or crashing.
- Google's UMP test consent form loads; declining consent returns to the app.
  An AdMob banner marked “Test Ad” subsequently renders.
- Debug assembly and lint pass. Lint reports no `NewApi` errors.
  Widget `targetCellWidth`/`targetCellHeight` attributes are ignored below API 31;
  the existing `minWidth`/`minHeight` provide the older-platform dimensions.
- `./scripts/fastlane.sh android release` passes quality checks, release lint,
  release-script tests, optimized APK/AAB builds, signature verification, and
  asset packaging. The signed APK declares minimum API 24 and target API 37.
- After shader isolation, the minified signed APK passes API 24 cold launch,
  OFF/ON rendering, main-toggle restoration, and Quick Settings ON/OFF with no
  app crash in logcat. These release checks use screenshot mode to suppress
  production ads; the live consent/banner checks above used the debug app.
- The same signed APK also passes Android 15 / API 35 launch, shader rendering,
  main-toggle restoration, and tile ON/OFF. UIAutomator could not reach idle
  during the shader animation on this emulator, so interaction used inspected
  screenshot coordinates; tile assertions waited for the asynchronous update.

Screenshots are local, ignored artifacts under `dist/verification/android-7/`.

## Repeat locally

```sh
# Install the API 24 Google APIs ARM64 system image using Android Studio's SDK
# Manager, then create an AVD named never-sleep-api24.
emulator -avd never-sleep-api24 -port 5580
./gradlew :app:androidApp:assembleDebug :app:androidApp:lintDebug
adb -s emulator-5580 install -r \
  app/androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s emulator-5580 shell am start -n \
  com.lizz.neversleep.debug/com.lizz.neversleep.MainActivity
```

Use the real permission flow and all three toggle surfaces. Read
`adb -s emulator-5580 shell settings get system screen_off_timeout` before ON,
after ON, and after OFF. Disable the emulator's charging stay-awake option when
checking actual display sleep. Use debug builds for live advertising tests;
release screenshot mode suppresses ads.

Always repeat cold launch and both toggle states with the **minified release
APK** on API 24. Debug and lint alone did not catch the shader class-resolution
failure. Keep this check when changing either renderer.

## Release boundary

This establishes API 24 compatibility on the emulator, not certification of
every Android 7 phone. Still test a physical older device before publishing the
expanded support claim, especially OEM timeout clamping, power saving, launcher
behavior, and animation performance. The Redmi installation restriction is
unrelated and was not changed by this experiment.

The public website and store listing still say Android 12+. Update those claims
together when accepting Android 7 support for release. This experiment does not
publish the website or upload to Play.

## Primary references

- [Tile.setSubtitle](https://developer.android.com/reference/android/service/quicksettings/Tile#setSubtitle(java.lang.CharSequence))
  was added in API 29.
- [TileService](https://developer.android.com/reference/android/service/quicksettings/TileService)
  is available from API 24.
- [AGSL](https://developer.android.com/develop/ui/views/graphics/agsl)
  is available from Android 13.
- [Google Mobile Ads setup](https://developers.google.com/admob/android/quick-start)
  documents the SDK prerequisites. The actual merged manifest and successful
  test-ad rendering verify the dependencies resolved by this project.
