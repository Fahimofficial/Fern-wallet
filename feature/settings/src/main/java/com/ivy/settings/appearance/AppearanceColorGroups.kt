package com.ivy.settings.appearance

import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.ColorGroup
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernPalette
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.colors
import com.ivy.design.system.appearance.get
import com.ivy.design.system.appearance.overrides
import com.ivy.design.system.appearance.toHex
import com.ivy.design.system.motion.IvyMotion
import com.ivy.ui.R

@Composable
internal fun GroupSection(
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
            AnimatedVisibility(
                visible = customised > 0,
                enter = fadeIn(IvyMotion.standard(IvyMotion.Duration.Medium)),
                exit = fadeOut(IvyMotion.standard(IvyMotion.Duration.Short)),
            ) {
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

private const val MaxGroupDots = 4

private const val ChevronExpandedDegrees = 180f

private val GroupDotOverlap = 6.dp
