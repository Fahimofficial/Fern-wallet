package com.ivy.data.repository

import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.DbTransactionRunner
import com.ivy.data.db.dao.read.CreditCardDao
import com.ivy.data.db.dao.write.WriteCreditCardDao
import com.ivy.data.model.AccountId
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.StatementOverride
import com.ivy.data.repository.mapper.CreditCardMapper
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Card details and per-statement issuer overrides. The ledger (purchases, refunds, payments)
 * lives in the ordinary transactions table, so this repository never stores amounts that
 * could disagree with the account balance.
 */
@Singleton
class CreditCardRepository @Inject constructor(
    private val dao: CreditCardDao,
    private val writeDao: WriteCreditCardDao,
    private val mapper: CreditCardMapper,
    private val dataObserver: DataObserver,
    private val dispatchersProvider: DispatchersProvider,
    private val transactionRunner: DbTransactionRunner,
) {
    suspend fun findAll(): List<CreditCard> = withContext(dispatchersProvider.io) {
        dao.findAll().map { with(mapper) { it.toDomain() } }
    }

    /**
     * Ids of closed cards. A closed card keeps its history (and stays visible wherever balances
     * or reports are shown) but must not be offered in pickers for new money movements.
     */
    suspend fun findClosedCardIds(): Set<UUID> = findAll()
        .filter { it.status == CardStatus.CLOSED }
        .map { it.accountId.value }
        .toSet()

    suspend fun findByAccountId(accountId: AccountId): CreditCard? =
        withContext(dispatchersProvider.io) {
            dao.findByAccountId(accountId.value)?.let { with(mapper) { it.toDomain() } }
        }

    suspend fun save(card: CreditCard) {
        withContext(dispatchersProvider.io) {
            writeDao.save(with(mapper) { card.toEntity() })
        }
        dataObserver.post(DataWriteEvent.CreditCardsChanged(listOf(card.accountId)))
    }

    suspend fun findStatementOverrides(accountId: AccountId): List<StatementOverride> =
        withContext(dispatchersProvider.io) {
            dao.findStatementsByAccountId(accountId.value).map { with(mapper) { it.toDomain() } }
        }

    suspend fun saveStatementOverride(value: StatementOverride) {
        withContext(dispatchersProvider.io) {
            val isEmpty = value.actualClosingDate == null && value.actualDueDate == null &&
                value.issuerStatementBalance == null && value.minimumDue == null
            if (isEmpty) {
                writeDao.deleteStatement(value.accountId.value, value.nominalClosingDate.toEpochDay())
            } else {
                writeDao.saveStatement(with(mapper) { value.toEntity() })
            }
        }
        dataObserver.post(DataWriteEvent.CreditCardsChanged(listOf(value.accountId)))
    }

    /** Replaces all saved statement values of a card (e.g. after its statement day changed). */
    suspend fun replaceStatementOverrides(accountId: AccountId, values: List<StatementOverride>) {
        // Delete + insert in one transaction, so an interruption can't lose the saved figures.
        transactionRunner.run {
            withContext(dispatchersProvider.io) {
                writeDao.deleteStatementsByAccountId(accountId.value)
                writeDao.saveStatements(values.map { with(mapper) { it.toEntity() } })
            }
        }
        dataObserver.post(DataWriteEvent.CreditCardsChanged(listOf(accountId)))
    }

    suspend fun deleteStatementOverride(accountId: AccountId, nominalClosingDate: LocalDate) {
        withContext(dispatchersProvider.io) {
            writeDao.deleteStatement(accountId.value, nominalClosingDate.toEpochDay())
        }
        dataObserver.post(DataWriteEvent.CreditCardsChanged(listOf(accountId)))
    }

    /** Removes card details only. Callers decide what happens to the account and its history. */
    suspend fun deleteByAccountId(accountId: AccountId) {
        withContext(dispatchersProvider.io) {
            writeDao.deleteStatementsByAccountId(accountId.value)
            writeDao.deleteByAccountId(accountId.value)
        }
        dataObserver.post(DataWriteEvent.CreditCardsChanged(listOf(accountId)))
    }

    suspend fun deleteAll() {
        withContext(dispatchersProvider.io) {
            writeDao.deleteAllStatements()
            writeDao.deleteAll()
        }
    }
}
