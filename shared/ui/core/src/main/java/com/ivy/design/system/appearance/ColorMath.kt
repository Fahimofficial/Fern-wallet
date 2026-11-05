package com.ivy.design.system.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/** WCAG 2 contrast ratio, from 1 (same colour) to 21 (black on white). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.copy(alpha = 1f).luminance() + LuminanceFlare
    val lb = b.copy(alpha = 1f).luminance() + LuminanceFlare
    return maxOf(la, lb) / minOf(la, lb)
}

/** Black or white, whichever reads better on this colour. */
fun Color.contentColor(): Color =
    if (contrastRatio(this, OnLight) >= contrastRatio(this, OnDark)) OnLight else OnDark

/**
 * This colour, moved towards black or white just enough to reach [minContrast] on [background].
 * Lets one income green work as a badge fill and as readable text on both light and dark.
 */
fun Color.readableOn(background: Color, minContrast: Float = 3f): Color {
    val target = background.contentColor()
    var step = 0
    var result = this
    while (contrastRatio(result, background) < minContrast && step < ReadableSteps) {
        step++
        result = lerp(this, target, step / ReadableSteps.toFloat())
    }
    return result
}

/** Accepts RRGGBB or AARRGGBB, with or without '#'. */
fun parseHexColor(text: String): Color? {
    val hex = text.trim().removePrefix("#")
    val value = hex
        .takeIf { (it.length == RgbDigits || it.length == ArgbDigits) && it.all { c -> c in HexChars } }
        ?.toLong(radix = 16)
    return value?.let { Color(if (hex.length == RgbDigits) it or OpaqueAlpha else it) }
}

fun Color.toHex(withAlpha: Boolean = false): String {
    val argb = toArgb().toLong() and ArgbMask
    return if (withAlpha) "%08X".format(argb) else "%06X".format(argb and RgbMask)
}

private val OnLight = Color(0xFF111114)
private val OnDark = Color(0xFFFAFAFA)
private const val ReadableSteps = 10

// WCAG's allowance for screen flare in the contrast formula.
private const val LuminanceFlare = 0.05f
private const val RgbDigits = 6
private const val ArgbDigits = 8
private const val HexChars = "0123456789abcdefABCDEF"
private const val OpaqueAlpha = 0xFF000000
private const val ArgbMask = 0xFFFFFFFF
private const val RgbMask = 0xFFFFFFL
