# English (U.S.) App Store copy

## Name

Never Sleep

## Subtitle

Keep this display awake

## Promotional text

Start a focused display session when a recipe, dashboard, reference, or timer
needs to stay visible.

## Description

Never Sleep keeps your iPhone or iPad display awake while the app remains open
and active.

Choose a focused duration or continue until you end the session. The warm
ready state shifts into a calm night view so you can see the current state at a
glance.

Never Sleep is designed around iOS:

- It does not change your system Auto-Lock setting.
- It works only while the app is visible and active.
- Sessions stop when the app leaves the foreground.
- A Control Center control can open Never Sleep and begin a session quickly.

Useful for recipes, reference material, dashboards, timers, presentations, and
other moments when you need one screen to remain visible.

## Keywords

display,awake,screen,auto-lock,focus,recipe,dashboard,timer,reference,utility

## Support URL

https://neversleep.app/

## Privacy policy URL

https://neversleep.app/privacy

## App Review notes

Never Sleep uses `UIApplication.shared.isIdleTimerDisabled` only while a
foreground display session is active. Leaving the app or ending the session
restores the normal iOS idle-timer behavior. The Control Center control opens
the app via an App Intent; it does not attempt to keep the display awake in the
background.
