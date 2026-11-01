package com.ivy.settings.support

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.links.SupportDestinations
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.settings.SettingsSectionDivider
import com.ivy.ui.R
import com.ivy.settings.R as SettingsR

private val KofiBrandRed = Color(0xFFFF5E5B)

/** A Settings-style link row: same shape, colours and spacing as the rows in Settings. */
@Composable
internal fun SupportDestinationLinkRow(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    iconTint: Color = UI.colors.pureInverse,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(UI.colors.medium, UI.shapes.r4)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
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
                style = UI.typo.nB2.style(color = UI.colors.gray, fontWeight = FontWeight.Normal)
                    .copy(fontSize = 14.sp),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = UI.colors.gray,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun String.withoutScheme(): String = removePrefix("https://").removeSuffix("/")

@Composable
internal fun PaymentOptionsSection(actions: SupportScreenActions) {
    Column {
        SettingsSectionDivider(text = stringResource(R.string.support_payment_options))
        Spacer(Modifier.height(16.dp))

        SupportDestinationLinkRow(
            icon = SettingsR.drawable.ic_kofi,
            iconTint = KofiBrandRed,
            title = stringResource(R.string.support_kofi),
            description = SupportDestinations.KOFI_URL.withoutScheme(),
            onClick = { actions.onOpenUrl(SupportDestinations.KOFI_URL) },
        )

        Spacer(Modifier.height(12.dp))

        UpiCard(actions)
    }
}

@Composable
internal fun ProjectContactSection(actions: SupportScreenActions) {
    Column {
        SettingsSectionDivider(text = stringResource(R.string.support_project_and_contact))
        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.support_project_info),
            style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal),
        )

        Spacer(Modifier.height(16.dp))

        ProjectLink(
            icon = R.drawable.github_logo,
            title = stringResource(R.string.support_app_repository),
            url = SupportDestinations.APP_REPOSITORY,
            onOpenUrl = actions.onOpenUrl,
        )
        ProjectLink(
            icon = R.drawable.ic_import_web,
            title = stringResource(R.string.support_website),
            url = SupportDestinations.WEBSITE,
            onOpenUrl = actions.onOpenUrl,
        )
        ProjectLink(
            icon = R.drawable.github_logo,
            title = stringResource(R.string.support_github_profile),
            url = SupportDestinations.GITHUB_PROFILE,
            onOpenUrl = actions.onOpenUrl,
        )
    }
}

@Composable
private fun ProjectLink(
    @DrawableRes icon: Int,
    title: String,
    url: String,
    onOpenUrl: (String) -> Unit,
) {
    Column {
        SupportDestinationLinkRow(
            icon = icon,
            title = title,
            description = url.withoutScheme(),
            onClick = { onOpenUrl(url) },
        )
        Spacer(Modifier.height(12.dp))
    }
}
