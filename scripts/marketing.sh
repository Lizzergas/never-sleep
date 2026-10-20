#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="$ROOT/.tools/bin:$PATH"
cd "$ROOT/web"
case "${1:-prepare}" in
  prepare) bun run scripts/prepare-launch.ts ;;
  render)
    bun install --frozen-lockfile
    node apps/promo/sync-assets.mjs
    bun --filter @neversleep/promo typecheck
    node apps/promo/render.mjs
    bun run scripts/prepare-launch.ts
    ;;
  studio) bun --filter @neversleep/promo dev ;;
  report)
    cd "$ROOT"
    python3 scripts/measure-launch.py "${2:-marketing/measurement-template.json}"
    ;;
  *) echo 'Usage: scripts/marketing.sh [prepare|render|studio|report path/to/period.json]' >&2; exit 1 ;;
esac
