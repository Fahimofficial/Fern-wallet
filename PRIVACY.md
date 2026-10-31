# Privacy

Fern Wallet is an offline-first money manager. This page describes what the app does with your data as
of the current source code in this repository.

## Data stored on your device

Accounts (including credit-card details: only the last four digits, never the full card number, CVV or expiry), transactions, categories, budgets, planned payments, loans, tags and settings are stored locally
in an app-private SQLite database and preferences. Android backup is disabled (`allowBackup="false"`), so
this data is not copied to Google Drive by the system. Backups are only created when you choose
**Settings → Backup data**, and saved wherever you pick.

> Backup ZIP and CSV export files are **not encrypted**. Store and share them carefully.

## Network requests

The app makes these HTTPS requests (cleartext HTTP is disabled):

| Purpose | Destination | When | Data sent |
|---|---|---|---|
| Exchange rates | `currency-api.pages.dev`, `cdn.jsdelivr.net` | automatically each time the main screen opens, and when you change the base currency or open Exchange rates | none beyond a normal HTTP request (your IP address and app user agent); the app always downloads EUR-based rates, so your currency isn't revealed |
| Releases / Contributors screens | `api.github.com` (Fern Wallet repository); contributor avatars from GitHub's image servers | only when you open those screens | none beyond a normal HTTP request |

There is currently no setting to turn off the automatic exchange-rate download.

Links you tap (GitHub, website, UPI) open in other apps you choose; Fern Wallet does not see what happens there.
If you choose to rate the app from the home screen card, Google Play's in-app review dialog is shown by the
Google Play app on your device (Google's privacy policy applies to it).

## No analytics, ads or tracking

Fern Wallet contains no analytics, advertising, crash-reporting or tracking SDKs. (The upstream Ivy Wallet
Firebase Crashlytics and Firestore integrations were removed.)

## What can be visible outside the app

- **Notifications:** credit-card due reminders include the card name and the amount due. Whether they show on
  the lock screen depends on your Android notification settings.
- **Balance widget:** shows your balance, income and expense on the home screen unless the app lock is on.
- Screenshots and screen recording of the app are allowed.

## Permissions

Internet / network state (exchange rates), notifications (optional reminders), boot completed and wake lock
and foreground service (scheduling reminders and widget updates via WorkManager), biometric/fingerprint (optional app lock), vibrate.
Fern Wallet does not request location, contacts, or storage permissions.

## Contact

Questions: open an issue at https://github.com/Fahimofficial/Fern-wallet/issues.
