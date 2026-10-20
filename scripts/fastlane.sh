#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
# Use a managed Ruby on macOS, not Apple's unsupported system Ruby.
if [[ -d /opt/homebrew/opt/ruby/bin ]]; then
    export PATH="/opt/homebrew/opt/ruby/bin:$PATH"
fi
export PATH="$ROOT/.tools/bin:$PATH"
export BUNDLE_PATH="$ROOT/vendor/bundle"
export LANG="${LANG:-en_US.UTF-8}"
export FASTLANE_SKIP_UPDATE_CHECK=1
export FASTLANE_HIDE_CHANGELOG=1
export FASTLANE_OPT_OUT_USAGE=1
if ! bundle check >/dev/null 2>&1; then
    echo "Installing pinned local release tools..."
    bundle install
fi
exec bundle exec fastlane "$@"
