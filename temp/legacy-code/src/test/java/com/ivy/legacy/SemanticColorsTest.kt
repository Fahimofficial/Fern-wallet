package com.ivy.legacy

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.base.model.TransactionType
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.ColorToken
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.ui.testing.PaparazziRenderTest
import com.ivy.ui.testing.SemanticTestPalette
import com.ivy.ui.testing.countPixels
import com.ivy.legacy.ui.component.transaction.TypeAmountCurrency
import com.ivy.legacy.legacy.ui.theme.components.BudgetBattery
import com.ivy.wallet.ui.theme.components.BufferBattery
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

/** Legacy transaction and budget components draw with the user's palette, not fixed colours. */
@RunWith(TestParameterInjector::class)
class SemanticColorsTest(
    @param:TestParameter private val theme: TestTheme,
) : PaparazziRenderTest() {
    enum class TestTheme(val value: Theme) { Light(Theme.LIGHT), Dark(Theme.DARK) }

    private val amountTokens = listOf(
        ColorToken.Income,
        ColorToken.Expense,
        ColorToken.Transfer,
        ColorToken.Upcoming,
        ColorToken.Overdue,
    )

    @Test
    fun `transaction amounts use the income, expense, transfer, upcoming and overdue colours`() {
        val image = render { TransactionAmounts(SemanticTestPalette.appearance) }

        amountTokens.forEach { token ->
            withClue(token) { image.countPixels(SemanticTestPalette[token]) shouldBeGreaterThan MinPixels }
        }
    }

    @Test
    fun `without custom colours none of the test colours is drawn`() {
        val image = render { TransactionAmounts(AppearanceSettings.Default) }

        amountTokens.forEach { token -> withClue(token) { image.countPixels(SemanticTestPalette[token]) shouldBe 0 } }
    }

    @Test
    fun `budget and savings bars use progress, warning and overspending colours`() {
        val image = render {
            IvyWalletPreview(theme.value, SemanticTestPalette.appearance) {
                Column {
                    BudgetBattery(currency = "USD", expenses = 20.0, budget = 100.0)
                    BudgetBattery(currency = "USD", expenses = 70.0, budget = 100.0)
                    BudgetBattery(currency = "USD", expenses = 150.0, budget = 100.0)
                    BufferBattery(buffer = 200.0, balance = 1000.0, currency = "USD")
                }
            }
        }

        listOf(ColorToken.BudgetProgress, ColorToken.Upcoming, ColorToken.Overdue, ColorToken.SavingsProgress)
            .forEach { token ->
                withClue(token) { image.countPixels(SemanticTestPalette[token]) shouldBeGreaterThan MinPixels }
            }
    }

    @Composable
    private fun TransactionAmounts(appearance: AppearanceSettings) {
        IvyWalletPreview(theme.value, appearance) {
            Column {
                TypeAmountCurrency(TransactionType.INCOME, dueDate = null, currency = "USD", amount = 10.0)
                TypeAmountCurrency(TransactionType.EXPENSE, dueDate = null, currency = "USD", amount = 10.0)
                TypeAmountCurrency(TransactionType.TRANSFER, dueDate = null, currency = "USD", amount = 10.0)
                TypeAmountCurrency(
                    TransactionType.EXPENSE,
                    dueDate = timeNowUTC().plusDays(DueInDays),
                    currency = "USD",
                    amount = 10.0
                )
                TypeAmountCurrency(
                    TransactionType.EXPENSE,
                    dueDate = timeNowUTC().minusDays(DueInDays),
                    currency = "USD",
                    amount = 10.0
                )
            }
        }
    }

    private companion object {
        // A badge or bar is thousands of pixels; antialiasing alone never reaches this.
        const val MinPixels = 200
        const val DueInDays = 3L
    }
}
