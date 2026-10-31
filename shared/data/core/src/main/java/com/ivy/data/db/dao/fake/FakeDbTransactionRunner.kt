package com.ivy.data.db.dao.fake

import androidx.annotation.VisibleForTesting
import com.ivy.data.db.DbTransactionRunner

/**
 * Runs the block directly (fake DAOs have no transactions). [inTransaction] lets tests check that
 * writes happen inside a transaction.
 */
@VisibleForTesting
class FakeDbTransactionRunner : DbTransactionRunner {
    var inTransaction = false
        private set
    var runs = 0
        private set

    override suspend fun <T> run(block: suspend () -> T): T {
        if (inTransaction) return block()
        runs++
        inTransaction = true
        return try {
            block()
        } finally {
            inTransaction = false
        }
    }
}
