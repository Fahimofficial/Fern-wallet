#!/usr/bin/env bash
# Install an APK on the running emulator, launch it from the launcher, and fail if it
# crashes or is not running after a few seconds. Catches startup regressions that
# compilation cannot: R8/ProGuard rules, Hilt graph errors, manifest/merge problems.
#
# Usage: scripts/ci/smoke_test_apk.sh <apk> <application-id> [<output-dir>]
set -euo pipefail

apk="$1"
pkg="$2"
out="${3:-build/smoke-test}"
mkdir -p "$out"

# Test a fresh install. A copy left in a reused emulator snapshot is signed with another run's
# throwaway key, and `adb install -r` over it fails with INSTALL_FAILED_UPDATE_INCOMPATIBLE.
if adb shell pm list packages "$pkg" 2>/dev/null | tr -d '\r' | grep -qx "package:$pkg"; then
  echo "Removing previously installed $pkg"
  adb uninstall "$pkg" > /dev/null || true
fi

echo "Installing $apk"
adb install "$apk"

adb logcat -c || true
activity="$(adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN \
  -c android.intent.category.LAUNCHER "$pkg" | tail -n 1 | tr -d '\r')"
if [[ "$activity" != "$pkg/"* ]]; then
  echo "::error title=Smoke test::No launcher activity found for $pkg (got: $activity)"
  exit 1
fi

echo "Launching $activity"
adb shell am start -W -n "$activity" | tee "$out/am-start.txt"
if ! grep -q "Status: ok" "$out/am-start.txt"; then
  echo "::error title=Smoke test::'am start' did not report Status: ok"
  adb logcat -d > "$out/logcat.txt" || true
  exit 1
fi

# Give the first screen time to compose and background work (WorkManager, exchange rates) to start.
sleep 15

adb logcat -d > "$out/logcat.txt" || true
adb logcat -d -b crash > "$out/crash.txt" || true
adb shell dumpsys activity activities > "$out/activities.txt" || true

if grep -q "Process: $pkg" "$out/crash.txt" || grep -q "FATAL EXCEPTION.*" "$out/crash.txt"; then
  echo "::error title=Smoke test::$pkg crashed after launch; see crash.txt in the smoke-test artifact"
  grep -A 30 "FATAL EXCEPTION" "$out/crash.txt" | head -n 60 || true
  exit 1
fi
if grep -q "ANR in $pkg" "$out/logcat.txt"; then
  echo "::error title=Smoke test::$pkg is not responding (ANR) after launch"
  exit 1
fi
if ! adb shell pidof "$pkg" > /dev/null; then
  echo "::error title=Smoke test::$pkg is not running 15 s after launch"
  exit 1
fi

echo "Smoke test passed: $pkg launched and is still running."
