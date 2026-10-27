package com.ivy.legacy.utils

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(TestParameterInjector::class)
class ShortenAmountTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setup() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    enum class Case(val amount: Double, val expected: String) {
        BALANCE_FROM_SCREENSHOT(114_434.33, "114.43k"),
        THOUSAND(1_000.0, "1k"),
        THOUSAND_AND_NINE(1_009.0, "1.01k"),
        ONE_AND_HALF_K(1_500.0, "1.5k"),
        ROUND_HALF_UP_K(1_005.0, "1.01k"),

        // Previously rendered as "1000.00k"
        ALMOST_MILLION(999_999.0, "1m"),
        ROUNDS_TO_MILLION(999_995.0, "1m"),
        JUST_BELOW_ROUNDING(994_999.0, "995k"),

        // Between 995,000 and 999,995: stays in "k" until rounding reaches 1000.00k
        K_995(995_000.0, "995k"),
        K_997_5(997_500.0, "997.5k"),
        K_999_99(999_990.0, "999.99k"),
        K_999_994(999_994.0, "999.99k"),
        K_999_995_PROMOTES(999_995.0, "1m"),
        M_995(995_000_000.0, "995m"),
        M_999_99(999_990_000.0, "999.99m"),
        M_999_995_PROMOTES(999_995_000.0, "1b"),
        NEGATIVE_995K(-995_000.0, "-995k"),

        // Below 1,000: only promoted when rounding reaches 1000
        BELOW_K(999.99, "999.99"),
        BELOW_K_PROMOTES(999.996, "1k"),

        // Largest unit is never promoted further
        HUGE(2_500_000_000_000.0, "2500b"),
        MILLIONS(2_345_678.0, "2.35m"),

        // Previously rendered as "1000.00m"
        ALMOST_BILLION(999_999_999.0, "1b"),
        NEGATIVE(-1_500.0, "-1.5k"),
        NEGATIVE_MILLION(-999_999.0, "-1m"),

        // Previously used Double.toString(): "500.0", "1.0E-4"
        SMALL(500.0, "500"),
        SMALL_DECIMALS(12.345, "12.35"),
        TINY(0.0001, "0"),
        ZERO(0.0, "0"),
    }

    @Test
    fun `shortens amounts with correct rounding and unit promotion`(
        @TestParameter case: Case
    ) {
        shortenAmount(case.amount) shouldBe case.expected
    }

    @Test
    fun `uses the locale decimal separator`() {
        Locale.setDefault(Locale.GERMANY)
        shortenAmount(114_434.33) shouldBe "114,43k"
    }
}
