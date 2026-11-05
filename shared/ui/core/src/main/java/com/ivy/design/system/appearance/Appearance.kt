package com.ivy.design.system.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

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
