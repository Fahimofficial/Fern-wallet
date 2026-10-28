package com.ivy.domain.creditcard

import com.ivy.data.model.TransactionId
import java.math.BigDecimal
import java.time.LocalDate

/** One movement on a card, derived from the transactions table. [amount] is always >= 0. */
@Suppress("DataClassDefaultValues", "DataClassTypedIDs") // null transactionId marks the opening balance
data class CardLedgerEntry(
    val transactionId: TransactionId?,
    val date: LocalDate,
    val amount: BigDecimal,
    val kind: Kind,
    /** For [Kind.OPENING] only: the card started with money owed TO the user. */
    val openingIsCreditBalance: Boolean = false,
) {
    enum class Kind {
        /** Purchase, fee, interest, cash withdrawal (expense or transfer out of the card). */
        CHARGE,

        /** Refund, reversal, chargeback, cashback (income on the card). */
        CREDIT,

        /** Bill payment (transfer into the card). */
        PAYMENT,

        /** Outstanding entered when the card was added. */
        OPENING,
    }

    /** Signed effect on the outstanding amount (positive = more owed). */
    val outstandingEffect: BigDecimal
        get() = when (kind) {
            Kind.CHARGE -> amount
            Kind.CREDIT, Kind.PAYMENT -> amount.negate()
            Kind.OPENING -> if (openingIsCreditBalance) amount.negate() else amount
        }
}

data class CycleWindow(val start: LocalDate, val closingDate: LocalDate)

enum class StatementStatus {
    /** Statement balance was zero or a credit. */
    NOTHING_DUE,
    UPCOMING,
    DUE_TODAY,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,

    /** An older statement that wasn't fully paid; its balance carried into later statements. */
    CARRIED_FORWARD,
}

data class CardStatement(
    /** Key for user overrides: the closing date computed from the configured statement day. */
    val nominalClosingDate: LocalDate,
    val periodStart: LocalDate,
    val closingDate: LocalDate,
    val closingDateFromIssuer: Boolean,
    val dueDate: LocalDate?,
    val dueDateFromIssuer: Boolean,
    val previousBalance: BigDecimal,
    val openingBalance: BigDecimal,
    val charges: BigDecimal,
    val credits: BigDecimal,
    val payments: BigDecimal,
    /** Calculated by Fern Wallet from the recorded transactions. */
    val statementBalance: BigDecimal,
    /** Entered by the user from the issuer's statement, if any (takes precedence for "due"). */
    val issuerStatementBalance: BigDecimal?,
    val minimumDue: BigDecimal?,
    val minimumDueRemaining: BigDecimal?,
    val paidSinceClosing: BigDecimal,
    val remainingDue: BigDecimal,
    val status: StatementStatus,
    val isLatest: Boolean,
    val entries: List<CardLedgerEntry>,
) {
    /** The amount the user has to pay for this statement. */
    val amountDue: BigDecimal
        get() = (issuerStatementBalance ?: statementBalance).max(BigDecimal.ZERO)
}

data class CardOverview(
    /** Total owed now, including unbilled spending. Negative = credit balance. */
    val outstanding: BigDecimal,
    val creditLimit: BigDecimal?,
    /** limit - outstanding (exceeds the limit with a credit balance, negative when over limit). */
    val availableCredit: BigDecimal?,
    /** outstanding / limit, 0..1+ (null without a limit). */
    val utilisation: BigDecimal?,
    val currentCycle: CycleWindow?,
    val unbilledCharges: BigDecimal,
    val unbilledCredits: BigDecimal,
    val unbilledEntries: List<CardLedgerEntry>,
    /** Closed statements, newest first. Empty when no statement day is configured. */
    val statements: List<CardStatement>,
) {
    val latestStatement: CardStatement? get() = statements.firstOrNull()

    val creditBalance: BigDecimal
        get() = outstanding.negate().max(BigDecimal.ZERO)
}
