package com.ivy.settings.support

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.links.SupportDestinations
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.readableOn
import com.ivy.design.utils.thenIf
import com.ivy.settings.SettingsSectionDivider
import com.ivy.ui.R

/** How much a link row stands out from the plain rows around it. */
internal enum class LinkEmphasis {
    None,

    /** Accent-tinted fill and accent border. */
    Filled,

    /** Accent border on the normal row colour: visibly different from [Filled]. */
    Outlined,
}

/**
 * A Settings-style link row: same shape, colours and spacing as the rows in Settings.
 * [external] rows leave the app (external-link icon); the others open a screen (chevron).
 */
@Composable
internal fun SupportDestinationLinkRow(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    external: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: LinkEmphasis = LinkEmphasis.None,
) {
    // The user's accent, nudged just enough to stay 3:1 against the row in light and dark themes.
    val accent = UI.colors.primary.readableOn(UI.colors.medium, minContrast = 3f)
    val container = if (emphasis == LinkEmphasis.Filled) {
        accent.copy(alpha = 0.14f).compositeOver(UI.colors.medium)
    } else {
        UI.colors.medium
    }
    val emphasised = emphasis != LinkEmphasis.None
    val descriptionColor = if (emphasis == LinkEmphasis.Filled) {
        UI.colors.pureInverse.copy(alpha = 0.72f)
    } else {
        UI.colors.gray
    }
    val shape = UI.shapes.r4
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(container, shape)
            .thenIf(emphasised) { border(2.dp, accent, shape) }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (emphasised) accent else UI.colors.pureInverse,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold),
            )
            Text(
                text = description,
                // Grey on the accent tint is too faint, so filled rows use a softened main text colour.
                style = UI.typo.nB2.style(color = descriptionColor, fontWeight = FontWeight.Normal)
                    .copy(fontSize = 14.sp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = if (external) {
                rememberVectorPainter(Icons.AutoMirrored.Filled.OpenInNew)
            } else {
                painterResource(R.drawable.ic_arrow_right)
            },
            // Read with the row, so screen-reader users know the link leaves the app.
            contentDescription = if (external) stringResource(R.string.support_opens_in_another_app) else null,
            tint = if (emphasised) accent else UI.colors.gray,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun PaymentOptionsSection(actions: SupportScreenActions) {
    Column {
        SettingsSectionDivider(text = stringResource(R.string.support_payment_options), topSpacing = 16.dp)
        Spacer(Modifier.height(8.dp))

        UpiCard(actions)

        Spacer(Modifier.height(12.dp))

        SupportDestinationLinkRow(
            icon = R.drawable.ic_vue_money_card,
            title = stringResource(R.string.support_more_ways_to_pay),
            description = stringResource(R.string.support_more_ways_to_pay_description),
            external = true,
            onClick = { actions.onOpenUrl(SupportDestinations.MORE_WAYS_TO_PAY) },
            emphasis = LinkEmphasis.Outlined,
        )
    }
}
