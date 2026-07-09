# Never Sleep — web workspace

Marketing site and local dev tools for [Never Sleep](https://neversleep.app), kept
separate from the Gradle/Android build.

## Apps

| Package | Purpose |
|---------|---------|
| `@neversleep/landing` | Public site at **neversleep.app** (Astro, static) |
| `@neversleep/admin` | Template Vite admin (local only; proxies `/api` → Ktor `:8080`) |
| `@neversleep/api-client` | Placeholder TS API boundary |

## Commands

```bash
bun install
bun run dev:landing
bun run dev:landing:play   # local Play listing page at /play (not deployed)
bun run lint
bun run format:check
bun run typecheck
bun run test
bun run build
```

Format: `bun run format` (Oxfmt; `.astro` files are manual for now).

## Landing site

- **Home** — hero, feature cards, official Google Play badge
- **Privacy** — Play-required policy at `/privacy`
- **Deploy** — GitHub Pages via `.github/workflows/deploy-landing.yml`

Dev-only Play Console reference: `apps/landing/dev/play-listing.astro` (use
`bun run dev:landing:play`).

Agent conventions: **[AGENTS.md](./AGENTS.md)** · repo-wide: **[../AGENTS.md](../AGENTS.md)**