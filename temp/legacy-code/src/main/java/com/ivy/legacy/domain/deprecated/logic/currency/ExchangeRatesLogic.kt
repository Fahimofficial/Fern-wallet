package com.ivy.wallet.domain.deprecated.logic.currency

import com.ivy.base.legacy.Transaction
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.ExchangeRatesDao
import com.ivy.data.db.dao.read.SettingsDao
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import java.util.UUID
import javax.inject.Inject

/**
 * Currency conversion for the legacy screens.
 *
 * When a needed exchange rate is missing (e.g. never downloaded because the app was offline):
 * - [amountBaseCurrency] leaves the amount out (returns 0), like the wallet balance does, instead
 *   of silently treating 1 unit of the foreign currency as 1 unit of the base currency.
 *   `MissingExchangeRatesUseCase` tells the user which currencies are affected.
 * - [convertAmount] returns null, so each caller decides what to show.
 */
@Deprecated("Use FP style, look into `domain.fp` package")
class ExchangeRatesLogic @Inject constructor(
    private val exchangeRatesDao: ExchangeRatesDao
) {
    suspend fun amountBaseCurrency(
        plannedPayment: PlannedPaymentRule,
        baseCurrency: String,
        accounts: List<Account> // helper
    ): Double {
        return amountBaseCurrency(
            amount = plannedPayment.amount,
            accountId = plannedPayment.accountId,
            baseCurrency = baseCurrency,
            accounts = accounts
        )
    }

    suspend fun amountBaseCurrency(
        transaction: Transaction,
        baseCurrency: String,
        accounts: List<Account> // helper
    ): Double {
        return amountBaseCurrency(
            amount = transaction.amount.toDouble(),
            accountId = transaction.accountId,
            baseCurrency = baseCurrency,
            accounts = accounts
        )
    }

    suspend fun toAmountBaseCurrency(
        transaction: Transaction,
        baseCurrency: String,
        accounts: List<Account> // helper
    ): Double {
        val amount = transaction.toAmount ?: transaction.amount
        val toCurrency = accounts.find { it.id == transaction.toAccountId }?.currency
            ?: return amount.toDouble() // no conversion

        return amountBaseCurrency(
            amount = amount.toDouble(),
            amountCurrency = toCurrency,
            baseCurrency = baseCurrency
        )
    }

    private suspend fun amountBaseCurrency(
        amount: Double,
        accountId: UUID,
        baseCurrency: String,
        accounts: List<Account> // helper
    ): Double {
        val trnCurrency = accounts.find { it.id == accountId }?.currency
            ?: return amount // no conversion

        return amountBaseCurrency(
            amount = amount,
            amountCurrency = trnCurrency,
            baseCurrency = baseCurrency
        )
    }

    suspend fun amountBaseCurrency(
        amount: Double,
        amountCurrency: String,
        baseCurrency: String
    ): Double {
        // No rate: left out of totals (see the class docs).
        val rate = exchangeRate(baseCurrency = baseCurrency, currency = amountCurrency) ?: return 0.0
        return amount / rate
    }

    /** Converts [amount], or returns null if a needed exchange rate is missing. */
    suspend fun convertAmount(
        baseCurrency: String,
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double? {
        if (fromCurrency == toCurrency) return amount

        val fromRate = exchangeRate(baseCurrency = baseCurrency, currency = fromCurrency)
        val toRate = exchangeRate(baseCurrency = baseCurrency, currency = toCurrency)
        return if (fromRate != null && toRate != null) amount / fromRate * toRate else null
    }

    /**
     * base = BGN, currency = EUR => rate = 0.51
     */
    private suspend fun exchangeRate(
        baseCurrency: String,
        currency: String
    ): Double? {
        if (currency == baseCurrency) return 1.0
        return exchangeRatesDao.findByBaseCurrencyAndCurrency(
            baseCurrency = baseCurrency,
            currency = currency
        )?.rate?.takeIf { it > 0 && it.isFinite() }
    }
}

@Deprecated("Use FP style, look into `domain.fp` package")
suspend fun Iterable<Transaction>.sumInBaseCurrency(
    exchangeRatesLogic: ExchangeRatesLogic,
    settingsDao: SettingsDao,
    accountDao: AccountDao,
): Double {
    val baseCurrency = settingsDao.findFirst().currency
    val accounts = accountDao.findAll()

    return sumOf {
        exchangeRatesLogic.amountBaseCurrency(
            transaction = it,
            baseCurrency = baseCurrency,
            accounts = accounts.map { it.toLegacyDomain() }
        )
    }
}

@Deprecated("Use FP style, look into `domain.fp` package")
suspend fun Iterable<PlannedPaymentRule>.sumByDoublePlannedInBaseCurrency(
    exchangeRatesLogic: ExchangeRatesLogic,
    settingsDao: SettingsDao,
    accountDao: AccountDao,
): Double {
    val baseCurrency = settingsDao.findFirst().currency
    val accounts = accountDao.findAll()

    return sumOf {
        exchangeRatesLogic.amountBaseCurrency(
            plannedPayment = it,
            baseCurrency = baseCurrency,
            accounts = accounts.map { it.toLegacyDomain() }
        )
    }
}
