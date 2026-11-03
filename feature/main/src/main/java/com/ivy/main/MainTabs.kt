package com.ivy.main

import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.zIndex
import com.ivy.design.system.motion.IvyMotion
import com.ivy.legacy.data.model.MainTab

/**
 * Home and Accounts with a fade-through between them.
 *
 * A tab stays composed once it has been shown, so switching only animates alpha: no screen is
 * rebuilt in the frame after the tap. Each tab gets the [Modifier] for its root content only and
 * is emitted straight into this screen's Box (no wrapper), so its sheets and menus keep their
 * zIndex above the bottom bar. Wrapping the tabs (as AnimatedContent did) drew the bar and the
 * + button over Home's period picker and other sheets.
 */
@Composable
fun BoxWithConstraintsScope.MainTabs(
    tab: MainTab,
    home: @Composable BoxWithConstraintsScope.(Modifier) -> Unit,
    accounts: @Composable BoxWithConstraintsScope.(Modifier) -> Unit,
) {
    // Plain set: it only grows and is read in the same composition that adds to it.
    val composed = remember { mutableSetOf<MainTab>() }
    composed += tab
    val transition = updateTransition(tab, label = "main_tab")
    if (MainTab.HOME in composed) home(Modifier.tabVisibility(transition, MainTab.HOME))
    if (MainTab.ACCOUNTS in composed) accounts(Modifier.tabVisibility(transition, MainTab.ACCOUNTS))
}

@Composable
private fun Modifier.tabVisibility(transition: Transition<MainTab>, tab: MainTab): Modifier {
    val alpha: State<Float> = transition.animateFloat(
        transitionSpec = {
            if (targetState == tab) IvyMotion.fadeThroughIn() else IvyMotion.fadeThroughOut()
        },
        label = "tab_alpha",
    ) { if (it == tab) 1f else 0f }
    val selected = transition.targetState == tab
    // The selected tab is on top, so it takes touches even while the other one fades out.
    var modifier = zIndex(if (selected) 1f else 0f)
    if (!selected) modifier = modifier.clearAndSetSemantics { }
    return modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            // An unplaced tab isn't drawn, hit-tested or read by TalkBack, but keeps its state.
            if (selected || alpha.value > 0f) placeable.placeWithLayer(0, 0) { this.alpha = alpha.value }
        }
    }
}
