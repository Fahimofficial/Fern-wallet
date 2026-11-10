package com.ivy.settings.support

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.ivy.legacy.rootScreen
import com.ivy.navigation.SupportScreen
import com.ivy.navigation.navigation
import com.ivy.settings.SettingsSectionDivider
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyToolbar
import com.ivy.settings.R as SettingsR

/** Test tag of the "Support My Work" row on Help & Support. */
const val SupportMyWorkRowTag = "help_support_my_work"

/**
 * Help & Support: getting help comes first, then the project, the optional donation screen and
 * the developer's contact page. Help never goes through the payment options.
 */
@Composable
fun HelpSupportScreenImpl() {
    val rootScreen = rootScreen()
    HelpSupportUi(onOpenUrl = rootScreen::openUrlInBrowser)
}

/** [onOpenUrl] opens an external https link; in-app navigation uses the current [navigation]. */
@Composable
internal fun HelpSupportUi(onOpenUrl: (String) -> Unit) {
    val nav = navigation()
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        IvyToolbar(onBack = { nav.onBackPressed() })

        Text(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .semantics { heading() },
            text = stringResource(R.string.help_support),
            style = UI.typo.h2.style(fontWeight = FontWeight.Black),
        )

        HelpSection(R.string.help_get_help) {
            SupportDestinationLinkRow(
                icon = SettingsR.drawable.ic_telegram,
                title = stringResource(R.string.help_telegram_community),
                description = stringResource(R.string.help_telegram_community_description),
                external = true,
                onClick = { onOpenUrl(SupportDestinations.TELEGRAM_COMMUNITY) },
            )
            SupportDestinationLinkRow(
                icon = R.drawable.github_logo,
                title = stringResource(R.string.report_bug),
                description = stringResource(R.string.help_report_bug_description),
                external = true,
                onClick = { onOpenUrl(SupportDestinations.REPORT_BUG) },
            )
        }

        HelpSection(R.string.help_project) {
            SupportDestinationLinkRow(
                icon = R.drawable.ic_vue_location_global,
                title = stringResource(R.string.help_project_website),
                description = stringResource(R.string.help_project_website_description),
                external = true,
                onClick = { onOpenUrl(SupportDestinations.PROJECT_WEBSITE) },
            )
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(R.string.support_project_info),
                style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal),
            )
        }

        HelpSection(R.string.help_support_development) {
            SupportDestinationLinkRow(
                icon = R.drawable.ic_vue_support_heart,
                title = stringResource(R.string.support_my_work),
                description = stringResource(R.string.support_my_work_entry_description),
                external = false,
                onClick = { nav.navigateTo(SupportScreen) },
                modifier = Modifier.testTag(SupportMyWorkRowTag),
            )
        }

        HelpSection(R.string.help_developer) {
            SupportDestinationLinkRow(
                icon = R.drawable.ic_vue_people_person,
                title = stringResource(R.string.help_contact_developer),
                description = stringResource(R.string.help_contact_developer_description),
                external = true,
                onClick = { onOpenUrl(SupportDestinations.CONTACT_DEVELOPER) },
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun HelpSection(@StringRes title: Int, content: @Composable () -> Unit) {
    Column {
        SettingsSectionDivider(text = stringResource(title))
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

/** For screenshot testing */
@Composable
fun HelpSupportScreenUiTest(isDark: Boolean) {
    IvyWalletPreview(theme = if (isDark) Theme.DARK else Theme.LIGHT) {
        HelpSupportUi(onOpenUrl = {})
    }
}
