package com.ivy.legacy.exchange

import com.ivy.data.db.dao.read.ExchangeRatesDao
import com.ivy.data.db.entity.ExchangeRateEntity
import com.ivy.wallet.domain.deprecated.logic.currency.ExchangeRatesLogic
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExchangeRatesLogicTest {
    private val dao = mockk<ExchangeRatesDao>()

    private fun rates(vararg rates: Pair<String, Double>) {
        coEvery { dao.findByBaseCurrencyAndCurrency(any(), any()) } returns null
        rates.forEach { (currency, rate) ->
            coEvery { dao.findByBaseCurrencyAndCurrency("BGN", currency) } returns
                ExchangeRateEntity(baseCurrency = "BGN", currency = currency, rate = rate)
        }
    }

    private val logic = ExchangeRatesLogic(dao)

    private val tolerance = 0.0001

    @Test
    fun `converts to the base currency with the stored rate`() = runTest {
        rates("EUR" to 0.51)

        logic.amountBaseCurrency(amount = 10.0, amountCurrency = "EUR", baseCurrency = "BGN") shouldBe
            (19.6078 plusOrMinus tolerance)
    }

    @Test
    fun `a missing rate leaves the amount out instead of converting 1 to 1`() = runTest {
        rates()

        logic.amountBaseCurrency(amount = 10.0, amountCurrency = "USD", baseCurrency = "BGN") shouldBe 0.0
    }

    @Test
    fun `zero, negative and non-finite rates count as missing`() = runTest {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { bad ->
            rates("USD" to bad)
            logic.amountBaseCurrency(amount = 10.0, amountCurrency = "USD", baseCurrency = "BGN") shouldBe 0.0
            logic.convertAmount("BGN", 10.0, fromCurrency = "USD", toCurrency = "BGN") shouldBe null
        }
    }

    @Test
    fun `the base currency needs no stored rate`() = runTest {
        rates("EUR" to 0.5)

        logic.convertAmount("BGN", 10.0, fromCurrency = "BGN", toCurrency = "EUR") shouldBe 5.0
        logic.convertAmount("BGN", 5.0, fromCurrency = "EUR", toCurrency = "BGN") shouldBe 10.0
        logic.amountBaseCurrency(amount = 3.0, amountCurrency = "BGN", baseCurrency = "BGN") shouldBe 3.0
    }

    @Test
    fun `converts between two foreign currencies through the base currency`() = runTest {
        rates("EUR" to 0.5, "USD" to 0.55)

        logic.convertAmount("BGN", 10.0, fromCurrency = "EUR", toCurrency = "USD")!! shouldBe
            (11.0 plusOrMinus tolerance)
    }

    @Test
    fun `convertAmount returns null when either rate is missing`() = runTest {
        rates("EUR" to 0.5)

        logic.convertAmount("BGN", 10.0, fromCurrency = "EUR", toCurrency = "USD") shouldBe null
        logic.convertAmount("BGN", 10.0, fromCurrency = "USD", toCurrency = "EUR") shouldBe null
        logic.convertAmount("BGN", 10.0, fromCurrency = "USD", toCurrency = "USD") shouldBe 10.0
    }
}
