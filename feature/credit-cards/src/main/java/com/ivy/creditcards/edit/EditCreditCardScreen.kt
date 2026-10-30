package com.ivy.creditcards.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.creditcards.ui.CardLoading
import com.ivy.creditcards.ui.CardScreen
import com.ivy.creditcards.ui.CardVisualData
import com.ivy.creditcards.ui.ChoiceChip
import com.ivy.creditcards.ui.CreditCardVisual
import com.ivy.creditcards.ui.Disclaimer
import com.ivy.creditcards.ui.FormField
import com.ivy.creditcards.ui.IssuerMonogram
import com.ivy.creditcards.ui.PrimaryButton
import com.ivy.creditcards.ui.SectionTitle
import com.ivy.creditcards.ui.VerticalSpace
import com.ivy.creditcards.ui.mediumDate
import com.ivy.creditcards.ui.palette
import com.ivy.data.creditcard.CatalogueBank
import com.ivy.data.creditcard.CatalogueProduct
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardNetwork
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.navigation.EditCreditCardScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Ivy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun EditCreditCardScreenImpl(screen: EditCreditCardScreen) {
    val viewModel: EditCreditCardViewModel = viewModel()
    LaunchedEffect(screen) { viewModel.start(screen) }
    EditCreditCardUi(state = viewModel.uiState(), onEvent = viewModel::onEvent)
}

@Composable
internal fun EditCreditCardUi(state: EditCardState, onEvent: (EditCardEvent) -> Unit) {
    val nav = navigation()
    CardScreen(
        title = stringResource(if (state.isEdit) R.string.cc_edit_card else R.string.cc_add_card),
        onBack = { nav.back() },
        backDescription = stringResource(R.string.cc_back),
        bottomBar = {
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = stringResource(if (state.isEdit) R.string.cc_save_changes else R.string.cc_add_card),
                enabled = !state.loading && !state.saving,
                onClick = { onEvent(EditCardEvent.Save) }
            )
        }
    ) {
        if (state.loading) {
            CardLoading()
            return@CardScreen
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            val form = state.form
            CreditCardVisual(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                data = CardVisualData(
                    nickname = form.nickname.ifBlank {
                        state.selectedProduct?.name
                            ?: form.customProductName.ifBlank { stringResource(R.string.cc_new_card) }
                    },
                    issuerName = state.selectedBank?.displayName ?: form.customBankName.takeIf { it.isNotBlank() },
                    productName = state.selectedProduct?.name ?: form.customProductName.takeIf { it.isNotBlank() },
                    network = form.network,
                    lastFour = form.lastFour.takeIf { it.length == LastFourLength },
                    appearance = form.appearance,
                )
            )
            Disclaimer(stringResource(R.string.cc_illustration_note))

            BankSection(state, onEvent)
            ProductSection(state, onEvent)
            DetailsSection(state, onEvent)
            BillingSection(state, onEvent)
            OpeningSection(state, onEvent)
            AppearanceSection(form.appearance) { a -> onEvent(EditCardEvent.Update { it.copy(appearance = a) }) }
            PreferencesSection(state, onEvent)
            VerticalSpace(24.dp)
        }
    }
}

private fun update(onEvent: (EditCardEvent) -> Unit, transform: (CardForm) -> CardForm): Unit =
    onEvent(EditCardEvent.Update(transform))

@StringRes
private fun errorRes(state: EditCardState, field: CardFormField): Int? = state.errors[field]?.let {
    when (it) {
        CardFormError.NICKNAME_REQUIRED -> R.string.cc_error_nickname
        CardFormError.CURRENCY_INVALID -> R.string.cc_error_currency
        CardFormError.BANK_REQUIRED -> R.string.cc_error_bank
        CardFormError.LAST_FOUR_INVALID -> R.string.cc_error_last_four
        CardFormError.LIMIT_INVALID -> R.string.cc_error_limit
        CardFormError.DUE_RULE_INVALID -> R.string.cc_error_due_rule
        CardFormError.DUE_RULE_NEEDS_STATEMENT_DAY -> R.string.cc_error_due_needs_statement
        CardFormError.OPENING_INVALID -> R.string.cc_error_opening
        CardFormError.OPENING_DATE_IN_FUTURE -> R.string.cc_error_opening_date
    }
}

