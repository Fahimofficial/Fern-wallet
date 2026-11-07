package com.ivy.settings.support

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.links.SupportDestinations
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.readableOn
import com.ivy.design.system.motion.IvyHaptics
import com.ivy.design.system.motion.IvyMotion
import com.ivy.legacy.utils.MotionPreferences
import com.ivy.settings.SettingsSectionDivider
import com.ivy.ui.R
import com.ivy.settings.R as SettingsR

private const val MinTextContrast = 4.5f
private val EntranceOffset = 12.dp

/** One personal contact tile: [uri] is opened on tap, [copyValue] is copied on long-press. */
private class ContactLink(
    @DrawableRes val icon: Int,
    val label: String,
    val value: String,
    val uri: String,
    val copyValue: String,
)

/** The Telegram group as the primary contact, then smaller personal contact tiles. */
@Composable
internal fun SupportContactSections(actions: SupportScreenActions) {
    EntranceReveal {
        Column {
            SettingsSectionDivider(text = stringResource(R.string.support_community))
            Spacer(Modifier.height(16.dp))
            TelegramGroupCard(actions)

            PersonalContactSection(actions)
        }
    }
}

// Fades and lifts the content in. Drawn in the graphics layer only, so the layout never moves.
@Composable
private fun EntranceReveal(content: @Composable () -> Unit) {
    // Previews and "Remove animations" show the settled state from the first frame.
    val animate = !LocalInspectionMode.current && !MotionPreferences.reduceMotion
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(IvyMotion.Duration.Medium, easing = IvyMotion.Easings.EmphasizedDecelerate),
        )
    }
    val offsetPx = with(LocalDensity.current) { EntranceOffset.toPx() }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * offsetPx
        }
    ) {
        content()
    }
}

/** Tap: light tick and [onClick]. Long-press: long-press haptic and [onCopy]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.contactClickable(
    description: String,
    onClick: () -> Unit,
    onCopy: () -> Unit,
): Modifier {
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    return this
        .combinedClickable(
            role = Role.Button,
            onLongClickLabel = stringResource(R.string.support_copy_action),
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCopy()
            },
            onClick = {
                IvyHaptics.tick(view)
                onClick()
            },
        )
        // The tile is read as one item; the click and long-click actions above are kept.
        .clearAndSetSemantics { contentDescription = description }
}

@Composable
private fun TelegramGroupCard(actions: SupportScreenActions) {
    val url = SupportDestinations.telegramUrl(SupportDestinations.TELEGRAM_GROUP_USERNAME)
    val title = stringResource(R.string.support_telegram_group_title)
    val subtitle = stringResource(R.string.support_telegram_group_subtitle)
    val cta = stringResource(R.string.support_telegram_group_cta)

    // Fern green (or the user's accent), nudged just enough to keep text at 4.5:1 with any accent.
    val content = UI.colors.primary.contentColor()
    val container = UI.colors.primary.readableOn(content, minContrast = MinTextContrast)

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r2)
            .background(container, UI.shapes.r2)
            .contactClickable(
                description = "$cta. $title. $subtitle",
                onClick = { actions.onOpenLink(url, url) },
                onCopy = { actions.onCopy(url) },
            )
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(SettingsR.drawable.ic_telegram),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = UI.typo.b1.style(color = content, fontWeight = FontWeight.ExtraBold),
                )
                Text(
                    text = url.removePrefix("https://"),
                    style = UI.typo.nC.style(color = content, fontWeight = FontWeight.Medium),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = subtitle,
            style = UI.typo.b2.style(color = content, fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(16.dp))
        // Inverted colours: the same 4.5:1 pair as the card.
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(UI.shapes.rFull)
                .background(content, UI.shapes.rFull)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = cta, style = UI.typo.b2.style(color = container, fontWeight = FontWeight.Bold))
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = container,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PersonalContactSection(actions: SupportScreenActions) {
    val instagram = stringResource(R.string.support_instagram)
    val email = stringResource(R.string.support_email)
    val telegram = stringResource(R.string.support_personal_telegram)
    val links = buildList {
        val handle = SupportDestinations.INSTAGRAM_HANDLE
        val instagramUrl = SupportDestinations.instagramUrl(handle)
        add(ContactLink(SettingsR.drawable.ic_instagram, instagram, "@$handle", instagramUrl, instagramUrl))
        SupportDestinations.EMAILS.forEach { address ->
            val mailto = SupportDestinations.mailtoUri(address)
            add(ContactLink(SettingsR.drawable.ic_mail, email, address, mailto, address))
        }
        SupportDestinations.PERSONAL_TELEGRAM_USERNAME?.let { username ->
            val telegramUrl = SupportDestinations.telegramUrl(username)
            add(ContactLink(SettingsR.drawable.ic_telegram, telegram, "@$username", telegramUrl, telegramUrl))
        }
    }

    Column {
        SettingsSectionDivider(text = stringResource(R.string.support_personal_contact))
        Spacer(Modifier.height(12.dp))
        links.forEach { link ->
            ContactTile(link, actions)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ContactTile(link: ContactLink, actions: SupportScreenActions) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(UI.shapes.r4)
            .background(UI.colors.medium, UI.shapes.r4)
            .contactClickable(
                description = stringResource(R.string.support_contact_description, link.label, link.value),
                onClick = { actions.onOpenLink(link.uri, link.copyValue) },
                onCopy = { actions.onCopy(link.copyValue) },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Primary text colour on cards is held at 4.5:1 by the palette, so it is used for all text here.
        Icon(
            painter = painterResource(link.icon),
            contentDescription = null,
            tint = UI.colors.pureInverse,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = link.label,
            style = UI.typo.c.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = link.value,
            style = UI.typo.nC.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Normal),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
