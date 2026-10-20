# Web workspace — agent guide

Self-contained **Bun** workspace for Never Sleep web surfaces. Do not wire
Node/Vite/Astro into the root Gradle project.

Human setup: **README.md** in this folder.

## What we ship

| App                      | Deployed?               | URL / port                            |
| ------------------------ | ----------------------- | ------------------------------------- |
| `apps/landing`           | **Yes** — GitHub Pages  | https://neversleep.app                |
| `apps/admin`             | No (template scaffold)  | `127.0.0.1:5174` local only           |
| `dev/play-listing.astro` | **No** — local dev only | `/play` when using `dev:landing:play` |

**Public routes:** `/`, `/privacy`, `404`. No `/play` in production builds.

## Commands

```bash
bun install
bun run dev:landing              # http://127.0.0.1:5173
bun run dev:landing:play         # + temporary /play page for Play Console copy
bun run lint && bun run format:check && bun run typecheck && bun run test && bun run build
```

Landing-only build:

```bash
bun --filter @neversleep/landing build
# artifact: apps/landing/dist/
```

Deploy locally from the repository root: `./scripts/fastlane.sh website`.
The script checks and builds the site locally, then publishes static output to
`gh-pages`. Both custom GitHub workflows are disabled and manual-only.
Custom domain `neversleep.app` via `public/CNAME`.

## Landing architecture

- **Astro-first, static output** — no server runtime on GitHub Pages.
- **React islands:** `AppToggle.tsx` provides the interactive phone demo (`client:visible`).
  The page is neutral charcoal/slate. A fixed Android shader backdrop follows the
  toggle at 25% opacity, sharing its clock and motion preferences. The landing is
  only a hero and small footer; do not add lower marketing sections or header links.
- **Styles:** `src/styles/site.css`; Android preview styles are isolated in
  `src/components/android/app-screen.css`.
- **Hero preview:** `AppToggleMock.astro` → `AppToggle.tsx` → `android/AppScreen.tsx`.
  The same `AppScreen` is used by Remotion. It mirrors the Android 360×780 dp
  screenshot layout with ads hidden, using Roboto, 210 dp toggle, 32 dp gap,
  and the Android Canvas sun/moon drawing. Do not use store screenshot PNGs.
- **Shaders:** `node scripts/sync-android-demo.mjs` generates both WebGL shaders
  directly from Android AGSL. Landing build/dev regenerate them automatically.
  `android/parity.test.ts` checks source drift and Compose animation timing.
  The background eases for 1100 ms; glow for 400 ms; icons and face palette switch
  immediately, matching Android. Respect reduced motion and pause offscreen.
- **Google Play CTA:** official badge via `GooglePlayBadge.astro` +
  `public/badges/en_badge_web_generic.png` (do not recolor/crop).

## Conventions

- Bun workspaces; keep `bun.lock` committed.
- Oxlint + Oxfmt only — no ESLint/Prettier/Biome without an explicit decision.
- Oxfmt skips `.astro` templates; format those manually.
- Strict TypeScript; ignore `dist/`, `.astro/`, `src/pages/play.astro` (gitignored
  dev copy).
- `packages/api-client` stays minimal until a real OpenAPI boundary exists.

## Privacy & Play content

- Privacy policy source: `src/pages/privacy.astro` → `https://neversleep.app/privacy`
- Play listing fields (local): `dev/play-listing.astro`
- Store graphics reference: `../../assets/play-store/preview.html`

When changing legal copy or URLs, keep `app/androidApp` privacy strings in sync
(`BuildConfig`, `strings.xml`).

## Local marketing videos

`apps/promo` is a Remotion workspace cloned from the official starter. It is not
deployed. Run `../scripts/marketing.sh render` from `web/` to export videos.
The landing uses an allowlisted campaign URL helper in `src/lib/attribution.ts`;
keep it synchronized with `web/scripts/prepare-launch.ts`. No analytics SDK is added.
