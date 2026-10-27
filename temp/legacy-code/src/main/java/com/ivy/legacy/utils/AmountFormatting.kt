package com.ivy.legacy.utils

import com.ivy.wallet.domain.data.IvyCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.truncate

const val MILLION = 1000000
const val N_100K = 100000
const val THOUSAND = 1000

fun String.amountToDoubleOrNull(): Double? {
    return this.normalizeAmount().toDoubleOrNull()
}

fun String.amountToDouble(): Double {
    return this.normalizeAmount().toDouble()
}

fun String.normalizeAmount(): String {
    return this.removeGroupingSeparator()
        .normalizeDecimalSeparator()
}

fun String.normalizeExpression(): String {
    return this.removeGroupingSeparator()
        .normalizeDecimalSeparator()
}

fun String.removeGroupingSeparator(): String {
    return replace(localGroupingSeparator(), "")
}

fun String.normalizeDecimalSeparator(): String {
    return replace(localDecimalSeparator(), ".")
}

fun localDecimalSeparator(): String {
    return DecimalFormatSymbols.getInstance().decimalSeparator.toString()
}

fun localGroupingSeparator(): String {
    return DecimalFormatSymbols.getInstance().groupingSeparator.toString()
}

// Display Formatting
fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun Double.format(currencyCode: String): String {
    return this.format(IvyCurrency.fromCode(currencyCode))
}

fun Double.format(currency: IvyCurrency?): String {
    return if (currency?.isCrypto == true) {
        val result = this.formatCrypto()
        return when {
            result.lastOrNull() == localDecimalSeparator().firstOrNull() -> {
                val newResult = result.dropLast(1)
                if (newResult.isEmpty()) "0" else newResult
            }

            result.isEmpty() -> {
                "0"
            }

            else -> result
        }
    } else {
        formatFIAT()
    }
}

fun Double.formatCrypto(): String {
    val pattern = "###,###,##0.${"0".repeat(9)}"
    val format = DecimalFormat(pattern)
    val numberStringWithZeros = format.format(this)

    var lastTrailingZeroIndex: Int? = null
    for (i in numberStringWithZeros.lastIndex.downTo(0)) {
        if (numberStringWithZeros[i] == '0') {
            lastTrailingZeroIndex = i
        } else {
            break
        }
    }

    return if (lastTrailingZeroIndex != null) {
        numberStringWithZeros.substring(0, lastTrailingZeroIndex)
    } else {
        numberStringWithZeros
    }
}

private fun Double.formatFIAT(): String = DecimalFormat("#,##0.00").format(this)

private const val BILLION = 1_000_000_000L

/**
 * Shortens big amounts for compact display, e.g. 114_434.33 -> "114.43k".
 *
 * The value is rounded (HALF_UP, 2 decimals) *before* choosing the unit so that
 * values like 999_999 render as "1m" instead of "1000.00k". Trailing zeros are dropped
 * ("1.50k" -> "1.5k") and the device locale's decimal separator is used.
 */
fun shortenAmount(amount: Double): String {
    val value = amount.toBigDecimal()
    val absValue = value.abs()
    val units = listOf(
        BILLION.toBigDecimal() to "b",
        MILLION.toBigDecimal() to "m",
        THOUSAND.toBigDecimal() to "k",
    )
    // Units are checked from largest to smallest on the *rounded* value, so a rounding
    // carry (e.g. 999_999 -> 1.00m) is naturally promoted to the bigger unit.
    for ((unit, suffix) in units) {
        val scaled = absValue.divide(unit).setScale(2, RoundingMode.HALF_UP)
        if (scaled >= BigDecimal.ONE) {
            val sign = if (value.signum() < 0) "-" else ""
            return "$sign${formatShortDecimal(scaled)}$suffix"
        }
    }
    return formatShortDecimal(value.setScale(2, RoundingMode.HALF_UP))
}

private fun formatShortDecimal(value: BigDecimal): String {
    val format = DecimalFormat("0.##")
    format.roundingMode = RoundingMode.HALF_UP
    return format.format(value)
}

fun hasSignificantDecimalPart(number: Double): Boolean {
    // TODO: Review, might cause trouble when integrating crypto
    val intPart = number.toInt()
    return abs(number - intPart) >= 0.009
}

fun shouldShortAmount(amount: Double): Boolean {
    return abs(amount) >= N_100K
}

fun formatInt(number: Int): String {
    return DecimalFormat("#,###,###,###").format(number)
}

fun decimalPartFormatted(currency: String, value: Double): String {
    return if (IvyCurrency.fromCode(currency)?.isCrypto == true) {
        val decimalPartFormatted = value.formatCrypto()
            .split(localDecimalSeparator())
            .getOrNull(1) ?: "null"
        if (decimalPartFormatted.isNotBlank()) {
            "${localDecimalSeparator()}$decimalPartFormatted"
        } else {
            ""
        }
    } else {
        "${localDecimalSeparator()}${decimalPartFormattedFIAT(value)}"
    }
}

private fun decimalPartFormattedFIAT(value: Double): String {
    return DecimalFormat(".00").format(value)
        .split(localDecimalSeparator())
        .getOrNull(1)
        ?: value.toString()
            .split(localDecimalSeparator())
            .getOrNull(1)
        ?: "null"
}

fun Long.length() = when (this) {
    0L -> 1
    else -> log10(abs(toDouble())).toInt() + 1
}

fun formatInputAmount(
    currency: String,
    amount: String,
    newSymbol: String,
    decimalCountMax: Int = 2,
): String? {
    val newlyEnteredNumberString = amount + newSymbol

    val decimalPartString = newlyEnteredNumberString
        .split(localDecimalSeparator())
        .getOrNull(1)
    val decimalCount = decimalPartString?.length ?: 0

    val amountDouble = newlyEnteredNumberString.amountToDoubleOrNull()

    val decimalCountOkay = IvyCurrency.fromCode(currency)?.isCrypto == true ||
            decimalCount <= decimalCountMax
    if (amountDouble != null && decimalCountOkay) {
        val intPart = truncate(amountDouble).toInt()
        val decimalPartFormatted = if (decimalPartString != null) {
            "${localDecimalSeparator()}$decimalPartString"
        } else {
            ""
        }

        return formatInt(intPart) + decimalPartFormatted
    }

    return null
}

/**
toInt on numbers in the range (-1.0, 0.0) (exclusive of boundaries) will produce a positive int 0
So, this function append negative sign in that case
 */
fun integerPartFormatted(value: Double): String {
    val preciseValue = value.toBigDecimal()
    val formattedValue = DecimalFormat("###,###").format(preciseValue.toInt())
    return if (value > -1.0 && value < 0.0) {
        "-$formattedValue"
    } else {
        formattedValue
    }
}
