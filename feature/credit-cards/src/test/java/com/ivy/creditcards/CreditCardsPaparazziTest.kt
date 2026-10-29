package com.ivy.creditcards

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CreditCardsPaparazziTest(
    @param:TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    private val dark get() = theme == PaparazziTheme.Dark

    @Test
    fun `card tiles`() = snapshot(theme) { CreditCardTilesUiTest(dark) }

    @Test
    fun `card details`() = snapshot(theme) { CreditCardDetailsUiTest(dark) }

    @Test
    fun `add card form`() = snapshot(theme) { EditCreditCardUiTest(dark) }

    @Test
    fun `add card bank search`() = snapshot(theme) { EditCreditCardBankSearchUiTest(dark) }

    @Test
    fun `pay card`() = snapshot(theme) { PayCreditCardUiTest(dark) }

    @Test
    fun `statement`() = snapshot(theme) { CreditCardStatementUiTest(dark) }
}
