# UI/UX, motion and interaction audit (October 2026)

Reference: the `apple-design` skill (Apple's fluid-interface principles: respond on press, track
the finger 1:1, interruptible springs, velocity handoff, momentum projection, rubber-banding,
spatial consistency, restraint with haptics). I applied these to Compose and kept Fern's identity:
the Raleway/Open Sans type, the Fern green, the rounded legacy cards, the circular More menu and the
FAB fan-out are all unchanged.

## How the findings were gathered

- I read the code: design tokens, `IvyMotion`, the legacy springs, every `animate*`, `AnimatedContent`
  and `Animatable` call site, navigation, the modal system, Home, the bottom bar and the transaction list.
- I looked at the real UI through the 70 Paparazzi golden screenshots, which render the actual
  composables in light and dark themes.
- No emulator or device was available, so gestures, frame timing and haptics could not be tested
  on hardware. The performance findings come from reading which composition phase each value is
  read in. They are not profiler measurements.

## Architecture summary

- The app is single-Activity Compose. `Navigation` is a custom back stack. `NavigationRoot` shows every
  screen through one `AnimatedContent` using IvyMotion's shared-axis X transition.
- Screens get per-entry ViewModel stores. Most ViewModels are `ComposeViewModel`s that build their
  state in `@Composable uiState()`.
- Three design layers are active at once:
  - the legacy `UI.colors/typo/shapes` system, used by most screens;
  - a duplicate legacy palette in `com.ivy.wallet.ui.theme`;
  - Material 3 (`IvyMaterial3Theme`), which sets colours only and no typography or shapes.
- There are two motion systems: `IvyMotion` (M3 tweens) and legacy `springBounce*` springs
  (stiffness 20 to 2000, damping 0.85).
- All legacy modals use one custom overlay, `IvyModal`. It is not `ModalBottomSheet`.

## Findings

Legend: complexity S/M/L, priority C(ritical)/H/M/L. "Done" means it is implemented in this change.

### Animation and transitions

