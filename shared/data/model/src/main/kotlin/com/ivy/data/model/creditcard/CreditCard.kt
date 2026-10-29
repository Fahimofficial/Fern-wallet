package com.ivy.data.model.creditcard

import com.ivy.data.model.AccountId
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Card-specific details for an account whose type is CREDIT_CARD.
 * Linked 1:1 to the account by [accountId]; the account holds name, currency, color and order.
 *
 * Only [accountId] is required: a user may track a card without product, limit or schedule.
 */
@Suppress("DataClassTypedIDs") // a card is keyed by its AccountId; bank/product ids are catalogue slugs
data class CreditCard(
    val accountId: AccountId,
    /** Catalogue bank id (see CardCatalogue). Null for custom banks. */
    val bankId: String?,
    /** Free-text bank name for issuers that aren't in the catalogue. */
    val customBankName: String?,
    /** Catalogue product id. Null for custom products. */
    val productId: String?,
    /** Product name as shown to the user (catalogue name or custom text). */
    val productName: String?,
    val network: CardNetwork?,
    /** Exactly 4 digits or null. Only the last four digits are ever stored. */
    val lastFour: String?,
    val creditLimit: BigDecimal?,
    /** Configured statement closing day 1..31 (clamped to short months). */
    val statementDay: Int?,
    val dueDateRule: DueDateRule?,
    /** Outstanding owed when the card was added to Fern Wallet (not an expense). */
    val openingOutstanding: BigDecimal,
    /** Date the opening outstanding applies from. */
    val openingDate: LocalDate,
    val status: CardStatus,
    val appearance: CardAppearance,
    val remindersEnabled: Boolean,
    /** Days before the due date to remind; 0 means only on the due date. */
    val reminderDaysBefore: Int,
)

enum class CardNetwork(val displayName: String) {
    RUPAY("RuPay"),
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express"),
    DINERS("Diners Club"),
    JCB("JCB"),
    OTHER("Other"),
}

enum class CardStatus {
    ACTIVE,
    INACTIVE,

    /** Closed cards keep their full history but are hidden from new-transaction pickers. */
    CLOSED,
}

/** Generic Fern Wallet card designs. Never presented as official issuer artwork. */
enum class CardAppearance {
    FERN,
    MIDNIGHT,
    OCEAN,
    SUNSET,
    GRAPHITE,
    ROSE,
}

/** How the payment due date is derived from a statement closing date. */
sealed interface DueDateRule {
    /** Due N days after the statement closes (common for Indian issuers, e.g. 15–20). */
    data class DaysAfterStatement(val days: Int) : DueDateRule

    /** Due on a fixed day of the month following the closing date (clamped to short months). */
    data class DayOfMonth(val day: Int) : DueDateRule
}

/**
 * User-entered corrections for one billing cycle, keyed by the cycle's nominal closing date
 * (the date computed from [CreditCard.statementDay]).
 * Values here come from the issuer's statement and take precedence over calculated ones.
 */
@Suppress("DataClassTypedIDs") // keyed by the card's AccountId
data class StatementOverride(
    val accountId: AccountId,
    val nominalClosingDate: LocalDate,
    val actualClosingDate: LocalDate?,
    val actualDueDate: LocalDate?,
    val issuerStatementBalance: BigDecimal?,
    val minimumDue: BigDecimal?,
)
