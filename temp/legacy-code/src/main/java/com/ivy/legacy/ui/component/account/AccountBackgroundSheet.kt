package com.ivy.legacy.ui.component.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.data.model.AccountAppearance
import com.ivy.data.model.AccountBackgroundStyle
import com.ivy.design.system.appearance.scrimAlphaForWhiteText
import com.ivy.design.system.appearance.toHex
import com.ivy.ui.R

/**
 * Picks an account card's background: None, Solid (the account colour), Gradient (a preset or
 * two custom colours plus an angle) or Image (with a dim slider), over a live preview of the card.
 * Changes apply as they're made; the account is saved by the editor's Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBackgroundSheet(
    appearance: AccountAppearance,
    accountName: String,
    accountColor: Color,
    surface: Color,
    onChange: (AccountAppearance) -> Unit,
    onPickImage: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(text = stringResource(R.string.account_background), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            CardPreview(appearance, accountName, accountColor, surface)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountBackgroundStyle.entries.forEach { style ->
                    FilterChip(
                        selected = appearance.backgroundStyle == style,
                        onClick = { onChange(appearance.withStyle(style, accountColor)) },
                        label = { Text(stringResource(style.label())) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            when (appearance.backgroundStyle) {
                AccountBackgroundStyle.Gradient -> GradientControls(appearance, onChange)
                AccountBackgroundStyle.Image -> ImageControls(appearance, onChange, onPickImage)
                AccountBackgroundStyle.None, AccountBackgroundStyle.Solid -> Unit
            }
        }
    }
}

@Composable
private fun CardPreview(appearance: AccountAppearance, name: String, color: Color, surface: Color) {
    val paint = remember(appearance, color, surface) { accountPaint(appearance, color, surface) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PreviewHeight)
            .clip(RoundedCornerShape(16.dp))
            .accountBackground(paint)
            .padding(horizontal = 20.dp)
            .testTag("account_background_preview"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountMark(
            appearance = appearance,
            name = name,
            color = color,
            iconName = null,
            tint = paint.ink,
            defaultIcon = R.drawable.ic_custom_account_s,
            size = 28.dp,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text = name.ifBlank { " " }, color = paint.ink, fontWeight = FontWeight.ExtraBold)
            Text(text = PreviewBalance, color = paint.ink, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun GradientControls(appearance: AccountAppearance, onChange: (AccountAppearance) -> Unit) {
    Text(stringResource(R.string.account_background_presets), style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(GradientPresets, key = { (a, b) -> "$a-$b" }) { (start, end) ->
            val stops = listOf(start, end)
            val selected = appearance.backgroundColors == stops
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(stops.map(::Color)))
                    .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .selectable(selected = selected, role = Role.RadioButton) {
                        onChange(appearance.copy(backgroundColors = stops))
                    }
                    .semantics { contentDescription = "${Color(start).toHex()} → ${Color(end).toHex()}" }
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    val colors = appearance.backgroundColors.takeIf { it.size >= 2 } ?: GradientPresets.first().toList()
    SwatchRow(R.string.account_background_start, colors[0]) {
        onChange(appearance.copy(backgroundColors = listOf(it, colors[1])))
    }
    SwatchRow(R.string.account_background_end, colors[1]) {
        onChange(appearance.copy(backgroundColors = listOf(colors[0], it)))
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.account_background_angle, appearance.gradientAngle.toInt()),
        style = MaterialTheme.typography.titleSmall,
    )
    Slider(
        value = appearance.gradientAngle,
        onValueChange = { onChange(appearance.copy(gradientAngle = it)) },
        valueRange = 0f..MaxAngle,
        steps = AngleSteps,
    )
}

@Composable
private fun SwatchRow(label: Int, selected: Int, onPick: (Int) -> Unit) {
    Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
        items(Swatches, key = { it }) { argb ->
            val isSelected = argb == selected
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(if (isSelected) 3.dp else 1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onPick(argb) }
                    .semantics { contentDescription = Color(argb).toHex() }
            )
        }
    }
}

@Composable
private fun ImageControls(
    appearance: AccountAppearance,
    onChange: (AccountAppearance) -> Unit,
    onPickImage: (() -> Unit)?,
) {
    val hasImage = appearance.backgroundImagePath != null
    if (onPickImage != null) {
        if (hasImage) {
            OutlinedButton(onClick = onPickImage) { Text(stringResource(R.string.account_background_replace_image)) }
        } else {
            Button(onClick = onPickImage) { Text(stringResource(R.string.account_background_choose_image)) }
        }
    }
    Spacer(Modifier.height(12.dp))
    // The least dim keeps white text at 4.5:1 even over a white photo; it can only go darker.
    val minDim = remember { scrimAlphaForWhiteText(listOf(Color.White)) }
    Text(stringResource(R.string.account_background_dim), style = MaterialTheme.typography.titleSmall)
    Slider(
        value = appearance.backgroundDim.coerceIn(minDim, MaxDim),
        onValueChange = { onChange(appearance.copy(backgroundDim = it)) },
        valueRange = minDim..MaxDim,
    )
}

private fun AccountAppearance.withStyle(style: AccountBackgroundStyle, accountColor: Color): AccountAppearance =
    copy(
        backgroundStyle = style,
        backgroundColors = if (style == AccountBackgroundStyle.Gradient && backgroundColors.size < 2) {
            listOf(accountColor.toArgb(), GradientPresets.first().second)
        } else {
            backgroundColors
        },
    )

private fun AccountBackgroundStyle.label(): Int = when (this) {
    AccountBackgroundStyle.None -> R.string.account_background_none
    AccountBackgroundStyle.Solid -> R.string.account_background_solid
    AccountBackgroundStyle.Gradient -> R.string.account_background_gradient
    AccountBackgroundStyle.Image -> R.string.account_background_image
}

// Curated pairs (ARGB): pink to blue first, the look of a transfer between two cards.
private val GradientPresets: List<Pair<Int, Int>> = listOf(
    0xFFE91E63 to 0xFF3F51B5,
    0xFF13876A to 0xFF0B3D91,
    0xFFFF8A00 to 0xFFE52E71,
    0xFF7F00FF to 0xFFE100FF,
    0xFF00C6FF to 0xFF0072FF,
    0xFF11998E to 0xFF38EF7D,
    0xFF232526 to 0xFF414345,
    0xFFF7971E to 0xFFFFD200,
).map { (a, b) -> a.toInt() to b.toInt() }

private val Swatches: List<Int> = listOf(
    0xFFE91E63, 0xFFF44336, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF13876A,
    0xFF009688, 0xFF00BCD4, 0xFF2196F3, 0xFF3F51B5, 0xFF673AB7, 0xFF9C27B0,
    0xFF795548, 0xFF607D8B, 0xFF212121, 0xFFFAFAFA,
).map { it.toInt() }

private val PreviewHeight = 96.dp
private const val PreviewBalance = "12,480.00"
private const val MaxAngle = 315f
private const val AngleSteps = 6
private const val MaxDim = 0.9f
