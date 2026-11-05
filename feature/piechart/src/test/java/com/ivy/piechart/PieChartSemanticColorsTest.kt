package com.ivy.piechart

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.design.system.appearance.ColorToken
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.PaparazziRenderTest
import com.ivy.ui.testing.SemanticTestPalette
import com.ivy.ui.testing.countPixels
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

/** The income and expense chart's add button follows the user's money colours. */
@RunWith(TestParameterInjector::class)
class PieChartSemanticColorsTest(
    @param:TestParameter private val dark: Boolean,
) : PaparazziRenderTest() {

    @Test
    fun `income chart button uses the income colour`() {
        val image = bottomBar(TransactionType.INCOME)

        image.countPixels(SemanticTestPalette[ColorToken.Income], GradientTolerance) shouldBeGreaterThan MinPixels
        image.countPixels(SemanticTestPalette[ColorToken.Expense], GradientTolerance) shouldBe 0
    }

    @Test
    fun `expense chart button uses the expense colour`() {
        val image = bottomBar(TransactionType.EXPENSE)

        image.countPixels(SemanticTestPalette[ColorToken.Expense], GradientTolerance) shouldBeGreaterThan MinPixels
        image.countPixels(SemanticTestPalette[ColorToken.Income], GradientTolerance) shouldBe 0
    }

    private fun bottomBar(type: TransactionType) = render {
        IvyWalletPreview(if (dark) Theme.DARK else Theme.LIGHT, SemanticTestPalette.appearance) {
            PieChartStatisticBottomBar(type = type, onClose = {}, onAdd = {})
        }
    }

    private companion object {
        const val MinPixels = 200

        // The button's gradient lifts its colour slightly towards white.
        const val GradientTolerance = 24
    }
}
