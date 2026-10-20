#!/usr/bin/env bash
# Build locally and publish only static output to the gh-pages branch.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="$ROOT/.tools/bin:$PATH"
cd "$ROOT"
REPOSITORY="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
REMOTE="$(git remote get-url origin)"

cd "$ROOT/web"
bun install --frozen-lockfile
bun run lint
bun run format:check
bun run typecheck
bun run test
bun --filter @neversleep/landing build
SITE="$ROOT/web/apps/landing/dist"
for file in index.html privacy/index.html CNAME .nojekyll app-ads.txt; do
    test -f "$SITE/$file"
done
test ! -e "$SITE/play"
test "$(cat "$SITE/CNAME")" = "neversleep.app"

# This temporary repository contains generated website output only.
DEPLOY_DIR="$(mktemp -d "${TMPDIR:-/tmp}/never-sleep-pages.XXXXXX")"
trap 'rm -rf "$DEPLOY_DIR"' EXIT
git -C "$DEPLOY_DIR" init -q
git -C "$DEPLOY_DIR" remote add origin "$REMOTE"
if [[ -n "$(git -C "$DEPLOY_DIR" ls-remote --heads origin gh-pages)" ]]; then
    git -C "$DEPLOY_DIR" fetch --depth=1 origin gh-pages
    git -C "$DEPLOY_DIR" checkout -q -b gh-pages FETCH_HEAD
    git -C "$DEPLOY_DIR" rm -qr --ignore-unmatch .
else
    git -C "$DEPLOY_DIR" checkout -q --orphan gh-pages
fi
cp -R "$SITE/." "$DEPLOY_DIR/"
git -C "$DEPLOY_DIR" add .
if ! git -C "$DEPLOY_DIR" diff --cached --quiet; then
    git -C "$DEPLOY_DIR" -c user.name="Lizz Studio" -c user.email="home@lizz.com" \
        -c commit.gpgsign=false commit -qm "Publish locally built Never Sleep website"
    git -C "$DEPLOY_DIR" push origin HEAD:gh-pages
fi
# Preserve the existing domain and HTTPS configuration.
gh api --method PUT "repos/$REPOSITORY/pages" \
    -f build_type=legacy -f "source[branch]=gh-pages" -f "source[path]=/"
# Required on the first switch from Actions: the branch push happened before
# Pages was configured to watch it.
gh api --method POST "repos/$REPOSITORY/pages/builds" --jq '{status, url}'
echo "Published locally built HTML to gh-pages. GitHub Pages deployment may take a few minutes."
echo "Check: https://neversleep.app/privacy and https://neversleep.app/app-ads.txt"
