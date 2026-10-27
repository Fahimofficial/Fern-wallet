package com.ivy.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.links.SupportDestinations
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R

/**
 * Callbacks for the Support section. Kept as plain lambdas so the section is a stateless,
 * previewable composable and all Android intent handling stays in the RootScreen.
 */
@Immutable
class SupportActions(
    val onOpenUrl: (String) -> Unit,
    val onPayViaUpi: () -> Unit,
    val onCopyUpiId: () -> Unit,
)

/**
 * "Support the developer" section.
 *
 * The destinations belong to the developer's QuantumMPV project (verified against the
 * QuantumMPV README); the copy states that explicitly so users know whom they are supporting.
 * Paying is always an explicit user action: the UPI link only pre-fills the payee, never an amount.
 */
@Composable
internal fun SupportSection(actions: SupportActions) {
    Column(Modifier.fillMaxWidth()) {
        SettingsSectionDivider(text = stringResource(R.string.support_the_developer))

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(
                R.string.support_the_developer_description,
                SupportDestinations.DEVELOPER_NAME,
                SupportDestinations.PROJECT_NAME,
            ),
            style = UI.typo.nC.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Normal
            )
        )

        Spacer(Modifier.height(16.dp))

        UpiIdCard()

        Spacer(Modifier.height(12.dp))

        SettingsDefaultButton(
            icon = R.drawable.ic_vue_support_heart,
            iconPadding = 6.dp,
            text = stringResource(R.string.support_pay_via_upi),
            description = stringResource(R.string.support_pay_via_upi_description),
            onClick = actions.onPayViaUpi,
        )

        Spacer(Modifier.height(12.dp))

        SettingsDefaultButton(
            icon = R.drawable.ic_copy,
            iconPadding = 8.dp,
            text = stringResource(R.string.support_copy_upi_id),
            onClick = actions.onCopyUpiId,
        )

        Spacer(Modifier.height(12.dp))

        SettingsDefaultButton(
            icon = R.drawable.ic_import_web,
            iconPadding = 6.dp,
            text = stringResource(R.string.support_website, SupportDestinations.PROJECT_NAME),
            description = SupportDestinations.WEBSITE,
            onClick = { actions.onOpenUrl(SupportDestinations.WEBSITE) },
        )

        Spacer(Modifier.height(12.dp))

        SettingsDefaultButton(
            icon = R.drawable.github_logo,
            iconPadding = 10.dp,
            text = stringResource(R.string.support_github, SupportDestinations.PROJECT_NAME),
            description = SupportDestinations.REPOSITORY,
            onClick = { actions.onOpenUrl(SupportDestinations.REPOSITORY) },
        )

        Spacer(Modifier.height(12.dp))

        SettingsDefaultButton(
            icon = R.drawable.ic_vue_dev_arrow,
            iconPadding = 10.dp,
            text = stringResource(R.string.support_issues, SupportDestinations.PROJECT_NAME),
            description = stringResource(R.string.support_issues_description),
            onClick = { actions.onOpenUrl(SupportDestinations.ISSUES) },
        )
    }
}

@Composable
private fun UpiIdCard() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(1.dp, UI.colors.medium, UI.shapes.r4)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.support_upi_id_label, SupportDestinations.DEVELOPER_NAME),
            style = UI.typo.c.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = SupportDestinations.UPI_ID,
            style = UI.typo.nB1.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.support_upi_verify_hint),
            style = UI.typo.nC.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

/** For screenshot testing */
@Composable
fun SupportSectionUiTest(isDark: Boolean, modifier: Modifier = Modifier) {
    IvyWalletPreview(theme = if (isDark) Theme.DARK else Theme.LIGHT) {
        Column(modifier.verticalScroll(rememberScrollState())) {
            SupportSection(SupportActions(onOpenUrl = {}, onPayViaUpi = {}, onCopyUpiId = {}))
        }
    }
}
