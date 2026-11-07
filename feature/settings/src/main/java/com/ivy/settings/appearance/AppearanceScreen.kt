package com.ivy.settings.appearance

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.ivy.design.system.motion.IvyHaptics
import com.ivy.legacy.ui.component.account.LocalAccountColorsInTransactions
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.get
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.ColorGroup
import com.ivy.design.system.appearance.FernPalette
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.colors
import com.ivy.design.system.appearance.overrides
import com.ivy.design.system.appearance.resetGroup
import com.ivy.design.system.appearance.withColor
import com.ivy.design.system.appearance.withPreset
import com.ivy.legacy.IvyWalletPreview
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyToolbar

@Immutable
internal class AppearanceActions(
    val onBack: () -> Unit,
    val onTheme: (Theme) -> Unit,
    val onUpdate: ((AppearanceSettings) -> AppearanceSettings) -> Unit,
    val onOpenCategories: () -> Unit,
    val onAccountColors: (Boolean) -> Unit = {},
)

/** Settings > Appearance: theme mode, presets and every customisable colour, grouped. */
@Composable
fun AppearanceScreenImpl() {
    val viewModel: AppearanceViewModel = screenScopedViewModel()
    val nav = navigation()
    AppearanceUi(
        appearance = viewModel.appearance,
        theme = viewModel.theme,
        palette = FernTheme.palette,
        saving = viewModel.saving,
        actions = remember(viewModel, nav) {
            AppearanceActions(
                onBack = { nav.onBackPressed() },
                onTheme = viewModel::setTheme,
                onUpdate = viewModel::update,
                onOpenCategories = { nav.navigateTo(CategoriesScreen) },
                onAccountColors = viewModel::setAccountColorsInTransactions,
            )
        },
    )
}

