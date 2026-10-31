# Privacy

Fern Wallet is an offline-first money manager. This page describes what the app does with your data as
of the current source code in this repository.

## Data stored on your device

Accounts (including credit-card details: only the last four digits, never the full card number, CVV or expiry), transactions, categories, budgets, planned payments, loans, tags and settings are stored locally
in an app-private SQLite database and preferences. Android backup is disabled (`allowBackup="false"`), so
this data is not copied to Google Drive by the system. Backups are only created when you choose
**Settings → Backup data**, and saved wherever you pick.

> Backups are **not encrypted unless you set a password** (when backing up, or for automatic
> backups in **Settings → Automatic backup**). Password-protected backups use AES-256-GCM with a key
> derived from the password (PBKDF2-HMAC-SHA256); if you forget the password, they can't be restored.
> CSV exports are never encrypted. Store and share unprotected files carefully.

Automatic backups are written only to the folder you choose. If that folder is synced to a cloud
service, the service's own privacy terms apply; Fern Wallet itself uploads nothing. For automatic
backups with a password, the derived key is kept on the device, wrapped by a non-exportable
Android Keystore key.

## Network requests

The app makes these HTTPS requests (cleartext HTTP is disabled):

| Purpose | Destination | Data sent |
|---|---|---|
| Exchange rates | `currency-api.pages.dev`, `cdn.jsdelivr.net` | none beyond a normal HTTP request |
| Releases / Contributors screens | `api.github.com` (Fern Wallet repository) | none beyond a normal HTTP request; only when you open those screens |

Links you tap (GitHub, website, UPI) open in other apps you choose; Fern Wallet does not see what happens there.

## No analytics, ads or tracking

Fern Wallet contains no analytics, advertising, crash-reporting or tracking SDKs. (The upstream Ivy Wallet
Firebase Crashlytics and Firestore integrations were removed.)

## Permissions

Internet / network state (exchange rates), notifications (optional reminders), boot completed and wake lock
and foreground service (scheduling reminders and widget updates via WorkManager), biometric/fingerprint (optional app lock), vibrate.
Fern Wallet does not request location, contacts, or storage permissions.

## Contact

Questions: open an issue at https://github.com/Fahimofficial/Fern-wallet/issues.
