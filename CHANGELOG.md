# Changelog

Release notes for published builds are generated on the
[Releases page](https://github.com/Fahimofficial/Fern-wallet/releases). This file summarizes notable changes.

## Unreleased

First Fern Wallet changes on top of Ivy Wallet `2025.07.17 (206)`.

### Backups
- **Automatic backup** (Settings): daily or weekly backups to a folder you choose (phone storage, SD
  card, or a synced cloud folder), keeping the newest 10, with the last result shown in Settings.
- Backups can be **protected with a password** (AES-256-GCM). Importing a protected backup asks for
  the password. Unprotected backups work as before.
- Importing a backup is now all-or-nothing: the file is checked first, then every row is saved in
  one database transaction, so a failure part-way can no longer leave a half-imported wallet.
- Before an import, a copy of the current wallet is saved; **Undo import** on the result screen
  puts everything back exactly as it was.
- Clear messages when a file isn't a backup, is damaged, is too large, or comes from a newer app
  version, instead of "0 imported".
- Backups are read in memory with size limits and never extracted to disk (no path traversal or
  zip bombs). Backup files now carry a format version; older backups still import.
- Exporting a backup no longer wipes the app's whole cache folder, and is a consistent snapshot.
- A backup without settings no longer deletes the current settings (e.g. base currency).
- Accounts, categories and tags no longer show stale cached data right after an import.

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
- When Android closes the app in the background, reopening it returns to the screen you were on
  (and the screens behind it) instead of Home.
- Switching the phone between light and dark mode while the app is open no longer jumps back to
  Home, adds duplicate screens to the back stack, or locks the app again.
- An "add transaction" shortcut used while the app is already open now opens the add screen.
- Saving a transaction with a zero amount now says "Enter an amount first." instead of doing
  nothing, and opening "add transaction" without any account explains why it closes.
- A missing exchange rate no longer converts 1:1 (e.g. US dollars counted as rupees). Amounts in a
  currency without a rate are left out of totals, and Home shows an **Exchange rate missing** card
  that opens the exchange-rates screen. A transfer between such currencies shows a placeholder
  rate of 1 in its editable rate field.
- Recurring planned payments no longer stop 3 years after their start date: upcoming payments are
  kept generated up to 3 years ahead of today (at most 72 per rule), topped up on every app start.
- Monthly payments on the 29th–31st no longer drift to the 28th after February.
- Amount shortening: `999,999` showed `1000.00k` (now `1m`), small amounts showed `500.0`/scientific notation,
  the decimal separator ignored the locale.
- Monthly period with a start day of 29–31 picked the wrong month in shorter months (e.g. start day 31 on Feb 28),
  and "today" used UTC instead of local time around midnight. The last second of custom periods is now included.
- Accounts: the right-hand total was labelled "Total Balance (excluded)" but shows the total **including**
  excluded accounts; relabelled "Total incl. excluded".
- Importing a backup no longer silently turns off an enabled app lock.
- Opening links no longer crashes when no browser is installed.

### Privacy & security
- App lock can no longer be silently skipped: it can only be turned on when the phone has a screen
  lock, and if the screen lock is removed later the app stays locked and explains how to unlock.
- Network requests time out (10 s to connect, 30 s in total) instead of hanging on a bad
  connection; HTTP logging is limited to headers in debug builds.
- Removed the unused Google Play libraries (in-app review, app update, Play Services tasks, the
  coroutines Play Services adapter) and the unused security-crypto library. The app no longer
  contains proprietary Google code, a step towards F-Droid.
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