@Composable
private fun BankSection(state: EditCardState, onEvent: (EditCardEvent) -> Unit) {
    SectionTitle(stringResource(R.string.cc_issuing_bank))
    val form = state.form
    when {
        state.selectedBank != null -> SelectedRow(
            title = state.selectedBank.displayName,
            subtitle = state.selectedBank.name,
            monogramOf = state.selectedBank.displayName,
            changeLabel = stringResource(R.string.cc_change),
            onChange = { onEvent(EditCardEvent.ChangeBank) },
        )

        form.useCustomBank -> {
            FormField(
                value = form.customBankName,
                onValueChange = { v -> update(onEvent) { it.copy(customBankName = v) } },
                label = stringResource(R.string.cc_bank_name),
                error = errorRes(state, CardFormField.BANK)?.let { stringResource(it) },
            )
            TextButton(
                modifier = Modifier.padding(horizontal = 8.dp),
                onClick = { onEvent(EditCardEvent.ChangeBank) }
            ) { Text(stringResource(R.string.cc_search_catalogue), color = Ivy) }
        }

        else -> {
            FormField(
                value = state.bankQuery,
                onValueChange = { onEvent(EditCardEvent.BankQuery(it)) },
                label = stringResource(R.string.cc_search_bank),
                supportingText = stringResource(R.string.cc_search_bank_hint),
                error = errorRes(state, CardFormField.BANK)?.let { stringResource(it) },
                imeAction = ImeAction.Search,
            )
            state.bankResults.forEach { bank -> BankRow(bank) { onEvent(EditCardEvent.SelectBank(bank)) } }
            OptionRow(
                text = stringResource(R.string.cc_bank_not_listed),
                onClick = { onEvent(EditCardEvent.UseCustomBank(state.bankQuery)) }
            )
        }
    }
}

