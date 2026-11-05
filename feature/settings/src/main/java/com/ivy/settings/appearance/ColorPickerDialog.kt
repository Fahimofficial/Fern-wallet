package com.ivy.settings.appearance

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ivy.design.IVY_COLOR_PICKER_COLORS_FREE
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.ContrastCheck
import com.ivy.design.system.appearance.FernPalette
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.contrastChecks
import com.ivy.design.system.appearance.get
import com.ivy.design.system.appearance.parseHexColor
import com.ivy.design.system.appearance.passes
import com.ivy.design.system.appearance.toHex
import com.ivy.ui.R
import kotlinx.collections.immutable.persistentListOf
import java.util.Locale

/**
 * Picks a colour for [token]: swatches, HSV sliders or HEX. Shows how the colour reads next to
 * the colours around it, and won't apply a colour that would make text or icons unreadable.
 *
 * @param onApply null means "back to the preset's colour".
 */
@Composable
internal fun ColorPickerDialog(
    token: ColorToken,
    palette: FernPalette,
    isCustomised: Boolean,
    onApply: (Color?) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = palette[token]
    var hsv by remember { mutableStateOf(initial.toHsv()) }
    var hexText by remember { mutableStateOf(initial.toHex()) }
    val candidate = parseHexColor(hexText)
    val checks = candidate?.let { palette.contrastChecks(token, it) }.orEmpty()
    val readable = checks.all { it.passes }

    fun pick(color: Color) {
        hsv = color.toHsv()
        hexText = color.toHex()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(token.nameRes())) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(token.descriptionRes()),
                    style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Medium),
                )
                Spacer(Modifier.height(12.dp))
                ColorPreview(palette, candidate ?: initial, checks)
                Spacer(Modifier.height(12.dp))
                Swatches(selected = candidate, onPick = ::pick)
                Spacer(Modifier.height(8.dp))
                HsvSliders(hsv) {
                    hsv = it
                    hexText = it.toColor().toHex()
                }
                HexField(
                    text = hexText,
                    error = when {
                        candidate == null -> R.string.appearance_hex_invalid
                        !readable -> R.string.appearance_unreadable
                        else -> null
                    },
                ) { text ->
                    hexText = sanitizeHexInput(text)
                    // Typing must not fight the user: only sync sliders once the value is valid.
                    parseHexColor(hexText)?.let { hsv = it.toHsv() }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = candidate != null && readable,
                onClick = { onApply(candidate) },
            ) { Text(stringResource(R.string.appearance_apply)) }
        },
        dismissButton = {
            Row {
                if (isCustomised) {
                    TextButton(onClick = { onApply(null) }) {
                        Text(stringResource(R.string.appearance_use_preset_colour))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.appearance_cancel)) }
            }
        },
    )
}

@Composable
private fun HsvSliders(hsv: Hsv, onChange: (Hsv) -> Unit) {
    Column {
        HsvSlider(R.string.appearance_hue, hsv.hue, 0f..MaxHue) { onChange(hsv.copy(hue = it)) }
        HsvSlider(R.string.appearance_saturation, hsv.saturation, 0f..1f) { onChange(hsv.copy(saturation = it)) }
        HsvSlider(R.string.appearance_brightness, hsv.value, 0f..1f) { onChange(hsv.copy(value = it)) }
    }
}

@Composable
private fun HexField(text: String, error: Int?, onTextChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = onTextChange,
        prefix = { Text("#") },
        label = { Text(stringResource(R.string.appearance_hex)) },
        singleLine = true,
        isError = error == R.string.appearance_hex_invalid,
        supportingText = error?.let { res -> { Text(stringResource(res)) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
        ),
    )
}

/** What the HEX field keeps of what was typed or pasted: up to 6 hex digits, upper case. */
internal fun sanitizeHexInput(text: String): String =
    text.filter { it in HexInputChars }.take(HexDigits).uppercase(Locale.ROOT)

// The colour itself, then a chip per neighbouring colour showing real text over it.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPreview(palette: FernPalette, color: Color, checks: List<ContrastCheck>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PreviewChip(background = color, foreground = color.contentColor(), label = "#${color.toHex()}")
        checks.forEach { check ->
            val other = palette[check.against]
            // The picked colour is text on a surface, or a surface under the other colour.
            val pickedIsForeground = check.against.isSurfaceLike()
            val ratio = String.format(Locale.ROOT, "%.1f", check.ratio)
            PreviewChip(
                background = if (pickedIsForeground) other else color,
                foreground = if (pickedIsForeground) color else other,
                label = if (check.passes) {
                    stringResource(R.string.appearance_contrast_ok, stringResource(check.against.nameRes()), ratio)
                } else {
                    stringResource(
                        R.string.appearance_contrast_low,
                        stringResource(check.against.nameRes()),
                        ratio,
                        String.format(Locale.ROOT, "%.1f", check.minimum),
                    )
                },
                warning = !check.passes,
            )
        }
    }
}

private fun ColorToken.isSurfaceLike(): Boolean =
    this == ColorToken.Background || this == ColorToken.Card || this == ColorToken.Elevated || this == ColorToken.NavBar

@Composable
private fun PreviewChip(background: Color, foreground: Color, label: String, warning: Boolean = false) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, if (warning) UI.colors.red else UI.colors.medium, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.appearance_sample_text),
            style = UI.typo.b2.style(color = foreground, fontWeight = FontWeight.Bold),
        )
        Text(text = label, style = UI.typo.c.style(color = foreground, fontWeight = FontWeight.Medium))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(selected: Color?, onPick: (Color) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (Neutrals + IVY_COLOR_PICKER_COLORS_FREE).forEach { color ->
            val isSelected = selected?.toArgb() == color.toArgb()
            val hex = "#${color.toHex()}"
            Box(
                Modifier
                    .size(SwatchSize)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) color.contentColor() else UI.colors.medium,
                        shape = CircleShape,
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton) { onPick(color) }
                    .semantics { contentDescription = hex },
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
}

@Composable
private fun HsvSlider(label: Int, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(text = stringResource(label), style = UI.typo.c.style(fontWeight = FontWeight.Bold))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(),
        )
    }
}

private data class Hsv(val hue: Float, val saturation: Float, val value: Float)

private fun Color.toHsv(): Hsv {
    val out = FloatArray(HsvComponents)
    android.graphics.Color.colorToHSV(toArgb(), out)
    return Hsv(out[0], out[1], out[2])
}

private fun Hsv.toColor(): Color = Color.hsv(hue.coerceIn(0f, MaxHue), saturation, value)

private val Neutrals = persistentListOf(
    Color(0xFFFFFFFF),
    Color(0xFFFAFAFA),
    Color(0xFFEFEEF0),
    Color(0xFF939199),
    Color(0xFF2B2C2D),
    Color(0xFF1C1C1F),
    Color(0xFF111114),
    Color(0xFF000000),
)
private const val HsvComponents = 3
private val SwatchSize = 32.dp
private const val MaxHue = 359.9f
private const val HexDigits = 6
private const val HexInputChars = "0123456789abcdefABCDEF"
