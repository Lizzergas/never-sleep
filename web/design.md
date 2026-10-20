# Never Sleep web design

The public site uses a clean charcoal and slate-gray style. Keep the website
quiet, with the Android shader visible at 25% opacity behind the hero.

## Website

- Canvas: `#15191e`; dividers: `#30363e`.
- Primary type: `#eceef1`; secondary: `#929ca9` through `#adb5c1`.
- Inter for headings and body. Moderate heading weight, restrained tracking.
- A larger brand icon in the header, one hero with a phone demo and Play badge,
  and a small support/privacy footer. No navigation or lower marketing sections.
- Subtitle: “Keep your screen on just by one tap.” No extra feature copy.
- Headline: lavender for “Never sleep.”, warm yellow for “Always awake.”
- The page background reuses the Android shader at 25% opacity, synchronized
  with the demo state and clock. No glass panels or inline middle-dot/bullet separators.
- Official Google Play badge remains unmodified.
- Layout and public page styling live in `apps/landing/src/styles/site.css`.
  The older `hallmark.css` and `HeroBackdrop` are not used by public pages.
- Privacy and 404 use the same palette and navigation, with readable document widths.

## Android demo

`AppToggleMock.astro` hosts the interactive `AppToggle.tsx` island. The rendered
phone screen is `components/android/AppScreen.tsx`, also used by the Remotion video.
Keep its styling isolated from the website: Roboto, Android colors, Android spacing.

The reference viewport is 360×780 dp with system insets of 24 dp. It uses the
Android screenshot presentation (no settings button or ads), a 210 dp control,
32 dp title gap, and the native title padding and ON/OFF typography. The canvas
sun and moon follow `NeverSleepToggleButton.kt`, including gradients and small stars.
The device frame and fixed system-bar indicators are presentation chrome.

Generate both shader strings from the actual Kotlin source with:

```bash
node web/scripts/sync-android-demo.mjs
node web/scripts/sync-android-demo.mjs --check
```

The generator changes only AGSL types, main/output syntax, and the fragment
coordinate origin for WebGL. Never hand-edit `shaders.generated.ts`. Source parity
is checked by the web test suite. Generated files are excluded from formatting.

- ON: sun and warm dawn; keeps the screen awake.
- OFF: moon and night; restores the previous timeout.
- Icons and face palette change immediately, as Android does.
- Background: 1100 ms with Compose FastOutSlowIn easing.
- Glow: 400 ms with the same easing.
- Background clock: 40 seconds per loop; face clock: 12 seconds.
- Website renders at up to 30 fps, pauses offscreen/hidden, and uses static states
  for reduced-motion visitors. Keyboard and touch toggling remain available.
- WebGL unavailable: readable gradient fallback and Canvas icons.

Use the live renderer, never store screenshot PNGs, as the landing hero.
