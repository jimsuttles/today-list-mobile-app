#!/bin/zsh
# Captures the iPhone Duo outer-display (folded) App Store screenshots, 1398 x 2034.
# Requires Xcode 27.1 with the iOS 27.1 simulator runtime, and a Debug build of
# TodayList for the iOS Simulator. Seeds demo data with -ScreenshotDemo, which
# deletes all tasks in that simulator.
#
# Usage: capture_duo_outer.sh <path/to/TodayList.app>
# Tab switching needs a tap on each tab; simctl cannot tap, so this script
# captures Today automatically and pauses for you to select the next tab.
set -euo pipefail

app=${1:?usage: capture_duo_outer.sh <path/to/TodayList.app>}
out=${0:A:h}/outer
name="iPhone Duo (screenshots)"

udid=$(xcrun simctl list devices available | awk -v n="$name" 'index($0, n) { gsub(/[()]/, "", $(NF-1)); print $(NF-1); exit }')
if [[ -z $udid ]]; then
  udid=$(xcrun simctl create "$name" com.apple.CoreSimulator.SimDeviceType.iPhone-Duo com.apple.CoreSimulator.SimRuntime.iOS-27-1)
fi
xcrun simctl boot "$udid" 2>/dev/null || true
xcrun simctl bootstatus "$udid" -b >/dev/null
open -a Simulator --args -CurrentDeviceUDID "$udid"

xcrun simctl status_bar "$udid" override --time 9:41 --batteryState charged --batteryLevel 100
xcrun simctl ui "$udid" appearance light
xcrun simctl ui "$udid" content_size large
xcrun simctl install "$udid" "$app"
xcrun simctl launch --terminate-running-process "$udid" com.fourctech.todaylist -ScreenshotDemo >/dev/null
sleep 6

mkdir -p "$out"
shoot() { xcrun simctl io "$udid" screenshot --display=1 "$out/$1.png" >/dev/null; echo "saved $1.png"; }

shoot duo-outer-1-today
for shot in duo-outer-2-later duo-outer-3-history duo-outer-4-settings; do
  read "?Select the ${shot##*-} tab in Simulator, then press Return "
  sleep 1
  shoot "$shot"
done
