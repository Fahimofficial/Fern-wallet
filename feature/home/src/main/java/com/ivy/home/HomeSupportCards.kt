package com.ivy.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.data.datasource.SupportCardsPrefs
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.readableOn
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.MotionPreferences
import com.ivy.navigation.HelpSupportScreen
import com.ivy.navigation.SupportScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R

/** Which of the Home menu's Help & Support / Support My Work cards are shown right now. */
@Immutable
data class SupportCardsUi(
    /** One-time Help & Support card. */
    val help: Boolean,
    /** One-time, highlighted Support My Work card. */
    val highlightedSupport: Boolean,
    /** Always-on Support My Work card (Appearance switch). */
    val permanentSupport: Boolean,
) {
    companion object {
        val Hidden = SupportCardsUi(help = false, highlightedSupport = false, permanentSupport = false)
    }
}

/**
 * The one-time cards show until dismissed for the running install, identified by [installStamp]
 * (when the app was last installed or updated), so they come back after every fresh install and
 * every update, even one that keeps the same version code. Each is dismissed on its own. The
 * permanent card follows its switch and steps aside while the one-time Support My Work card is
 * up, so the two never appear together.
 *
 * [prefs] is null until stored values are read: nothing is shown rather than a card that was
 * already dismissed flashing in.
 */
fun supportCardsUi(prefs: SupportCardsPrefs?, installStamp: Long): SupportCardsUi {
    if (prefs == null) return SupportCardsUi.Hidden
    val highlightedSupport = prefs.supportDismissedFor != installStamp
    return SupportCardsUi(
        help = prefs.helpDismissedFor != installStamp,
        highlightedSupport = highlightedSupport,
        permanentSupport = prefs.permanentSupportCard && !highlightedSupport,
    )
}

/** Test tag of the one-time Help & Support card. The Support My Work cards share `SupportCardTag`. */
const val HelpCardTag = "home_help_card"

/** Test tags of the dismiss buttons. */
const val HelpCardDismissTag = "home_help_card_dismiss"
const val SupportCardDismissTag = "home_support_card_dismiss"

private enum class SupportSlot { None, Highlighted, Permanent }

private val CardGap = 12.dp
private const val HighlightTint = 0.14f
private const val MinAccentContrast = 3f

/**
 * Cards for the Home menu, in order: the one-time Help & Support card, then one Support My Work
 * card (one-time and highlighted, or the permanent one). Each ends with its own gap, so a card that
 * leaves takes its spacing with it.
 */
@Composable
internal fun SupportCards(
    ui: SupportCardsUi,
    onDismissHelp: () -> Unit,
    onDismissSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = navigation()
    val slot = when {
        ui.highlightedSupport -> SupportSlot.Highlighted
        ui.permanentSupport -> SupportSlot.Permanent
        else -> SupportSlot.None
    }
    Column(modifier) {
        AnimatedVisibility(
            visible = ui.help,
            enter = if (MotionPreferences.reduceMotion) EnterTransition.None else IvyMotion.expandSection(),
            exit = if (MotionPreferences.reduceMotion) ExitTransition.None else IvyMotion.collapseSection(),
        ) {
            Column {
                CompactCard(
                    icon = R.drawable.ic_vue_messages_msg,
                    title = stringResource(R.string.help_support),
                    subtitle = stringResource(R.string.home_help_card_description),
                    highlighted = true,
                    onClick = { nav.navigateTo(HelpSupportScreen) },
                    onDismiss = onDismissHelp,
                    contentTag = HelpCardTag,
                    dismissTag = HelpCardDismissTag,
                )
                Spacer(Modifier.height(CardGap))
            }
        }

        // One slot for both Support My Work cards: they trade places in it, never stack.
        AnimatedContent(
            targetState = slot,
            transitionSpec = {
                if (MotionPreferences.reduceMotion) {
                    (EnterTransition.None togetherWith ExitTransition.None).using(null)
                } else {
                    val fadeInSpec = fadeIn(IvyMotion.standard(IvyMotion.Duration.Medium))
                    val fadeOutSpec = fadeOut(IvyMotion.standard(IvyMotion.Duration.Short))
                    (fadeInSpec togetherWith fadeOutSpec).using(
                        SizeTransform(clip = true) { _, _ -> IvyMotion.standard(IvyMotion.Duration.Medium) }
                    )
                }
            },
            label = "support_card_slot",
        ) { shown ->
            when (shown) {
                SupportSlot.None -> Spacer(Modifier)
                SupportSlot.Highlighted -> Column {
                    CompactCard(
                        icon = R.drawable.ic_vue_support_heart,
                        title = stringResource(R.string.support_my_work),
                        subtitle = stringResource(R.string.home_support_card_description),
                        highlighted = true,
                        onClick = { nav.navigateTo(SupportScreen) },
                        onDismiss = onDismissSupport,
                        contentTag = SupportCardTag,
                        dismissTag = SupportCardDismissTag,
                    )
                    Spacer(Modifier.height(CardGap))
                }

                SupportSlot.Permanent -> Column {
                    SupportMyWork()
                    Spacer(Modifier.height(CardGap))
                }
            }
        }
    }
}

/**
 * A short card with a round icon, a title and a line of text, and its own dismiss button. The tap
 * area and the dismiss button are siblings, so a screen reader reaches each one separately.
 * [highlighted] gives it the accent-tinted fill and a filled accent icon badge; both one-time cards use it.
 */
@Composable
private fun CompactCard(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    contentTag: String,
    dismissTag: String,
    modifier: Modifier = Modifier,
) {
    val shape = UI.shapes.r4
    // The user's accent, nudged just enough to stay 3:1 on the card in light and dark themes.
    val accent = UI.colors.primary.readableOn(UI.colors.pure, minContrast = MinAccentContrast)
    val container = if (highlighted) {
        accent.copy(alpha = HighlightTint).compositeOver(UI.colors.pure)
    } else {
        UI.colors.pure
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(container, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(contentTag)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (highlighted) accent else UI.colors.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (highlighted) accent.contentColor() else UI.colors.pureInverse,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = UI.typo.b2.style(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.testTag(dismissTag)) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_clear_24),
                contentDescription = stringResource(R.string.home_card_dismiss, title),
                tint = UI.colors.gray,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
