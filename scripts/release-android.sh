#!/usr/bin/env bash
# Build signed release APK + AAB, copy artifacts, and optionally capture Play Store screenshots.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

FAST=false
CAPTURE_SCREENSHOTS=false
SKIP_CHECKS=false

usage() {
    cat <<'EOF'
Usage: ./scripts/release-android.sh [options]

Options:
  --fast                 Skip qualityCheck (faster local iteration)
  --skip-checks          Skip all pre-build verification
  --screenshots          Capture screenshots on a connected device/emulator
  -h, --help             Show this help

Requires:
  - keystores/never-sleep-release.keystore + keystore.properties (see keystore.properties.example)
  - app/androidApp/admob.properties (see admob.properties.example)

Outputs:
  dist/release/androidApp-release.apk
  dist/release/androidApp-release.aab
  assets/play-store/screenshots/ (with --screenshots)
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --fast) FAST=true ;;
        --skip-checks) SKIP_CHECKS=true ;;
        --screenshots) CAPTURE_SCREENSHOTS=true ;;
        -h | --help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage
            exit 1
            ;;
    esac
    shift
done

if [[ ! -f "$ROOT/keystore.properties" ]]; then
    echo "Missing keystore.properties. Copy keystore.properties.example and configure signing." >&2
    exit 1
fi

if [[ ! -f "$ROOT/app/androidApp/admob.properties" ]]; then
    echo "Missing app/androidApp/admob.properties. Copy admob.properties.example." >&2
    exit 1
fi

VERSION_CODE="$(awk '/versionCode =/ { print $3; exit }' "$ROOT/app/androidApp/build.gradle.kts")"
VERSION_NAME="$(awk '/versionName =/ && !/Suffix/ { gsub(/"/, "", $3); print $3; exit }' "$ROOT/app/androidApp/build.gradle.kts")"

echo "==> Never Sleep Android release v${VERSION_NAME} (${VERSION_CODE})"

if [[ "$SKIP_CHECKS" == false ]]; then
    if [[ "$FAST" == true ]]; then
        echo "==> Running jvmTest"
        ./gradlew jvmTest --quiet
    else
        echo "==> Running qualityCheck + jvmTest"
        ./gradlew qualityCheck jvmTest --quiet
    fi
fi

echo "==> Building signed release APK and AAB"
./gradlew :app:androidApp:assembleRelease :app:androidApp:bundleRelease --quiet

mkdir -p "$ROOT/dist/release"
APK_SRC="$ROOT/app/androidApp/build/outputs/apk/release/androidApp-release.apk"
AAB_SRC="$ROOT/app/androidApp/build/outputs/bundle/release/androidApp-release.aab"

cp "$APK_SRC" "$ROOT/dist/release/androidApp-release.apk"
cp "$AAB_SRC" "$ROOT/dist/release/androidApp-release.aab"

APK_SHA="$(shasum -a 256 "$ROOT/dist/release/androidApp-release.apk" | awk '{print $1}')"
AAB_SHA="$(shasum -a 256 "$ROOT/dist/release/androidApp-release.aab" | awk '{print $1}')"

cat >"$ROOT/dist/release/release-manifest.txt" <<EOF
versionName=$VERSION_NAME
versionCode=$VERSION_CODE
apk=dist/release/androidApp-release.apk
aab=dist/release/androidApp-release.aab
apk_sha256=$APK_SHA
aab_sha256=$AAB_SHA
privacy_policy_url=https://neversleep.app/privacy
play_listing_url=https://neversleep.app/play
application_id=com.lizz.neversleep
EOF

echo "==> Release artifacts"
echo "    APK: $ROOT/dist/release/androidApp-release.apk"
echo "    AAB: $ROOT/dist/release/androidApp-release.aab"
echo "    Manifest: $ROOT/dist/release/release-manifest.txt"

if [[ "$CAPTURE_SCREENSHOTS" == true ]]; then
    echo "==> Generating polished Play Store screenshots"
    python3 "$ROOT/scripts/generate-play-screenshots.py" --apk "$ROOT/dist/release/androidApp-release.apk"
fi

echo "==> Done. Upload dist/release/androidApp-release.aab to Play Console."