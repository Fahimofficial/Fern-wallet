package com.ivy.creditcards.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.creditcards.ui.AmountLine
import com.ivy.creditcards.ui.CardLoading
import com.ivy.creditcards.ui.CardScreen
import com.ivy.creditcards.ui.ChoiceChip
import com.ivy.creditcards.ui.CreditCardVisual
import com.ivy.creditcards.ui.Disclaimer
import com.ivy.creditcards.ui.DueChip
import com.ivy.creditcards.ui.DueSummary
import com.ivy.creditcards.ui.DueTone
import com.ivy.creditcards.ui.FormField
import com.ivy.creditcards.ui.IconAction
import com.ivy.creditcards.ui.InfoCard
import com.ivy.creditcards.ui.PrimaryButton
import com.ivy.creditcards.ui.SecondaryButton
import com.ivy.creditcards.ui.SectionTitle
import com.ivy.creditcards.ui.UtilisationBar
import com.ivy.creditcards.ui.VerticalSpace
import com.ivy.creditcards.ui.color
import com.ivy.creditcards.ui.labelRes
import com.ivy.creditcards.ui.money
import com.ivy.creditcards.ui.shortDate
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.design.l0_system.IvyColors
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.domain.creditcard.CardOverview
import com.ivy.domain.creditcard.CardStatement
import com.ivy.navigation.CreditCardDetailsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Red
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun CreditCardDetailsScreenImpl(screen: CreditCardDetailsScreen) {
    val viewModel: CreditCardDetailsViewModel = viewModel()
    LaunchedEffect(screen) { viewModel.start(screen) }
    CreditCardDetailsUi(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

@Composable
internal fun CreditCardDetailsUi(state: CardDetailsState, onEvent: (CardDetailsEvent) -> Unit) {
    val nav = navigation()
    var confirmClose by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    CardScreen(
        title = state.name.ifBlank { stringResource(R.string.cc_credit_card) },
        onBack = { nav.back() },
        backDescription = stringResource(R.string.cc_back),
        actions = {
            if (state.visual != null) {
                IconAction(
                    icon = R.drawable.ic_edit,
                    description = stringResource(R.string.cc_edit_card),
                    onClick = { onEvent(CardDetailsEvent.Edit) }
                )
            }
        }
    ) {
        val overview = state.overview
        if (state.loading) {
            CardLoading()
            return@CardScreen
        }
        if (state.notFound || overview == null || state.visual == null) {
            Disclaimer(stringResource(R.string.cc_not_found))
            return@CardScreen
        }
        LazyColumn {
            item {
                CreditCardVisual(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    data = state.visual
                )
                Disclaimer(stringResource(R.string.cc_illustration_note))
                SummaryCard(overview = overview, currency = state.currency)
                ActionsRow(state = state, onEvent = onEvent)
                BillCard(state = state, overview = overview, onEvent = onEvent)
                CurrentCycle(state = state, overview = overview)
            }
            statementsSection(state = state, overview = overview, onEvent = onEvent)
            historySection(state = state, onEvent = onEvent)
            item {
                ManageSection(
                    status = state.status,
                    onClose = { confirmClose = true },
                    onReopen = { onEvent(CardDetailsEvent.SetStatus(CardStatus.ACTIVE)) },
                    onDelete = { confirmDelete = true },
                )
                VerticalSpace(32.dp)
            }
        }
    }

    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text(stringResource(R.string.cc_close_card_title)) },
            text = { Text(stringResource(R.string.cc_close_card_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClose = false
                    onEvent(CardDetailsEvent.SetStatus(CardStatus.CLOSED))
                }) { Text(stringResource(R.string.cc_close_card)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClose = false }) { Text(stringResource(R.string.cc_cancel)) }
            }
        )
    }
    if (confirmDelete) {
        val paymentTitle = stringResource(R.string.cc_deleted_card_payment, state.name)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.cc_delete_card_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.cc_delete_card_text,
                        state.transactionCount,
                        state.transactionCount
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onEvent(CardDetailsEvent.Delete(paymentTitle))
                }) { Text(stringResource(R.string.cc_delete_permanently), color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cc_cancel)) }
            }
        )
    }
}

