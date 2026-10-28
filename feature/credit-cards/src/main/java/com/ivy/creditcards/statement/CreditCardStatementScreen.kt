package com.ivy.creditcards.statement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.creditcards.details.TransactionRow
import com.ivy.creditcards.edit.DateRow
import com.ivy.creditcards.ui.AmountLine
import com.ivy.creditcards.ui.CardScreen
import com.ivy.creditcards.ui.Disclaimer
import com.ivy.creditcards.ui.DueSummary
import com.ivy.creditcards.ui.FormField
import com.ivy.creditcards.ui.InfoCard
import com.ivy.creditcards.ui.SecondaryButton
import com.ivy.creditcards.ui.SectionTitle
import com.ivy.creditcards.ui.VerticalSpace
import com.ivy.creditcards.ui.color
import com.ivy.creditcards.ui.labelRes
import com.ivy.creditcards.ui.mediumDate
import com.ivy.creditcards.ui.money
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.domain.creditcard.CardStatement
import com.ivy.navigation.CreditCardStatementScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R

@Composable
fun CreditCardStatementScreenImpl(screen: CreditCardStatementScreen) {
    val viewModel: CreditCardStatementViewModel = viewModel()
    LaunchedEffect(screen) { viewModel.start(screen) }
    CreditCardStatementUi(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

@Composable
internal fun CreditCardStatementUi(state: StatementState, onEvent: (StatementEvent) -> Unit) {
    val nav = navigation()
    val statement = state.statement
    CardScreen(
        title = statement?.let { stringResource(R.string.cc_statement_of, it.closingDate.mediumDate()) }
            ?: stringResource(R.string.cc_statement),
        onBack = { nav.back() },
        backDescription = stringResource(R.string.cc_back),
    ) {
        if (state.loading) return@CardScreen
        if (statement == null) {
            Disclaimer(stringResource(R.string.cc_statement_not_found))
            return@CardScreen
        }
        LazyColumn {
            item {
                Breakdown(state, statement)
                SecondaryButton(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    text = stringResource(R.string.cc_enter_issuer_values),
                    onClick = { onEvent(StatementEvent.EditIssuerValues) }
                )
                Disclaimer(stringResource(R.string.cc_statement_disclaimer))
                SectionTitle(
                    stringResource(R.string.cc_statement_transactions, state.rows.size)
                )
                if (state.rows.isEmpty()) Disclaimer(stringResource(R.string.cc_no_transactions_period))
            }
            items(state.rows, key = { it.id }) { row ->
                TransactionRow(row = row, currency = state.currency, today = state.today) {
                    onEvent(StatementEvent.OpenTransaction(row.id))
                }
            }
            item { VerticalSpace(32.dp) }
        }
    }

    state.issuerForm?.let { form -> IssuerDialog(form, state.invalidAmount, state.currency, onEvent) }
}

@Composable
private fun Breakdown(state: StatementState, s: CardStatement) {
    val c = state.currency
    InfoCard(Modifier.padding(top = 8.dp)) {
        val tone = DueSummary(s.status, s.dueDate, s.remainingDue, null).tone
        Text(
            stringResource(s.status.labelRes()),
            style = UI.typo.b2.style(color = tone.color(UI.colors), fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = stringResource(R.string.cc_period_range, s.periodStart.mediumDate(), s.closingDate.mediumDate()),
            style = UI.typo.nC.style(color = UI.colors.gray)
        )
        if (s.closingDateFromIssuer) {
            Text(
                stringResource(R.string.cc_closing_from_issuer),
                style = UI.typo.nC.style(color = UI.colors.gray)
            )
        }
        VerticalSpace(8.dp)
        AmountLine(stringResource(R.string.cc_previous_balance), s.previousBalance.money(c))
        if (s.openingBalance.signum() != 0) {
            AmountLine(stringResource(R.string.cc_opening_balance), s.openingBalance.money(c))
        }
        AmountLine(stringResource(R.string.cc_charges), "+ ${s.charges.money(c)}")
        AmountLine(stringResource(R.string.cc_refunds_credits), "− ${s.credits.money(c)}")
        AmountLine(stringResource(R.string.cc_payments), "− ${s.payments.money(c)}")
        AmountLine(
            label = stringResource(R.string.cc_statement_balance),
            value = s.statementBalance.money(c),
            emphasize = true,
            note = stringResource(R.string.cc_calculated),
        )
        s.issuerStatementBalance?.let {
            AmountLine(
                label = stringResource(R.string.cc_issuer_statement_balance),
                value = it.money(c),
                emphasize = true,
                note = stringResource(R.string.cc_from_issuer),
            )
        }
        s.minimumDue?.let {
            AmountLine(
                stringResource(R.string.cc_minimum_due),
                it.money(c),
                note = stringResource(R.string.cc_from_issuer)
            )
        }
        VerticalSpace(8.dp)
        AmountLine(stringResource(R.string.cc_paid_since_statement), s.paidSinceClosing.money(c))
        AmountLine(
            stringResource(R.string.cc_remaining_due),
            s.remainingDue.money(c),
            emphasize = true,
            valueColor = tone.color(UI.colors)
        )
        AmountLine(
            label = stringResource(R.string.cc_due_date),
            value = s.dueDate?.mediumDate() ?: stringResource(R.string.cc_not_set),
            note = when {
                s.dueDate == null -> stringResource(R.string.cc_set_due_rule_hint)
                s.dueDateFromIssuer -> stringResource(R.string.cc_from_issuer)
                else -> stringResource(R.string.cc_calculated)
            }
        )
    }
}

@Composable
private fun IssuerDialog(form: IssuerForm, invalid: Boolean, currency: String, onEvent: (StatementEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(StatementEvent.DismissIssuerValues) },
        title = { Text(stringResource(R.string.cc_issuer_values_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.cc_issuer_values_hint), style = UI.typo.nC.style(color = UI.colors.gray))
                DateRow(
                    label = stringResource(R.string.cc_actual_closing_date),
                    date = form.actualClosingDate,
                    placeholder = stringResource(R.string.cc_use_calculated),
                    onDate = { onEvent(StatementEvent.UpdateIssuerForm(form.copy(actualClosingDate = it))) },
                )
                DateRow(
                    label = stringResource(R.string.cc_actual_due_date),
                    date = form.actualDueDate,
                    placeholder = stringResource(R.string.cc_use_calculated),
                    onDate = { onEvent(StatementEvent.UpdateIssuerForm(form.copy(actualDueDate = it))) },
                )
                FormField(
                    value = form.issuerBalance,
                    onValueChange = { onEvent(StatementEvent.UpdateIssuerForm(form.copy(issuerBalance = it))) },
                    label = stringResource(R.string.cc_issuer_statement_balance),
                    keyboardType = KeyboardType.Decimal,
                    prefix = "$currency ",
                )
                FormField(
                    value = form.minimumDue,
                    onValueChange = { onEvent(StatementEvent.UpdateIssuerForm(form.copy(minimumDue = it))) },
                    label = stringResource(R.string.cc_minimum_due),
                    keyboardType = KeyboardType.Decimal,
                    prefix = "$currency ",
                    error = if (invalid) stringResource(R.string.cc_error_amount) else null,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(StatementEvent.SaveIssuerValues) }
            ) { Text(stringResource(R.string.cc_save)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(StatementEvent.ClearIssuerValues) }) {
                Text(stringResource(R.string.cc_clear_issuer_values))
            }
        }
    )
}
