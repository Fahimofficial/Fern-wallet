package com.ivy.data.db.migration

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

/**
 * JVM guard for the 130 -> 131 migration: the CREATE statements must be exactly what Room
 * expects for the exported v131 schema, and the new account column must default to ASSET.
 * (The full migration is exercised on a device by IvyRoomDatabaseMigrationTest.)
 */
class Migration130to131SchemaTest {
    private val schema = Json.parseToJsonElement(
        File("schemas/com.ivy.data.db.IvyRoomDatabase/131.json").readText()
    ).jsonObject["database"]!!.jsonObject

    private fun entity(table: String) = schema["entities"]!!.jsonArray
        .map { it.jsonObject }
        .single { it["tableName"]!!.jsonPrimitive.content == table }

    private fun createSql(table: String) =
        entity(table)["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)

    @Test
    fun `schema version is 131`() {
        schema["version"]!!.jsonPrimitive.content shouldBe "131"
    }

    @Test
    fun `credit_cards create statement matches Room`() {
        Migration130to131_CreditCards.CREATE_CREDIT_CARDS shouldBe createSql("credit_cards")
    }

    @Test
    fun `credit_card_statements create statement matches Room`() {
        Migration130to131_CreditCards.CREATE_CREDIT_CARD_STATEMENTS shouldBe
            createSql("credit_card_statements")
    }

    @Test
    fun `accounts type column defaults to ASSET`() {
        val typeField = entity("accounts")["fields"]!!.jsonArray.map { it.jsonObject }
            .single { it["columnName"]!!.jsonPrimitive.content == "type" }

        typeField["affinity"]!!.jsonPrimitive.content shouldBe "TEXT"
        typeField["notNull"]!!.jsonPrimitive.content shouldBe "true"
        typeField["defaultValue"]!!.jsonPrimitive.content shouldBe "'ASSET'"
        Migration130to131_CreditCards.ADD_ACCOUNT_TYPE shouldBe
            "ALTER TABLE accounts ADD COLUMN type TEXT NOT NULL DEFAULT 'ASSET'"
    }
}
