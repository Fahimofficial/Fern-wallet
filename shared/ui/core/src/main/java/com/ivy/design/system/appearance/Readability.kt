package com.ivy.design.system.appearance

import androidx.compose.ui.graphics.Color

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
