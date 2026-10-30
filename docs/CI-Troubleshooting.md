# Troubleshooting CI failures

If you see any of the PR checks failing (❌) go to [Actions](https://github.com/Fahimofficial/Fern-wallet/actions) and find it there. Or simply click "Details" next to the failed check and explore the logs to see why it has failed.

## PR description check

It means that you didn't follow our [official PR template](../.github/PULL_REQUEST_TEMPLATE.md).
Update your PR description with all necessary information. You can also check the exact error by
clicking "Details" on the failing (❌) check.

## Detekt
[Detekt](https://detekt.dev/) is a static code analyzer for Kotlin that we use to enforce code readability and good practices.

**To run Detekt locally:**
```
./gradlew detekt
```

If the Detekt errors are caused by a legacy code, you can suppress them using a baseline.

**Suppress Detekt** (only if you're sure that Detekt is wrong)

Add `@Suppress("ID_OF_THE_CHECK")` to ignore the error on a single place. For example:
```kotlin
@Suppress("FunctionMaxLength")
fun veryVeryLongFunction() {
    // ...
}
```

**Detekt baseline** (not recommended)
```
./scripts/detektBaseline.sh
```

## Lint

We use the [standard Android Lint](https://developer.android.com/studio/write/lint) plus [Slack's compose-lints](https://slackhq.github.io/compose-lints/) as an addition to enforce proper Compose usage.

**To run Lint locally:**
```
./scripts/lint.sh
```

If the Lint errors are caused by a legacy code, you can suppress them using a baseline.

**Suppress Lint** (only if you're sure that Lint is wrong)

Same as suppressing Detekt, just add `@Suppress("ID_OF_CHECK")`.

**Lint baseline** (not recommended)
```
./scripts/lintBaseline.sh
```

## Unit tests

If this job is failing this means that your changes break an existing unit test. You must identify the failing tests and fix your code.

**To run the Unit tests locally:**
```
./gradlew testDebugUnitTest
```

## Compose Stability

This GitHub Action checks whether your `@Composable` functions are stable (i.e. "restartable" and "skippable"). If it fails it means that some of your composables are unstable. That causes unnecessary recompositions which can lead to lost frames and laggy UI/UX especially when animation or scrolling. You must fix that! To fix it, open the failing working and see the output from the report - it tells you which `@Composable` functions are unstable and what parameters cause that.

**Fixing Stability issues:**
1. Read https://developer.android.com/jetpack/compose/performance/stability/fix
2. https://developer.android.com/jetpack/compose/performance/stability

**Compose Stability baseline** (not recommended)
```
./scripts/composeStabilityBaseline.sh
```
Do that only if the failure is in legacy code. If the script is failing, open it and execute the commands inside it manually.


## Paparazzi Tests
Paparazzi is used for automated visual testing. It captures screenshots of various `@Composable` and then compares them against baseline images to detect any visual differences or regressions.

**Fixing Paparazzi issues:**

1. Run `./gradlew verifyPaparazziDebug` locally to execute the Paparazzi tests and identify the specific screens where failures occur.
2. Upon failure, the system generates detailed reports pinpointing the changes introduced since the last successful test run. Review these reports to understand the nature of the failures.
3. If the identified changes are intentional, proceed by running `./gradlew {module_name}:recordPaparazziDebug` to update the baseline screenshots specifically for the failed UI components.
4. After updating the baseline screenshots, rerun `./gradlew {module_name}:verifyPaparazziDebug` to ensure that the tests now pass with the updated baselines.
5. Repeat this process iteratively for all modules that have failed Paparazzi tests, ensuring thorough validation and updating of baseline images as needed.

> **Note**: Ensure the use of static data only. Dynamic data, such as current time or date, will cause test failures due to its variability.

## Release build & APK policy

This job builds the minified release APK (signed with a throwaway key created in the job), then checks the
manifest that actually ships, after all library manifests are merged, against
[`.github/apk-policy.json`](../.github/apk-policy.json).

- **"permission X is not in the allowlist"**: your change (or a new/updated library) added a permission. Remove it,
  or add it to `permissions` in `apk-policy.json` with the feature that needs it **and** update `PRIVACY.md`.
- **"exported … is not in the allowlist"**: a component became reachable by other apps. Set
  `android:exported="false"` (or `tools:node="remove"` for an unwanted library component), or review it and add it
  to `exported_components` with the permission that guards it.
- **debuggable / cleartext / allowBackup**: release builds must keep `isDebuggable = false`,
  `usesCleartextTraffic="false"` and `allowBackup="false"`.
- **"not signed with the key from SIGNING_STORE_FILE"**: `signingConfigs.release` in `app/build.gradle.kts` no longer
  reads the signing environment variables.

Run it locally:

```sh
keytool -genkeypair -keystore /tmp/ci.jks -storetype PKCS12 -alias ci -keyalg RSA -dname "CN=local" -storepass password -keypass password
SIGNING_STORE_FILE=/tmp/ci.jks SIGNING_STORE_PASSWORD=password SIGNING_KEY_ALIAS=ci SIGNING_KEY_PASSWORD=password ./gradlew :app:assembleRelease
python3 scripts/ci/check_apk_policy.py --apk app/build/outputs/apk/release/app-release.apk \
  --aapt2 "$ANDROID_HOME/build-tools/<version>/aapt2" --policy .github/apk-policy.json --package com.fernwallet.app
```

The same job runs the Compose stability analysis on the release compile.

## Integration tests and release smoke test (emulator)

Both boot an Android 14 emulator. Integration tests run the `androidTest` suites of `:shared:data:core` and
`:shared:domain` (Room migrations, backup, DB). The smoke test installs the minified release APK, launches it and
fails if it crashes, ANRs or isn't running after 15 seconds. That usually means an R8 rule is missing or the Hilt
graph is broken. Download `smoke-test-logs` (contains `crash.txt` and `logcat.txt`) from the failed run.

## Security checks

- **Secret scan (gitleaks)**: a commit in your PR contains something that looks like a credential. Values are
  redacted in the log. If it is real: **revoke it first**, then remove it from the branch history (rebase) before
  merging. If it is a false positive (test data), change the test data; ask a maintainer before adding a
  fingerprint to `.gitleaksignore`.
- **Dependency review**: your PR adds or upgrades to a dependency version with a High/Critical advisory. Use a
  patched version (shown in the job summary).
- **Dependency vulnerabilities (OSV)**: on PRs this only reports. On `main` and at release time it blocks on
  High/Critical/unscored advisories in the dependencies that ship in the app. Upgrade the dependency, or record a
  justified, expiring exception in `osv-scanner.toml`.
- **CodeQL**: open the alert under *Security → Code scanning* and fix the flagged code. If the job itself fails with
  `KotlinVersionTooRecentError`, see [CI_CD.md → Notes](CI_CD.md#notes-and-known-limitations).
- **zizmor / actionlint**: a workflow change uses a risky pattern (e.g. `${{ github.event.* }}` inside `run:`, missing
  `persist-credentials: false`, broad permissions) or has a syntax error. The annotation names the audit; its
  documentation is at <https://docs.zizmor.sh/audits/>.
