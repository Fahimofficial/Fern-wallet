package com.ivy.settings.appearance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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

/**
 * System / Light / Dark / AMOLED as a segmented control. One accent thumb springs to the chosen
 * mode (moved in the layout phase, so sliding never recomposes the row); each mode has an icon.
 */
@Composable
internal fun ThemeModeRow(selected: Theme, onSelect: (Theme) -> Unit) {
    val selectedIndex = ThemeModes.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val thumb by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = IvyMotion.Springs.bouncy(),
        label = "theme_thumb",
    )
    val accent = FernTheme.palette[ColorToken.Accent]
    Column {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(CircleShape)
                .background(UI.colors.medium)
                .padding(4.dp)
        ) {
            val segment = maxWidth / ThemeModes.size
            Box(
                Modifier
                    .offset { IntOffset(x = (segment.toPx() * thumb).roundToInt(), y = 0) }
                    .width(segment)
                    .height(ThemeChipHeight)
                    .clip(CircleShape)
                    .background(accent)
            )
            Row(Modifier.fillMaxWidth()) {
                ThemeModes.forEachIndexed { index, (theme, label, icon) ->
                    val isSelected = index == selectedIndex
                    val content by animateColorAsState(
                        if (isSelected) accent.contentColor() else UI.colors.pureInverse,
                        IvyMotion.standard(IvyMotion.Duration.Short),
                        label = "theme_mode_content",
                    )
                    val interactionSource = rememberInteractionSource()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(ThemeChipHeight)
                            .pressScale(interactionSource)
                            .clip(CircleShape)
                            .selectable(
                                selected = isSelected,
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                role = Role.RadioButton,
                            ) { onSelect(theme) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = content,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(label),
                            style = UI.typo.c.style(color = content, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Text(
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
            text = stringResource(R.string.appearance_amoled_hint),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
        )
    }
}

private val ThemeChipHeight = 48.dp

private val ThemeModes = listOf(
    Triple(Theme.AUTO, R.string.appearance_mode_system, R.drawable.home_more_menu_auto_mode),
    Triple(Theme.LIGHT, R.string.appearance_mode_light, R.drawable.ic_vue_weather_sun),
    Triple(Theme.DARK, R.string.appearance_mode_dark, R.drawable.ic_vue_weather_moon),
    Triple(Theme.AMOLED_DARK, R.string.appearance_mode_amoled, R.drawable.home_more_menu_amoled_dark_mode),
)

@Composable
internal fun PresetRow(
    appearance: AppearanceSettings,
    dark: Boolean,
    onSelect: (AppearancePreset) -> Unit,
) {
    val selected = appearance.preset
    val hasCustomColours = appearance.lightOverrides.isNotEmpty() || appearance.darkOverrides.isNotEmpty()
    val fadeColor = UI.colors.pure
    Row(
        Modifier
            .fillMaxWidth()
            // The next preset peeks in at the edge and fades out, so the row reads as scrollable.
            .drawWithContent {
                drawContent()
                val fade = PresetEdgeFade.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, fadeColor),
                        startX = size.width - fade,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - fade, 0f),
                    size = Size(fade, size.height),
                )
            }
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 32.dp),
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Always laid out, so every card has the same height whether or not it says "Custom".
        val customAlpha by animateFloatAsState(if (showCustom) 1f else 0f, IvyMotion.standard(), label = "custom_tag")
        Text(
            modifier = Modifier
                .graphicsLayer { alpha = customAlpha }
                .then(if (showCustom) Modifier else Modifier.clearAndSetSemantics { }),
            text = stringResource(R.string.appearance_custom),
            style = UI.typo.c.style(color = accent, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

private val PresetWidth = 112.dp
private val PresetEdgeFade = 32.dp