@Composable
private fun SummaryCard(overview: CardOverview, currency: String) {
    InfoCard(Modifier.padding(top = 8.dp)) {
        val creditBalance = overview.outstanding.signum() < 0
        Text(
            text = stringResource(if (creditBalance) R.string.cc_credit_balance else R.string.cc_outstanding),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
        )
        Text(
            text = overview.outstanding.abs().money(currency),
            style = UI.typo.nH2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold)
        )
        if (creditBalance) {
            Text(
                text = stringResource(R.string.cc_credit_balance_hint),
                style = UI.typo.nC.style(color = UI.colors.gray)
            )
        }
        val limit = overview.creditLimit
        val available = overview.availableCredit
        if (limit != null && available != null) {
            VerticalSpace(12.dp)
            UtilisationBar(utilisation = overview.utilisation?.toFloat()?.coerceIn(0f, 1f) ?: 0f)
            VerticalSpace(8.dp)
            AmountLine(
                label = stringResource(R.string.cc_available_credit),
                value = available.money(currency),
                valueColor = if (available.signum() < 0) DueTone.DANGER.color(UI.colors) else UI.colors.pureInverse,
                note = if (available.signum() < 0) stringResource(R.string.cc_over_limit) else null,
            )
            AmountLine(label = stringResource(R.string.cc_credit_limit), value = limit.money(currency))
            overview.utilisation?.let {
                AmountLine(
                    label = stringResource(R.string.cc_utilisation),
                    value = "${it.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()}%"
                )
            }
        }
    }
}

@Composable
private fun ActionsRow(state: CardDetailsState, onEvent: (CardDetailsEvent) -> Unit) {
    if (state.status == CardStatus.CLOSED) return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cc_pay_card),
            onClick = { onEvent(CardDetailsEvent.Pay) }
        )
        VerticalSpace(8.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cc_add_charge),
                onClick = { onEvent(CardDetailsEvent.AddCharge) }
            )
            SecondaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cc_add_refund),
                onClick = { onEvent(CardDetailsEvent.AddCredit) }
            )
        }
    }
}

@Composable
private fun BillCard(state: CardDetailsState, overview: CardOverview, onEvent: (CardDetailsEvent) -> Unit) {
    SectionTitle(stringResource(R.string.cc_current_bill))
    val statement = overview.latestStatement
    if (statement == null) {
        Disclaimer(
            stringResource(
                if (state.statementDayConfigured) R.string.cc_no_statement_yet else R.string.cc_set_statement_day
            )
        )
        return
    }
    InfoCard(
        Modifier.clickable(role = Role.Button) { onEvent(CardDetailsEvent.OpenStatement(statement)) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cc_statement_of, statement.closingDate.shortDate(state.today)),
                style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold)
            )
            DueChip(
                due = DueSummary(
                    status = statement.status,
                    dueDate = statement.dueDate,
                    remaining = statement.remainingDue,
                    daysLeft = statement.dueDate?.let { ChronoUnit.DAYS.between(state.today, it) },
                ),
                currency = state.currency,
                today = state.today,
                hideAmounts = false,
            )
        }
        VerticalSpace(8.dp)
        AmountLine(
            label = stringResource(R.string.cc_amount_due),
            value = statement.amountDue.money(state.currency),
            note = if (statement.issuerStatementBalance != null) {
                stringResource(R.string.cc_from_issuer)
            } else {
                stringResource(R.string.cc_calculated)
            }
        )
        AmountLine(
            label = stringResource(R.string.cc_paid_since_statement),
            value = statement.paidSinceClosing.money(state.currency)
        )
        AmountLine(
            label = stringResource(R.string.cc_remaining_due),
            value = statement.remainingDue.money(state.currency),
            emphasize = true,
            valueColor = statusColor(statement, UI.colors)
        )
        statement.minimumDueRemaining?.let {
            AmountLine(label = stringResource(R.string.cc_minimum_due_remaining), value = it.money(state.currency))
        }
        AmountLine(
            label = stringResource(R.string.cc_due_date),
            value = statement.dueDate?.shortDate(state.today) ?: stringResource(R.string.cc_not_set),
            note = when {
                statement.dueDate == null -> stringResource(R.string.cc_set_due_rule_hint)
                statement.dueDateFromIssuer -> stringResource(R.string.cc_from_issuer)
                else -> stringResource(R.string.cc_calculated)
            }
        )
    }
}

private fun statusColor(statement: CardStatement, colors: IvyColors): Color = DueSummary(
    status = statement.status,
    dueDate = statement.dueDate,
    remaining = statement.remainingDue,
    daysLeft = null,
).tone.color(colors)

