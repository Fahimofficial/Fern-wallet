# Security practices

How Fern Wallet handles vulnerabilities, secrets, dependencies and releases, and what is known to be open.
Reporting policy (short form): [`/SECURITY.md`](../SECURITY.md). Pipeline: [CI_CD.md](CI_CD.md).
Privacy disclosures: [`/PRIVACY.md`](../PRIVACY.md).

## Reporting a vulnerability

Use GitHub's private vulnerability reporting (enabled):
[Report a vulnerability](https://github.com/Fahimofficial/Fern-wallet/security/advisories/new). Don't open a public
issue. Include the version, device/Android version, steps and impact, and never real financial data, backups or
credentials. Fixes go into the next release; only the latest release is supported.

## What runs automatically

| Area | Tool | Where | Blocks |
|---|---|---|---|
| Secrets in the whole git history | gitleaks (redacted) + GitHub secret scanning & push protection | `security.yml`, GitHub | merge |
| Kotlin/Java SAST | CodeQL `security-extended` | `codeql.yml` | merge (via code scanning ruleset: High+) |
| Android security lint | lint checks promoted to errors in `app/build.gradle.kts` (WebView JS/interfaces, trust-all TLS, hostname verifiers, unsafe intents and PendingIntents, world-readable files, insecure random, crypto `getInstance`, exported providers, hard-coded debuggable …) | `android-checks.yml` | merge |
| Shipped manifest | `scripts/ci/check_apk_policy.py` + `.github/apk-policy.json`: permissions and exported components allowlist, not debuggable, no cleartext, backup off | `android-checks.yml`, `release.yml` | merge, release |
| New vulnerable dependencies | GitHub dependency review | `dependency-review.yml` | merge (High+) |
| Known vulnerabilities in shipped dependencies | osv-scanner on the CycloneDX SBOM of `:app` `releaseRuntimeClasspath` | `security.yml`, `release.yml` | release; `main` goes red |
| Workflow security | zizmor, actionlint, CodeQL `actions` | `workflow-audit.yml`, `codeql.yml` | merge |
| Gradle wrapper integrity | `wrapper-validation`, `setup-gradle` | every Gradle job | merge, release |
| Release artifact integrity | signature/cert checks, SHA-256 sums, Sigstore provenance | `release.yml` | release |

## Severity policy

| Severity | Examples | Pull request | Release |
|---|---|---|---|
| **Critical** (CVSS ≥ 9) | leaked live credential; RCE in a shipped dependency | block | block |
| **High** (CVSS ≥ 7) | path traversal; auth bypass; High CVE in a shipped dependency; new exported component or permission not in the policy | block | block |
| **Medium** (CVSS ≥ 4) | Medium CVE; logic flaw needing user interaction | report; fix in the next release | report |
| **Low** | hardening gaps, Low CVEs | report | report |
| **Informational** | style, defence-in-depth suggestions | report | – |
| *Unknown* (advisory without a score) | | report on PRs | block until triaged |

Exceptions are explicit, scoped and reviewed in a PR, never broad patterns:

- **Dependencies:** `osv-scanner.toml` `[[IgnoredVulns]]` with `id`, `reason` and `ignoreUntil` (an expiry date).
- **Secrets:** `.gitleaksignore`, exact fingerprints (`commit:file:rule:line`) only, with the reason above each line.
- **Lint:** fix security findings; `app/lint-baseline.xml` is only for pre-existing, non-security legacy issues.
- **Workflows:** inline `# zizmor: ignore[<audit>]` with a justification on that line; `.github/zizmor.yml` only for
  tool incompatibilities.

## Secret management

- **Release signing** uses four secrets (`SIGNING_KEYSTORE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`,
  `SIGNING_KEY_PASSWORD`). They belong in the `release` environment, restricted to `v*` tags, with a required
  reviewer ([RELEASE.md](RELEASE.md#one-time-setup-repository-owner)). Only `release.yml`'s `build` job reads them;
  the keystore is decoded with `umask 077` into `RUNNER_TEMP` and deleted in an `always()` step; the build runs
  with `--no-configuration-cache --no-daemon` so values aren't written to disk caches; no cache is used.
- **No other secrets** are used by any workflow. PR workflows, including from forks, get a read-only token.
- **Never commit** keystores, `keystore.properties`, `google-services.json` or Play credentials; `.gitignore` covers
  them. `debug.jks` is a deliberately **public** debug key (password in `app/build.gradle.kts`) for `debug`/`demo`
  builds; anything signed with it must never be distributed, and `release.yml` rejects it.
- If a secret is ever committed: **revoke or rotate it first**, then remove it. Deleting it from the tree, or even
  rewriting history, doesn't invalidate a credential that was public.

### Secrets in git history

gitleaks found 7 secrets in old commits inherited from upstream Ivy Wallet (2021–2022). They are not in the current
tree and are listed by fingerprint in `.gitleaksignore`:

| Commit | File | What | Status / owner action |
|---|---|---|---|
| `ad0622d28`, `d16abaf03` | `app/google-services.json`, `google-services.json` | Firebase Android API key of upstream project `com.ivy.wallet` (2 keys, 2 commits) | Not Fern Wallet's credential and unused since Firebase was removed (`a6d35bc3d`). Firebase Android keys are client identifiers, but anyone can use them against that Firebase project. Notify the upstream owner (Ivy Apps) if not already done. |
| `ceb0e7650`, `102665fb1` | `app/src/main/java/com/ivy/wallet/network/service/GithubService.kt` | GitHub token of an upstream service account, committed whole and in two halves | GitHub automatically revokes classic PATs found in public repositories. Treat it as compromised regardless; the upstream owner must confirm it is revoked. Nothing in Fern Wallet uses it. |

Rewriting public history would break every fork and clone and wouldn't revoke anything, so it is not planned.

## Dependency updates

- Dependabot (`.github/dependabot.yml`): security updates as soon as an advisory exists; version updates weekly
  with a **7-day cooldown** (security updates skip it). Kotlin/KSP and AGP-tied libraries are grouped because they
  must move together; other minor/patch updates are grouped; majors get individual PRs.
- Every update goes through the full PR validation, including the emulator tests and the release smoke test.
- **JitPack** builds artifacts from mutable git tags, so `settings.gradle.kts` allows it to serve only
  `com.github.Ivy-Apps` (the detekt rule set) and that group only from JitPack (`exclusiveContent`).
- **Licenses**: Fern Wallet is GPL-3.0. Dependency review reports licenses of new dependencies but can't enforce an
  allowlist (Android/Play SDK licenses aren't SPDX). Check new dependencies by hand. Known item: the Google Play
  review/app-update libraries are proprietary (Play Core terms); decide whether that is acceptable for your
  distribution channel (e.g. F-Droid would not accept it).

## Android application security review

Findings from the October 2026 audit (manifest, components, storage, network, crypto, logging). Fixed items link
to the change; open items are tracked here until fixed.

| ID | Severity | Finding | Status |
|---|---|---|---|
| F1 | **High** | **Zip Slip in backup import**: `ZipUtils.unzip` wrote entries to `destination + entryName` without checking for `../`. A crafted "backup" could overwrite the database or `shared_prefs` (e.g. disable the app lock) on Android 9–13; Android 14+ blocks `..` entries itself. | **Fixed**: canonical-path check (`resolveZipEntry`), regression tests in `shared/base/.../ZipUtilsTest.kt`. |
| F2 | Medium | App lock unlocks without a prompt when the device has no secure lock screen (`AppLockedScreen.kt`, `hasLockScreen()` → `onContinueWithoutAuthentication`). | Open. Refuse to enable the lock without a device credential; never auto-unlock. |
| F3 | Medium | Re-lock timeout counts 1-second coroutine ticks (`RootViewModel.startUserInactiveTimeCounter`); time while the process is frozen isn't counted, so the app can resume unlocked long after 60 s. | Open. Compare `SystemClock.elapsedRealtime()` between `onStop` and `onStart`. |
| F4 | Low | The lock is UI-only (`BIOMETRIC_WEAK`, no `CryptoObject`); Room DB and preferences are not encrypted. | Open / design decision. |
| F5 | Low | `FLAG_SECURE` only on focus loss and only with the lock on; screenshots/recording allowed while in use. | Open. |
| F6 | Low | Exported widget receivers act on custom actions from any app (relaunch with `CLEAR_TASK`). | Open. |
| F7 | Low | `RootViewModel` calls `getSerializableExtra` on extras from the exported activity; only `IllegalArgumentException` is caught, so another app can crash it. | Open. Use a string extra with an enum whitelist. |
| F8 | Low | Credit-card reminders show card name and amount due; no redacted lock-screen version; posted even when the app is locked. | Open. |
| F9 | Low | `androidx.compose.ui.tooling.PreviewActivity` ships exported in release because `ui-tooling` is an `implementation` dependency. It refuses to run in non-debuggable apps. | Open (allowlisted in `apk-policy.json` with this note). Move `ui-tooling` to `debugImplementation` and add `ui-tooling-preview` as `implementation`. |
| F10 | Low | `allowBackup=false`, but no `dataExtractionRules`, so Android 12+ device-to-device transfer can still copy app data. | Open. Decide the policy, then add `dataExtractionRules`. |
| F11 | Low | Release logcat isn't stripped (`Log.w`, many `printStackTrace()`); no user data seen in them. | Open. |
| F12 | Low | Exchange rates come from an unpinned `@latest` CDN path, with no timeouts and no sanity checks on values. | Open. |
| F13 | Low | Backup/CSV import reads whole files, decodes JSON twice, has no size limits and disables CSV validators. | Open. |
| F14 | Info | `debug.jks` and its password are public; `demo` builds are signed with it. | Accepted (documented). |
| F15 | Info | Default `taskAffinity` on minSdk 28 (task-hijacking class issues on Android 9/10). | Open. |
| F16 | Info | `androidx.security:security-crypto` and `com.google.android.play:app-update` are declared but unused. | Open. Remove. |

Checked and fine: no WebView; no custom TLS/TrustManager/HostnameVerifier; cleartext disabled; every
`PendingIntent` is `FLAG_IMMUTABLE` with an explicit intent; no FileProvider; no deep links; release builds are
minified and not debuggable; Timber logs only in debug builds; no analytics/crash SDKs; no hard-coded production
secrets in the current tree.

## Supply chain

- Actions pinned to full SHAs; zizmor online audits detect impostor commits and known-vulnerable action versions.
- Tools downloaded in CI are pinned and SHA-256 verified.
- Gradle wrapper jar validated in every Gradle job; `distributionSha256Sum` pins the distribution.
- SBOM (CycloneDX 1.6) for every release, scanned before signing and published with the release.
- Sigstore build provenance for the APK, AAB and SBOM (`gh attestation verify`).
- Not done, by decision: **Gradle dependency verification** (`verification-metadata.xml`) and **dependency
  locking**. Both are valuable but Dependabot doesn't update `verification-metadata.xml` or Gradle lockfiles, so every
  Dependabot PR would fail. Revisit if Dependabot gains support, or regenerate them in a trusted workflow.

## Manual and legal reviews still needed

Automated checks don't establish legal compliance. The owner should review:

1. PRIVACY.md against the behaviour described there, after each feature change (the APK policy forces a review
   when permissions change, nothing forces it for new network calls).
2. Whether GDPR, India's DPDP Act 2023 or other laws apply to your distribution, and whether you need a contact
   address that isn't a public issue tracker.
3. GPL-3.0 compatibility of bundled proprietary Google Play libraries for your distribution channels.
4. Data-safety forms if you publish to Google Play (network calls, on-device storage, no data collection).
5. Repository settings in [CI_CD.md → Repository settings](CI_CD.md#repository-settings-manual).
