package com.ivy.settings.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.AppearancePreset
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.ColorGroup
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernPalette
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.colors
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.get
import com.ivy.design.system.appearance.overrides
import com.ivy.design.system.appearance.resetGroup
import com.ivy.design.system.appearance.toHex
import com.ivy.design.system.appearance.withColor
import com.ivy.design.system.appearance.withPreset
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.pressScale
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.utils.rememberInteractionSource
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
        actions = remember(viewModel, nav) {
            AppearanceActions(
                onBack = { nav.onBackPressed() },
                onTheme = viewModel::setTheme,
                onUpdate = viewModel::update,
                onOpenCategories = { nav.navigateTo(CategoriesScreen) },
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
            .verticalScroll(rememberScrollState())
    ) {
        IvyToolbar(onBack = actions.onBack)
        AppearanceHeader()
        AppearancePreview(palette)

        SectionTitle(R.string.appearance_theme)
        ThemeModeRow(theme, actions.onTheme)

        SectionTitle(R.string.appearance_presets)
        PresetRow(
            selected = appearance.preset,
            hasCustomColours = appearance.lightOverrides.isNotEmpty() || appearance.darkOverrides.isNotEmpty(),
            dark = dark,
        ) { preset -> actions.onUpdate { it.withPreset(preset) } }

        Text(
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 4.dp),
            text = stringResource(if (dark) R.string.appearance_editing_dark else R.string.appearance_editing_light),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
        )

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
        ResetAllDialog(
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

@Composable
private fun ResetAllDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.appearance_reset_all_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.appearance_reset_group)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.appearance_cancel)) }
        },
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
private fun ThemeModeRow(selected: Theme, onSelect: (Theme) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CircleShape)
            .background(UI.colors.medium)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ThemeModes.forEach { (theme, label) ->
            val isSelected = theme == selected
            val accent = FernTheme.palette[ColorToken.Accent]
            val background by animateColorAsState(
                if (isSelected) accent else Color.Transparent,
                IvyMotion.standard(IvyMotion.Duration.Short),
                label = "theme_mode_bg",
            )
            val interactionSource = rememberInteractionSource()
            Text(
                modifier = Modifier
                    .weight(1f)
                    .pressScale(interactionSource)
                    .clip(CircleShape)
                    .background(background)
                    .selectable(
                        selected = isSelected,
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.RadioButton,
                    ) { onSelect(theme) }
                    .padding(vertical = 10.dp),
                text = stringResource(label),
                style = UI.typo.c.style(
                    color = if (isSelected) accent.contentColor() else UI.colors.pureInverse,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
            )
        }
    }
}

private val ThemeModes = listOf(
    Theme.AUTO to R.string.appearance_mode_system,
    Theme.LIGHT to R.string.appearance_mode_light,
    Theme.DARK to R.string.appearance_mode_dark,
    Theme.AMOLED_DARK to R.string.appearance_mode_amoled,
)

@Composable
private fun PresetRow(
    selected: AppearancePreset,
    hasCustomColours: Boolean,
    dark: Boolean,
    onSelect: (AppearancePreset) -> Unit,
) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppearancePreset.entries.forEach { preset ->
            PresetCard(
                preset = preset,
                colors = preset.colors(dark),
                selected = preset == selected,
                showCustom = preset == selected && hasCustomColours,
            ) { onSelect(preset) }
        }
    }
}

