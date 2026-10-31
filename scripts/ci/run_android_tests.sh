#!/usr/bin/env bash
# Run connected (emulator) Android tests and fail on problems the Gradle task doesn't fail on.
#
# AGP 9's connected-test runner logs "AndroidTestRunner failed … Failed to install APK" and still
# ends the build successfully, so a test APK that never installed looked like a pass. This script
# removes test packages left in a reused emulator snapshot (installed with another debug key, they
# make installs fail with INSTALL_FAILED_UPDATE_INCOMPATIBLE), runs Gradle, and fails on those
# messages. It also saves logcat next to the Gradle log, because a crashing instrumentation process
# is reported only as "Process crashed" without the exception. junit_summary.py then checks that
# tests actually ran.
#
# Usage: scripts/ci/run_android_tests.sh <log-file> <gradle task>...
set -euo pipefail

log="$1"
shift
logdir="$(dirname "$log")"
mkdir -p "$logdir"

# Test APKs of this repo's modules are named <namespace>.test, e.g. com.ivy.data.test.
stale="$(adb shell pm list packages 2>/dev/null | tr -d '\r' | sed -n 's/^package://p' | grep -E '^com\.ivy\..*\.test$' || true)"
for pkg in $stale; do
  echo "Removing stale test package $pkg"
  adb uninstall "$pkg" > /dev/null || true
done
adb logcat -c || true

set +e
./gradlew "$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}
set -e

adb logcat -d > "$logdir/logcat.txt" 2>/dev/null || true
adb logcat -d -b crash > "$logdir/logcat-crash.txt" 2>/dev/null || true
if grep -q "FATAL EXCEPTION" "$logdir/logcat-crash.txt" "$logdir/logcat.txt" 2>/dev/null; then
  echo "::group::Crashes reported by the emulator (logcat)"
  grep -h -A 40 "FATAL EXCEPTION" "$logdir/logcat-crash.txt" "$logdir/logcat.txt" | head -n 200 || true
  echo "::endgroup::"
fi

if grep -qE "AndroidTestRunner failed|Failed to install APK|INSTALL_FAILED_[A-Z_]+" "$log"; then
  echo "::error title=Android tests did not run::A test APK failed to install or the test runner failed; see the log above (search for 'AndroidTestRunner failed')."
  grep -E "AndroidTestRunner failed|Failed to install APK|INSTALL_FAILED_[A-Z_]+" "$log" | head -n 10
  exit 1
fi
exit "$status"
