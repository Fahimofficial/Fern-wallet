# Releasing Fern Wallet

Releases are built, signed and verified only by [`.github/workflows/release.yml`](../.github/workflows/release.yml),
from a version tag on a commit that is already on `main` and passed CI. A human publishes the resulting draft.
Pipeline overview: [CI_CD.md](CI_CD.md). Security model: [SECURITY.md](SECURITY.md).

## Versioning

Defined in `gradle/libs.versions.toml`:

- `version-name`: release date, `YYYY.MM.DD` (e.g. `2026.10.27`).
- `version-code`: integer that **must increase by at least 1 for every published build**, prereleases included.
  Android refuses to install an update with a lower code, and the release workflow refuses a tag whose
  `version-code` is not greater than the previous `v*` tag's.

Tags:

| Tag | Meaning |
|---|---|
| `v<version-name>` (`v2026.10.27`) | stable release |
| `v<version-name>-<suffix>` (`v2026.10.27-rc.1`) | prerelease; suffix is letters, digits and dots only |

The tag must match `version-name`, otherwise the workflow stops before building anything.

## What a release contains

| File | What it is |
|---|---|
| `FernWallet-<tag-without-v>-<code>.apk` | universal APK (arm64-v8a, armeabi-v7a, x86, x86_64), signed with APK Signature Scheme v2 |
| `FernWallet-<…>.aab` | Android App Bundle, signed with the same key (for Play or other stores) |
| `FernWallet-<…>-sbom.cdx.json` | CycloneDX SBOM of every dependency inside the app (release runtime classpath) |
| `SHA256SUMS.txt` | SHA-256 of the APK, AAB and SBOM |
| provenance attestation | Sigstore-signed SLSA provenance for the APK, AAB and SBOM, stored by GitHub |

The R8 `mapping.txt` (needed to deobfuscate crash stack traces) is kept as the `release-mapping` workflow
artifact for 90 days and is not published. Download it from the release run if you need it later.

A single universal APK is produced: the only native code is two tiny AndroidX libraries per ABI, so ABI splits
would not meaningfully reduce download size and would complicate installs. There are no product flavors.

## One-time setup (repository owner)

These are GitHub settings; nothing in the repository can configure them.

1. **Create the signing key** once, on a trusted machine, and back it up offline in two places. Losing it means
   users can't update without uninstalling (and losing local data).
   ```sh
   keytool -genkeypair -v -keystore release.jks -alias fern -keyalg RSA -keysize 4096 -validity 10000
   ```
   Never commit it. `*.jks` (except the public `debug.jks`), `*.keystore`, `*.p12` and `*.pem` are git-ignored.
2. **Create the `release` environment**: *Settings → Environments → New environment → `release`*.
   - *Deployment branches and tags*: **Selected branches and tags** → add a **tag** rule `v*`.
     Then no other ref (and no pull request) can ever receive these secrets.
   - *Required reviewers*: add yourself, so every signing run waits for your approval.
   - *Environment secrets* (*Add environment secret*):

     | Secret | Value |
     |---|---|
     | `SIGNING_KEYSTORE` | `base64 -w0 release.jks` (macOS: `base64 -i release.jks`) |
     | `SIGNING_STORE_PASSWORD` | keystore password |
     | `SIGNING_KEY_ALIAS` | key alias (`fern` above) |
     | `SIGNING_KEY_PASSWORD` | key password |

   - If these secrets exist as **repository** secrets today, delete the repository copies after adding the
     environment ones (*Settings → Secrets and variables → Actions*). Repository secrets are available to every
     workflow on every branch; environment secrets only to jobs that pass the environment's rules.
3. **Pin the certificate** (recommended): add a repository *variable* (not a secret) `RELEASE_CERT_SHA256` with the
   certificate's SHA-256, lowercase hex without colons:
   ```sh
   keytool -list -v -keystore release.jks -alias fern | sed -n 's/.*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f'
   ```
   The workflow then rejects an APK signed with any other key.
4. **Protect release tags**: add a tag ruleset for `v*` (see [CI_CD.md → Repository settings](CI_CD.md#repository-settings-manual)),
   so only you can create, move or delete release tags.

## Cutting a release

1. Run **Actions → Prepare release → Run workflow**. It sets `version-name` to today's UTC date, increments
   `version-code` and opens a PR from `release/v<date>`.
2. GitHub doesn't start CI for PRs opened by GitHub Actions. Run **Actions → PR validation → Run workflow** on the
   `release/v<date>` branch (or close and reopen the PR), wait for it to pass, and merge.
3. Wait for **CI** and **Security** to pass on the merge commit on `main`, then tag it:
   ```sh
   git checkout main && git pull
   git tag v2026.10.27 && git push origin v2026.10.27
   ```
4. The **Release** workflow:
   1. *validate*: checks the tag format and that it matches `version-name`, that the commit is on `main`, that
      `version-code` increased, and waits (up to 45 min) for **CI** and **Security** to have succeeded on that commit.
   2. *build* (environment `release`, waits for your approval if configured): validates the Gradle wrapper,
      builds the minified release APK and AAB without any cache, deletes the keystore, then verifies:
      - APK signature scheme v2/v3, not the public debug key or a CI key, and `RELEASE_CERT_SHA256` if set;
      - the AAB is signed with the same certificate;
      - application ID `com.fernwallet.app`, label "Fern Wallet", version name/code, not debuggable, no cleartext,
        backup off, permissions and exported components match [`.github/apk-policy.json`](../.github/apk-policy.json);
      - no High/Critical known vulnerability in the bundled dependencies (OSV scan of the SBOM).
   3. *smoke-test*: installs the signed APK on a fresh Android 14 emulator, launches it and fails on a crash or ANR.
   4. *publish*: verifies checksums, creates the provenance attestation, and creates a **draft** release with
      the files above and generated notes.
5. Review the draft notes on the Releases page and click **Publish**.

## Verifying a published release

```sh
sha256sum -c SHA256SUMS.txt --ignore-missing
gh attestation verify FernWallet-<…>.apk --repo Fahimofficial/Fern-wallet
apksigner verify --print-certs FernWallet-<…>.apk   # compare with the published certificate SHA-256
```

`gh attestation verify` proves the file was built by this repository's `release.yml` from the tagged commit.

## Recovering from failures

| Failure | What to do |
|---|---|
| *Invalid tag* / *version mismatch* | Delete the tag (`git push --delete origin vX && git tag -d vX`), fix `libs.versions.toml` in a PR, tag again. |
| *Not on main* | Only tag commits that are on `main`. |
| *version-code must increase* | Bump `version-code` in a PR, then tag the new commit. Never reuse a published code. |
| *Release blocked: CI/Security …* | Fix or re-run the failing workflow on `main`, then re-run the failed release jobs. |
| *Signing not configured* | Add the environment secrets above, then re-run the failed jobs. |
| *Certificate does not match* | The secrets contain a different key than `RELEASE_CERT_SHA256`. Fix the secret; don't change the variable unless you are deliberately rotating keys. |
| *Vulnerable dependency* | Upgrade it (Dependabot may already have a PR). If it doesn't affect Fern Wallet, add a justified, expiring entry to `osv-scanner.toml` in a reviewed PR. |
| *Smoke test failed* | Download `release-smoke-test-logs` from the run; fix the crash on `main` with a new version code. |
| *Release already exists* | Delete the draft in the GitHub UI, then re-run. |
| *Bad build already published* | Never reuse a version code. Bump to a new version and release again. |
