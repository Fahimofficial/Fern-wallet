package com.ivy.data.db

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Runs a multi-step write as one database transaction: every write inside [run] is committed
 * together, or none is (on an exception, or if the app is killed part-way). Nested calls join
 * the outer transaction.
 */
interface DbTransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomDbTransactionRunner @Inject constructor(
    private val db: IvyRoomDatabase,
) : DbTransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
