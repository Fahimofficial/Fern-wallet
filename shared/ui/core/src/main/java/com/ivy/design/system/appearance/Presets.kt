package com.ivy.design.system.appearance

import androidx.compose.ui.graphics.Color
import com.ivy.design.system.appearance.ColorToken.Accent
import com.ivy.design.system.appearance.ColorToken.AccentSecondary
import com.ivy.design.system.appearance.ColorToken.Background
import com.ivy.design.system.appearance.ColorToken.BudgetProgress
import com.ivy.design.system.appearance.ColorToken.Card
import com.ivy.design.system.appearance.ColorToken.Elevated
import com.ivy.design.system.appearance.ColorToken.Expense
import com.ivy.design.system.appearance.ColorToken.Fab
import com.ivy.design.system.appearance.ColorToken.Income
import com.ivy.design.system.appearance.ColorToken.NavBar
import com.ivy.design.system.appearance.ColorToken.NavSelected
import com.ivy.design.system.appearance.ColorToken.NavUnselected
import com.ivy.design.system.appearance.ColorToken.Outline
import com.ivy.design.system.appearance.ColorToken.Overdue
import com.ivy.design.system.appearance.ColorToken.SavingsProgress
import com.ivy.design.system.appearance.ColorToken.TextPrimary
import com.ivy.design.system.appearance.ColorToken.TextSecondary
import com.ivy.design.system.appearance.ColorToken.Transfer
import com.ivy.design.system.appearance.ColorToken.Upcoming

fun AppearancePreset.colors(dark: Boolean): Map<ColorToken, Color> = when (this) {
    AppearancePreset.Fern -> if (dark) FernDark else FernLight
    AppearancePreset.Ivy -> if (dark) IvyDark else IvyLight
    AppearancePreset.Midnight -> if (dark) MidnightDark else MidnightLight
    AppearancePreset.Ember -> if (dark) EmberDark else EmberLight
}

private data class PaletteColors(
    val background: Long,
    val card: Long,
    val elevated: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    val outline: Long,
    val accent: Long,
    val accentSecondary: Long,
    val navBar: Long,
    val navSelected: Long,
    val navUnselected: Long,
    val fab: Long,
    val income: Long,
    val expense: Long,
    val transfer: Long,
    val upcoming: Long,
    val overdue: Long,
    val budgetProgress: Long,
    val savingsProgress: Long,
) {
    fun toColorMap(): Map<ColorToken, Color> = mapOf(
        Background to Color(background),
        Card to Color(card),
        Elevated to Color(elevated),
        TextPrimary to Color(textPrimary),
        TextSecondary to Color(textSecondary),
        Outline to Color(outline),
        Accent to Color(accent),
        AccentSecondary to Color(accentSecondary),
        NavBar to Color(navBar),
        NavSelected to Color(navSelected),
        NavUnselected to Color(navUnselected),
        Fab to Color(fab),
        Income to Color(income),
        Expense to Color(expense),
        Transfer to Color(transfer),
        Upcoming to Color(upcoming),
        Overdue to Color(overdue),
        BudgetProgress to Color(budgetProgress),
        SavingsProgress to Color(savingsProgress),
    )
}

// Fern: the colours the app shipped with before Appearance existed. The one change: dialogs and
// sheets (Elevated) sit one step above the background instead of matching it.
// Expenses use the text colour on purpose: calm ink instead of an alarming red.
private val FernDark = PaletteColors(
    background = 0xFF111114,
    card = 0xFF2B2C2D,
    elevated = 0xFF1C1C1F,
    textPrimary = 0xFFFAFAFA,
    textSecondary = 0xFF939199,
    outline = 0xFF74747A,
    accent = 0xFF13876A,
    accentSecondary = 0xFF12B880,
    navBar = 0xFF111114,
    navSelected = 0xFF13876A,
    navUnselected = 0xFFFAFAFA,
    fab = 0xFF13876A,
    income = 0xFF14CC9E,
    expense = 0xFFFAFAFA,
    transfer = 0xFF3D6FE0,
    upcoming = 0xFFF29F30,
    overdue = 0xFFFF4060,
    budgetProgress = 0xFF14CC9E,
    savingsProgress = 0xFF14CC9E,
).toColorMap()

private val FernLight = PaletteColors(
    background = 0xFFFAFAFA,
    card = 0xFFEFEEF0,
    elevated = 0xFFFFFFFF,
    textPrimary = 0xFF111114,
    textSecondary = 0xFF939199,
    outline = 0xFF74747A,
    accent = 0xFF13876A,
    accentSecondary = 0xFF12B880,
    navBar = 0xFFFAFAFA,
    navSelected = 0xFF13876A,
    navUnselected = 0xFF111114,
    fab = 0xFF13876A,
    income = 0xFF14CC9E,
    expense = 0xFF111114,
    transfer = 0xFF3D6FE0,
    upcoming = 0xFFF29F30,
    overdue = 0xFFFF4060,
    budgetProgress = 0xFF14CC9E,
    savingsProgress = 0xFF14CC9E,
).toColorMap()

