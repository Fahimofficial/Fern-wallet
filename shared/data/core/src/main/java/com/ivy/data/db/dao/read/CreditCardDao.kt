package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.Query
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.db.entity.CreditCardStatementEntity
import java.util.UUID

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards")
    suspend fun findAll(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE accountId = :accountId")
    suspend fun findByAccountId(accountId: UUID): CreditCardEntity?

    @Query("SELECT * FROM credit_card_statements")
    suspend fun findAllStatements(): List<CreditCardStatementEntity>

    @Query("SELECT * FROM credit_card_statements WHERE accountId = :accountId")
    suspend fun findStatementsByAccountId(accountId: UUID): List<CreditCardStatementEntity>
}
