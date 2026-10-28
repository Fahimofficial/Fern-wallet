package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Card details for an account of type CREDIT_CARD. Shares the account's id (1:1).
 * Dates are stored as epoch days (LocalDate.toEpochDay) and amounts in the
 * account's currency, like every other amount in the app.
 */
@Suppress("DataClassDefaultValues")
@Keep
@Serializable
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    @SerialName("bankId")
    val bankId: String? = null,
    @SerialName("customBankName")
    val customBankName: String? = null,
    @SerialName("productId")
    val productId: String? = null,
    @SerialName("productName")
    val productName: String? = null,
    @SerialName("network")
    val network: String? = null,
    @SerialName("lastFour")
    val lastFour: String? = null,
    @SerialName("creditLimit")
    val creditLimit: Double? = null,
    @SerialName("statementDay")
    val statementDay: Int? = null,
    /** "DAYS_AFTER_STATEMENT" or "DAY_OF_MONTH" */
    @SerialName("dueRuleType")
    val dueRuleType: String? = null,
    @SerialName("dueRuleValue")
    val dueRuleValue: Int? = null,
    @SerialName("openingOutstanding")
    @ColumnInfo(defaultValue = "0")
    val openingOutstanding: Double = 0.0,
    @SerialName("openingDateEpochDay")
    val openingDateEpochDay: Long,
    @SerialName("status")
    @ColumnInfo(defaultValue = "ACTIVE")
    val status: String = "ACTIVE",
    @SerialName("appearance")
    @ColumnInfo(defaultValue = "FERN")
    val appearance: String = "FERN",
    @SerialName("remindersEnabled")
    @ColumnInfo(defaultValue = "1")
    val remindersEnabled: Boolean = true,
    @SerialName("reminderDaysBefore")
    @ColumnInfo(defaultValue = "3")
    val reminderDaysBefore: Int = 3,
)

/** User-entered issuer values for one billing cycle. */
@Suppress("DataClassDefaultValues")
@Keep
@Serializable
@Entity(
    tableName = "credit_card_statements",
    primaryKeys = ["accountId", "nominalClosingEpochDay"]
)
data class CreditCardStatementEntity(
    @SerialName("accountId")
    @Serializable(with = KSerializerUUID::class)
    val accountId: UUID,
    @SerialName("nominalClosingEpochDay")
    val nominalClosingEpochDay: Long,
    @SerialName("actualClosingEpochDay")
    val actualClosingEpochDay: Long? = null,
    @SerialName("actualDueEpochDay")
    val actualDueEpochDay: Long? = null,
    @SerialName("issuerStatementBalance")
    val issuerStatementBalance: Double? = null,
    @SerialName("minimumDue")
    val minimumDue: Double? = null,
)
