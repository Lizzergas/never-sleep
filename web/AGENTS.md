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

Landing-only build (what CI deploys):

```bash
bun --filter @neversleep/landing build
# artifact: apps/landing/dist/
```

Deploy: `.github/workflows/deploy-landing.yml` on push to `main` when `web/**`
changes. Custom domain `neversleep.app` via `public/CNAME`.

## Landing architecture

- **Astro-first, static output** — no server runtime on GitHub Pages.
- **React islands** only where needed: `HeroBackdrop.tsx` (Paper Shaders:
  `@paper-design/shaders-react`, `client:only="react"`) and `AppToggle.tsx` (the
  hero's live Never/Normal toggle, `client:visible`).
- **Design tokens:** `src/styles/tokens.css` · shared styles: `site.css`.
- **UI components:** `src/components/ui/` (`Button`, `Card`, `Kicker`, …).
- **Hero preview:** interactive toggle (`AppToggleMock.astro` → `AppToggle.tsx`), a
  web port of the app's `NeverSleepToggleButton` — the AGSL shader face is ported to
  WebGL in `toggleFace.ts` with a CSS-gradient fallback. **Not** Play Store
  screenshot PNGs (those live in `assets/play-store/` for upload only). If the app's
  `TOGGLE_FACE_SHADER` changes, mirror it in `toggleFace.ts`.
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
