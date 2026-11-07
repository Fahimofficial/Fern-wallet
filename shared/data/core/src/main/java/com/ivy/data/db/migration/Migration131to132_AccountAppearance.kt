package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Account logos and backgrounds: nullable columns only, so every existing row reads NULL, which
 * maps to the original look (solid card in the account colour, no logo). Additive; no data moves.
 * Migration131to132SchemaTest checks the columns against Room's exported v132 schema.
 */
@Suppress("ClassNaming", "MagicNumber") // database versions
class Migration131to132_AccountAppearance : Migration(131, 132) {
    override fun migrate(db: SupportSQLiteDatabase) {
        STATEMENTS.forEach(db::execSQL)
    }

    companion object {
        /** Column name to SQLite affinity. */
        val COLUMNS = listOf(
            "logoKey" to "TEXT",
            "customLogoPath" to "TEXT",
            "backgroundStyle" to "TEXT",
            "backgroundColors" to "TEXT",
            "backgroundAngle" to "REAL",
            "backgroundImagePath" to "TEXT",
            "backgroundDim" to "REAL",
        )

        val STATEMENTS = COLUMNS.map { (name, type) -> "ALTER TABLE accounts ADD COLUMN `$name` $type" }
    }
}
