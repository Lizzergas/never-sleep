# Web workspace guide

This directory is a self-contained Bun workspace for web surfaces. Keep Node,
Vite, Astro, Oxc, and TypeScript tooling under `web/`; do not wire these tasks
into the root Gradle project unless explicitly requested.

Human-facing setup notes live in `README.md`; this file records agent-specific
workflow rules for this nested workspace.

## Commands

- Install dependencies: `bun install`
- Landing dev server: `bun run dev:landing`
- Admin dev server: `bun run dev:admin`
- Build all web packages: `bun run build`
- Lint: `bun run lint`
- Format: `bun run format`
- Check formatting: `bun run format:check`
- Typecheck all packages: `bun run typecheck`
- Test all packages: `bun run test`

## Architecture

- `apps/landing` is Astro-first and static-first. Use React islands only where
  interactivity is needed.
- `apps/admin` is a Vite React TypeScript app. During local development,
  requests to `/api` proxy to the Ktor server at `http://localhost:8080`.
- `packages/api-client` is the shared web API boundary. Keep it small until the
  Ktor server exposes a stable OpenAPI spec.
- Do not add a shared React UI package until duplication between apps is real.

## Tooling

- Use Bun workspaces and keep `bun.lock` committed.
- Use Oxlint and Oxfmt. Do not add Biome, ESLint, or Prettier without an
  explicit decision.
- Oxlint uses `.oxlintrc.json` so CLI defaults and editor/LSP integration find
  the same config.
- Oxfmt currently ignores `.astro` page files in this workspace; keep Astro
  templates tidy manually until formatter support is strong enough to enable.
- Use strict TypeScript and keep generated/build artifacts ignored.
