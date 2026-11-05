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

private fun palette(
    surfaces: List<Long>, // background, card, elevated
    text: List<Long>, // primary, secondary, outline
    accents: List<Long>, // accent, secondary accent
    nav: List<Long>, // bar, selected, unselected, fab
    money: List<Long>, // income, expense, transfer, upcoming, overdue
    progress: List<Long>, // budget, savings
): Map<ColorToken, Color> {
    val tokens = listOf(
        Background, Card, Elevated,
        TextPrimary, TextSecondary, Outline,
        Accent, AccentSecondary,
        NavBar, NavSelected, NavUnselected, Fab,
        Income, Expense, Transfer, Upcoming, Overdue,
        BudgetProgress, SavingsProgress,
    )
    val values = surfaces + text + accents + nav + money + progress
    check(tokens.size == ColorToken.entries.size && values.size == tokens.size)
    return tokens.zip(values) { token, argb -> token to Color(argb) }.toMap()
}

// Fern: the colours the app shipped with before Appearance existed. The one change: dialogs and
// sheets (Elevated) sit one step above the background instead of matching it.
// Expenses use the text colour on purpose: calm ink instead of an alarming red.
private val FernDark = palette(
    surfaces = listOf(0xFF111114, 0xFF2B2C2D, 0xFF1C1C1F),
    text = listOf(0xFFFAFAFA, 0xFF939199, 0xFF74747A),
    accents = listOf(0xFF13876A, 0xFF12B880),
    nav = listOf(0xFF111114, 0xFF13876A, 0xFFFAFAFA, 0xFF13876A),
    money = listOf(0xFF14CC9E, 0xFFFAFAFA, 0xFF3D6FE0, 0xFFF29F30, 0xFFFF4060),
    progress = listOf(0xFF14CC9E, 0xFF14CC9E),
)
private val FernLight = palette(
    surfaces = listOf(0xFFFAFAFA, 0xFFEFEEF0, 0xFFFFFFFF),
    text = listOf(0xFF111114, 0xFF939199, 0xFF74747A),
    accents = listOf(0xFF13876A, 0xFF12B880),
    nav = listOf(0xFFFAFAFA, 0xFF13876A, 0xFF111114, 0xFF13876A),
    money = listOf(0xFF14CC9E, 0xFF111114, 0xFF3D6FE0, 0xFFF29F30, 0xFFFF4060),
    progress = listOf(0xFF14CC9E, 0xFF14CC9E),
)

// Ivy: the original Ivy Wallet look, violet accents with emerald income.
private val IvyDark = palette(
    surfaces = listOf(0xFF111114, 0xFF2B2C2D, 0xFF1C1C1F),
    text = listOf(0xFFFAFAFA, 0xFF939199, 0xFF74747A),
    accents = listOf(0xFF5C3DF5, 0xFFC34CFF),
    nav = listOf(0xFF111114, 0xFF7B62F5, 0xFFFAFAFA, 0xFF5C3DF5),
    money = listOf(0xFF14CC9E, 0xFFFAFAFA, 0xFF4CC3FF, 0xFFF29F30, 0xFFFF4060),
    progress = listOf(0xFF5C3DF5, 0xFF14CC9E),
)
private val IvyLight = palette(
    surfaces = listOf(0xFFFAFAFA, 0xFFEFEEF0, 0xFFFFFFFF),
    text = listOf(0xFF111114, 0xFF939199, 0xFF74747A),
    accents = listOf(0xFF5C3DF5, 0xFFC34CFF),
    nav = listOf(0xFFFAFAFA, 0xFF5C3DF5, 0xFF111114, 0xFF5C3DF5),
    money = listOf(0xFF14CC9E, 0xFF111114, 0xFF457BE6, 0xFFF29F30, 0xFFFF4060),
    progress = listOf(0xFF5C3DF5, 0xFF14CC9E),
)

// Midnight: blue-tinted surfaces with a clear step between background, cards and sheets.
private val MidnightDark = palette(
    surfaces = listOf(0xFF0D1321, 0xFF1B2436, 0xFF151C2B),
    text = listOf(0xFFF2F5FA, 0xFF8E99AE, 0xFF3A4660),
    accents = listOf(0xFF4C8DFF, 0xFF7BC4FF),
    nav = listOf(0xFF0D1321, 0xFF4C8DFF, 0xFFF2F5FA, 0xFF4C8DFF),
    money = listOf(0xFF2ED6A0, 0xFFF2F5FA, 0xFF9B8CFF, 0xFFFFB547, 0xFFFF5C7A),
    progress = listOf(0xFF4C8DFF, 0xFF2ED6A0),
)
private val MidnightLight = palette(
    surfaces = listOf(0xFFF4F7FC, 0xFFE6ECF5, 0xFFFFFFFF),
    text = listOf(0xFF0D1321, 0xFF5B6578, 0xFF9AA6BA),
    accents = listOf(0xFF2D6FE0, 0xFF5A9BEA),
    nav = listOf(0xFFF4F7FC, 0xFF2D6FE0, 0xFF0D1321, 0xFF2D6FE0),
    money = listOf(0xFF16A37A, 0xFF0D1321, 0xFF6C5CE0, 0xFFD98A1F, 0xFFD93A55),
    progress = listOf(0xFF2D6FE0, 0xFF16A37A),
)

// Ember: warm neutrals with a coral accent.
private val EmberDark = palette(
    surfaces = listOf(0xFF15110F, 0xFF2A2320, 0xFF1F1916),
    text = listOf(0xFFFAF4EF, 0xFFA3968D, 0xFF4A3F39),
    accents = listOf(0xFFF2673A, 0xFFFFB070),
    nav = listOf(0xFF15110F, 0xFFF2673A, 0xFFFAF4EF, 0xFFF2673A),
    money = listOf(0xFF3CCB8B, 0xFFFAF4EF, 0xFF6EA8FF, 0xFFFFB347, 0xFFFF4F5E),
    progress = listOf(0xFFF2673A, 0xFF3CCB8B),
)
private val EmberLight = palette(
    surfaces = listOf(0xFFFBF7F4, 0xFFF1E9E3, 0xFFFFFFFF),
    text = listOf(0xFF1E1814, 0xFF7D7068, 0xFFB8AAA0),
    accents = listOf(0xFFD9541F, 0xFFE8893F),
    nav = listOf(0xFFFBF7F4, 0xFFD9541F, 0xFF1E1814, 0xFFD9541F),
    money = listOf(0xFF1E9E66, 0xFF1E1814, 0xFF3A73D9, 0xFFD98A1F, 0xFFD93A48),
    progress = listOf(0xFFD9541F, 0xFF1E9E66),
)
