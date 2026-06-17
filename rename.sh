#!/usr/bin/env bash
#
# Renames the template into your app, in place.
#
#   ./rename.sh MyCoolApp                  -> package com.lizz.mycoolapp
#   ./rename.sh MyCoolApp org.acme.cool    -> custom package
#
# Replaces (in every tracked text file) and moves source directories:
#   com.lizz.myapptemplate -> <package>
#   MyAppTemplate          -> <AppName>   (display name, window title, iOS product, ...)
#   myapptemplate          -> <appname>   (lowercase leftovers, e.g. ~/.myapptemplate dirs)
#   com/lizz/myapptemplate -> <package path>
#
# (build-logic's brand-neutral `buildlogic` package is intentionally untouched.)
# Run once from the repo root, review with `git diff`, then commit.
set -euo pipefail

OLD_PACKAGE="com.lizz.myapptemplate"
OLD_PATH="com/lizz/myapptemplate"
OLD_NAME="MyAppTemplate"
OLD_LOWER="myapptemplate"
SCRIPT_PATH="${BASH_SOURCE[0]}"

NEW_NAME="${1:-}"
if [[ -z "$NEW_NAME" ]]; then
  echo "Usage: ./rename.sh <AppName> [package.id]" >&2
  echo "  e.g. ./rename.sh MyCoolApp" >&2
  echo "  e.g. ./rename.sh MyCoolApp org.acme.coolapp" >&2
  exit 1
fi
if [[ ! "$NEW_NAME" =~ ^[A-Za-z][A-Za-z0-9]*$ ]]; then
  echo "error: app name must be alphanumeric and start with a letter (got '$NEW_NAME')" >&2
  exit 1
fi

NEW_LOWER="$(echo "$NEW_NAME" | tr '[:upper:]' '[:lower:]')"
NEW_PACKAGE="${2:-com.lizz.$NEW_LOWER}"
if [[ ! "$NEW_PACKAGE" =~ ^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$ ]]; then
  echo "error: package must look like com.example.app (got '$NEW_PACKAGE')" >&2
  exit 1
fi
NEW_PATH="$(echo "$NEW_PACKAGE" | tr '.' '/')"

if [[ ! -f settings.gradle.kts ]]; then
  echo "error: run from the repository root" >&2
  exit 1
fi
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "error: working tree is not clean — commit or stash first" >&2
  exit 1
fi

echo "Renaming: $OLD_NAME -> $NEW_NAME, $OLD_PACKAGE -> $NEW_PACKAGE"

# 1. Rewrite tokens in all tracked text files (longest token first so the
#    package replacement doesn't eat the substring of the next ones).
git ls-files -z | while IFS= read -r -d '' f; do
  [[ "$f" == "rename.sh" ]] && continue
  [[ -f "$f" ]] || continue
  grep -Iq . "$f" 2>/dev/null || continue   # skip binary files
  if grep -qiE "$OLD_LOWER" "$f"; then
    perl -pi -e "s|\Q$OLD_PACKAGE\E|$NEW_PACKAGE|g; s|\Q$OLD_PATH\E|$NEW_PATH|g; s|\Q$OLD_NAME\E|$NEW_NAME|g; s|\Q$OLD_LOWER\E|$NEW_LOWER|g" "$f"
  fi
done

# 1b. Drop template-only onboarding sections from app docs after the repo is
#     no longer a template checkout.
for f in README.md AGENTS.md; do
  [[ -f "$f" ]] || continue
  perl -0pi -e 's/\n?<!-- TEMPLATE_ONLY_RENAME_START -->.*?<!-- TEMPLATE_ONLY_RENAME_END -->\n?/\n/gs; s/\n{3,}/\n\n/g' "$f"
done
if [[ -f README.md ]]; then
  perl -0pi -e 's/A Kotlin\/Compose Multiplatform template targeting/A Kotlin\/Compose Multiplatform application targeting/' README.md
fi
if [[ -f AGENTS.md ]]; then
  perl -0pi -e 's/Compose Multiplatform template \(Android, iOS, Desktop JVM, Ktor server\)/Compose Multiplatform app (Android, iOS, Desktop JVM, Ktor server)/' AGENTS.md
fi

# 2. Rename dotted-FQN directories (e.g. Room schema exports like
#    core/database/schemas/com.lizz.myapptemplate.database.AppDatabase/).
find . -depth -type d -name "*${OLD_PACKAGE}*" -not -path "./.git/*" -not -path "*/build/*" | while read -r dir; do
  mv "$dir" "${dir//${OLD_PACKAGE}/${NEW_PACKAGE}}"
done

# 3. Move Kotlin/Java source trees to the new package path.
find . -type d -path "*/$OLD_PATH" -not -path "./.git/*" -not -path "*/build/*" | while read -r dir; do
  src_root="${dir%/"$OLD_PATH"}"
  target="$src_root/$NEW_PATH"
  mkdir -p "$target"
  if [ -n "$(ls -A "$dir")" ]; then
    mv "$dir"/* "$target"/
  fi
  # Remove the now-empty old package directories (stops at non-empty parents).
  rmdir -p "$dir" 2>/dev/null || true
done

# 4. This is a one-shot bootstrap script; removing it keeps the generated app
#    free of stale template identifiers.
if [[ -f "$SCRIPT_PATH" ]]; then
  rm -- "$SCRIPT_PATH"
fi

echo
echo "Done. Next steps:"
echo "  1. Review:  git diff --stat"
echo "  2. Verify:  ./gradlew :app:shared:jvmTest :server:test"
echo "  3. Commit:  git add -A && git commit -m 'Rename template to $NEW_NAME'"
echo "  4. Rename the repo folder itself if desired: cd .. && mv $OLD_NAME $NEW_NAME"
