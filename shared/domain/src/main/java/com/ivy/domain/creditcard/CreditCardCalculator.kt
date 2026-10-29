package com.ivy.domain.creditcard

import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.data.model.creditcard.StatementOverride
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure credit-card billing engine. It never reads or writes the database: callers pass the
 * card's ledger (see [CardLedger]) and the user's statement overrides.
 *
 * Accounting rules (single ledger; see docs/Credit-Cards.md):
 * - CHARGE (purchase, fee, interest, cash withdrawal) increases the outstanding amount.
 * - CREDIT (refund, reversal, cashback) and PAYMENT (transfer into the card) decrease it.
 * - OPENING is the outstanding entered when the card was added; it is not an expense.
 *
 * Billing-cycle rules:
 * - The nominal closing date of a month is `min(statementDay, lengthOfMonth)`, so day 31
 *   closes on Feb 28/29, Apr 30, ... An issuer-provided closing date replaces it.
 * - A transaction dated ON the closing date belongs to that statement. The period of a statement
 *   is (previous closing, closing].
 * - A statement is closed once its closing date has passed (today > closing).
 * - Statement balance = previous statement balance + charges - credits - payments in the period
 *   (i.e. the outstanding at closing). Later payments never change it; they reduce the
 *   remaining due instead.
 */
object CreditCardCalculator {

    fun nominalClosingDate(month: YearMonth, statementDay: Int): LocalDate =
        month.atDay(statementDay.coerceIn(1, month.lengthOfMonth()))

