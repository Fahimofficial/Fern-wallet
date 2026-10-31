# Fern design system

Fern Wallet is moving from the legacy in-house design system (`temp/old-design`, `UI.colors`,
`UI.typo`, `UI.shapes`, `IvyModal`) to **Material 3 with Fern tokens**. The move happens screen by
screen; there is no big-bang rewrite. Both systems read the same colours, so migrated and legacy
screens can sit next to each other.

## Tokens (`shared/ui/core`, package `com.ivy.design.system`)

| What | Where | Use |
|---|---|---|
| Colours | `IvyMaterial3Theme` (`MaterialTheme.colorScheme`) | Fern green primary, transfer colour, light/dark/AMOLED |
| Typography | `FernTypography` (`MaterialTheme.typography`) | Raleway for display, headline, title and label roles; Open Sans for body text and numbers |
| Shapes | `FernShapes` (`MaterialTheme.shapes`) | 8 / 12 / 16 / 24 / 32 dp, the legacy radii |
| Spacing | `FernSpacing` | 4 / 8 / 12 / 16 / 24 / 32 dp, screen padding, 48 dp touch target |
| Motion | `FernMotion` | durations, easing and springs (below) |

Rules for new or migrated code:

- Use `MaterialTheme.*` and `FernSpacing` instead of `UI.*` and raw `dp`/`sp`/`Color(0x…)` values.
- Use Material 3 components (`Button`, `OutlinedTextField`, `AlertDialog`, `Surface`…) instead of
  the legacy `IvyButton`, `IvyModal` and similar.
- Text gets its style from a typography role; don't set font sizes by hand.
- Icons that only decorate get `contentDescription = null`; icon-only buttons need a real label.
- Every interactive element is at least `FernSpacing.MinTouchTarget` (48 dp).

`AppLockedScreen` (`app/.../ui/applocked`) is the reference migration: compare it with its git
history to see a legacy screen rewritten with the tokens above.

## Motion

All animation timing comes from `FernMotion`, so motion is consistent and respects the system
**Remove animations** setting (animator duration scale 0, set from `RootActivity`). When it's on,
tweens take no time and springs settle instantly without overshoot.

| Use | Spec |
|---|---|
| Press feedback, toggles, small fades | `FernMotion.tween(Duration.Fast)` (100 ms) |
| Expand/collapse, content changes | `FernMotion.tween(Duration.Standard)` (250 ms) |
| Sheets and modals | `FernMotion.tween(Duration.Emphasized)` (350 ms), `EmphasizedDecelerate` for entering |
| Following a gesture or value | `FernMotion.spring()` (damping 0.85, < 1 % overshoot) |

The legacy helpers (`springBounce*` in legacy-code and old-design) and `IvyModal` now delegate to
`FernMotion`. Don't call Compose's `tween`/`spring` directly in feature code.

## Not done yet

- **Screen transitions.** Navigation still swaps screens instantly. Animating it (e.g.
  `AnimatedContent` in `NavigationRoot`) keeps the outgoing screen composed for a moment, and
  legacy screens share activity-scoped ViewModels and clear the ViewModel store on every screen
  change. That needs care, and testing on a device, before it is turned on.
- **List item animations** (`Modifier.animateItem()`) in transaction, account and category lists.
- Migrating the remaining legacy screens. Suggested order: Settings (largest, simple rows), then
  Edit transaction (most used), then the lists.
