package com.ivy.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.get
import com.ivy.ui.R
import java.util.Locale

@Composable
internal fun HsvSliders(hsv: Hsv, onChange: (Hsv) -> Unit) {
    Column {
        HsvSlider(R.string.appearance_hue, hsv.hue, 0f..MaxHue) { onChange(hsv.copy(hue = it)) }
        HsvSlider(R.string.appearance_saturation, hsv.saturation, 0f..1f) { onChange(hsv.copy(saturation = it)) }
        HsvSlider(R.string.appearance_brightness, hsv.value, 0f..1f) { onChange(hsv.copy(value = it)) }
    }
}

@Composable
internal fun HexField(text: String, error: Int?, onTextChange: (String) -> Unit) {
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

internal data class Hsv(val hue: Float, val saturation: Float, val value: Float)

internal fun Color.toHsv(): Hsv {
    val out = FloatArray(HsvComponents)
    android.graphics.Color.colorToHSV(toArgb(), out)
    return Hsv(out[0], out[1], out[2])
}

internal fun Hsv.toColor(): Color = Color.hsv(hue.coerceIn(0f, MaxHue), saturation, value)

private const val HsvComponents = 3

private const val MaxHue = 359.9f

private const val HexDigits = 6

private const val HexInputChars = "0123456789abcdefABCDEF"
