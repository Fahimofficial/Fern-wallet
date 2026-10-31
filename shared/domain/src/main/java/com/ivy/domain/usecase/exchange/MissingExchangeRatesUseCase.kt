package com.ivy.domain.usecase.exchange

import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.ExchangeRatesDao
import com.ivy.data.repository.CurrencyRepository
import javax.inject.Inject

/**
 * Currencies used by accounts that have no valid exchange rate to the base currency.
 * Amounts in these currencies are left out of totals until a rate is downloaded or set manually,
 * so the app shows a warning instead of a silently wrong total.
 */
class MissingExchangeRatesUseCase @Inject constructor(
    private val accountDao: AccountDao,
    private val exchangeRatesDao: ExchangeRatesDao,
    private val currencyRepository: CurrencyRepository,
) {
    suspend fun missingCurrencies(): List<String> {
        val base = currencyRepository.getBaseCurrency().code
        return accountDao.findAll()
            .map { it.currency?.takeIf(String::isNotBlank) ?: base }
            .filter { it != base }
            .distinct()
            .filter { currency ->
                val rate = exchangeRatesDao.findByBaseCurrencyAndCurrency(
                    baseCurrency = base,
                    currency = currency
                )?.rate
                rate == null || rate <= 0 || !rate.isFinite()
            }
            .sorted()
    }
}