@Composable
private fun ProductSection(state: EditCardState, onEvent: (EditCardEvent) -> Unit) {
    val form = state.form
    if (state.selectedBank == null && !form.useCustomBank) return
    SectionTitle(stringResource(R.string.cc_card_product))
    when {
        state.selectedProduct != null -> SelectedRow(
            title = state.selectedProduct.name,
            subtitle = listOfNotNull(
                state.selectedProduct.coBrandPartner,
                stringResource(R.string.cc_verified_on, state.selectedProduct.verifiedOn ?: state.catalogueVerifiedOn),
            ).joinToString(" · "),
            monogramOf = null,
            changeLabel = stringResource(R.string.cc_change),
            onChange = { onEvent(EditCardEvent.ChangeProduct) },
        )

        form.useCustomProduct || !state.bankHasProducts -> {
            FormField(
                value = form.customProductName,
                onValueChange = { v -> update(onEvent) { it.copy(customProductName = v) } },
                label = stringResource(R.string.cc_product_name_optional),
                supportingText = stringResource(R.string.cc_product_name_hint),
            )
            if (state.bankHasProducts) {
                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = { onEvent(EditCardEvent.ChangeProduct) }
                ) { Text(stringResource(R.string.cc_search_catalogue), color = Ivy) }
            }
        }

        else -> {
            FormField(
                value = state.productQuery,
                onValueChange = { onEvent(EditCardEvent.ProductQuery(it)) },
                label = stringResource(R.string.cc_search_product),
                imeAction = ImeAction.Search,
            )
            state.productResults.forEach { product ->
                ProductRow(product) { onEvent(EditCardEvent.SelectProduct(product)) }
            }
            OptionRow(
                text = stringResource(R.string.cc_product_not_listed),
                onClick = { onEvent(EditCardEvent.UseCustomProduct) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsSection(state: EditCardState, onEvent: (EditCardEvent) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        val form = state.form
        SectionTitle(stringResource(R.string.cc_card_details))
        FormField(
            value = form.nickname,
            onValueChange = { v -> update(onEvent) { it.copy(nickname = v) } },
            label = stringResource(R.string.cc_nickname),
            supportingText = stringResource(R.string.cc_nickname_hint),
            error = errorRes(state, CardFormField.NICKNAME)?.let { stringResource(it) },
        )
        FormField(
            value = form.lastFour,
            onValueChange = { v ->
                update(onEvent) { it.copy(lastFour = v.filter(Char::isDigit).take(LastFourLength)) }
            },
            label = stringResource(R.string.cc_last_four),
            supportingText = stringResource(R.string.cc_last_four_hint),
            error = errorRes(state, CardFormField.LAST_FOUR)?.let { stringResource(it) },
            keyboardType = KeyboardType.NumberPassword,
        )
        FormField(
            value = form.creditLimit,
            onValueChange = { v -> update(onEvent) { it.copy(creditLimit = v) } },
            label = stringResource(R.string.cc_credit_limit_optional),
            error = errorRes(state, CardFormField.LIMIT)?.let { stringResource(it) },
            keyboardType = KeyboardType.Decimal,
            prefix = form.currency.takeIf { it.isNotBlank() }?.let { "$it " },
        )
        FormField(
            value = form.currency,
            onValueChange = { v -> update(onEvent) { it.copy(currency = v.uppercase().take(CurrencyLength)) } },
            label = stringResource(R.string.cc_currency),
            supportingText = if (state.isEdit && state.hasTransactions) {
                stringResource(R.string.cc_currency_locked)
            } else {
                null
            },
            error = errorRes(state, CardFormField.CURRENCY)?.let { stringResource(it) },
            enabled = !(state.isEdit && state.hasTransactions),
        )
        Text(
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
            text = stringResource(R.string.cc_network),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardNetwork.entries.forEach { network ->
                ChoiceChip(
                    text = network.displayName,
                    selected = form.network == network,
                    onClick = {
                        update(onEvent) { it.copy(network = if (it.network == network) null else network) }
                    }
                )
            }
        }
    }
}

@Composable
private fun BillingSection(state: EditCardState, onEvent: (EditCardEvent) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        val form = state.form
        SectionTitle(stringResource(R.string.cc_billing))
        Disclaimer(stringResource(R.string.cc_billing_hint))
        Text(
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp),
            text = stringResource(R.string.cc_statement_day),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChoiceChip(
                text = stringResource(R.string.cc_not_set),
                selected = form.statementDay == null,
                onClick = { update(onEvent) { it.copy(statementDay = null) } }
            )
            for (day in 1..CardForm.MAX_DAY_OF_MONTH) {
                ChoiceChip(
                    modifier = Modifier.semantics { contentDescription = "Statement day $day" },
                    text = day.toString(),
                    selected = form.statementDay == day,
                    onClick = { update(onEvent) { it.copy(statementDay = day) } }
                )
            }
        }
        if ((form.statementDay ?: 0) > ShortMonthDay) {
            Disclaimer(stringResource(R.string.cc_statement_day_short_months))
        }
        Text(
            modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
            text = stringResource(R.string.cc_due_date_rule),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceChip(
                text = stringResource(R.string.cc_not_set),
                selected = form.dueRuleKind == DueRuleKind.NONE,
                onClick = { update(onEvent) { it.copy(dueRuleKind = DueRuleKind.NONE) } }
            )
            ChoiceChip(
                text = stringResource(R.string.cc_days_after_statement),
                selected = form.dueRuleKind == DueRuleKind.DAYS_AFTER_STATEMENT,
                onClick = { update(onEvent) { it.copy(dueRuleKind = DueRuleKind.DAYS_AFTER_STATEMENT) } }
            )
            ChoiceChip(
                text = stringResource(R.string.cc_day_of_month),
                selected = form.dueRuleKind == DueRuleKind.DAY_OF_MONTH,
                onClick = { update(onEvent) { it.copy(dueRuleKind = DueRuleKind.DAY_OF_MONTH) } }
            )
        }
        if (form.dueRuleKind != DueRuleKind.NONE) {
            FormField(
                value = form.dueRuleValue,
                onValueChange = { v -> update(onEvent) { it.copy(dueRuleValue = v.filter(Char::isDigit).take(2)) } },
                label = stringResource(
                    if (form.dueRuleKind == DueRuleKind.DAYS_AFTER_STATEMENT) R.string.cc_days else R.string.cc_day
                ),
                supportingText = stringResource(R.string.cc_due_hint),
                error = errorRes(state, CardFormField.DUE_RULE)?.let { stringResource(it) },
                keyboardType = KeyboardType.Number,
            )
        } else {
            errorRes(state, CardFormField.DUE_RULE)?.let { stringResource(it) }?.let { Disclaimer(it) }
        }
    }
}

@Composable
private fun OpeningSection(state: EditCardState, onEvent: (EditCardEvent) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        val form = state.form
        SectionTitle(stringResource(R.string.cc_opening_balance))
        Disclaimer(stringResource(R.string.cc_opening_hint))
        FormField(
            value = form.openingOutstanding,
            onValueChange = { v -> update(onEvent) { it.copy(openingOutstanding = v) } },
            label = stringResource(R.string.cc_opening_amount),
            error = errorRes(state, CardFormField.OPENING)?.let { stringResource(it) },
            keyboardType = KeyboardType.Decimal,
            prefix = form.currency.takeIf { it.isNotBlank() }?.let { "$it " },
        )
        SwitchRow(
            text = stringResource(R.string.cc_opening_is_credit),
            checked = form.openingIsCreditBalance,
            onChange = { c -> update(onEvent) { it.copy(openingIsCreditBalance = c) } },
        )
        DateRow(
            label = stringResource(R.string.cc_opening_date),
            date = form.openingDate,
            maxDate = state.today,
            error = errorRes(state, CardFormField.OPENING_DATE)?.let { stringResource(it) },
            onDate = { d -> update(onEvent) { it.copy(openingDate = d) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSection(selected: CardAppearance, onSelect: (CardAppearance) -> Unit) {
    SectionTitle(stringResource(R.string.cc_appearance))
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardAppearance.entries.forEach { appearance ->
            val palette = appearance.palette()
            val name = appearance.name.lowercase().replaceFirstChar { it.uppercase() }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (appearance == selected) 3.dp else 0.dp,
                        color = if (appearance == selected) UI.colors.pureInverse else UI.colors.pure,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(palette.start, palette.end)))
                    .clickable(role = Role.RadioButton) { onSelect(appearance) }
                    .semantics {
                        contentDescription = name
                        this.selected = appearance == selected
                    }
            )
        }
    }
}

@Composable
private fun PreferencesSection(state: EditCardState, onEvent: (EditCardEvent) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        val form = state.form
        SectionTitle(stringResource(R.string.cc_preferences))
        SwitchRow(
            text = stringResource(R.string.cc_include_in_balance),
            description = stringResource(R.string.cc_include_in_balance_hint),
            checked = form.includeInBalance,
            onChange = { c -> update(onEvent) { it.copy(includeInBalance = c) } },
        )
        SwitchRow(
            text = stringResource(R.string.cc_reminders),
            description = stringResource(R.string.cc_reminders_hint),
            checked = form.remindersEnabled,
            onChange = { c -> update(onEvent) { it.copy(remindersEnabled = c) } },
        )
        if (form.remindersEnabled) {
            Text(
                modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp),
                text = stringResource(R.string.cc_remind_days_before),
                style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReminderDayOptions.forEach { days ->
                    ChoiceChip(
                        text = if (days == 0) stringResource(R.string.cc_only_on_due_date) else days.toString(),
                        selected = form.reminderDaysBefore == days,
                        onClick = { update(onEvent) { it.copy(reminderDaysBefore = days) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedRow(
    title: String,
    subtitle: String?,
    monogramOf: String?,
    changeLabel: String,
    onChange: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(UI.colors.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        monogramOf?.let {
            IssuerMonogram(name = it, size = 40.dp, onDark = false)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold))
            subtitle?.let { Text(it, style = UI.typo.nC.style(color = UI.colors.gray)) }
        }
        TextButton(onClick = onChange) { Text(changeLabel, color = Ivy) }
    }
}

@Composable
private fun BankRow(bank: CatalogueBank, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IssuerMonogram(name = bank.displayName, size = 36.dp, onDark = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                bank.displayName,
                style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (bank.creditCardIssuer) {
                    stringResource(R.string.cc_issues_credit_cards)
                } else {
                    stringResource(R.string.cc_no_cards_reported)
                },
                style = UI.typo.nC.style(color = UI.colors.gray)
            )
        }
    }
}

@Composable
private fun ProductRow(product: CatalogueProduct, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text(product.name, style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold))
        val meta = listOfNotNull(
            product.networks.takeIf { it.isNotEmpty() }?.joinToString { n ->
                CardNetwork.entries.firstOrNull { it.name == n }?.displayName ?: n
            },
            product.coBrandPartner,
        )
        if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = UI.typo.nC.style(color = UI.colors.gray))
    }
}

