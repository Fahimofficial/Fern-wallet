package com.ivy.legacy.data.model

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(TestParameterInjector::class)
class TimePeriodCurrentMonthTest {

    enum class Case(
        val startDayOfMonth: Int,
        val today: LocalDate,
        val expectedMonth: Int,
        val expectedYear: Int,
    ) {
        // Documented behaviour (unchanged)
        START_1_MID_MONTH(1, LocalDate.of(2026, 11, 10), 11, 2026),
        START_10_BEFORE_START(10, LocalDate.of(2026, 11, 9), 10, 2026),
        START_10_ON_START(10, LocalDate.of(2026, 11, 10), 11, 2026),
        START_10_JANUARY_BEFORE_START(10, LocalDate.of(2026, 1, 9), 12, 2025),

        // Regression: start day > month length. Today must be inside the returned period.
        START_31_LAST_DAY_OF_FEB(31, LocalDate.of(2026, 2, 28), 2, 2026),
        START_31_LAST_DAY_OF_LEAP_FEB(31, LocalDate.of(2028, 2, 29), 2, 2028),
        START_30_FEB_27(30, LocalDate.of(2026, 2, 27), 1, 2026),
        START_31_LAST_DAY_OF_APRIL(31, LocalDate.of(2026, 4, 30), 4, 2026),
        START_31_APRIL_29(31, LocalDate.of(2026, 4, 29), 3, 2026),
        START_31_ON_31ST(31, LocalDate.of(2026, 3, 31), 3, 2026),
    }

    @Test
    fun `current month period contains today`(@TestParameter case: Case) {
        val periodDate = TimePeriod.currentMonthPeriodDate(
            startDayOfMonth = case.startDayOfMonth,
            today = case.today,
        )

        periodDate.monthValue shouldBe case.expectedMonth
        periodDate.year shouldBe case.expectedYear
    }
}