// Ivy: the original Ivy Wallet look, violet accents with emerald income.
private val IvyDark = PaletteColors(
    background = 0xFF111114,
    card = 0xFF2B2C2D,
    elevated = 0xFF1C1C1F,
    textPrimary = 0xFFFAFAFA,
    textSecondary = 0xFF939199,
    outline = 0xFF74747A,
    accent = 0xFF5C3DF5,
    accentSecondary = 0xFFC34CFF,
    navBar = 0xFF111114,
    navSelected = 0xFF7B62F5,
    navUnselected = 0xFFFAFAFA,
    fab = 0xFF5C3DF5,
    income = 0xFF14CC9E,
    expense = 0xFFFAFAFA,
    transfer = 0xFF4CC3FF,
    upcoming = 0xFFF29F30,
    overdue = 0xFFFF4060,
    budgetProgress = 0xFF5C3DF5,
    savingsProgress = 0xFF14CC9E,
).toColorMap()

private val IvyLight = PaletteColors(
    background = 0xFFFAFAFA,
    card = 0xFFEFEEF0,
    elevated = 0xFFFFFFFF,
    textPrimary = 0xFF111114,
    textSecondary = 0xFF939199,
    outline = 0xFF74747A,
    accent = 0xFF5C3DF5,
    accentSecondary = 0xFFC34CFF,
    navBar = 0xFFFAFAFA,
    navSelected = 0xFF5C3DF5,
    navUnselected = 0xFF111114,
    fab = 0xFF5C3DF5,
    income = 0xFF14CC9E,
    expense = 0xFF111114,
    transfer = 0xFF457BE6,
    upcoming = 0xFFF29F30,
    overdue = 0xFFFF4060,
    budgetProgress = 0xFF5C3DF5,
    savingsProgress = 0xFF14CC9E,
).toColorMap()

// Midnight: blue-tinted surfaces with a clear step between background, cards and sheets.
private val MidnightDark = PaletteColors(
    background = 0xFF0D1321,
    card = 0xFF1B2436,
    elevated = 0xFF151C2B,
    textPrimary = 0xFFF2F5FA,
    textSecondary = 0xFF8E99AE,
    outline = 0xFF3A4660,
    accent = 0xFF4C8DFF,
    accentSecondary = 0xFF7BC4FF,
    navBar = 0xFF0D1321,
    navSelected = 0xFF4C8DFF,
    navUnselected = 0xFFF2F5FA,
    fab = 0xFF4C8DFF,
    income = 0xFF2ED6A0,
    expense = 0xFFF2F5FA,
    transfer = 0xFF9B8CFF,
    upcoming = 0xFFFFB547,
    overdue = 0xFFFF5C7A,
    budgetProgress = 0xFF4C8DFF,
    savingsProgress = 0xFF2ED6A0,
).toColorMap()

private val MidnightLight = PaletteColors(
    background = 0xFFF4F7FC,
    card = 0xFFE6ECF5,
    elevated = 0xFFFFFFFF,
    textPrimary = 0xFF0D1321,
    textSecondary = 0xFF5B6578,
    outline = 0xFF9AA6BA,
    accent = 0xFF2D6FE0,
    accentSecondary = 0xFF5A9BEA,
    navBar = 0xFFF4F7FC,
    navSelected = 0xFF2D6FE0,
    navUnselected = 0xFF0D1321,
    fab = 0xFF2D6FE0,
    income = 0xFF16A37A,
    expense = 0xFF0D1321,
    transfer = 0xFF6C5CE0,
    upcoming = 0xFFD98A1F,
    overdue = 0xFFD93A55,
    budgetProgress = 0xFF2D6FE0,
    savingsProgress = 0xFF16A37A,
).toColorMap()

// Ember: warm neutrals with a coral accent.
private val EmberDark = PaletteColors(
    background = 0xFF15110F,
    card = 0xFF2A2320,
    elevated = 0xFF1F1916,
    textPrimary = 0xFFFAF4EF,
    textSecondary = 0xFFA3968D,
    outline = 0xFF4A3F39,
    accent = 0xFFF2673A,
    accentSecondary = 0xFFFFB070,
    navBar = 0xFF15110F,
    navSelected = 0xFFF2673A,
    navUnselected = 0xFFFAF4EF,
    fab = 0xFFF2673A,
    income = 0xFF3CCB8B,
    expense = 0xFFFAF4EF,
    transfer = 0xFF6EA8FF,
    upcoming = 0xFFFFB347,
    overdue = 0xFFFF4F5E,
    budgetProgress = 0xFFF2673A,
    savingsProgress = 0xFF3CCB8B,
).toColorMap()

private val EmberLight = PaletteColors(
    background = 0xFFFBF7F4,
    card = 0xFFF1E9E3,
    elevated = 0xFFFFFFFF,
    textPrimary = 0xFF1E1814,
    textSecondary = 0xFF7D7068,
    outline = 0xFFB8AAA0,
    accent = 0xFFD9541F,
    accentSecondary = 0xFFE8893F,
    navBar = 0xFFFBF7F4,
    navSelected = 0xFFD9541F,
    navUnselected = 0xFF1E1814,
    fab = 0xFFD9541F,
    income = 0xFF1E9E66,
    expense = 0xFF1E1814,
    transfer = 0xFF3A73D9,
    upcoming = 0xFFD98A1F,
    overdue = 0xFFD93A48,
    budgetProgress = 0xFFD9541F,
    savingsProgress = 0xFF1E9E66,
).toColorMap()
