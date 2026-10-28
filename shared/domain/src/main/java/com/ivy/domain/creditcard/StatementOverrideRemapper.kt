package com.ivy.domain.creditcard

import com.ivy.data.model.creditcard.StatementOverride
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Moves saved bank-statement values to a new statement day.
 *
 * Saved values are keyed by the nominal closing date of the schedule they were entered under,
 * so changing the statement day would silently detach them. Each saved statement is instead:
 * - pinned to the closing date it really had (`actualClosingDate`, or the old nominal date), so
 *   its period, balance, minimum payment and due date stay exactly as they were, and
 * - re-keyed to the nearest closing date of the new schedule, keeping keys unique and in order.
 *   Keys before the card's first cycle are never used: they'd never be applied (e.g. day 15 ->
 *   day 1 for a card opened on the 5th would otherwise land on the 1st, before the card existed).
 *
 * Statements without saved values simply follow the new schedule.
 */
object StatementOverrideRemapper {
    fun remap(
        overrides: List<StatementOverride>,
        newStatementDay: Int,
        openingDate: LocalDate,
    ): List<StatementOverride> {
        // The calculator's first cycle closes on or after the opening date, never earlier.
        val firstKey = firstNominalOnOrAfter(openingDate, newStatementDay)
        var previousKey: LocalDate? = firstKey.minusDays(1)
        return overrides
            .sortedBy { it.effectiveClosingDate }
            .map { override ->
                val closing = override.effectiveClosingDate
                var key = nearestNominal(closing, newStatementDay)
                // Two saved statements must never share (or reorder) a key.
                val prev = previousKey
                if (prev != null && !key.isAfter(prev)) key = firstNominalOnOrAfter(prev.plusDays(1), newStatementDay)
                previousKey = key
                override.copy(nominalClosingDate = key, actualClosingDate = closing)
            }
    }

    private val StatementOverride.effectiveClosingDate: LocalDate
        get() = actualClosingDate ?: nominalClosingDate

    private fun nearestNominal(date: LocalDate, statementDay: Int): LocalDate {
        val month = YearMonth.from(date)
        return listOf(month.minusMonths(1), month, month.plusMonths(1))
            .map { CreditCardCalculator.nominalClosingDate(it, statementDay) }
            .minWith(compareBy<LocalDate> { abs(ChronoUnit.DAYS.between(date, it)) }.thenBy { it })
    }

    private fun firstNominalOnOrAfter(date: LocalDate, statementDay: Int): LocalDate {
        val sameMonth = CreditCardCalculator.nominalClosingDate(YearMonth.from(date), statementDay)
        return if (sameMonth.isBefore(date)) {
            CreditCardCalculator.nominalClosingDate(YearMonth.from(date).plusMonths(1), statementDay)
        } else {
            sameMonth
        }
    }
}
