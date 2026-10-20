# Never Sleep promo videos

Started from the official Remotion Hello World template, cloned from GitHub:
https://github.com/remotion-dev/template-helloworld

Upstream commit: `ef997c7c1587a34c37c6f2c1e529f941ccc2051f`.
The original checkout is retained locally at `.tools/remotion-template`.
Remotion packages are pinned to 4.0.537 in this workspace and `web/bun.lock`.
Remotion's terms: https://www.remotion.dev/license

Run `./scripts/marketing.sh studio` or `./scripts/marketing.sh render` from the
repository root. A managed Node.js LTS and Bun are required; the wrapper prefers
repo-local tools when installed. Remotion downloads its render browser on first
use. The project stays local and is not deployed with the landing site.

`src/Promo.tsx` defines the silent app demo. `src/variants.ts` supplies
the opening hooks, durations, and whether to show the second tap for the compositions
in `src/Root.tsx`: `app-demo-screen-off`, `app-demo-one-tap`, and
`app-demo-stay-on-task`. All animation is derived
from Remotion's frame clock, including shader uniforms. `sync-assets.mjs` ports the
Android background and face shaders to WebGL and copies the icon, Roboto font, and official Play badge.
The phone uses the same `android/AppScreen.tsx` as the website, including the
Android control dimensions and Canvas icons.
`render.mjs` bundles once and produces three H.264 videos without audio streams,
plus inspection frames.

Preferred one-tap cut (12 seconds): 0–1.5s OFF; 1.5–2.6s tap ON and transition
to sun; 2.6–5.5s hold ON; 5.5–6.5s fade into the ending; 6.5–12s closing card.
The opening reads “Keep your screen on with one tap.” It does not toggle OFF again.

Other two cuts (14 seconds): 0–1.5s OFF; 1.5–2.6s tap ON and transition to sun; 2.6–7s hold ON;
7–8.1s tap OFF and transition to night; 8.1–9.5s hold OFF; 9.5–10.5s fade
into the ending; 10.5–14s “Never sleep. Always awake.” and the official
“Get it on Google Play” badge. Opening text is visible for 3.5 seconds and fades
out by 4 seconds. There is no second call to action or website address.
There are no task scenarios, reference screens, voiceovers, or music.

Use the launch and measurement instructions in `marketing/README.md`.
