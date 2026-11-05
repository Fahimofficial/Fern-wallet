package com.ivy.home

import androidx.compose.animation.ExperimentalAnimationApi
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.design.system.appearance.ColorToken
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.PaparazziRenderTest
import com.ivy.ui.testing.SemanticTestPalette
import com.ivy.ui.testing.countPixels
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.junit.Test
import org.junit.runner.RunWith

/** Home's income and expense cards follow the user's money colours. */
@RunWith(TestParameterInjector::class)
class HomeSemanticColorsTest(
    @param:TestParameter private val dark: Boolean,
) : PaparazziRenderTest() {

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun `home income and expense cards use their own colours`() {
        val image = render {
            IvyWalletPreview(if (dark) Theme.DARK else Theme.LIGHT, SemanticTestPalette.appearance) {
                CashFlowInfo(
                    currency = "USD",
                    balance = 1_000.0,
                    monthlyIncome = 100.0,
                    // Negative cashflow is grey, so the card badges are the only coloured pixels.
                    monthlyExpenses = 500.0,
                    hideBalance = false,
                    hideIncome = false,
                    onHiddenIncomeClick = {},
                    onOpenMoreMenu = {},
                    onBalanceClick = {},
                    onHiddenBalanceClick = {},
                )
            }
        }

        listOf(ColorToken.Income, ColorToken.Expense).forEach { token ->
            withClue(token) { image.countPixels(SemanticTestPalette[token]) shouldBeGreaterThan MinPixels }
        }
    }

    private companion object {
        const val MinPixels = 200
    }
}
