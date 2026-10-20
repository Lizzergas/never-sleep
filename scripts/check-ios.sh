#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEVICE_ID="${1:-${IOS_SIMULATOR_UDID:-}}"
if [[ -z "$DEVICE_ID" ]]; then
    DEVICE_ID="$(xcrun simctl list devices available -j | python3 -c '
import json, sys
devices = [d for group in json.load(sys.stdin)["devices"].values()
           for d in group if "iPhone" in d["name"]]
devices.sort(key=lambda d: d["state"] != "Booted")
if not devices:
    sys.exit("Install an iPhone Simulator runtime in Xcode first.")
print(devices[0]["udid"])
')"
fi

STATE="$(xcrun simctl list devices -j | python3 -c '
import json, sys
device_id = sys.argv[1]
for group in json.load(sys.stdin)["devices"].values():
    for device in group:
        if device["udid"] == device_id:
            print(device["state"])
            sys.exit(0)
sys.exit("Simulator not found: " + device_id)
' "$DEVICE_ID")"
if [[ "$STATE" != "Booted" ]]; then
    xcrun simctl boot "$DEVICE_ID"
fi
xcrun simctl bootstatus "$DEVICE_ID" -b

OUTPUT="$ROOT/dist/ios-check/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUTPUT"
xcodebuild \
    -project "$ROOT/app/iosApp/iosApp.xcodeproj" \
    -scheme NeverSleep \
    -configuration Debug \
    -destination "platform=iOS Simulator,id=$DEVICE_ID" \
    -parallel-testing-enabled NO \
    -resultBundlePath "$OUTPUT/Tests.xcresult" \
    test CODE_SIGNING_ALLOWED=NO
xcrun xcresulttool export attachments \
    --path "$OUTPUT/Tests.xcresult" --output-path "$OUTPUT/screenshots"
echo "Test results and simulator screenshots: $OUTPUT"
