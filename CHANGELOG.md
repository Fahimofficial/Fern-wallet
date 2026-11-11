# Changelog

Release notes for published builds are generated on the
[Releases page](https://github.com/SimplyFahimOfficial/Fern-wallet/releases). This file summarizes notable changes.

## Unreleased

First Fern Wallet changes on top of Ivy Wallet `2025.07.17 (206)`.

### Help & Support
- **Settings → Help & Support** replaces the separate Support entry and the About section's
  "Report a bug" and "Request a feature" rows. It groups the Telegram community, bug reports
  (GitHub issues), the project website, **Support My Work** and the developer's contact page.
  External links show an external-link icon; Support My Work opens in the app.
- **Support My Work** keeps the on-device UPI QR code, Copy UPI ID, Pay with UPI and Share/Save QR,
  and adds **More Ways to Pay** (`simplyfahim.pages.dev/pay-options`). Ko-fi and the personal
  Instagram, email and GitHub links are gone from the app; the contact page lists them.
- The UPI buttons wrap their labels at large font sizes instead of cutting them off.
- **Highlights:** the Settings entry and the Support My Work row get an accent-tinted fill and
  border; **More Ways to Pay** gets an outlined accent border of its own. They follow the accent
  colour you pick in Appearance.
- **Tap the QR code** on Support My Work to see it enlarged (up to 420dp) with a **Close** button
  below it. Back and tapping outside also close it. Save, Share and Copy are unchanged.
- **Support My Work** fits on one screen on most phones: the title sits beside the back button,
  the QR code shares a row with the UPI heading, and spacing is tighter. Nothing was removed, and
  the screen still scrolls on small phones or with large fonts.

### Home menu cards
- The "Fern Wallet is open-source" card is gone from the Home menu.
- After a fresh install and after every app update, the menu shows two short cards you can dismiss
  with the ✕: **Help & Support** (opens the Help & Support page) and a highlighted
  **Support My Work** (accent-tinted, with a round icon badge). Each is remembered separately per
  app version, so dismissing one never hides the other, and both come back after the next update.
- **Settings → Appearance → Home menu → Show Support My Work card** keeps or removes the permanent
  Support My Work card. It doesn't affect the one-time cards. While the one-time Support My Work
  card is showing, the permanent one steps aside, so you never see two.
- Cards fade and collapse in and out (instantly with reduced motion).

### Fixes, connected motion, account logos and backgrounds
Root causes are in `PLAN.md`.

- **Hub chevron:** taps work again; it was under Home's root, which took every touch. The
  chevron now sits above the tabs, and a tap or a drag opens and closes the hub. On Accounts it
  fades out and ignores touches.
- **System back:** it no longer stops working after using the transaction editor or an account.
  A modal that closed out of order left its back handler behind, and that handler swallowed
  back everywhere.
- **Nav bar:** now opaque, with a taller scrim, so list text no longer shows through the labels.
- **Screen motion:**
  - A screen enters in 300 ms and leaves in 200 ms. The screen behind scales down and dims
    instead of vanishing.
  - A card opening into a screen is driven only by the shared bounds, and its corners morph.
  - Predictive back scrubs all of it.
- **Opening an account:** the first frame already has the account's colour. The data arrives in
  one update, and the history list appears once the screen has landed (placeholders until then).
- **Account logos:**
  - 48 brands and 8 generic icons, with search and categories, plus your own image (Photo
    Picker, no permission needed).
  - 16 brand glyphs come from Simple Icons. The rest are initials (see `LOGO_LICENSES.md`).
    This replaces the earlier note that no bank or network logos are bundled.
- **Account backgrounds:** None, Solid, Gradient (presets or custom colours, plus an angle) or
  Image (with a dim slider), with a live preview. Text always reaches 4.5:1 contrast.
- **Settings > Appearance > Use account colours on transactions** (off by default): account
  chips take the account's colour and logo, and transfers blend From to To. The setting is read
  once for the whole app.
- **Database 131 to 132:** an additive migration with nullable columns, so existing accounts
  look the same. Old backups still import. Uploaded images aren't part of backups.

### UI polish, motion and micro-interactions
All motion uses the shared `IvyMotion` tokens (150/250/300 ms, M3 easings, springs). With
"Remove animations" on, every animation finishes at once or is skipped: Compose follows the
animator scale, and the stagger, count-up, pulse and confetti effects check `MotionPreferences`.
See `PLAN.md` for the decisions behind each item.

- **Foundations.** New tokens: stagger and a bouncy spring. `MotionPreferences` is observable
  state. Haptics gain success and error. New `AnimatedAmountText` and tabular figures for all
  amount styles. `pressScale` now presses to 0.97 and can lower an elevation while pressed.
- **Home layout.**
  - The bottom bar used to be painted over by the list. It now sits above it, with a gradient
    scrim and bottom padding so the last row clears the bar and the + button.
  - The bar and + hide while scrolling down and come back on scroll-up. They never hide with
    TalkBack on.
  - The + shows "Add" during the first session only.
  - Days with no income or expense collapse to one line. Day totals are signed and tinted
    (income, expense or grey).
  - Untitled rows show the category, or else the time.
  - Amounts are right-aligned with a smaller currency.
  - Cashflow is a chip that opens Reports. The month pill has its own caret.
- **Home motion.**
  - The balance counts to new values. Tap switches compact/exact. Long-press, or the TalkBack
    action, opens Balance details. An eye button toggles the saved hide-balance setting.
  - Income and expense count up.
  - On month change, rows slide in from the side you moved to.
  - On first load, rows enter in a stagger.
  - Swipe a row right to edit or left to delete with Undo. Both are also TalkBack actions.
  - Nav icons bounce when selected.
- **Transitions.** After you edit a transaction, its row glows once when you return to the list.
- **Add/Edit.**
  - The converted-amount line shows only when the currencies differ.
  - One style for the selected account chip: colour fill plus a check.
  - The sheet has a drag handle.
  - Transfers keep category and tags under "More". A From/To swap button turns half a turn,
    and "To" is highlighted when it's the next thing to pick.
  - Save gives a haptic and a progress state.
- **Quick access.**
  - Tiles press, tick and enter in a diagonal wave.
  - A due-count badge on Planned Payments.
  - The savings bar fills in. Confetti plays once when a goal is reached.
  - A "most frequent category this month" insight.
  - The GitHub card shows an open arrow, with the URL on one line.
  - The Support heart beats once.
  - Search highlights the query in results.
- **Appearance.**
  - The preview stays pinned while the controls scroll.
  - Colour changes sweep through the preview in a wave.
  - The theme switch has a sliding thumb, an icon per mode, and an AMOLED hint.
  - The preset row shows a peek of the next preset with an edge fade, and a fixed label height.
  - Section Reset fades in and out.
  - "Due soon" in the preview has a warning icon, so it isn't shown by colour alone.
  - Reset all asks for confirmation in a summary sheet.
  - A "Saved on this device" indicator.
- **Performance.**
  - Keys and contentType on every Home list item.
  - Every new composable is skippable (checked in the Compose compiler reports).
  - Animated values are read in the layout or draw phase.
  - A baseline profile is compiled into release builds (R8 and resource shrinking unchanged).
- **Skipped, and why.**
  - Pull-to-refresh: pulling down on Home opens Quick access, and data reloads after every change.
  - FAB morphing into a sheet: the radial add menu and its swipe shortcuts stay.
  - Amount as the hero of the edit screen, and a segmented type switch: both are redesigns of
    the legacy sheet.
  - Sparkline, circular-reveal sweep, transfer-icon flip, Macrobenchmark module: low value for
    their cost.
  - A save checkmark: it would mean delaying navigation in the ViewModel.
  - Not profiled on a device: there was no emulator, so frame-time targets are unmeasured and
    the baseline profile is hand-written, not generated.

### Support my work
- New **Settings → Support my work** screen: Ko-fi, UPI (`SimplyFahim@sbi`) with a QR code
  generated on the device, Copy UPI ID, Open UPI app (chooser when several are installed, clear
  message when none is), and Share/Save QR. No amount is pre-filled and no payment is ever
  reported as made.
- Project links are kept separate from payment options: Fern Wallet's own GitHub repository,
  the developer's website and GitHub profile. The old QuantumMPV links are gone.

### Backups
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
