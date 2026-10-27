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
