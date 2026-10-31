package com.ivy.legacy.domain.deprecated.logic

import com.ivy.data.model.IntervalType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Due dates of recurring planned payments. Pure, so it can be tested without a database.
 *
 * The k-th occurrence is computed from the start date (start + k × interval), not by adding the
 * interval to the previous date, so a payment on the 31st stays on the 31st after February
 * (falling back to the last day of shorter months). Months and years use UTC, like the rest of
 * the planned-payments code.
 */
object PlannedOccurrences {
    /** Upcoming instances are generated up to this many years from today. */
    const val HORIZON_YEARS = 3L

    /** At most this many upcoming (unpaid) instances per rule. */
    const val UPCOMING_LIMIT = 72

    private const val DAYS_IN_WEEK = 7L
    private const val MAX_ITERATIONS = 200_000

    data class Schedule(
        val start: Instant,
        val intervalType: IntervalType,
        val intervalN: Int,
    )

    fun occurrence(schedule: Schedule, index: Long): Instant {
        val steps = index * schedule.intervalN
        return when (schedule.intervalType) {
            IntervalType.DAY -> schedule.start.plus(steps, ChronoUnit.DAYS)
            IntervalType.WEEK -> schedule.start.plus(steps * DAYS_IN_WEEK, ChronoUnit.DAYS)
            IntervalType.MONTH -> schedule.start.atZone(ZoneOffset.UTC).plusMonths(steps).toInstant()
            IntervalType.YEAR -> schedule.start.atZone(ZoneOffset.UTC).plusYears(steps).toInstant()
        }
    }

    /**
     * Due dates to create after a rule's upcoming instances were deleted, when [paidCount]
     * instances were already paid. Like before, paid instances are assumed to be the first ones.
     */
    fun regenerate(schedule: Schedule, paidCount: Int, now: Instant): List<Instant> =
        collect(schedule, firstIndex = paidCount.toLong(), alreadyUpcoming = 0, now = now)

    /**
     * Due dates to add so the window reaches [HORIZON_YEARS] from [now] again: after the latest
     * existing upcoming instance, or after the [paidCount] paid ones when there is none. Returns
     * nothing when the window is already full.
     *
     * This runs automatically (app start), so it only adds due dates from today on. Past
     * occurrences without an instance were skipped by the user (skipping deletes the instance)
     * or fell outside an earlier window; recreating them would add overdue payments nobody asked for.
     */
    fun extend(
        schedule: Schedule,
        upcomingDueDates: List<Instant>,
        paidCount: Int,
        now: Instant,
    ): List<Instant> {
        val afterExisting = upcomingDueDates.maxOrNull()?.let { firstIndexAfter(schedule, it) }
            ?: paidCount.toLong()
        return collect(
            schedule = schedule,
            firstIndex = maxOf(afterExisting, firstIndexFrom(schedule, now)),
            alreadyUpcoming = upcomingDueDates.size,
            now = now
        )
    }

    private fun firstIndexAfter(schedule: Schedule, last: Instant): Long = firstIndexWhere(schedule) {
        it.isAfter(last) && !inSameCalendarPeriod(schedule.intervalType, it, last)
    }

    // Due today counts as upcoming, whatever the time of day.
    private fun firstIndexFrom(schedule: Schedule, now: Instant): Long {
        val today = now.truncatedTo(ChronoUnit.DAYS)
        return firstIndexWhere(schedule) { !it.isBefore(today) }
    }

    private inline fun firstIndexWhere(schedule: Schedule, predicate: (Instant) -> Boolean): Long {
        var index = 0L
        while (index < MAX_ITERATIONS && !predicate(occurrence(schedule, index))) {
            index++
        }
        return index
    }

    private fun collect(
        schedule: Schedule,
        firstIndex: Long,
        alreadyUpcoming: Int,
        now: Instant,
    ): List<Instant> {
        val windowStart = if (schedule.start.isAfter(now)) schedule.start else now
        val windowEnd = windowStart.atZone(ZoneOffset.UTC).plusYears(HORIZON_YEARS).toInstant()
        val result = mutableListOf<Instant>()
        var index = firstIndex
        while (alreadyUpcoming + result.size < UPCOMING_LIMIT) {
            val date = occurrence(schedule, index)
            if (!date.isBefore(windowEnd)) break
            result += date
            index++
        }
        return result
    }

    // Instances created before occurrences were computed from the start date could drift (31 Jan,
    // 28 Feb, 28 Mar...). An instance in the same month (or year) as an existing one is treated as
    // that one, so topping up never creates a second payment for the same month.
    private fun inSameCalendarPeriod(type: IntervalType, a: Instant, b: Instant): Boolean = when (type) {
        IntervalType.DAY, IntervalType.WEEK -> false
        IntervalType.MONTH -> YearMonth.from(a.atZone(ZoneOffset.UTC)) == YearMonth.from(b.atZone(ZoneOffset.UTC))
        IntervalType.YEAR -> a.atZone(ZoneOffset.UTC).year == b.atZone(ZoneOffset.UTC).year
    }
}
