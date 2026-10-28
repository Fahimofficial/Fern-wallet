package com.ivy.domain.creditcard

import com.ivy.base.time.INSTANT_MIN_SAFE
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountType
import com.ivy.data.model.Transaction
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import javax.inject.Inject

/** A card account with its details and calculated billing state. */
data class CreditCardWithOverview(
    val account: Account,
    val card: CreditCard,
    val overview: CardOverview,
    val transactions: List<Transaction>,
)

class CreditCardOverviewUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) {
    suspend fun load(accountId: AccountId): CreditCardWithOverview? {
        val account = accountRepository.findById(accountId)?.takeIf { it.type == AccountType.CREDIT_CARD }
        val card = account?.let { creditCardRepository.findByAccountId(accountId) }
        return if (account != null && card != null) calculate(account, card) else null
    }

    suspend fun loadAll(): List<CreditCardWithOverview> {
        val cards = creditCardRepository.findAll().associateBy { it.accountId }
        return accountRepository.findAll()
            .filter { it.type == AccountType.CREDIT_CARD }
            .mapNotNull { account -> cards[account.id]?.let { calculate(account, it) } }
    }

    private suspend fun calculate(account: Account, card: CreditCard): CreditCardWithOverview {
        // Same window as the account balance (CalcAccBalanceAct): settled transactions up to now.
        val now = timeProvider.utcNow()
        val transactions = (
            transactionRepository.findAllByAccountAndBetween(account.id, INSTANT_MIN_SAFE, now) +
                transactionRepository.findAllToAccountAndBetween(account.id, INSTANT_MIN_SAFE, now)
            ).distinctBy { it.id }
            .sortedByDescending { it.time }
        val ledger = CardLedger.entries(account.id, transactions) { instant ->
            with(timeConverter) { instant.toLocalDate() }
        }
        val overrides = creditCardRepository.findStatementOverrides(account.id)
        val overview = CreditCardCalculator.overview(
            card = card,
            ledger = ledger,
            overrides = overrides,
            today = timeProvider.localDateNow(),
        )
        return CreditCardWithOverview(account, card, overview, transactions)
    }
}
