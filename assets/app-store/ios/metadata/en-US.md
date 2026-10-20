# English (U.S.) App Store copy

## Name

Never Sleep

## Subtitle

Keep this display awake

## Promotional text

Choose a duration and tap once to keep this display awake while Never Sleep is open.

## Description

Never Sleep keeps your iPhone or iPad display awake while the app remains open
and active.

Choose a focused duration or continue until you end the session. The warm
ready state shifts into a calm night view so you can see the current state at a
glance.

Your chosen duration is saved locally. Tap again to end the session.

Never Sleep is designed around iOS:

- It does not change your system Auto-Lock setting.
- It works only while the app is visible and active.
- Leaving the app restores normal Auto-Lock. Return before your session expires
  to resume keeping the display awake.
- A Control Center control can open Never Sleep and begin a session quickly.
- A Home Screen widget and App Shortcut can also open the app and start a session.

Never Sleep does not keep other apps awake or display their content. It has no
ads, accounts, or analytics.

## Keywords

display,awake,screen,auto-lock,focus,timer,simple,utility

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
