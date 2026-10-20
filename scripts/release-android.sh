#!/usr/bin/env bash
# Fastlane owns the local release workflow.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ $# -gt 0 ]]; then
    echo "Usage: ./scripts/release-android.sh"
    echo "Screenshots: python3 scripts/generate-play-screenshots.py --device SERIAL"
    echo "Release checks are mandatory; --fast and --skip-checks have been removed."
    [[ "$1" == "--help" || "$1" == "-h" ]] && exit 0
    exit 1
fi
exec "$ROOT/scripts/fastlane.sh" android release
