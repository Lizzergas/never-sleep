# NeverSleep Web Workspace

Self-contained Bun workspace for web surfaces that live beside the Kotlin
Multiplatform app without joining the Gradle module graph.

## Apps

- `apps/landing`: Astro + React islands, static-first, runs on `127.0.0.1:5173`.
- `apps/admin`: Vite + React + TypeScript, runs on `127.0.0.1:5174` and proxies
  `/api` to the Ktor server at `http://localhost:8080`.
- `packages/api-client`: placeholder TypeScript API boundary for future
  OpenAPI-generated clients.

## Commands

```bash
bun install
bun run dev:landing
bun run dev:admin
bun run lint
bun run format:check
bun run typecheck
bun run test
bun run build
```

Use `bun run format` to rewrite web files with Oxfmt.

Oxfmt is configured for the web workspace's supported file types and currently
skips `.astro` page templates; format those manually for now.