@Composable
private fun OptionRow(text: String, onClick: () -> Unit) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        text = text,
        style = UI.typo.b2.style(color = Ivy, fontWeight = FontWeight.Bold)
    )
}

@Composable
internal fun SwitchRow(
    text: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.SemiBold))
            description?.let { Text(it, style = UI.typo.nC.style(color = UI.colors.gray)) }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null, // the whole row is the toggle
            colors = SwitchDefaults.colors(checkedTrackColor = Ivy)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateRow(
    label: String,
    date: LocalDate?,
    onDate: (LocalDate) -> Unit,
    maxDate: LocalDate? = null,
    error: String? = null,
    placeholder: String = "",
) {
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = label) { open = true }
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text(label, style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold))
        Text(
            text = date?.mediumDate() ?: placeholder,
            style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold)
        )
        error?.let { Text(it, style = UI.typo.nC.style(color = com.ivy.wallet.ui.theme.Red)) }
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDate(if (maxDate != null && picked.isAfter(maxDate)) maxDate else picked)
                    }
                    open = false
                }) { Text(stringResource(R.string.cc_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.cc_cancel)) }
            }
        ) { DatePicker(state = state) }
    }
}

private const val CurrencyLength = 3
private const val ShortMonthDay = 28
private const val LastFourLength = 4
private val ReminderDayOptions = listOf(0, 1, 2, 3, 5, 7, 10)
