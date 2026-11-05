package com.ivy.design.system.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/** What a colour token is used for, in the order the Appearance screen shows them. */
enum class ColorGroup { Surfaces, Text, Accents, Navigation, Money, Progress }

/**
 * Every colour the user can customise. Each one is read by real components (see the KDoc).
 * The enum name is persisted: renaming an entry drops users' saved value for it.
 */
enum class ColorToken(val group: ColorGroup) {
    /** Screen background, bottom bar backdrop. */
    Background(ColorGroup.Surfaces),

    /** Cards and list rows (transactions, header cards, inputs). */
    Card(ColorGroup.Surfaces),

    /** Dialogs, bottom sheets, menus. */
    Elevated(ColorGroup.Surfaces),
    TextPrimary(ColorGroup.Text),
    TextSecondary(ColorGroup.Text),

    /** Borders and dividers. */
    Outline(ColorGroup.Text),

    /** Primary buttons, switches, selection, links. */
    Accent(ColorGroup.Accents),

    /** Secondary Material components (chips, tonal buttons). */
    AccentSecondary(ColorGroup.Accents),
    NavBar(ColorGroup.Navigation),
    NavSelected(ColorGroup.Navigation),
    NavUnselected(ColorGroup.Navigation),
    Fab(ColorGroup.Navigation),
    Income(ColorGroup.Money),
    Expense(ColorGroup.Money),
    Transfer(ColorGroup.Money),

    /** Upcoming and pending payments, warnings. */
    Upcoming(ColorGroup.Money),

    /** Overdue payments, overspending and errors. */
    Overdue(ColorGroup.Money),
    BudgetProgress(ColorGroup.Progress),
    SavingsProgress(ColorGroup.Progress),
}

/** The resolved colours the UI draws with. */
@Immutable
data class FernPalette(
    val colors: Map<ColorToken, Color>,
    val isLight: Boolean,
)

operator fun FernPalette.get(token: ColorToken): Color = colors.getValue(token)

/** Built-in starting points. The name is persisted. */
enum class AppearancePreset { Fern, Ivy, Midnight, Ember }

/**
 * The user's appearance choice: a preset plus their own colours on top of it. Light and dark
 * keep separate custom colours because a colour picked for one rarely works on the other.
 */
@Immutable
data class AppearanceSettings(
    val preset: AppearancePreset,
    val lightOverrides: Map<ColorToken, Color>,
    val darkOverrides: Map<ColorToken, Color>,
) {
    companion object {
        val Default = AppearanceSettings(AppearancePreset.Fern, emptyMap(), emptyMap())
    }
}

fun AppearanceSettings.overrides(dark: Boolean): Map<ColorToken, Color> =
    if (dark) darkOverrides else lightOverrides

fun AppearanceSettings.resolve(dark: Boolean, trueBlack: Boolean): FernPalette {
    val base = preset.colors(dark).toMutableMap()
    if (dark && trueBlack) {
        base[ColorToken.Background] = Color.Black
        base[ColorToken.NavBar] = Color.Black
    }
    base.putAll(overrides(dark))
    return FernPalette(base, isLight = !dark)
}

/** Pass null to go back to the preset's colour. */
fun AppearanceSettings.withColor(dark: Boolean, token: ColorToken, color: Color?): AppearanceSettings {
    val updated = overrides(dark).toMutableMap().apply {
        if (color == null) remove(token) else put(token, color)
    }
    return if (dark) copy(darkOverrides = updated) else copy(lightOverrides = updated)
}

fun AppearanceSettings.resetGroup(dark: Boolean, group: ColorGroup): AppearanceSettings {
    val updated = overrides(dark).filterKeys { it.group != group }
    return if (dark) copy(darkOverrides = updated) else copy(lightOverrides = updated)
}

/** A preset is a fresh start: picking one drops custom colours. */
fun AppearanceSettings.withPreset(preset: AppearancePreset): AppearanceSettings =
    AppearanceSettings(preset, emptyMap(), emptyMap())

// region Persistence: "v1;preset=Fern;L.Income=FF14CC9E;D.Background=FF000000"
private const val FormatVersion = "v1"

