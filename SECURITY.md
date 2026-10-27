# Security policy

## Supported versions

Only the latest release on the [Releases page](https://github.com/Fahimofficial/Fern-wallet/releases) receives fixes.

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Use GitHub's private vulnerability reporting:
[Report a vulnerability](https://github.com/Fahimofficial/Fern-wallet/security/advisories/new)
(the maintainer must enable it under *Settings → Code security*). If that is not available, open an issue
asking for a private contact channel **without** including any details.

Include the affected version, device/Android version, reproduction steps and impact. Never include real
financial data, backups or credentials.

## Scope notes

- `debug.jks` in the repository is a **public** debug key used only for `debug`/`demo` builds. Builds signed with
  it must never be distributed. Official releases are signed with a private key held in GitHub Actions secrets.
- Backup ZIP and CSV export files are not encrypted by design; protecting exported files is the user's responsibility.
