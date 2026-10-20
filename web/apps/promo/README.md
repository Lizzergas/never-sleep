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

`src/Promo.tsx` defines one silent 14-second app demo. All animation is derived
from Remotion's frame clock, including shader uniforms. `sync-assets.mjs` ports the
Android MORPH_SHADER to WebGL and copies the icon and official Play badge.
`render.mjs` bundles once and produces a H.264 video without an audio stream,
plus inspection frames.

Timeline: 0–1.5s OFF; 1.5–3s tap ON and transition to sun; 3–7s hold ON;
7–8.5s tap OFF and transition to night; 8.5–9.5s hold OFF; 9.5–10.5s fade
into the ending; 10.5–14s “Never Sleep”, “Always awake”, and Play CTA.
There are no task scenarios, reference screens, voiceovers, or music.

Use the launch and measurement instructions in `marketing/README.md`.
