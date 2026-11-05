package com.ivy.ui.testing

import androidx.compose.ui.graphics.Color
import com.ivy.design.system.appearance.AppearancePreset
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.ColorToken

/**
 * Money and progress colours no preset uses and far apart from each other, so a test can find
 * exactly where each one was drawn. The same in light and dark.
 */
object SemanticTestPalette {
    val colors: Map<ColorToken, Color> = mapOf(
        ColorToken.Income to Color(0xFF7A00C8),
        ColorToken.Expense to Color(0xFFC86400),
        ColorToken.Transfer to Color(0xFF00A0A0),
        ColorToken.Upcoming to Color(0xFFA0A000),
        ColorToken.Overdue to Color(0xFFC80064),
        ColorToken.BudgetProgress to Color(0xFF3C3CC8),
        ColorToken.SavingsProgress to Color(0xFF64C800),
    )

    val appearance = AppearanceSettings(
        preset = AppearancePreset.Fern,
        lightOverrides = colors,
        darkOverrides = colors,
    )

    operator fun get(token: ColorToken): Color = colors.getValue(token)
}
