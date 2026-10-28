package com.ivy.creditcards.pay

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.creditcards.edit.DateRow
import com.ivy.creditcards.ui.AmountLine
import com.ivy.creditcards.ui.CardScreen
import com.ivy.creditcards.ui.ChoiceChip
import com.ivy.creditcards.ui.Disclaimer
import com.ivy.creditcards.ui.DueTone
import com.ivy.creditcards.ui.FormField
import com.ivy.creditcards.ui.InfoCard
import com.ivy.creditcards.ui.PrimaryButton
import com.ivy.creditcards.ui.SectionTitle
import com.ivy.creditcards.ui.color
import com.ivy.creditcards.ui.money
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.navigation.PayCreditCardScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R

@Composable
fun PayCreditCardScreenImpl(screen: PayCreditCardScreen) {
    val viewModel: PayCreditCardViewModel = viewModel()
    LaunchedEffect(screen) { viewModel.start(screen) }
    PayCreditCardUi(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

@Composable
internal fun PayCreditCardUi(state: PayState, onEvent: (PayEvent) -> Unit) {
    val nav = navigation()
    val defaultTitle = stringResource(R.string.cc_payment_title, state.cardName)
    CardScreen(
        title = stringResource(R.string.cc_pay_card),
        onBack = { nav.back() },
        backDescription = stringResource(R.string.cc_back),
        bottomBar = {
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = stringResource(R.string.cc_record_payment),
                enabled = !state.loading && state.sources.isNotEmpty(),
                onClick = { onEvent(PayEvent.Save(defaultTitle)) }
            )
        }
    ) {
        if (state.loading) return@CardScreen
        Column(Modifier.verticalScroll(rememberScrollState())) {
            InfoCard(Modifier.padding(top = 8.dp)) {
                Text(
                    state.cardName,
                    style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold)
                )
                AmountLine(
                    label = stringResource(
                        if (state.outstanding.signum() < 0) R.string.cc_credit_balance else R.string.cc_outstanding
                    ),
                    value = state.outstanding.abs().money(state.currency)
                )
                state.remainingDue?.let {
                    AmountLine(label = stringResource(R.string.cc_remaining_due), value = it.money(state.currency))
                }
                state.minimumDueRemaining?.let {
                    AmountLine(
                        label = stringResource(R.string.cc_minimum_due_remaining),
                        value = it.money(state.currency)
                    )
                }
            }

            SectionTitle(stringResource(R.string.cc_amount))
            QuickAmounts(state, onEvent)
            FormField(
                value = state.amount,
                onValueChange = { onEvent(PayEvent.Amount(it)) },
                label = stringResource(R.string.cc_amount_in, state.currency),
                error = if (PayError.AMOUNT_INVALID in state.errors) stringResource(R.string.cc_error_amount) else null,
                keyboardType = KeyboardType.Decimal,
            )
            state.overpayment?.let {
                InfoCard {
                    Text(
                        text = stringResource(R.string.cc_overpayment_warning, it.money(state.currency)),
                        style = UI.typo.b2.style(
                            color = DueTone.WARNING.color(UI.colors),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            SectionTitle(stringResource(R.string.cc_pay_from))
            if (state.sources.isEmpty()) {
                Disclaimer(stringResource(R.string.cc_no_source_accounts))
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.sources.forEach { src ->
                    ChoiceChip(
                        text = if (src.currency == state.currency) src.name else "${src.name} (${src.currency})",
                        selected = state.source?.id == src.id,
                        onClick = { onEvent(PayEvent.Source(src)) }
                    )
                }
            }
            if (PayError.SOURCE_REQUIRED in state.errors) Disclaimer(stringResource(R.string.cc_error_source))
            val source = state.source
            if (source != null && source.currency != state.currency) {
                FormField(
                    value = state.sourceAmount,
                    onValueChange = { onEvent(PayEvent.SourceAmount(it)) },
                    label = stringResource(R.string.cc_debited_from, source.name, source.currency),
                    supportingText = stringResource(R.string.cc_debited_hint),
                    error = if (PayError.SOURCE_AMOUNT_INVALID in state.errors) {
                        stringResource(R.string.cc_error_amount)
                    } else {
                        null
                    },
                    keyboardType = KeyboardType.Decimal,
                )
            }

            SectionTitle(stringResource(R.string.cc_details))
            DateRow(
                label = stringResource(R.string.cc_payment_date),
                date = state.date,
                maxDate = state.today,
                error = if (PayError.DATE_IN_FUTURE in state.errors) {
                    stringResource(
                        R.string.cc_error_future_date
                    )
                } else {
                    null
                },
                onDate = { onEvent(PayEvent.Date(it)) },
            )
            FormField(
                value = state.note,
                onValueChange = { onEvent(PayEvent.Note(it)) },
                label = stringResource(R.string.cc_note_optional),
                imeAction = ImeAction.Done,
            )
            if (PayError.SAVE_FAILED in state.errors) Disclaimer(stringResource(R.string.cc_error_save))
            Disclaimer(stringResource(R.string.cc_payment_hint))
        }
    }
}

@Composable
private fun QuickAmounts(state: PayState, onEvent: (PayEvent) -> Unit) {
    val options = listOfNotNull(
        state.remainingDue?.let { stringResource(R.string.cc_quick_statement) to it },
        state.minimumDueRemaining?.let { stringResource(R.string.cc_quick_minimum) to it },
        state.outstanding.takeIf { it.signum() > 0 }?.let { stringResource(R.string.cc_quick_full) to it },
    ).distinctBy { it.second.stripTrailingZeros() }
    if (options.isEmpty()) return
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, value) ->
            ChoiceChip(
                text = "$label · ${value.money(state.currency)}",
                selected = state.amount.isNotBlank() &&
                    state.amount.toBigDecimalOrNull()?.compareTo(value) == 0,
                onClick = { onEvent(PayEvent.Amount(value.toPlainString())) }
            )
        }
    }
}
