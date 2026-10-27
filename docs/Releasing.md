# Releasing Fern Wallet

## Versioning

Defined in `gradle/libs.versions.toml`:

- `version-name` – release date, `YYYY.MM.DD` (e.g. `2026.10.27`).
- `version-code` – integer, **must increase by at least 1 for every published build** (Android refuses
  to install an update with a lower or equal code).

Tags: `v<version-name>` for stable releases (`v2026.10.27`), `v<version-name>-<suffix>` for prereleases
(`v2026.10.27-rc.1`). The tag must match `version-name`, otherwise the release workflow fails.

## One-time setup (repository settings)

Add these **Actions secrets** (*Settings → Secrets and variables → Actions*):

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE` | `base64 -w0 release.jks` |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | key alias |
| `SIGNING_KEY_PASSWORD` | key password |

Create a keystore once with `keytool -genkeypair -v -keystore release.jks -alias fern -keyalg RSA -keysize 4096 -validity 10000`.
**Back it up somewhere safe**: losing it means users can't update to future releases without uninstalling.
Never commit it (`sign.jks`, `*.keystore` are git-ignored).

Optionally, add required reviewers to the `release` environment (*Settings → Environments*) so every release
build needs approval.

## Cutting a release

1. Run **Actions → Prepare release → Run workflow** (optionally enter a version name). It opens a PR bumping
   `version-name`/`version-code`.
2. Review and merge the PR once CI is green.
3. Tag the merge commit and push the tag:
   ```sh
   git checkout main && git pull
   git tag v2026.10.27 && git push origin v2026.10.27
   ```
4. The **Release** workflow then:
   - checks the tag matches `version-name`, and that all signing secrets exist (otherwise it stops; it never
     publishes unsigned or debug-signed builds);
   - runs unit tests, builds the minified release APK and AAB, signs them, and deletes the keystore from the runner;
   - verifies the signature (rejects debug certificates), application ID, version and app label;
   - creates a **draft** GitHub release with generated notes and `FernWallet-<version>-<code>.apk/.aab` +
     `SHA256SUMS.txt`. The R8 `mapping.txt` is kept as a workflow artifact (30 days) for deobfuscating stack traces.
5. Edit the draft notes and click **Publish**.

A single universal APK is produced: the only native code is a ~5 KB DataStore library per ABI, so ABI
splits would not meaningfully reduce download size.

## Recovering from failures

- **Tag/version mismatch** – delete the tag (`git push --delete origin vX; git tag -d vX`), fix
  `libs.versions.toml` via a PR, tag again.
- **Missing secrets** – add them, then re-run the failed workflow run.
- **Release already exists** – delete the draft release in the GitHub UI, then re-run.
- **Bad build already published** – never reuse a version code. Bump to a new version and release again.
