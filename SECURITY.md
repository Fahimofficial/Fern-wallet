# Security policy

## Supported versions

Only the latest release on the [Releases page](https://github.com/Fahimofficial/Fern-wallet/releases) receives fixes.

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Use GitHub's private vulnerability reporting:
[Report a vulnerability](https://github.com/Fahimofficial/Fern-wallet/security/advisories/new).

Include the affected version, device/Android version, reproduction steps and impact. Never include real
financial data, backups or credentials.

## Verifying releases

Every release has a `SHA256SUMS.txt`, an SBOM, and a build-provenance attestation:

```sh
sha256sum -c SHA256SUMS.txt --ignore-missing
gh attestation verify FernWallet-<version>.apk --repo Fahimofficial/Fern-wallet
```

## Scope notes

- `debug.jks` in the repository is a **public** debug key used only for `debug`/`demo` builds. Builds signed with
  it must never be distributed. Official releases are signed with a private key that only the release workflow
  can use, and the workflow rejects the debug key.
- Backup ZIP and CSV export files are not encrypted by design; protecting exported files is the user's responsibility.

Security practices, severity policy, known issues and the CI security checks: [docs/SECURITY.md](docs/SECURITY.md).
