# CI/CD

How Fern Wallet is built, tested, scanned and released on GitHub Actions, which checks must pass, and
what the repository owner has to configure by hand. Security policy: [SECURITY.md](SECURITY.md).
Release procedure: [RELEASE.md](RELEASE.md). Fixing a red check: [CI-Troubleshooting.md](CI-Troubleshooting.md).

## Architecture

```
pull request ──► PR validation ─┐  (android-checks.yml, reusable)
                 Security        │   build · unit/screenshot tests · detekt + lint
                 CodeQL          │   release build + APK policy · emulator integration tests
                 Dependency review   release APK smoke test · Compose stability
                 Workflow audit  │
push to main ──► CI ─────────────┘  same android-checks.yml; only main writes caches
                 Security, CodeQL, Workflow audit
weekly ────────► Security (new advisories), CodeQL, Workflow audit (online audits)
tag v* ────────► Release: validate → build & sign (environment "release") → smoke test → attest & draft
```

Principles:

- **One definition of "green".** `pr-validation.yml` and `ci.yml` both call `android-checks.yml`, so a PR is
  tested exactly like the code on `main` that releases are built from.
- **No secrets outside the release environment.** Every PR workflow uses a read-only token, also for forks.
  Only the `build` job of `release.yml` can read signing secrets, and only for `v*` tags (environment rule).
  No workflow uses `pull_request_target` or `workflow_run`.
- **Least privilege.** Every workflow sets `permissions: {}` and each job asks only for what it needs.
- **Pinned, verified tooling.** Actions are pinned to full commit SHAs (checked by zizmor's online
  `impostor-commit`/`ref-confusion` audits). Downloaded tools (gitleaks, osv-scanner, actionlint, zizmor) are
  pinned versions verified against a SHA-256 in the workflow. `setup-gradle` validates `gradle-wrapper.jar`
  checksums in every job before `./gradlew` runs.
- **Caches can't poison releases.** Gradle and AVD caches are written only by `main` runs; PR runs read them.
  Release builds use no cache at all and a fresh emulator.

## Workflows

| Workflow | Trigger | Purpose | Required |
|---|---|---|---|
| `pr-validation.yml` | PR, manual | `android-checks.yml` + PR description check | merge |
| `ci.yml` | push to `main`, manual | `android-checks.yml` on the integrated result | release |
| `android-checks.yml` | called by the two above | build, tests, lint, release build, emulator tests | (via callers) |
| `security.yml` | PR, push `main`, weekly, manual | gitleaks (full history); OSV scan of the release SBOM | merge (gitleaks), release (both) |
| `codeql.yml` | PR, push `main`, weekly, manual | CodeQL SAST for Kotlin/Java and for the workflows | merge (analysis must run); alerts via ruleset |
| `dependency-review.yml` | PR | blocks new High/Critical vulnerable dependencies; license + Scorecard info | merge |
| `workflow-audit.yml` | PR, push `main`, weekly, manual | actionlint, zizmor, config files parse | merge |
| `release.yml` | tag `v*` | validated, signed, verified, attested draft release | - |
| `prepare_release.yml` | manual | version bump PR | - |
| `stale.yml` | weekly | close stale issues/PRs (`security`, `bug`, `P0`, `keep` exempt) | - |
| `upgrade-gradle-wrapper.yml` | weekly | wrapper upgrade PR (overlaps with Dependabot; see below) | - |
| Dependabot (`.github/dependabot.yml`) | weekly + advisories | Gradle, Actions, Bundler updates with 7-day cooldown | - |

External apps that also post checks (Codacy, CodeRabbit, CodeAnt) are configured outside this repository and are
advisory.

### `android-checks.yml` jobs

