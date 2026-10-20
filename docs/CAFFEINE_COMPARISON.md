# Never Sleep compared with Caffeine

Researched 20 October 2026. Scope: **Caffeine – Keep Screen On by Akarin**,
package `moe.zhs.caffeine`. Several apps share this name; this comparison does
not combine their features. The [Play listing](https://play.google.com/store/apps/details?id=moe.zhs.caffeine)
identifies this developer and links the official guide. Its displayed update
date is 28 August 2025. No installation or runtime test of Caffeine was performed.

## What Caffeine does

Caffeine primarily works through a Quick Settings tile. It also exposes a
launcher “Toggle” shortcut on Android 7.1+, which can be placed on the home
screen. That is a shortcut, not evidence of a stateful home-screen widget.
The developer says the app has no advertising or in-app purchases.
[Official getting-started guide](https://lab.zhs.moe/caffeine/guide/).

The feature comparison below covers its functional options. Additional settings
include tile naming, launcher/recents visibility, title-only tile layout, and
battery-optimization setup. Timers apply to tile activation; repeated taps can
cycle durations. Dimming is unavailable in compatibility mode. Baseline defaults
are empty/disabled, with platform-specific restrictions and overrides. This is
a documentation inventory, not a hands-on inspection of the latest build.
[Configuration reference](https://lab.zhs.moe/caffeine/guide/config/).

Device presets override baseline choices: MIUI and Samsung Android 12+ force
compatibility mode and require battery configuration. Expert mode is unlocked
by tapping Version seven times. OPPO-family instructions vary by ROM generation.
[Official compatibility guide](https://lab.zhs.moe/caffeine/guide/device-compatibility/).

External automation uses `moe.zhs.caffeine.ToggleActivity`, with optional integer
extra `Status` (0 = deactivate, 1 = activate). This permits another automation
app to control Caffeine; it does not establish built-in per-app rules or schedules.
[Official automation guide](https://lab.zhs.moe/caffeine/guide/advanced/).

## How it keeps the screen awake

Caffeine documents a default method and a compatibility method; only the latter
is explicitly described as modifying system settings. Its Android 14+ setup
instructions reference foreground-service restrictions, and its configuration
guide mentions a notification as an entry point. No public implementation source
was located during this research. The exact default API, timeout value, service
lifecycle, restoration logic, and notification actions are therefore **not
verified**. Do not describe a specific wake-lock API as established fact.
[Setup](https://lab.zhs.moe/caffeine/guide/),
[configuration](https://lab.zhs.moe/caffeine/guide/config/).

Never Sleep's implementation is directly inspectable:
[`NeverSleepController.kt`](../app/androidApp/src/main/kotlin/com/lizz/neversleep/NeverSleepController.kt)
saves the current timeout, writes `SCREEN_OFF_TIMEOUT = 1_000_000_000` milliseconds
(about 11.6 days), and restores the saved value when disabled. It reconciles
externally changed values and refreshes widgets after successful writes.
The product manifest declares a Quick Settings service, but no ongoing keep-awake
foreground service, lock receiver, timer, or notification mechanism.
[`AndroidManifest.xml`](../app/androidApp/src/main/AndroidManifest.xml).

This is conceptually similar to Caffeine's compatibility mode, not a confirmed
identical implementation. Never Sleep changes a persistent system setting: closing
its UI does not stop it, and manually locking does not automatically restore the
previous timeout. Those are code-derived implications, not Caffeine observations.

## What we have and intentionally omit

| Capability | Caffeine | Never Sleep now |
|---|---|---|
| Keep display awake across apps | Yes | Yes |
| Quick Settings toggle | Yes | Yes |
| Home-screen control | Launcher shortcut documented | Stateful 1×1 widget |
| Main interaction | Tile-first setup | Large ON/OFF control; sun/night transition |
| Duration choices | Yes | None |
| Stop automatically on lock | Optional | No |
| Dimming choice | Optional | No |
| External automation interface | Documented | No documented public integration |
| OEM/compatibility/expert options | Yes | One settings-based approach |
| Launcher/tile presentation preferences | Yes | Fixed presentation |
| Advertising | None | Optional AdMob banners |
| Android baseline | Official website says 7.0+ | 12+ |

Caffeine sources: the guides above and
[official homepage](https://lab.zhs.moe/caffeine/).
Never Sleep sources:
[`NeverSleepScreen.kt`](../app/androidApp/src/main/kotlin/com/lizz/neversleep/NeverSleepScreen.kt),
[`SettingsDialog.kt`](../app/androidApp/src/main/kotlin/com/lizz/neversleep/SettingsDialog.kt),
the controller/manifest above, and
[`libs.versions.toml`](../gradle/libs.versions.toml).
Unlisted Caffeine features are not proven absent; avoid claiming it lacks a main
toggle or widget without inspecting the installed current build.

## Product recommendation

Keep the core promise: **one toggle, three access points, no configuration of
awake behavior**. Timers, dimming, custom titles, hidden icons, automation, and
expert modes would undermine the intended difference. Prioritize accurate state,
restoring the original timeout, and dependable behavior on real devices.
Some Caffeine options exist to work around manufacturer behavior; our single
method reduces choices but does not establish equivalent device compatibility.
Never Sleep's exported main activity accepts internal control extras; this is not
the same as a supported, documented automation contract.

The meaningful gap to decide deliberately is forgetting to turn it off.
Automatically ending a session when the user locks the screen could improve
simplicity without adding a visible setting, but changes the current persistent
ON contract and needs engineering/behavior validation. It is a product decision,
not a feature to copy automatically.

Advertising is the contrary pressure: Caffeine is ad-free, while our ads introduce
consent, a banner, and settings. A simpler interface is credible; claiming less
friction overall requires accounting for that tradeoff.