    fun dueDate(closingDate: LocalDate, rule: DueDateRule?): LocalDate? = when (rule) {
        null -> null
        is DueDateRule.DaysAfterStatement -> closingDate.plusDays(rule.days.toLong())
        is DueDateRule.DayOfMonth -> {
            val sameMonth = nominalClosingDate(YearMonth.from(closingDate), rule.day)
            if (sameMonth.isAfter(closingDate)) {
                sameMonth
            } else {
                nominalClosingDate(YearMonth.from(closingDate).plusMonths(1), rule.day)
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun overview(
        card: CreditCard,
        ledger: List<CardLedgerEntry>,
        overrides: List<StatementOverride>,
        today: LocalDate,
    ): CardOverview {
        val entries = (ledger + openingEntry(card)).sortedBy { it.date }
        val outstanding = entries.sumOf { it.outstandingEffect }
        val limit = card.creditLimit

        val statementDay = card.statementDay
        val cycles = if (statementDay != null) {
            buildCycles(statementDay, entries, overrides, today, card)
        } else {
            emptyList()
        }
        val closed = cycles.filter { it.closingDate.isBefore(today) }
        val open = cycles.firstOrNull { !it.closingDate.isBefore(today) }

        var previousBalance = BigDecimal.ZERO
        val statements = closed.mapIndexed { index, cycle ->
            val carriedIn = previousBalance
            val inPeriod = entries.filter { it.date in cycle.periodStart..cycle.closingDate }
            val opening = inPeriod.filter { it.kind == CardLedgerEntry.Kind.OPENING }
                .sumOf { it.outstandingEffect }
            val charges = inPeriod.filter { it.kind == CardLedgerEntry.Kind.CHARGE }.sumOf { it.amount }
            val credits = inPeriod.filter { it.kind == CardLedgerEntry.Kind.CREDIT }.sumOf { it.amount }
            val payments = inPeriod.filter { it.kind == CardLedgerEntry.Kind.PAYMENT }.sumOf { it.amount }
            val statementBalance = carriedIn + opening + charges - credits - payments

            val afterClosing = entries.filter { it.date.isAfter(cycle.closingDate) && !it.date.isAfter(today) }
            val paidSinceClosing = afterClosing
                .filter { it.kind == CardLedgerEntry.Kind.PAYMENT || it.kind == CardLedgerEntry.Kind.CREDIT }
                .sumOf { it.amount }

            val override = cycle.override
            val basis = override?.issuerStatementBalance ?: statementBalance
            val remaining = (basis - paidSinceClosing).max(BigDecimal.ZERO)
            val minimumRemaining = override?.minimumDue?.let { (it - paidSinceClosing).max(BigDecimal.ZERO) }
            val isLatest = index == closed.lastIndex
            val dueDate = override?.actualDueDate ?: dueDate(cycle.closingDate, card.dueDateRule)

            val status = when {
                basis.signum() <= 0 -> StatementStatus.NOTHING_DUE
                remaining.signum() == 0 -> StatementStatus.PAID
                // With a long due-date rule an older bill can still be ahead of its due date.
                !isLatest && (dueDate == null || today.isAfter(dueDate)) -> StatementStatus.CARRIED_FORWARD
                dueDate != null && today.isAfter(dueDate) -> StatementStatus.OVERDUE
                dueDate != null && today == dueDate -> StatementStatus.DUE_TODAY
                paidSinceClosing.signum() > 0 -> StatementStatus.PARTIALLY_PAID
                else -> StatementStatus.UPCOMING
            }
            previousBalance = statementBalance

            CardStatement(
                nominalClosingDate = cycle.nominalClosingDate,
                periodStart = cycle.periodStart,
                closingDate = cycle.closingDate,
                closingDateFromIssuer = override?.actualClosingDate != null,
                dueDate = dueDate,
                dueDateFromIssuer = override?.actualDueDate != null,
                previousBalance = carriedIn,
                openingBalance = opening,
                charges = charges,
                credits = credits,
                payments = payments,
                statementBalance = statementBalance,
                issuerStatementBalance = override?.issuerStatementBalance,
                minimumDue = override?.minimumDue,
                minimumDueRemaining = minimumRemaining,
                paidSinceClosing = paidSinceClosing,
                remainingDue = if (basis.signum() <= 0) BigDecimal.ZERO else remaining,
                status = status,
                isLatest = isLatest,
                entries = inPeriod.sortedByDescending { it.date },
            )
        }

        val unbilledFrom = closed.lastOrNull()?.closingDate?.plusDays(1)
        val unbilled = if (statementDay == null) {
            emptyList()
        } else {
            entries.filter { unbilledFrom == null || !it.date.isBefore(unbilledFrom) }
        }

        return CardOverview(
            outstanding = outstanding,
            creditLimit = limit,
            availableCredit = limit?.let { it - outstanding },
            utilisation = limit?.takeIf { it.signum() > 0 }?.let {
                outstanding.max(BigDecimal.ZERO).divide(it, UTILISATION_SCALE, RoundingMode.HALF_UP)
            },
            currentCycle = open?.let { CycleWindow(it.periodStart, it.closingDate) },
            unbilledCharges = unbilled.filter { it.kind == CardLedgerEntry.Kind.CHARGE }.sumOf { it.amount },
            unbilledCredits = unbilled.filter {
                it.kind == CardLedgerEntry.Kind.CREDIT || it.kind == CardLedgerEntry.Kind.PAYMENT
            }.sumOf { it.amount },
            unbilledEntries = unbilled.sortedByDescending { it.date },
            statements = statements.reversed(),
        )
    }

    private data class Cycle(
        val nominalClosingDate: LocalDate,
        val periodStart: LocalDate,
        val closingDate: LocalDate,
        val override: StatementOverride?,
    )

    private fun buildCycles(
        statementDay: Int,
        entries: List<CardLedgerEntry>,
        overrides: List<StatementOverride>,
        today: LocalDate,
        card: CreditCard,
    ): List<Cycle> {
        val byNominal = overrides.associateBy { it.nominalClosingDate }
        // Saved bank statements count too: cycles must start early enough to include every one of
        // them, or a saved statement before the first transaction/opening date would be dropped.
        val savedDates = overrides.flatMap { listOfNotNull(it.nominalClosingDate, it.actualClosingDate) }
        val earliest = (entries.map { it.date } + card.openingDate + savedDates).min()
        var month = YearMonth.from(earliest)
        if (nominalClosingDate(month, statementDay).isBefore(earliest)) month = month.plusMonths(1)

        val cycles = mutableListOf<Cycle>()
        var periodStart = earliest
        var previousClosing: LocalDate? = null
        // Closed cycles plus the one open cycle (contains today), bounded for safety.
        var reachedOpenCycle = false
        while (!reachedOpenCycle && cycles.size < MAX_CYCLES) {
            val nominal = nominalClosingDate(month, statementDay)
            val override = byNominal[nominal]
            val issuerClosing = override?.actualClosingDate ?: nominal
            // Ignore an issuer date that would put cycles out of order.
            val closing = if (isAfterPrevious(issuerClosing, previousClosing)) issuerClosing else nominal
            if (isAfterPrevious(closing, previousClosing)) {
                cycles += Cycle(nominal, periodStart, closing, override)
                reachedOpenCycle = !closing.isBefore(today)
                previousClosing = closing
                periodStart = closing.plusDays(1)
            }
            month = month.plusMonths(1)
        }
        return cycles
    }

    private fun isAfterPrevious(closing: LocalDate, previous: LocalDate?): Boolean =
        previous == null || closing.isAfter(previous)

    private fun openingEntry(card: CreditCard): List<CardLedgerEntry> =
        if (card.openingOutstanding.signum() == 0) {
            emptyList()
        } else {
            listOf(
                CardLedgerEntry(
                    transactionId = null,
                    date = card.openingDate,
                    amount = card.openingOutstanding.abs(),
                    kind = CardLedgerEntry.Kind.OPENING,
                    openingIsCreditBalance = card.openingOutstanding.signum() < 0,
                )
            )
        }

    private const val MAX_CYCLES = 1200 // 100 years of monthly statements
    private const val UTILISATION_SCALE = 4
}

private inline fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal =
    fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }
