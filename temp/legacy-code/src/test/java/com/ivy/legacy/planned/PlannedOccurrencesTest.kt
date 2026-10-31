package com.ivy.legacy.planned

import com.ivy.data.model.IntervalType
import com.ivy.legacy.domain.deprecated.logic.PlannedOccurrences
import com.ivy.legacy.domain.deprecated.logic.PlannedOccurrences.Schedule
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

/** Occurrence dates and regeneration after a rule is saved. Top-ups: [PlannedOccurrencesExtendTest]. */
class PlannedOccurrencesTest {
    @Test
    fun `a new monthly rule gets 3 years of upcoming payments`() {
        val rule = Schedule(day("2026-11-05"), IntervalType.MONTH, 1)

        val dates = PlannedOccurrences.regenerate(rule, paidCount = 0, now = NOW)

        dates shouldHaveSize 36
        dates.first() shouldBe rule.start
        dates.last() shouldBe day("2029-10-05")
    }

    @Test
    fun `a rule started more than 3 years ago still gets upcoming payments`() {
        // The old generator stopped at start + 3 years, so this rule produced nothing.
        val rule = Schedule(day("2021-01-10"), IntervalType.YEAR, 1)

        val dates = PlannedOccurrences.regenerate(rule, paidCount = 6, now = NOW)

        dates.days() shouldBe listOf("2027-01-10", "2028-01-10", "2029-01-10")
    }

    @Test
    fun `paid instances are skipped from the start`() {
        val rule = Schedule(day("2026-01-01"), IntervalType.MONTH, 1)

        PlannedOccurrences.regenerate(rule, paidCount = 10, now = NOW).first() shouldBe day("2026-11-01")
    }

    @Test
    fun `at most 72 upcoming instances, e g a daily rule`() {
        val rule = Schedule(NOW, IntervalType.DAY, 1)

        PlannedOccurrences.regenerate(rule, paidCount = 0, now = NOW) shouldHaveSize
            PlannedOccurrences.UPCOMING_LIMIT
    }

    @Test
    fun `monthly payments on the 31st stay on the 31st after February`() {
        val rule = Schedule(day("2027-01-31"), IntervalType.MONTH, 1)

        PlannedOccurrences.regenerate(rule, paidCount = 0, now = NOW).take(4).days() shouldBe
            listOf("2027-01-31", "2027-02-28", "2027-03-31", "2027-04-30")
    }

    @Test
    fun `every 2 weeks`() {
        val rule = Schedule(day("2026-11-02"), IntervalType.WEEK, 2)

        PlannedOccurrences.regenerate(rule, paidCount = 0, now = NOW).take(3).days() shouldBe
            listOf("2026-11-02", "2026-11-16", "2026-11-30")
    }
}
