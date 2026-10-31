package com.ivy.domain.usecase.exchange

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.legacy.Theme
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.db.dao.read.ExchangeRatesDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.ExchangeRateEntity
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.repository.CurrencyRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MissingExchangeRatesUseCaseTest {
    private val accounts = FakeAccountDao()
    private val ratesDao = mockk<ExchangeRatesDao>()
    private val settings = FakeSettingsDao()

    private val useCase = MissingExchangeRatesUseCase(
        accountDao = accounts,
        exchangeRatesDao = ratesDao,
        currencyRepository = CurrencyRepository(settings, settings, TestDispatchersProvider),
    )

    private suspend fun given(vararg currencies: String?, rates: Map<String, Double>) {
        settings.save(SettingsEntity(theme = Theme.AUTO, currency = "INR", bufferAmount = 0.0, name = ""))
        currencies.forEachIndexed { i, currency ->
            accounts.save(AccountEntity(name = "A$i", currency = currency, color = 0, orderNum = i.toDouble()))
        }
        coEvery { ratesDao.findByBaseCurrencyAndCurrency(any(), any()) } returns null
        rates.forEach { (currency, rate) ->
            coEvery { ratesDao.findByBaseCurrencyAndCurrency("INR", currency) } returns
                ExchangeRateEntity(baseCurrency = "INR", currency = currency, rate = rate)
        }
    }

    @Test
    fun `lists account currencies without a valid rate, once each, sorted`() = runTest {
        given("INR", null, "USD", "EUR", "USD", "GBP", rates = mapOf("EUR" to 0.011, "GBP" to 0.0))

        useCase.missingCurrencies() shouldBe listOf("GBP", "USD")
    }

    @Test
    fun `nothing is missing when all accounts use the base currency`() = runTest {
        given("INR", null, rates = emptyMap())

        useCase.missingCurrencies() shouldBe emptyList()
    }
}