@Composable
private fun AppearanceUi(
    appearance: AppearanceSettings,
    theme: Theme,
    palette: FernPalette,
    actions: AppearanceActions,
    saving: Boolean? = null,
) {
    // The colours being edited are those of the theme on screen now.
    val dark = !palette.isLight
    val overrides = appearance.overrides(dark)
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmResetAll by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(UI.colors.pure)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IvyToolbar(onBack = actions.onBack)
        // Pinned: every control below scrolls under it, so a change is always seen in context.
        val accountColors = LocalAccountColorsInTransactions.current
        AppearancePreview(palette, accountColors = accountColors)
        LocalAppearanceSaveStatusIndicator(saving)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppearanceHeader()

        SectionTitle(R.string.appearance_theme)
        ThemeModeRow(theme, actions.onTheme)

        SectionTitle(R.string.appearance_presets)
        PresetRow(appearance, dark) { preset -> actions.onUpdate { it.withPreset(preset) } }

        EditingThemeHint(dark)

        SectionTitle(R.string.appearance_transactions)
        TransactionAccountColorsSwitch(accountColors, actions.onAccountColors)

        ColorGroup.entries.forEach { group ->
            GroupSection(
                group = group,
                palette = palette,
                overrides = overrides,
                onTokenClick = { editing = it.name },
                onReset = { actions.onUpdate { it.resetGroup(dark, group) } },
            )
        }

        AccountsAndCategoriesCard(actions.onOpenCategories)

        ResetAllButton { confirmResetAll = true }
    }
    }

    val editingToken = editing?.let { name -> ColorToken.entries.firstOrNull { it.name == name } }
    if (editingToken != null) {
        ColorPickerDialog(
            token = editingToken,
            palette = palette,
            isCustomised = editingToken in overrides,
            onApply = { color ->
                actions.onUpdate { it.withColor(dark, editingToken, color) }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    if (confirmResetAll) {
        AppearanceResetConfirmationSheet(
            appearance = appearance,
            onConfirm = {
                actions.onUpdate { AppearanceSettings.Default }
                confirmResetAll = false
            },
            onDismiss = { confirmResetAll = false },
        )
    }
}

@Composable
private fun AppearanceHeader() {
    Column {
        Text(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .semantics { heading() },
            text = stringResource(R.string.appearance),
            style = UI.typo.h2.style(fontWeight = FontWeight.Black),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.appearance_intro),
            style = UI.typo.b2.style(color = UI.colors.gray, fontWeight = FontWeight.Medium).copy(lineHeight = 22.sp),
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ResetAllButton(onClick: () -> Unit) {
    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 48.dp),
        onClick = onClick,
    ) {
        Text(
            text = stringResource(R.string.appearance_reset_all),
            style = UI.typo.b2.style(color = UI.colors.red, fontWeight = FontWeight.Bold),
        )
    }
}

/** Confirms which appearance settings will be reset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceResetConfirmationSheet(
    appearance: AppearanceSettings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = UI.colors.pure) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(R.string.appearance_reset_all_title),
                style = UI.typo.b1.style(fontWeight = FontWeight.ExtraBold),
            )
            Spacer(Modifier.height(12.dp))
            val lines = listOf(
                stringResource(R.string.appearance_reset_summary_preset, stringResource(appearance.preset.nameRes())),
                stringResource(R.string.appearance_reset_summary_light, appearance.lightOverrides.size),
                stringResource(R.string.appearance_reset_summary_dark, appearance.darkOverrides.size),
                stringResource(R.string.appearance_reset_summary_theme),
            )
            lines.forEach { line ->
                Text(
                    modifier = Modifier.padding(vertical = 4.dp),
                    text = "• $line",
                    style = UI.typo.b2.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
                    Text(stringResource(R.string.appearance_cancel))
                }
                TextButton(modifier = Modifier.weight(1f), onClick = onConfirm) {
                    Text(
                        text = stringResource(R.string.appearance_reset_all),
                        color = UI.colors.red,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Shows whether changed appearance settings are still saving or saved locally. */
@Composable
private fun LocalAppearanceSaveStatusIndicator(saving: Boolean?) {
    AnimatedVisibility(visible = saving != null, enter = fadeIn(), exit = fadeOut()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(targetState = saving == true, label = "save_state") { inProgress ->
                if (inProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = UI.colors.gray,
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = FernTheme.palette[ColorToken.Income],
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(if (saving == true) R.string.appearance_saving else R.string.appearance_saved),
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
            )
        }
    }
}

/** Toggles account-specific colours in transaction rows. */
@Composable
private fun TransactionAccountColorsSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(UI.colors.medium)
            .toggleable(value = checked, role = Role.Switch) {
                IvyHaptics.toggle(view, it)
                onChange(it)
            }
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("appearance_account_colors"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.appearance_account_colors),
                style = UI.typo.b2.style(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.appearance_account_colors_description),
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
            )
        }
        Spacer(Modifier.width(12.dp))
        // The row is the toggle; the switch only shows its state.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun EditingThemeHint(dark: Boolean) {
    Text(
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 4.dp),
        text = stringResource(if (dark) R.string.appearance_editing_dark else R.string.appearance_editing_light),
        style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
    )
}

@Composable
private fun SectionTitle(text: Int) {
    Text(
        modifier = Modifier
            .padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 12.dp)
            .semantics { heading() },
        text = stringResource(text),
        style = UI.typo.b1.style(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun AccountsAndCategoriesCard(onOpenCategories: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(UI.colors.medium)
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.appearance_accounts_categories),
            style = UI.typo.b2.style(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.appearance_accounts_categories_description),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
        )
        TextButton(onClick = onOpenCategories) { Text(stringResource(R.string.appearance_open_categories)) }
    }
}

/** For screenshot testing */
@Composable
fun AppearanceScreenUiTest(isDark: Boolean) {
    IvyWalletPreview(theme = if (isDark) Theme.DARK else Theme.LIGHT) {
        AppearanceUi(
            appearance = AppearanceSettings.Default,
            theme = if (isDark) Theme.DARK else Theme.LIGHT,
            palette = FernTheme.palette,
            actions = AppearanceActions(onBack = {}, onTheme = {}, onUpdate = {}, onOpenCategories = {}),
        )
    }
}