@Composable
private fun PresetCard(
    preset: AppearancePreset,
    colors: Map<ColorToken, Color>,
    selected: Boolean,
    showCustom: Boolean,
    onClick: () -> Unit,
) {
    val accent = FernTheme.palette[ColorToken.Accent]
    val border by animateColorAsState(
        if (selected) accent else UI.colors.medium,
        IvyMotion.standard(IvyMotion.Duration.Short),
        label = "preset_border",
    )
    val interactionSource = rememberInteractionSource()
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .width(PresetWidth)
            .pressScale(interactionSource)
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(8.dp)
    ) {
        // The preset in miniature: background, a card, and its key colours as dots.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.getValue(ColorToken.Background))
                .padding(8.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.getValue(ColorToken.Card))
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(ColorToken.Accent, ColorToken.Income, ColorToken.Expense, ColorToken.Transfer).forEach {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(colors.getValue(it)))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(preset.nameRes()),
            style = UI.typo.c.style(fontWeight = FontWeight.Bold),
        )
        if (showCustom) {
            Text(
                text = stringResource(R.string.appearance_custom),
                style = UI.typo.c.style(color = accent, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun GroupSection(
    group: ColorGroup,
    palette: FernPalette,
    overrides: Map<ColorToken, Color>,
    onTokenClick: (ColorToken) -> Unit,
    onReset: () -> Unit,
) {
    var expanded by rememberSaveable(group.name) { mutableStateOf(false) }
    val tokens = remember(group) { ColorToken.entries.filter { it.group == group } }
    val customised = tokens.count { it in overrides }
    val chevron by animateFloatAsState(
        if (expanded) ChevronExpandedDegrees else 0f,
        IvyMotion.standard(IvyMotion.Duration.Medium),
        label = "group_chevron",
    )
    val expandedLabel = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(UI.colors.medium)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics { stateDescription = expandedLabel }
                .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupColorDots(tokens.take(MaxGroupDots).map { palette[it] })
            Spacer(Modifier.width(14.dp))
            GroupTitle(group, customised, palette[ColorToken.Accent], Modifier.weight(1f))
            if (customised > 0) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.appearance_reset_group))
                }
            }
            Icon(
                modifier = Modifier.graphicsLayer { rotationZ = chevron },
                painter = painterResource(R.drawable.ic_expandarrow),
                contentDescription = null,
                tint = UI.colors.pureInverse,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = IvyMotion.expandSection(),
            exit = IvyMotion.collapseSection(),
        ) {
            Column(Modifier.padding(bottom = 8.dp)) {
                tokens.forEach { token ->
                    TokenRow(token, palette[token], customised = token in overrides) { onTokenClick(token) }
                }
            }
        }
    }
}

// What's in the group at a glance, without opening it.
@Composable
private fun GroupColorDots(colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(-GroupDotOverlap)) {
        colors.forEach { color ->
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.5.dp, UI.colors.medium, CircleShape)
            )
        }
    }
}

@Composable
private fun GroupTitle(group: ColorGroup, customised: Int, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(text = stringResource(group.nameRes()), style = UI.typo.b2.style(fontWeight = FontWeight.Bold))
        Text(
            text = if (customised > 0) {
                stringResource(R.string.appearance_customized_count, customised)
            } else {
                stringResource(group.descriptionRes())
            },
            style = UI.typo.c.style(
                color = if (customised > 0) accent else UI.colors.gray,
                fontWeight = FontWeight.Medium
            ),
        )
    }
}

@Composable
private fun TokenRow(token: ColorToken, color: Color, customised: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val swatch by animateColorAsState(color, IvyMotion.standard(), label = "token_swatch")
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(swatch)
                .border(1.dp, UI.colors.gray.copy(alpha = 0.4f), CircleShape)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(token.nameRes()), style = UI.typo.b2.style(fontWeight = FontWeight.SemiBold))
            Text(
                text = stringResource(token.descriptionRes()),
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "#${color.toHex()}", style = UI.typo.nC.style(fontWeight = FontWeight.Bold))
            if (customised) {
                Text(
                    text = stringResource(R.string.appearance_custom),
                    style = UI.typo.c.style(color = FernTheme.palette[ColorToken.Accent], fontWeight = FontWeight.Bold),
                )
            }
        }
    }
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

private val PresetWidth = 112.dp
private const val MaxGroupDots = 4
private const val ChevronExpandedDegrees = 180f
private val GroupDotOverlap = 6.dp
