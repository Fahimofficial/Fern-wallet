package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds credit cards without touching existing data:
 * - `accounts.type` with default 'ASSET', so every existing account stays an asset account
 * - `credit_cards` (1:1 with accounts) and `credit_card_statements` (issuer overrides)
 *
 * The CREATE statements must match Room's generated schema (schemas/.../131.json);
 * Migration130to131SchemaTest checks that on the JVM.
 */
@Suppress("ClassNaming", "MagicNumber") // database versions
class Migration130to131_CreditCards : Migration(130, 131) {
    override fun migrate(db: SupportSQLiteDatabase) {
        STATEMENTS.forEach(db::execSQL)
    }

    companion object {
        const val ADD_ACCOUNT_TYPE =
            "ALTER TABLE accounts ADD COLUMN type TEXT NOT NULL DEFAULT 'ASSET'"
        const val CREATE_CREDIT_CARDS =
            "CREATE TABLE IF NOT EXISTS `credit_cards` (`accountId` TEXT NOT NULL, " +
                "`bankId` TEXT, `customBankName` TEXT, `productId` TEXT, " +
                "`productName` TEXT, `network` TEXT, `lastFour` TEXT, " +
                "`creditLimit` REAL, `statementDay` INTEGER, `dueRuleType` TEXT, " +
                "`dueRuleValue` INTEGER, `openingOutstanding` REAL NOT NULL DEFAULT 0, " +
                "`openingDateEpochDay` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL DEFAULT 'ACTIVE', " +
                "`appearance` TEXT NOT NULL DEFAULT 'FERN', " +
                "`remindersEnabled` INTEGER NOT NULL DEFAULT 1, " +
                "`reminderDaysBefore` INTEGER NOT NULL DEFAULT 3, " +
                "PRIMARY KEY(`accountId`))"
        const val CREATE_CREDIT_CARD_STATEMENTS =
            "CREATE TABLE IF NOT EXISTS `credit_card_statements` (`accountId` TEXT NOT NULL, " +
                "`nominalClosingEpochDay` INTEGER NOT NULL, " +
                "`actualClosingEpochDay` INTEGER, `actualDueEpochDay` INTEGER, " +
                "`issuerStatementBalance` REAL, `minimumDue` REAL, " +
                "PRIMARY KEY(`accountId`, `nominalClosingEpochDay`))"

        val STATEMENTS = listOf(ADD_ACCOUNT_TYPE, CREATE_CREDIT_CARDS, CREATE_CREDIT_CARD_STATEMENTS)
    }
}
