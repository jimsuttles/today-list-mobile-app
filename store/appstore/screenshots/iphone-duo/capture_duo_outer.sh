#!/bin/zsh
# Captures iPhone Duo App Store screenshots from a DEDICATED, DISPOSABLE simulator.
#
# Launching with -ScreenshotDemo deletes every task in the app, so this script
# fails closed: it only ever touches a simulator that it created itself
# (verified by a marker file in that simulator's own device directory), and
# only when you pass --reset-app-data. It never looks devices up by name and
# never reuses a testing simulator.
#
#   capture_duo_outer.sh --create
#       Create a new disposable iPhone Duo simulator and print its UDID.
#   capture_duo_outer.sh --udid <UDID> --reset-app-data <path/to/TodayList.app>
#       Seed demo data in that simulator and capture the outer display
#       (1398 x 2034). simctl cannot tap, so the script pauses for you to
#       select each tab in Xcode's Device Hub.
#
# Requires Xcode 27.1 with the iOS 27.1 simulator runtime.
set -euo pipefail

readonly name="TodayList Screenshots (disposable)"
readonly device_type="com.apple.CoreSimulator.SimDeviceType.iPhone-Duo"
readonly runtime="com.apple.CoreSimulator.SimRuntime.iOS-27-1"
readonly marker_name=".todaylist-screenshot-disposable"
readonly devices_dir="$HOME/Library/Developer/CoreSimulator/Devices"

fail() { print -u2 "error: $1"; exit 1; }

if [[ ${1:-} == --create ]]; then
  udid=$(xcrun simctl create "$name" "$device_type" "$runtime")
  print -r -- "$udid" > "$devices_dir/$udid/$marker_name"
  print "Created disposable screenshot simulator: $udid"
  exit 0
fi

udid="" reset=0 app=""
while (( $# )); do
  case $1 in
    --udid) udid=${2:-}; shift 2 ;;
    --reset-app-data) reset=1; shift ;;
    -*) fail "unknown option $1" ;;
    *) app=$1; shift ;;
  esac
done

[[ -n $udid ]] || fail "pass --udid <UDID> of a simulator made with --create"
[[ -n $app && -d $app ]] || fail "pass the path to a built TodayList.app"
(( reset )) || fail "refusing to seed demo data without --reset-app-data (it deletes all tasks in that simulator)"

# Identity gate: marker written by --create, exact name, and device type.
marker="$devices_dir/$udid/$marker_name"
[[ -f $marker && $(<"$marker") == "$udid" ]] || fail "$udid was not created by --create; refusing to touch it"
plist="$devices_dir/$udid/device.plist"
[[ $(/usr/libexec/PlistBuddy -c "Print :name" "$plist" 2>/dev/null) == "$name" ]] || fail "$udid is not named \"$name\""
[[ $(/usr/libexec/PlistBuddy -c "Print :deviceType" "$plist" 2>/dev/null) == "$device_type" ]] || fail "$udid is not an iPhone Duo"

out=${0:A:h}/outer
xcrun simctl boot "$udid" 2>/dev/null || true
xcrun simctl bootstatus "$udid" -b >/dev/null
print "In Xcode > Device Hub, show \"$name\" ($udid)."

xcrun simctl status_bar "$udid" override --time 9:41 --batteryState charged --batteryLevel 100
xcrun simctl ui "$udid" appearance light
xcrun simctl ui "$udid" content_size large
xcrun simctl install "$udid" "$app"
xcrun simctl launch --terminate-running-process "$udid" com.fourctech.todaylist -ScreenshotDemo >/dev/null
sleep 6

mkdir -p "$out"
shoot() { xcrun simctl io "$udid" screenshot --display=1 "$out/$1.png" >/dev/null; print "saved $1.png"; }

shoot duo-outer-1-today
for shot in duo-outer-2-later duo-outer-3-history duo-outer-4-settings; do
  read "?Select the ${shot##*-} tab in Device Hub, then press Return "
  sleep 1
  shoot "$shot"
done
