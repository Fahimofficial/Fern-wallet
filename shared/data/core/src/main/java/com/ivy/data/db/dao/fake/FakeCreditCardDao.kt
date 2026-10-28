package com.ivy.data.db.dao.fake

import com.ivy.data.db.dao.read.CreditCardDao
import com.ivy.data.db.dao.write.WriteCreditCardDao
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.db.entity.CreditCardStatementEntity
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@VisibleForTesting
class FakeCreditCardDao : CreditCardDao, WriteCreditCardDao {
    private val cards = mutableMapOf<UUID, CreditCardEntity>()
    private val statements = mutableMapOf<Pair<UUID, Long>, CreditCardStatementEntity>()

    override suspend fun findAll(): List<CreditCardEntity> = cards.values.toList()

    override suspend fun findByAccountId(accountId: UUID): CreditCardEntity? = cards[accountId]

    override suspend fun findAllStatements(): List<CreditCardStatementEntity> =
        statements.values.toList()

    override suspend fun findStatementsByAccountId(accountId: UUID): List<CreditCardStatementEntity> =
        statements.values.filter { it.accountId == accountId }

    override suspend fun save(value: CreditCardEntity) {
        cards[value.accountId] = value
    }

    override suspend fun saveMany(values: List<CreditCardEntity>) {
        values.forEach { save(it) }
    }

    override suspend fun saveStatement(value: CreditCardStatementEntity) {
        statements[value.accountId to value.nominalClosingEpochDay] = value
    }

    override suspend fun saveStatements(values: List<CreditCardStatementEntity>) {
        values.forEach { saveStatement(it) }
    }

    override suspend fun deleteStatement(accountId: UUID, nominalClosingEpochDay: Long) {
        statements.remove(accountId to nominalClosingEpochDay)
    }

    override suspend fun deleteStatementsByAccountId(accountId: UUID) {
        statements.keys.removeAll { it.first == accountId }
    }

    override suspend fun deleteByAccountId(accountId: UUID) {
        cards.remove(accountId)
    }

    override suspend fun deleteAll() {
        cards.clear()
    }

    override suspend fun deleteAllStatements() {
        statements.clear()
    }
}
