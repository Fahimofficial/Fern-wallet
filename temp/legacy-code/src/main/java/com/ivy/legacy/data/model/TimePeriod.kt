package com.ivy.legacy.data.model

import androidx.compose.runtime.Immutable
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.legacy.utils.endOfMonth
import com.ivy.legacy.utils.startOfMonth
import com.ivy.legacy.utils.withDayOfMonthSafe
import com.ivy.ui.time.TimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private const val MonthNameAbbreviationLength = 3

@Suppress("DataClassFunctions")
@Immutable
data class TimePeriod(
    val month: Month? = null,
    val year: Int? = null,
    val fromToRange: FromToTimeRange? = null,
    val lastNRange: LastNTimeRange? = null,
) {
    companion object {
        /**
         * Examples:
         * 1. startDateOfMonth = 1, today = Nov. 10
         * return Nov. 1 - Nov. 30
         *
         * 2. startDateOfMonth = 10, today = Nov. 9
         * return Oct. 10 - Nov. 9
         *
         * 3. startDateOfMonth = 10, today = Nov. 10
         * return Nov. 10 - Dec. 9
         */
        fun currentMonth(startDayOfMonth: Int): TimePeriod = currentMonth(
            startDayOfMonth = startDayOfMonth,
            // Period boundaries are computed in the device's local time zone
            // (see toRange), so "today" must be local too. Using UTC here picked the
            // wrong month around midnight for users far from UTC.
            today = LocalDate.now(),
        )

        /**
         * 4. startDateOfMonth = 31, today = Feb. 28 (non-leap)
         * return Feb. 28 - Mar. 30 (the start day is clamped to the month length,
         * consistent with [toRange]); previously Jan. 31 - Feb. 27 was returned,
         * which didn't contain today.
         */
        fun currentMonth(startDayOfMonth: Int, today: LocalDate): TimePeriod {
            val periodDate = currentMonthPeriodDate(startDayOfMonth, today)
            return TimePeriod(
                month = Month.fromMonthValue(
                    periodDate.monthValue
                ),
                year = periodDate.year
            )
        }

        /**
         * Returns a date in the month whose period contains [today]
         * (pure; unit-tested in TimePeriodCurrentMonthTest).
         */
        fun currentMonthPeriodDate(startDayOfMonth: Int, today: LocalDate): LocalDate {
            // Examples month = Nov. startDate = 7; Period = from Nov (7) till Dec (6)
            // => new period starts if today => startDayOfMonth
            // The start day is clamped to the month length (e.g. 31 -> 28 in Feb.)
            val effectiveStartDay = startDayOfMonth.coerceAtMost(today.lengthOfMonth())
            val newPeriodStarted = today.dayOfMonth >= effectiveStartDay

            return if (newPeriodStarted) {
                // new monthly period has already started then observe it => current month
                today
            } else {
                // new monthly period hasn't yet started then observe the ongoing one => previous month
                today.minusMonths(1)
            }
        }
    }

    fun isValid(): Boolean =
        month != null || fromToRange != null || lastNRange != null

    fun toRange(
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
        timeProvider: TimeProvider,
    ): FromToTimeRange = with(timeConverter) {
        when {
            month != null -> {
                val date = if (year != null) month.toDate().withYear(year) else month.toDate()
                val (from, to) = if (startDateOfMonth != 1) {
                    customStartDayOfMonthPeriodRange(
                        date = date,
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                    )
                } else {
                    Pair(startOfMonth(date, timeConverter), endOfMonth(date, timeConverter))
                }

                FromToTimeRange(
                    from = from,
                    to = to
                )
            }

            fromToRange != null -> {
                fromToRange
            }

            lastNRange != null -> {
                FromToTimeRange(
                    from = lastNRange.fromDate(timeProvider),
                    to = timeProvider.utcNow()
                )
            }

            else -> {
                val date = timeProvider.localNow().toLocalDate()
                FromToTimeRange(
                    from = startOfMonth(date, timeConverter),
                    to = endOfMonth(date, timeConverter)
                )
            }
        }
    }

    private fun customStartDayOfMonthPeriodRange(
        date: LocalDate,
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
    ): Pair<Instant, Instant> = with(timeConverter) {
        val from = date
            .withDayOfMonthSafe(startDateOfMonth)
            .atStartOfDay()
            .toUTC()

        val to = date
            // startDayOfMonth != 1 just shift N day the month forward so to should +1 month
            .plusMonths(1)
            .withDayOfMonthSafe(startDateOfMonth)
            // e.g. Correct: 14.10-13.11 (Incorrect: 14.10-14.11)
            .minusDays(1)
            // Include the whole last day (consistent with endOfMonth)
            .atTime(LocalTime.MAX)
            .toUTC()

        from to to
    }

    fun toDisplayShort(
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
        timeProvider: TimeProvider,
        timeFormatter: TimeFormatter,
    ): String = with(timeFormatter) {
        when {
            month != null -> {
                if (startDateOfMonth == 1) {
                    displayMonthStartingOn1st(month = month, timeProvider)
                } else {
                    val range = toRange(
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                        timeProvider = timeProvider,
                    )
                    val style = TimeFormatter.Style.DateOnly(includeWeekDay = false)
                    "${range.from?.formatLocal(style)} - ${range.to?.formatLocal(style)}"
                }
            }

            fromToRange != null -> {
                fromToRange.toDisplay(timeFormatter)
            }

            lastNRange != null -> {
                "Last ${lastNRange.forDisplay()}"
            }

            else -> "Custom"
        }
    }

    fun toDisplayLong(
        startDateOfMonth: Int,
        timeProvider: TimeProvider,
        timeConverter: TimeConverter,
        timeFormatter: TimeFormatter
    ): String {
        return when {
            month != null -> {
                if (startDateOfMonth == 1) {
                    displayMonthStartingOn1st(month = month, timeProvider)
                } else {
                    toRange(
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                        timeProvider = timeProvider
                    ).toDisplay(timeFormatter)
                }
            }

            fromToRange != null -> {
                fromToRange.toDisplay(timeFormatter)
            }

            lastNRange != null -> {
                "the last ${lastNRange.forDisplay()}"
            }

            else -> {
                toRange(
                    startDateOfMonth = startDateOfMonth,
                    timeConverter = timeConverter,
                    timeProvider = timeProvider
                ).toDisplay(timeFormatter)
            }
        }
    }

    private fun displayMonthStartingOn1st(
        month: Month,
        timeProvider: TimeProvider,
    ): String {
        val year = year
        return if (year != null && timeProvider.localNow().year != year) {
            // not this year
            "${month.name.substring(0, MonthNameAbbreviationLength)}, $year"
        } else {
            // this year
            month.name
        }
    }
}
