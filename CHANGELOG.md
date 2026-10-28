# Changelog

Release notes for published builds are generated on the
[Releases page](https://github.com/Fahimofficial/Fern-wallet/releases). This file summarizes notable changes.

## Unreleased

First Fern Wallet changes on top of Ivy Wallet `2025.07.17 (206)`.

### Identity
- Renamed to **Fern Wallet** with the wallet-and-leaf logo, a GitHub wordmark, and four selectable
  app icons (Settings → App icon: Fern, Mono, Graphite, Midnight) with Material You themed-icon support.
- New application ID `com.fernwallet.app`; installs alongside Ivy Wallet (see `docs/Package-Identity.md`).
- Project links (GitHub, issues, releases, contributors, privacy, license) point to this repository.
  Ivy Wallet is credited under **Settings → Attributions**.

### Credit cards
- New **Cards** section in Accounts. A card is an account of type credit card: purchases are expenses on it,
  refunds are income, and bill payments are transfers (not expenses). The card's balance is −outstanding.
- Card details screen with outstanding, available credit and limit used, the current and past statements
  (calculated from your transactions; you can enter the bank's own figures), a pay-bill flow, and the option
  to close a card without losing its history.
- Due-date reminders (a few days before and on the due date), which are cleared once the bill is paid.
- Bank and card picker built from official RBI and issuer sources (`docs/Card-Catalogue.md`). Cards use a
  generic Fern illustration; no bank or network logos are bundled.
- Database 130 → 131 is an additive migration; backups include card data, and older backups still import.

### Fixes
- Amount shortening: `999,999` showed `1000.00k` (now `1m`), small amounts showed `500.0`/scientific notation,
  the decimal separator ignored the locale.
- Monthly period with a start day of 29–31 picked the wrong month in shorter months (e.g. start day 31 on Feb 28),
  and "today" used UTC instead of local time around midnight. The last second of custom periods is now included.
- Accounts: the right-hand total was labelled "Total Balance (excluded)" but shows the total **including**
  excluded accounts; relabelled "Total incl. excluded".
- Importing a backup no longer silently turns off an enabled app lock.
- Opening links no longer crashes when no browser is installed.

### Privacy & security
- Removed Firebase Crashlytics and the Firestore poll (they sent data to upstream's Firebase project).
- Removed unused permissions: location, storage, `SET_ALARM`, `DISABLE_KEYGUARD`. Cleartext HTTP disabled.
- Release builds are signed only from CI secrets; the release workflow refuses debug-signed builds.

### Design
- Calmer, higher-contrast summary cards (no gradients or colored glow), compact transaction cards,
  dedicated transfer color, accessible income-green for text, one primary color for the FAB on every tab.
- Less spring overshoot, and animations become near-instant when "Remove animations" is enabled.
- Settings: new **About Fern Wallet** and **Support the developer** sections; removed Ivy's Telegram,
  Google Play rating and community links.

### CI / releases
- New `ci.yml` (wrapper validation, build, unit + screenshot tests, detekt, lint) with read-only permissions
  and actions pinned to commit SHAs. New tag-driven `release.yml` producing signed, verified draft releases
  with checksums; manual `prepare_release.yml` replaces the weekly auto-release and Google Play/Telegram uploads.