fun AppearanceSettings.encode(): String = buildList {
    add(FormatVersion)
    add("preset=${preset.name}")
    lightOverrides.forEach { (token, color) -> add("L.${token.name}=${color.toHex(withAlpha = true)}") }
    darkOverrides.forEach { (token, color) -> add("D.${token.name}=${color.toHex(withAlpha = true)}") }
}.joinToString(";")

/** Never throws: unknown or corrupt parts are skipped so a bad value can't stop the app starting. */
fun decodeAppearance(raw: String?): AppearanceSettings {
    val parts = raw?.split(";").orEmpty()
    if (parts.firstOrNull() != FormatVersion) return AppearanceSettings.Default
    val entries = parts.drop(1).associate { it.substringBefore("=") to it.substringAfter("=", "") }
    fun overrides(prefix: String) = ColorToken.entries.mapNotNull { token ->
        entries["$prefix.${token.name}"]?.let(::parseHexColor)?.let { token to it }
    }.toMap()
    return AppearanceSettings(
        preset = AppearancePreset.entries.firstOrNull { it.name == entries["preset"] } ?: AppearancePreset.Fern,
        lightOverrides = overrides("L"),
        darkOverrides = overrides("D"),
    )
}
// endregion

// region Colour maths
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

/** WCAG's allowance for screen flare in the contrast formula. */
private const val LuminanceFlare = 0.05f
private const val RgbDigits = 6
private const val ArgbDigits = 8
private const val HexChars = "0123456789abcdefABCDEF"
private const val OpaqueAlpha = 0xFF000000
private const val ArgbMask = 0xFFFFFFFF
private const val RgbMask = 0xFFFFFFL
// endregion

// region Readability
/** How [ColorToken] [against] reads next to the colour being picked. */
data class ContrastCheck(
    val against: ColorToken,
    val ratio: Float,
    val minimum: Float,
)

val ContrastCheck.passes: Boolean get() = ratio >= minimum

private class ReadabilityRule(val foreground: ColorToken, val background: ColorToken, val minimum: Float)

// Hard floors the picker won't go below. Primary text gets WCAG AA; the rest only needs to stay
// clearly visible (money colours are also auto-deepened where they are used as text).
private val ReadabilityRules = listOf(
    ReadabilityRule(ColorToken.TextPrimary, ColorToken.Background, minimum = 4.5f),
    ReadabilityRule(ColorToken.TextPrimary, ColorToken.Card, minimum = 4.5f),
    ReadabilityRule(ColorToken.TextPrimary, ColorToken.Elevated, minimum = 4.5f),
    ReadabilityRule(ColorToken.TextSecondary, ColorToken.Background, minimum = 2.5f),
    ReadabilityRule(ColorToken.TextSecondary, ColorToken.Card, minimum = 2.5f),
    ReadabilityRule(ColorToken.NavSelected, ColorToken.NavBar, minimum = 2.5f),
    ReadabilityRule(ColorToken.NavUnselected, ColorToken.NavBar, minimum = 2.5f),
    ReadabilityRule(ColorToken.Accent, ColorToken.Background, minimum = 1.3f),
    ReadabilityRule(ColorToken.Fab, ColorToken.Background, minimum = 1.3f),
) + listOf(
    ColorToken.Income, ColorToken.Expense, ColorToken.Transfer, ColorToken.Upcoming,
    ColorToken.Overdue, ColorToken.BudgetProgress, ColorToken.SavingsProgress,
).map { ReadabilityRule(it, ColorToken.Card, minimum = 1.3f) }

/** Every pairing that changes if [token] becomes [candidate]; apply only when all pass. */
fun FernPalette.contrastChecks(token: ColorToken, candidate: Color): List<ContrastCheck> =
    ReadabilityRules
        .filter { it.foreground == token || it.background == token }
        .map { rule ->
            val isForeground = rule.foreground == token
            val foreground = if (isForeground) candidate else this[rule.foreground]
            val background = if (isForeground) this[rule.background] else candidate
            ContrastCheck(
                against = if (isForeground) rule.background else rule.foreground,
                ratio = contrastRatio(foreground, background),
                minimum = rule.minimum,
            )
        }
// endregion

@Suppress("CompositionLocalAllowlist")
val LocalFernPalette = compositionLocalOf {
    AppearanceSettings.Default.resolve(dark = false, trueBlack = false)
}

/** Semantic colours of the current theme. */
object FernTheme {
    val palette: FernPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalFernPalette.current
}