| # | Where | Today | Problem / why it matters | Recommendation | Cx | Pri | Status |
|---|---|---|---|---|---|---|---|
| A1 | `IvyModal` (about 25 sheets) | The sheet slides with `tween(200)` and the scrim fades with `tween(400)`. The actions row moves only by its own height. | The scrim keeps fading after the sheet has gone. The actions row arrives before the sheet content, so the sheet does not move as one object. | Drive the sheet, the actions row and the scrim from one spring value. | M | H | Done |
| A2 | `IvyModal` | There is no drag-to-dismiss and no handle. | Every iOS and Android sheet can be pulled down. Here the only exit is the × button or the scrim. | Add 1:1 drag once the content is at its top, momentum projection, velocity handoff to the closing spring, and a grabber. | M | H | Done |
| A3 | `IvyModal` | The percent value is read in composition. | The sheet content recomposes on every frame of open and close. | Read it in the layout and graphics-layer phases. | S | M | Done |
| A4 | FAB menu (`MainBottomBar`) | The full-screen backdrop slides up from the bottom of the screen. | A large moving surface competes with the actions flying out of the FAB. Apple's guidance is to fade big surfaces and anchor motion to the trigger. | Fade the backdrop in place. | S | M | Done |
| A5 | FAB menu | Three animated values are read in composition and passed as `Float` parameters. | The bar and all buttons recompose on every frame. | Pass `() -> Float` and read it in `layout {}` / `graphicsLayer {}`. | S | M | Done |
| A6 | More menu (`HomeMoreMenu`) | Same pattern as A5 for the circular reveal. | Same as A5. | Read in draw/layout. Give the button its own restart scope. | S | M | Done |
| A7 | Home header | Collapse alpha is read in composition. | The header recomposes on every frame of a collapse. | `graphicsLayer` plus `derivedStateOf` for visibility. | S | L | Done |
| A8 | Upcoming/Overdue sections | Rows are added and removed instantly on expand and collapse. Lists jump on insert and delete. | Abrupt layout shift, and the user loses track of where content went. | `Modifier.animateItem()`: fade in, spring placement, fade out only for collapsing sections. | S | H | Done |
| A9 | Bottom tabs | The selected label pops in and pushes the icon sideways. | Visible layout jump on every tab switch. | `AnimatedVisibility` expand/fade, plus a tint cross-fade. | S | M | Done |
| A10 | Onboarding | Steps swap with no transition. | Every other navigation in the app slides along the X axis. Onboarding breaks that spatial model. | Reuse `IvyMotion.screenTransform` (forward/back by step order). | S | M | Done |
| A11 | Navigation | Back is a fixed 300 ms swap. Predictive back is not supported. | Back can't follow the finger and can't be cancelled. | `PredictiveBackHandler` plus `SeekableTransitionState`. The legacy back stack (modal stack, per-screen overrides) needs to move to Compose `BackHandler` first. | L | M | Recommended |
| A12 | Motion tokens | Two spring families (damping 0.75 vs 0.85) and 200/250/300/400 ms tweens. | Things that look alike move differently. | `IvyMotion.Springs` (response/damping, Apple's parameters) is now the shared vocabulary. Migrate the legacy `springBounce*` call sites. | M | M | Partly done |
| A13 | `EditBottomSheet` | A separate custom sheet with tween(200) insets. | Inconsistent with the updated `IvyModal`. | Give it the same `SheetMotion`. | M | M | Recommended |
| A14 | Keyboard insets | tween(200) next to the system IME animation. | The sheet lags or leads the keyboard. | Use `WindowInsets.ime` / `imePadding()` so the sheet tracks the IME frame by frame. | M | M | Recommended |

### Micro-interactions

| # | Where | Today | Problem | Change | Cx | Pri | Status |
|---|---|---|---|---|---|---|---|
| M1 | All legacy buttons (`IvyButton`, `IvyCircleButton`, `CircleButton*`, `IvyOutlinedButton*`, `IvyBorderButton`), tabs, Home income/expense cards | Ripple only. Many controls clip the ripple to a small circle. | Weak or invisible press feedback. Apple's guidance is to respond on press-down. | `Modifier.pressScale` (0.96, or 0.98 for cards). It is a `Modifier.Node` that scales in the graphics layer, re-targets springs mid-flight and adds no recomposition. | S | H | Done |
| M2 | FAB swipe shortcuts | Swiping up, up-left or up-right opens a screen with no feedback during the drag. Continuing past the threshold could open a second screen. | Hidden gesture with no feedback; risk of double navigation. | The FAB follows the finger with a rubber band. A haptic fires on the frame the shortcut commits. Only one action per swipe. The FAB springs back on release. | S | H | Done |
| M3 | FAB menu backdrop | Tapping outside the actions did nothing. | Users are trapped (wayfinding). | Tapping the backdrop closes the menu. | S | H | Done |
| M4 | `IvySwitch` | Plain `clickable` with no role or state. | TalkBack reads "double tap to activate" with no on/off state. No feedback. | `toggleable(role = Switch)` plus toggle haptics. | S | H | Done |
| M5 | Month swipe on the period button | Changes the month silently. | Easy to swipe twice without noticing. | Haptic tick per step. | S | L | Done |
| M6 | Deletes | Always a confirmation modal. No undo anywhere. | Apple's guidance is to prefer undo for reversible slips and confirm only irreversible actions. | Snackbar with Undo for transaction delete and skip. It needs repository support for soft delete. | M | M | Recommended |
| M7 | Customer-journey cards | Dismissed cards vanish instantly. The cards render in one non-lazy item. | Abrupt. | Give each card its own keyed item with `animateItem()`. | S | L | Recommended |

### Design

| # | Where | Finding | Recommendation | Pri |
|---|---|---|---|---|
| D1 | Theme | `UI.colors.primary` is Ivy purple (#6B4DFF). The Fern brand, M3 primary and FAB are green (#13876A). The legacy palette also defines a different `Purple` (#A020F0). | Decide whether primary should become Fern green. This is a brand decision, so I did not change it. | H (needs owner decision) |
| D2 | Typography | No tracking or line-height anywhere. A 40 sp Black heading has the same letter-spacing as a 12 sp caption. | Use size-specific tracking (about −0.02 em on h1/h2/nH1, slightly positive on c/nC) and explicit line heights. This changes every screenshot, so do it as its own reviewed PR. | M |
| D3 | M3 screens | `IvyMaterial3Theme` passes colours only, so M3 screens use Roboto and default shapes, while legacy screens use Raleway/Open Sans. | Map `UI.typo` and `UI.shapes` into M3 `Typography`/`Shapes` so both systems look alike. | M |
| D4 | Neutrals | The legacy and M3 neutrals differ slightly (#FAFAFA vs #FAFAFC, #111114 vs #09090A, gray #939199 vs #74747A). | Use one neutral scale. | L |
| D5 | Spacing | There are no spacing tokens. Values are hard-coded dp (24/20/16/12/8). | Add a spacing scale and use it in new code. | L |
| D6 | Theme switch | Light and dark swap instantly. | Cross-fade the root (Apple: avoid abrupt brightness jumps). | L |
| D7 | Empty state | The list empty state is all gray: icon, title and body at the same weight. | Give the title more contrast and add a primary action ("Add transaction"). | L |

### Accessibility

- `IvySwitch` now has switch semantics (M4, done).
- Touch targets below 48 dp: `IvySwitch` (40×24), `CircleButton` (about 36 dp), and the tag chips. Enlarge them with
  `minimumInteractiveComponentSize()`. This changes layout, so it needs screenshot review (recommended).
- `clickableNoIndication` removes the role and the visual feedback from about 40 controls (tabs, header balance,
  section headers, FAB labels). Section headers in particular should use `Role.Button` with
  expanded/collapsed state (recommended).
- Reduced motion: on Android this is "Remove animations" (animator scale 0). Compose already completes all
  springs and tweens instantly then, and all new motion here uses standard Compose animation APIs,
  so it respects that setting. No per-frame custom loops were added.

### Performance and architecture

- P1 (done): moved per-frame animation reads out of composition (A3, A5, A6, A7). I reasoned this from
  the snapshot-read phase of each value. It was not profiled; a Macrobenchmark/JankStats run is needed for numbers.
- P2: `uiState()` composables keep running while a screen animates out, and nothing uses
  `collectAsStateWithLifecycle`. Recommended: collect flows lifecycle-aware.
- P3: `MainScreen` still reads LiveData with `observeAsState`, and tab content is rebuilt on every switch
  (no `SaveableStateHolder`), so Home loses its scroll position each time. Recommended.
- P4: `drawColoredShadow` on every primary button and FAB draws a blur shadow per frame while animating.
  Consider caching the shadow in a graphics layer.

## Implementation plan (remaining work, in order)

1. Make the D1 primary-colour decision.
2. Migrate `EditBottomSheet` to the same sheet motion (A13), and move keyboard handling to IME insets (A14).
3. Add Undo for deletes (M6).
4. Predictive back (A11), after replacing `Navigation.onBackPressed` overrides with `BackHandler`.
5. Typography tracking and line height (D2) and the M3 typography mapping (D3), with re-recorded goldens.
6. Fix touch targets and semantics for `clickableNoIndication` call sites.
7. Add a baseline profile plus Macrobenchmark scrolling Home and opening a modal, to measure jank before and after.