@Composable
private fun CurrentCycle(state: CardDetailsState, overview: CardOverview) {
    val cycle = overview.currentCycle ?: return
    SectionTitle(stringResource(R.string.cc_current_cycle))
    InfoCard {
        Text(
            text = stringResource(
                R.string.cc_period_range,
                cycle.start.shortDate(state.today),
                cycle.closingDate.shortDate(state.today)
            ),
            style = UI.typo.nC.style(color = UI.colors.gray)
        )
        AmountLine(
            label = stringResource(R.string.cc_unbilled_spending),
            value = overview.unbilledCharges.money(state.currency)
        )
        AmountLine(
            label = stringResource(R.string.cc_unbilled_credits),
            value = overview.unbilledCredits.money(state.currency)
        )
    }
}

private fun LazyListScope.statementsSection(
    state: CardDetailsState,
    overview: CardOverview,
    onEvent: (CardDetailsEvent) -> Unit,
) {
    if (overview.statements.isEmpty()) return
    item { SectionTitle(stringResource(R.string.cc_statements)) }
    items(overview.statements.take(MaxStatements), key = { "stmt-${it.nominalClosingDate}" }) { statement ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onEvent(CardDetailsEvent.OpenStatement(statement)) }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = statement.closingDate.shortDate(state.today),
                    style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(statement.status.labelRes()),
                    style = UI.typo.nC.style(color = statusColor(statement, UI.colors))
                )
            }
            Text(
                text = statement.amountDue.money(state.currency),
                style = UI.typo.nB2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold)
            )
        }
    }
}

private fun LazyListScope.historySection(state: CardDetailsState, onEvent: (CardDetailsEvent) -> Unit) {
    item {
        SectionTitle(stringResource(R.string.cc_transactions))
        FormField(
            value = state.query,
            onValueChange = { onEvent(CardDetailsEvent.Search(it)) },
            label = stringResource(R.string.cc_search_transactions),
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilter.entries.forEach { f ->
                ChoiceChip(
                    text = stringResource(
                        when (f) {
                            HistoryFilter.ALL -> R.string.cc_filter_all
                            HistoryFilter.CURRENT_CYCLE -> R.string.cc_filter_cycle
                            HistoryFilter.LAST_30_DAYS -> R.string.cc_filter_30
                            HistoryFilter.LAST_90_DAYS -> R.string.cc_filter_90
                        }
                    ),
                    selected = state.filter == f,
                    onClick = { onEvent(CardDetailsEvent.Filter(f)) }
                )
            }
        }
        if (state.transactions.isEmpty()) {
            Disclaimer(
                stringResource(if (state.transactionCount == 0) R.string.cc_no_transactions else R.string.cc_no_matches)
            )
        }
    }
    items(state.transactions, key = { it.id }) { row ->
        TransactionRow(row = row, currency = state.currency, today = state.today) {
            onEvent(CardDetailsEvent.OpenTransaction(row.id))
        }
    }
}

@Composable
internal fun TransactionRow(row: CardTransactionRow, currency: String, today: LocalDate, onClick: () -> Unit) {
    val kindLabel = stringResource(
        when (row.kind) {
            RowKind.CHARGE -> R.string.cc_kind_charge
            RowKind.CREDIT -> R.string.cc_kind_credit
            RowKind.PAYMENT -> R.string.cc_kind_payment
            RowKind.TRANSFER_OUT -> R.string.cc_kind_transfer_out
        }
    )
    val reducesDebt = row.kind == RowKind.CREDIT || row.kind == RowKind.PAYMENT
    val amount = (if (reducesDebt) "− " else "+ ") + row.amount.money(currency)
    val title = row.title.ifBlank { kindLabel }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = "$title, $kindLabel, ${row.date.shortDate(today)}, $amount" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(kindLabel, row.category?.takeIf { it != title }, row.date.shortDate(today))
                    .joinToString(" · "),
                style = UI.typo.nC.style(color = UI.colors.gray),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = amount,
            style = UI.typo.nB2.style(
                color = if (reducesDebt) DueTone.GOOD.color(UI.colors) else UI.colors.pureInverse,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun ManageSection(status: CardStatus, onClose: () -> Unit, onReopen: () -> Unit, onDelete: () -> Unit) {
    SectionTitle(stringResource(R.string.cc_manage_card))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status == CardStatus.CLOSED) {
            SecondaryButton(
                text = stringResource(R.string.cc_reopen_card),
                onClick = onReopen,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            SecondaryButton(
                text = stringResource(R.string.cc_close_card),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Disclaimer(stringResource(R.string.cc_close_card_hint), Modifier.padding(horizontal = 0.dp))
        SecondaryButton(
            text = stringResource(R.string.cc_delete_card),
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            color = Red
        )
    }
}

private const val MaxStatements = 24
