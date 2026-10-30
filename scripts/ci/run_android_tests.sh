#!/usr/bin/env bash
# Run connected (emulator) Android tests and fail on problems the Gradle task doesn't fail on.
#
# AGP 9's connected-test runner logs "AndroidTestRunner failed … Failed to install APK" and still
# ends the build successfully, so a test APK that never installed looked like a pass. This script
# removes test packages left in a reused emulator snapshot (installed with another debug key, they
# make installs fail with INSTALL_FAILED_UPDATE_INCOMPATIBLE), runs Gradle, and fails on those
# messages. junit_summary.py then checks that tests actually ran.
#
# Usage: scripts/ci/run_android_tests.sh <log-file> <gradle task>...
set -euo pipefail

log="$1"
shift
mkdir -p "$(dirname "$log")"

# Test APKs of this repo's modules are named <namespace>.test, e.g. com.ivy.data.test.
stale="$(adb shell pm list packages 2>/dev/null | tr -d '\r' | sed -n 's/^package://p' | grep -E '^com\.ivy\..*\.test$' || true)"
for pkg in $stale; do
  echo "Removing stale test package $pkg"
  adb uninstall "$pkg" > /dev/null || true
done

set +e
./gradlew "$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}
set -e

if grep -qE "AndroidTestRunner failed|Failed to install APK|INSTALL_FAILED_[A-Z_]+" "$log"; then
  echo "::error title=Android tests did not run::A test APK failed to install or the test runner failed; see the log above (search for 'AndroidTestRunner failed')."
  grep -E "AndroidTestRunner failed|Failed to install APK|INSTALL_FAILED_[A-Z_]+" "$log" | head -n 10
  exit 1
fi
exit "$status"
