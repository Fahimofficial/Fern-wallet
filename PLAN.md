# UI/UX polish plan

## Recon

- **Stack:** Kotlin 2.4, Jetpack Compose 1.7.8 (foundation, animation, ui) with Material3 1.2.1.
  Hilt, Room 2.8 and DataStore. AGP 9.5, minSdk 28, targetSdk 34.
- **Architecture:** multi-module MVVM (`ComposeViewModel<State, Event>`). Most widgets are still
  legacy code in `temp/legacy-code` and `temp/old-design` (`UI.colors`, `UI.typo`).
- **Theme:** `FernPalette` / `ColorToken` (`shared/ui/core/.../appearance`) is the source of
  truth. It is mapped onto the old `IvyColors` in `IvyTheme.withPalette`. Typography lives in
  `IvyWalletDesign.typography()`; amounts use the Open Sans `n*` styles.
- **Navigation:** a custom `Navigation` stack and `NavigationRoot`.
  - Shared-axis X transitions, a seekable predictive back (`PredictiveBackHandler`) and
    `enableOnBackInvokedCallback="true"`.
  - A `SharedTransitionLayout` with `Modifier.sharedContainer` gives the transaction card →
    edit screen container transform.
- **Already present, reused rather than rewritten:**
  - `IvyMotion`: durations of 150/250/300 ms, M3 easings and springs.
  - `Modifier.pressScale`, a graphics-layer node.
  - `IvyHaptics`: `tick`, `toggle` and `gestureCommit`.
  - `MotionPreferences.reduceMotion`, set from `ANIMATOR_DURATION_SCALE` in `RootActivity.onResume`.
  - `UndoController` with an undo snackbar for deletes. The edit screen already deletes with no
    dialog.
  - The collapsing header: the greeting turns into the mini balance on scroll.
  - Lazy keys and `contentType` for transaction rows.
  - R8 with resource shrinking for release builds.

### Screens

| Screen | Files |
|---|---|
| Home | `feature/home/HomeTab.kt`, `HomeHeader.kt`, `HomeViewModel.kt`; rows in `temp/legacy-code/.../transaction/{Transactions,TransactionCard,HistoryDateDivider}.kt`; bar and FAB in `feature/main/MainBottomBar.kt` |
| Add/Edit expense and transfer | `feature/edit-transaction/EditTransactionScreen.kt`; `temp/legacy-code/.../edit/core/{EditBottomSheet,Toolbar}.kt` |
| Quick-access hub | `feature/home/HomeMoreMenu.kt`, `BufferBattery.kt` |
| Search | `feature/search/SearchScreen.kt` |
| Appearance | `feature/settings/.../appearance/*` |

## Phase order

Each phase is built, unit-tested and committed on its own.

1. **Foundations** (`shared/ui/core/.../motion`, the typography in `IvyWalletDesign`)
   - Add stagger and bounce tokens.
   - Make `MotionPreferences` snapshot state in `shared/ui/core`, so Compose reads are observable.
   - Haptics: add `success` and `error`.
   - Add `AnimatedAmountText` and a tabular-numerals style.
   - `pressScale`: change the scale to 0.97 and add an optional elevation.
2. **Home layout:** bar scrim, bottom padding, hide-on-scroll, a one-time extended FAB, compact
   empty days, tinted day totals, a fallback primary text, right-aligned amounts, the cashflow
   chip and the month pill.
3. **Home motion:** count-ups, compact/exact balance, the eye toggle, month-change slide,
   staggered entrance, swipe rows with undo, and a bounce on nav icons.
4. **Transitions:** a tint pulse on the row after returning from edit. The other items already
   exist (see Skipped).
5. **Add/Edit:** a converted line only when the value was converted, a unified selected chip,
   a drag handle and scrim on the sheet, "More" for transfers, From/To swap, and save feedback.
6. **Hub:** grid press, haptic and wave; a due badge; a goal progress bar with one-time
   confetti; a top-category insight; the GitHub card; the heart pulse; search-match highlight
   and recent searches.
7. **Appearance:** pinned preview, colour cross-fade and wave, a sliding theme thumb with icons,
   the AMOLED hint, presets peek and fade, section chevron and reset fade, a warning icon, a
   reset confirm sheet, and the saved indicator.
8. **Performance:** keys and `contentType` on every Home item, a stability check of the Compose
   reports, and a baseline profile.

## Decisions and risks (simplest safe option)

- **Reduce motion.** Compose tweens and springs already finish instantly when the animator
  scale is 0. The helper covers what the scale does not: `delay()`-based staggers, count-ups,
  confetti and one-shot pulses. All of these check `MotionPreferences.reduceMotion` and are
  skipped when it is on.
- **Scroll direction** comes from the Home list's existing `NestedScrollConnection` (scroll
  deltas), not from `derivedStateOf`. A `derivedStateOf` would have to write its "previous
  offset" state while deriving, which is a snapshot anti-pattern. `derivedStateOf` is still
  used for the derived "at top" check.
  - The bar is never hidden while TalkBack is on (touch exploration), or while the add menu is
    open.
- **Month selector.** The round chevron next to the month opens the Quick-access hub, not a
  month menu. Merging it into the month pill would remove the only button for the hub. So the
  month pill gets its own caret (the whole pill opens the picker), and the hub button stays.
  The month picker (`ChoosePeriodModal` / `IvyModal`) is already a bottom sheet.
- **Balance tap** now toggles compact/exact, as requested. The Balance screen, which a tap used
  to open, moves to long-press plus a TalkBack custom action, so it stays reachable. The eye
  icon toggles the existing persisted "hide balance" preference (`SharedPrefs`).
- **Cashflow chip** opens the Reports screen.
- **Swipe rows** are on Home only. They are wrapped in M3 `SwipeToDismissBox`.
  - Delete reuses the same restorable delete and Undo snackbar as the edit screen, with no
    dialog.
  - Both actions are also TalkBack custom actions.
  - A child row drag consumes the gesture, so the Home tab-switch swipe still works outside rows.
- **Save button.** The view model saves and then closes the screen itself, so the button shows
  progress plus a success haptic until the screen leaves. A checkmark would need the view model
  to report "saved" and then delay navigation. That changes app flow, so it is skipped.
- **Insight:** "top category this month" counts transactions per category instead of summing
  amounts. Summing would mix currencies without conversion, which would be wrong.
- **Confetti** runs for about 800 ms, longer than the 300 ms motion rule, because a celebration
  needs time to read. It plays once per goal (stored in `SharedPrefs`) and never with reduce
  motion.
- **Baseline profile.** There is no emulator or KVM here, so a profile can't be generated or
  measured. A hand-written `app/src/main/baseline-prof.txt` covers the startup, Home and
  navigation classes. `profileinstaller` already arrives transitively through Compose, so no
  new dependency is needed.
- **Profiling.** I can't profile a release build without a device. Frame-time targets are
  unverified, and the CHANGELOG says so.

## Skipped and why

- **Pull-to-refresh:** on Home, pull-down already opens the Quick-access hub, and data reloads
  on every write (`DataObserver`).
- **FAB morph into a sheet:** the existing radial add menu, with its swipe shortcuts, is kept.
  Replacing it is a redesign, not polish.
- **Amount as the hero at the top of the edit screen:** this means restructuring the legacy
  sheet. The keypad already opens automatically for new transactions.
- **Expense/Income/Transfer segmented switch:** the existing type chip and modal stay.
- **Sparkline, circular-reveal sweep, transfer-icon flip and a Macrobenchmark module:** these
  are optional or low value for their cost.
- **Count-up already shows change** on month switch, so no extra cross-fade is added on top.
