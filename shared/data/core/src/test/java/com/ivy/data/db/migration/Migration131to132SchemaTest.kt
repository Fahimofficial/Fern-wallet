package com.ivy.data.db.migration

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

/**
 * JVM guard for 131 -> 132: the migration adds exactly the columns Room expects, all nullable
 * with no default, and nothing else in the accounts table changes.
 * (IvyRoomDatabaseMigrationTest runs the migration itself on a device.)
 */
class Migration131to132SchemaTest {
    private fun accountsFields(version: Int): List<JsonObject> =
        Json.parseToJsonElement(File("schemas/com.ivy.data.db.IvyRoomDatabase/$version.json").readText())
            .jsonObject["database"]!!.jsonObject["entities"]!!.jsonArray
            .map { it.jsonObject }
            .single { it["tableName"]!!.jsonPrimitive.content == "accounts" }["fields"]!!.jsonArray
            .map { it.jsonObject }

    private fun JsonObject.name() = this["columnName"]!!.jsonPrimitive.content

    @Test
    fun `adds only the appearance columns`() {
        val before = accountsFields(131).map { it.name() }
        val after = accountsFields(132).map { it.name() }
        after.filterNot { it in before } shouldContainExactly
            Migration131to132_AccountAppearance.COLUMNS.map { it.first }
        after.containsAll(before) shouldBe true
    }

    @Test
    fun `new columns are nullable, without defaults, with the migration's affinity`() {
        val fields = accountsFields(132).associateBy { it.name() }
        Migration131to132_AccountAppearance.COLUMNS.forEach { (name, affinity) ->
            val field = fields.getValue(name)
            field["affinity"]!!.jsonPrimitive.content shouldBe affinity
            // Room omits notNull for nullable columns.
            (field["notNull"]?.jsonPrimitive?.content ?: "false") shouldBe "false"
            field["defaultValue"] shouldBe null
        }
    }
}
