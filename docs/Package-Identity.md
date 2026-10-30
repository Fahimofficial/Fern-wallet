# Package identity

| | Before (Ivy Wallet) | Now (Fern Wallet) |
|---|---|---|
| `applicationId` (release) | `com.ivy.wallet` | `com.fernwallet.app` |
| debug / demo | `com.ivy.wallet.debug` (both) | `com.fernwallet.app.debug` / `com.fernwallet.app.demo` |
| app module `namespace` (R / BuildConfig) | `com.ivy.wallet` | `com.fernwallet.app` |
| Shortcut intent action | `ivy.wallet.intent.action.add_transaction` | `com.fernwallet.app.intent.action.ADD_TRANSACTION` |
| Widget click actions | `com.ivy.wallet.ACTION_ADD_*` | `com.fernwallet.app.ACTION_ADD_*` |

The ID follows the developer's existing convention (`com.quantummpv.app`).

## What intentionally did not change

- **Kotlin packages (`com.ivy.*`) and library-module namespaces.** Renaming ~40 modules would produce a huge
  diff, break `git blame`, and make applying upstream Ivy Wallet fixes much harder, with no user-visible
  benefit. Manifest entries use fully-qualified class names, so they are independent of the namespace.
- **On-device storage names** (`ivywallet.db`, `ivy_wallet_datastore_v1`, `ivy_wallet`, `ivy_wallet_prefs`).
  These are private to each app install; renaming them would require migrations and would risk losing data.
  No database migration or destructive fallback was added.

## Consequences of the new application ID

- Android treats Fern Wallet as a **different app**: it installs next to Ivy Wallet and does **not** receive
  Ivy Wallet's data or updates. Each app's data is sandboxed, so it cannot be migrated automatically.
  Move data with **Ivy Wallet → Settings → Backup data**, then **Fern Wallet → Settings → Import data**.
- No release of this fork existed under the old ID (the repository had no GitHub releases), so no existing
  Fern Wallet users are affected.
- Home-screen widgets, pinned shortcuts and the Quick Settings tile from Ivy Wallet must be re-added for Fern Wallet.

## External configuration (cannot be done from this repository)

- **Firebase:** the upstream `google-services.json` belonged to Ivy Apps' Firebase project (`ivy-widget`) and only
  registered `com.ivy.wallet`. Firebase (Crashlytics and the Firestore poll) was removed instead of reusing it.
  To add crash reporting again, create your own Firebase project for `com.fernwallet.app`, and update `PRIVACY.md`.
- **Google Play:** publishing on Play would need a new Play Console listing for `com.fernwallet.app`
  (`fastlane/Appfile` already uses it). The old Play listing and its signing key belong to Ivy Apps.
- **Release signing key:** create your own and add it as GitHub secrets (see `docs/RELEASE.md`).