| Job (check name `Android checks / …`) | What it proves | Reproduce locally |
|---|---|---|
| Gradle wrapper validation | `gradle-wrapper.jar` is an official Gradle release | – |
| Build debug APK | the app compiles; resources and manifests merge; debug identity is right | `./gradlew :app:assembleDebug` |
| Unit & screenshot tests | all JVM tests, Paparazzi screenshots, CI-tooling tests pass | `./gradlew testDebugUnitTest verifyPaparazziDebug` |
| Detekt & Android lint | no Detekt findings; no lint Error/Fatal, including the security checks promoted to errors in `app/build.gradle.kts` | `./gradlew detekt lintRelease && python3 scripts/ci/lint_gate.py build/reports/lint/lint.xml` |
| Release build & APK policy | the R8-minified release build works and is signed through `signingConfigs.release` (with a throwaway key); the shipped manifest matches `.github/apk-policy.json`; Compose stability | see [CI-Troubleshooting.md](CI-Troubleshooting.md#release-build--apk-policy) |
| Integration tests (emulator) | Room migrations, backup and DB tests on Android 14 | `./gradlew :shared:data:core:connectedDebugAndroidTest :shared:domain:connectedDebugAndroidTest` |
| Release APK smoke test (emulator) | the minified release APK installs, launches and doesn't crash or ANR (catches R8 and DI regressions) | `scripts/ci/smoke_test_apk.sh app/build/outputs/apk/release/app-release.apk com.fernwallet.app` |

Failures put the reason in the job summary (failed tests, lint issues, policy violations, vulnerable packages)
and upload reports as artifacts (`test-reports`, `static-analysis-reports`, `android-test-reports`,
`smoke-test-logs`, 7 days).

## Merge and release gates

Make these **required status checks** on `main` (exact names as shown on a PR):

| Check | Why |
|---|---|
| `Android checks / Gradle wrapper validation` | supply chain |
| `Android checks / Build debug APK` | compiles |
| `Android checks / Unit & screenshot tests` | regressions |
| `Android checks / Detekt & Android lint` | code quality + Android security lint |
| `Android checks / Release build & APK policy` | release config, permissions, exported components |
| `Android checks / Integration tests (emulator)` | migrations / data safety |
| `Android checks / Release APK smoke test (emulator)` | startup |
| `PR description` | process (passes as "skipped" for Dependabot) |
| `Secret scan (gitleaks, full history)` | no new secrets |
| `Dependency review` | no new High/Critical vulnerable dependency |
| `actionlint`, `zizmor`, `Config files parse` | workflow security |
| `CodeQL (java-kotlin)`, `CodeQL (actions)` | SAST analysis completed |

Advisory (don't require): `Dependency vulnerabilities (OSV, release classpath)` on PRs (it reports existing
vulnerabilities but never fails a PR; it is blocking on `main`, weekly and at release), and the external apps.

Releases additionally require `CI` and `Security` to have **succeeded on the tagged commit** (`release.yml` waits
for them), plus the release-only signature, version, policy, vulnerability and smoke-test checks
([RELEASE.md](RELEASE.md)).

Severity policy: [SECURITY.md → Severity policy](SECURITY.md#severity-policy).

## Repository settings (manual)

Workflow files cannot change repository settings. The owner has to do these in the GitHub UI (or with an
admin token). Current state when this was written: no branch protection, no rulesets, no `release`
environment; secret scanning, push protection, Dependabot alerts and security updates, and private
vulnerability reporting are already enabled; CodeQL default setup is off (required, because `codeql.yml`
is an advanced setup).

1. **Ruleset for `main`** (*Settings → Rules → Rulesets → New branch ruleset*, target: default branch):
   - Restrict deletions; Block force pushes.
   - Require a pull request before merging. As the only maintainer you can't approve your own PRs, so set
     required approvals to **0** for now; raise it to 1 (with *Require review from Code Owners*) as soon as there is
     a second maintainer.
   - Require status checks to pass: add the checks in the table above. Enable *Require branches to be up to date*
     (or use a merge queue).
   - Require code scanning results: tool **CodeQL**, alert threshold **Errors**, security alert threshold
     **High or higher**. (zizmor and gitleaks fail their own checks instead of uploading results.)
   - Bypass list: leave empty, or add yourself only for emergencies.
2. **Ruleset for tags `v*`** (*New tag ruleset*): restrict creations, updates and deletions to yourself (bypass
   list), so nobody else can trigger a release.
3. **Environment `release`** with signing secrets, tag rule `v*` and required reviewer: [RELEASE.md → One-time setup](RELEASE.md#one-time-setup-repository-owner).
4. **Actions settings** (*Settings → Actions → General*):
   - *Actions permissions*: enable **Require actions to be pinned to a full-length commit SHA** (currently off; all
     workflows already comply). Optionally restrict to the actions used here: `actions/*`, `github/codeql-action/*`,
     `gradle/actions/*`, `reactivecircus/android-emulator-runner`, `peter-evans/create-pull-request`.
   - *Fork pull request workflows*: "Require approval for all external contributors".
   - *Workflow permissions*: keep "Read repository contents" (already set) and leave "Allow GitHub Actions to create
     and approve pull requests" **off** unless you want `prepare_release.yml` and `upgrade-gradle-wrapper.yml` to
     work (they open PRs with `GITHUB_TOKEN`; see below).
5. **Code security** (*Settings → Code security*): also enable *Secret scanning → Non-provider patterns* and
   *Validity checks* (both currently off).

## Notes and known limitations

- **PRs opened by workflows don't run CI.** `prepare_release.yml` and `upgrade-gradle-wrapper.yml` open PRs with
  `GITHUB_TOKEN`; GitHub doesn't trigger `pull_request` workflows for those. Run *PR validation* manually on the
  branch, or close and reopen the PR. Using a GitHub App token would fix this but adds a long-lived credential.
- **Two wrapper updaters.** Dependabot's Gradle ecosystem already updates the wrapper, so `upgrade-gradle-wrapper.yml`
  can open duplicate PRs. Keep one (recommended: Dependabot, which gets CI) and delete the other.
- **Dependency review and forks.** Gradle dependencies are visible to dependency review only through GitHub's
  automatic dependency submission, which doesn't run for fork PRs. For those, the OSV scan on `main` after merge
  and at release time is the backstop.
- **CodeQL lags Kotlin.** If `CodeQL (java-kotlin)` fails with `KotlinVersionTooRecentError` after a Kotlin bump,
  CodeQL doesn't support that Kotlin yet. Don't silence it: either wait for the next CodeQL release before
  merging the bump, or temporarily remove `java-kotlin` from the matrix in a reviewed PR and record that in
  docs/SECURITY.md.
- **zizmor `self-repository` is disabled** (`.github/zizmor.yml`) because actionlint doesn't understand GitHub's
  new `uses: $/…` syntax yet. Re-enable both together when actionlint supports it.
- **Emulator tests** use API 34 `google_apis` x86_64 with KVM. `SyncExchangeRatesUseCaseTest` calls the live
  exchange-rate API and can fail when that service is down.

## Troubleshooting

See [CI-Troubleshooting.md](CI-Troubleshooting.md). For workflow-file problems, run the same tools locally:

```sh
actionlint                      # https://github.com/rhysd/actionlint
zizmor .                        # https://docs.zizmor.sh (add GH_TOKEN for online audits)
gitleaks git --redact .         # uses .gitleaksignore automatically
```
