package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.get
import com.ivy.design.system.appearance.readableOn
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.dateNowLocal
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.formatLocal
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gray
import java.time.LocalDate

// A day with no income or expense (e.g. only transfers) gets a one-line header.
private val CompactSpacerTop = 20.dp

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun HistoryDateDivider(
    date: LocalDate,
    spacerTop: Dp,
    baseCurrency: String,
    income: Double,
    expenses: Double
) {
    val today = dateNowLocal()
    val dateText = date.formatLocal(if (today.year == date.year) "MMMM dd." else "MMM dd. yyy")
    val dayText = when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> date.formatLocal("EEEE")
    }

    if (income == 0.0 && expenses == 0.0) {
        Spacer(Modifier.height(minOf(spacerTop, CompactSpacerTop)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(24.dp))
            Text(text = dateText, style = UI.typo.b2.style(fontWeight = FontWeight.ExtraBold))
            Spacer(Modifier.width(8.dp))
            Text(text = dayText, style = UI.typo.c.style(fontWeight = FontWeight.Bold, color = UI.colors.gray))
        }
        Spacer(Modifier.height(4.dp))
        return
    }

    Spacer(Modifier.height(spacerTop))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        Column {
            Text(
                text = dateText,
                style = UI.typo.b1.style(
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = dayText,
                style = UI.typo.c.style(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.weight(1f))

        val cashflow = income - expenses
        val background = UI.colors.pure
        // The sign carries the meaning; the colour (income / expense / grey) only reinforces it.
        val color = when {
            cashflow > 0 -> FernTheme.palette[ColorToken.Income].readableOn(background)
            cashflow < 0 -> FernTheme.palette[ColorToken.Expense].readableOn(background)
            else -> Gray
        }
        Text(
            text = "${if (cashflow > 0) "+" else ""}${cashflow.format(baseCurrency)} $baseCurrency",
            style = UI.typo.nB2.style(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )

        Spacer(Modifier.width(32.dp))
    }

    Spacer(Modifier.height(4.dp))
}

@Preview
@Composable
private fun Preview_Today() {
    IvyWalletComponentPreview {
        HistoryDateDivider(
            date = dateNowUTC(),
            spacerTop = 32.dp,
            baseCurrency = "BGN",
            income = 13.50,
            expenses = 256.13
        )
    }
}

@Preview
@Composable
private fun Preview_Yesterday() {
    IvyWalletComponentPreview {
        HistoryDateDivider(
            date = dateNowUTC().minusDays(1),
            spacerTop = 32.dp,
            baseCurrency = "BGN",
            income = 13.50,
            expenses = 256.13
        )
    }
}

@Preview
@Composable
private fun Preview_OneYear_Ago() {
    IvyWalletComponentPreview {
        HistoryDateDivider(
            date = dateNowUTC().minusYears(1),
            spacerTop = 32.dp,
            baseCurrency = "BGN",
            income = 13.50,
            expenses = 256.13
        )
    }
}
