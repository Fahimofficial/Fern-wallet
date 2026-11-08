# Plan: bug fixes, connected motion, account logos and backgrounds

The previous UI-polish plan is now `CHANGELOG.md` (section "UI polish, motion and
micro-interactions"). Work continues on branch `ui-polish` (PR #46): these fixes touch the same
code.

## Diagnoses (Part A)

All found by reading the code; no device was available, so none of this is confirmed with a
Perfetto or JankStats trace.

**A1. The hub chevron only opens with a swipe.**
The chevron is emitted into MainScreen's Box with zIndex 0. Home's root `Column` gets zIndex 1
from `MainTabs.tabVisibility`, fills the screen, and carries `pointerInput` swipe listeners.
Compose hit-tests the topmost sibling first, so the Column takes every touch over the chevron.
A tap never reaches the button's `clickable`. A swipe "works" only because the Column's own
swipe-down listener opens the hub.
- Fix: put the chevron above the tabs.
- Make it `draggable` (touch slop decides tap vs drag) plus `clickable` on the same element.
  A release settles to the state the fling projects to.
- Haptic tick on tap and on settle.

**A2. The chevron shows on Accounts.**
`MainTabs` hides a tab through the Modifier of its root content only. `MoreMenu` is a sibling of
Home's root, not inside it, so it keeps drawing over Accounts.
- Fix: `HomeUi` knows whether its tab is selected and passes `available` to `MoreMenu`. The
  chevron then fades and scales out over 150 ms, its gestures turn off, and once invisible it
  isn't placed, so Accounts' reorder button underneath gets its touches back.
- The button is absolutely positioned, so the top bar doesn't jump.

**A3. Back does nothing on a transaction or an account.**
The root cause is in `AddModalBackHandling` (legacy modals). Its `onDispose` removed the handler
only if it was the top of `Navigation.modalBackHandling`.
- Modals that close out of order leaked their handler. For example, after an amount is entered
  the category picker opens before the amount keypad closes.
- The leaked handler captured `visible = true`. Every later back press called it, it returned
  "handled", and nothing happened. Back stayed dead in every screen until the process died.
- The manifest flag, `PredictiveBackHandler` and `MainScreen`'s `BackHandler` are correct.
- Fix: register and unregister by identity (`Navigation.registerModalBack`), so a modal always
  removes exactly its own handler.
- Back order is kept: the keyboard (system), then modal or sheet, then screen pop, then the hub
  (itself a modal on Home), then exit.
- The predictive-back animation already seeks the screen transition. Phase 2 makes it run the
  container transform backwards.

**A4. List text shows through the nav labels.**
The bar is drawn at 95 % alpha (`NavBarAlpha = 0.95f`) and the scrim is only 24 dp, while the +
rises 34 dp above the bar.
- Fix: an opaque bar, and a scrim tall enough to cover the + area. The list's bottom padding
  stays.

## Diagnosis: opening an account (B3)

Static analysis, unverified on a device.

- **(a) Load during the transition.** `TransactionsViewModel.start` loads everything
  asynchronously. The first frames have `account == null`, so the header is grey and has no
  balance rows. It then recolours and grows while `sharedBounds` animates toward it, and the
  history list composes mid-transition.
  - Fix: seed the account from the in-memory account cache on the first frame.
  - Publish the account's data in one snapshot.
  - Compose the history only once the enter transition has settled, with a fixed-height
    placeholder until then.
- **(c) Corner mismatch.** Both ends use `UI.shapes.r4`, but the header and the editor end
  square, so the corners pop at the end. Fix: the overlay clip animates the corner radius from
  card to screen along the transition, so predictive back follows it too.
- **(d) Competing animations.** The 8 % shared-axis slide runs on the screen while
  `sharedBounds` moves the container. Fix: when the target screen opens from a container, the
  screens only fade and scale; the bounds drive the motion.
- **(e) Recomposition storm.** `initForAccount` writes about 12 states across suspension points,
  and each write recomposes the screen during the transition. The single snapshot in (a) fixes
  this.
- **Side effect in composition.** `Header` calls `setStatusBarDarkTextCompat` on every
  recomposition. Fix: move it into an effect keyed on the colour.

## Phases (one commit each; build and tests after each)

1. **A1-A4 bugs** and a JVM test for the modal back registry.
2. **Motion** (B1, B2):
   - Screen enter 300 ms EmphasizedDecelerate, exit 200 ms EmphasizedAccelerate.
   - The screen behind scales to 0.94 and dims instead of vanishing.
   - Container screens fade; the bounds drive.
   - Animated corner clip.
   - Everything stays seekable, so taps or back mid-way reverse it without a restart.
3. **Account open** (B3, B4) as diagnosed above.
4. **Data model** (C):
   - `AccountAppearance` in the domain and legacy models.
   - 6 nullable columns, a `Migration131to132`, a schema JVM test and an androidTest.
   - The backup JSON uses `AccountEntity`, so the new fields ride along. Old backups decode to
     null, which means "as before".
5. **Logos** (D): a registry, Simple Icons assets, `LOGO_LICENSES.md`, `AccountLogo`, the picker
   sheet, Photo Picker upload and the media store.
6. **Backgrounds** (E): None / Solid / Gradient / Image editor with a live preview, plus card and
   header rendering with guaranteed contrast.
7. **Account colours on transactions** (F):
   - One root collection of the existing `show_account_color` flag (default OFF) through a
     CompositionLocal, replacing five ViewModel collections.
   - The switch lives in Appearance, with a crossfade, logos in chips and the live preview.
8. **Compose UI tests and verification** (G and the test list).

## Decisions and risks

- **Compose UI tests:** the repo has none, and CI's emulator job runs only data and domain tests.
- Added `:shared:ui:ui-tests`, a JVM module running Compose UI tests on Robolectric
  (test-only dependencies; nothing ships).
- Kept out of feature modules because Paparazzi and Robolectric conflict there.
- **Logo licensing.** I used only Simple Icons (CC0 icon data). An icon is used only when its
  Simple Icons entry has no `guidelines` or `license` field and its source isn't a
  brand-guidelines page.
  - Everything else gets an initials badge on the brand colour. See "Logos shipped as initials"
    below and `LOGO_LICENSES.md`.
  - The marks remain trademarks of their owners. This is identification only, and the owner of
    the app should review it before a store release.
- **Policy change.** `docs/Card-Catalogue.md` and the CHANGELOG said no bank or network logos
  are bundled. This feature reverses that at your request. Credit-card illustrations stay
  generic.
- **Image backgrounds** use white text over a dim scrim with a minimum alpha that guarantees
  4.5:1 against a pure-white image pixel, so the result doesn't depend on sampling. Blur is
  skipped: `Modifier.blur` only exists from API 31 and minSdk is 28.
- **Backup.** The image files aren't inside backups. A restored account whose file is missing
  falls back to its initials or solid colour.
- **Default background.** A null style means Solid with the account colour, which is today's
  look.
- **Not done here:** Perfetto, JankStats or Macrobenchmark profiling (no device or emulator).
  The jank fixes come from the diagnoses above, not from traces.

## Logos shipped as initials (no clean licence)

16 of the 48 brands have a bundled glyph; the other 32 show their initials on the brand
colour, with no artwork bundled. Sources and licences are in `LOGO_LICENSES.md`.

- **Not in Simple Icons (25):** SBI, Kotak Mahindra Bank, IDFC FIRST Bank, IndusInd Bank, Yes Bank, Canara Bank, Punjab National Bank, Bank of Baroda, Union Bank of India, Bank of India, IDBI Bank, Federal Bank, AU Small Finance Bank, slice, Jupiter, UPI, BHIM, Amazon Pay, CRED, MobiKwik, Freecharge, Groww, Upstox, RuPay, Citi.
- **Simple Icons entry restricted by brand guidelines or a marketing/brand source (7):** Mastercard, PayPal, Apple Pay, Wise, Revolut, HSBC, Venmo.
