package com.ivy.data.db.dao.write

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.db.entity.CreditCardStatementEntity
import java.util.UUID

@Dao
interface WriteCreditCardDao {
    @Upsert
    suspend fun save(value: CreditCardEntity)

    @Upsert
    suspend fun saveMany(values: List<CreditCardEntity>)

    @Upsert
    suspend fun saveStatement(value: CreditCardStatementEntity)

    @Upsert
    suspend fun saveStatements(values: List<CreditCardStatementEntity>)

    @Query(
        "DELETE FROM credit_card_statements " +
            "WHERE accountId = :accountId AND nominalClosingEpochDay = :nominalClosingEpochDay"
    )
    suspend fun deleteStatement(accountId: UUID, nominalClosingEpochDay: Long)

    @Query("DELETE FROM credit_card_statements WHERE accountId = :accountId")
    suspend fun deleteStatementsByAccountId(accountId: UUID)

    @Query("DELETE FROM credit_cards WHERE accountId = :accountId")
    suspend fun deleteByAccountId(accountId: UUID)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()

    @Query("DELETE FROM credit_card_statements")
    suspend fun deleteAllStatements()
}
