package com.ivy.settings.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.AppearancePreset
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.colors
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.get
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.pressScale
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.ui.R

@Composable
internal fun ThemeModeRow(selected: Theme, onSelect: (Theme) -> Unit) {
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
internal fun PresetRow(
    appearance: AppearanceSettings,
    dark: Boolean,
    onSelect: (AppearancePreset) -> Unit,
) {
    val selected = appearance.preset
    val hasCustomColours = appearance.lightOverrides.isNotEmpty() || appearance.darkOverrides.isNotEmpty()
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

private val PresetWidth = 112.dp
