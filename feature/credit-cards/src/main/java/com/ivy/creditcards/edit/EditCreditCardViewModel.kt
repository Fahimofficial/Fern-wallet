package com.ivy.creditcards.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import com.ivy.base.time.INSTANT_MAX_SAFE
import com.ivy.base.time.INSTANT_MIN_SAFE
import com.ivy.base.time.TimeProvider
import com.ivy.creditcards.ui.accountColor
import com.ivy.data.creditcard.CardCatalogue
import com.ivy.data.creditcard.CatalogueBank
import com.ivy.data.creditcard.CatalogueProduct
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountType
import com.ivy.data.model.creditcard.CardNetwork
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.creditcard.StatementOverrideRemapper
import com.ivy.navigation.CreditCardDetailsScreen
import com.ivy.navigation.EditCreditCardScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.deprecated.logic.notification.CreditCardReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@Immutable
data class EditCardState(
    val isEdit: Boolean,
    val loading: Boolean,
    val form: CardForm,
    val bankQuery: String,
    val bankResults: ImmutableList<CatalogueBank>,
    val selectedBank: CatalogueBank?,
    val productQuery: String,
    val productResults: ImmutableList<CatalogueProduct>,
    val selectedProduct: CatalogueProduct?,
    val bankHasProducts: Boolean,
    val errors: Map<CardFormField, CardFormError>,
    val hasTransactions: Boolean,
    val today: LocalDate,
    val catalogueVerifiedOn: String,
    /** True while the card is being written; the save button is disabled. */
    val saving: Boolean,
)

sealed interface EditCardEvent {
    data class BankQuery(val query: String) : EditCardEvent
    data class SelectBank(val bank: CatalogueBank) : EditCardEvent
    data class UseCustomBank(val name: String) : EditCardEvent
    data object ChangeBank : EditCardEvent
    data class ProductQuery(val query: String) : EditCardEvent
    data class SelectProduct(val product: CatalogueProduct) : EditCardEvent
    data object UseCustomProduct : EditCardEvent
    data object ChangeProduct : EditCardEvent
    data class Update(val transform: (CardForm) -> CardForm) : EditCardEvent
    data object Save : EditCardEvent
}

@HiltViewModel
class EditCreditCardViewModel @Inject constructor(
    private val catalogue: CardCatalogue,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionRepository: TransactionRepository,
    private val timeProvider: TimeProvider,
    private val navigation: Navigation,
    private val reminderScheduler: CreditCardReminderScheduler,
) : ComposeViewModel<EditCardState, EditCardEvent>() {

    private var accountId: AccountId? = null
    private var existingAccount: Account? = null
    private var started = false

    private var loading by mutableStateOf(true)
    private var form by mutableStateOf(CardForm())
    private var bankQuery by mutableStateOf("")
    private var productQuery by mutableStateOf("")
    private var errors by mutableStateOf<Map<CardFormField, CardFormError>>(emptyMap())
    private var hasTransactions by mutableStateOf(false)
    private var saving by mutableStateOf(false)

    fun start(screen: EditCreditCardScreen) {
        if (started) return
        started = true
        viewModelScope.launch {
            val base = currencyRepository.getBaseCurrency().code
            val id = screen.accountId?.let(::AccountId)
            accountId = id
            if (id == null) {
                form = CardForm(currency = base, openingDate = timeProvider.localDateNow())
            } else {
                val account = accountRepository.findById(id)
                val card = creditCardRepository.findByAccountId(id)
                existingAccount = account
                if (account != null && card != null) {
                    // Incoming transfers (bill payments) and entered bank-statement figures are
                    // stored in the card's currency too.
                    hasTransactions = creditCardRepository.findStatementOverrides(id).isNotEmpty() ||
                        transactionRepository.findAllByAccountAndBetween(id, INSTANT_MIN_SAFE, INSTANT_MAX_SAFE)
                            .isNotEmpty() ||
                        transactionRepository.findAllToAccountAndBetween(id, INSTANT_MIN_SAFE, INSTANT_MAX_SAFE)
                            .isNotEmpty()
                    // Catalogue entries can be removed in an app update. A card pointing at one
                    // that's gone keeps its saved name as a custom entry instead of losing it.
                    val bankKnown = card.bankId != null && catalogue.bank(card.bankId) != null
                    val productKnown = bankKnown && card.productId != null && catalogue.product(card.productId) != null
                    form = CardForm(
                        bankId = card.bankId.takeIf { bankKnown },
                        customBankName = card.customBankName
                            ?: card.bankId.takeIf { !bankKnown }?.let(::bankNameFromId).orEmpty(),
                        useCustomBank = !bankKnown,
                        productId = card.productId.takeIf { productKnown },
                        customProductName = if (productKnown) "" else card.productName.orEmpty(),
                        useCustomProduct = !productKnown && card.productName != null,
                        nickname = account.name.value,
                        currency = account.asset.code,
                        network = card.network,
                        lastFour = card.lastFour.orEmpty(),
                        creditLimit = card.creditLimit?.toPlainString().orEmpty(),
                        statementDay = card.statementDay,
                        dueRuleKind = when (card.dueDateRule) {
                            is DueDateRule.DaysAfterStatement -> DueRuleKind.DAYS_AFTER_STATEMENT
                            is DueDateRule.DayOfMonth -> DueRuleKind.DAY_OF_MONTH
                            null -> DueRuleKind.NONE
                        },
                        dueRuleValue = when (val rule = card.dueDateRule) {
                            is DueDateRule.DaysAfterStatement -> rule.days.toString()
                            is DueDateRule.DayOfMonth -> rule.day.toString()
                            null -> ""
                        },
                        openingOutstanding = card.openingOutstanding.abs().takeIf { it.signum() != 0 }
                            ?.toPlainString().orEmpty(),
                        openingIsCreditBalance = card.openingOutstanding.signum() < 0,
                        openingDate = card.openingDate,
                        appearance = card.appearance,
                        includeInBalance = account.includeInBalance,
                        remindersEnabled = card.remindersEnabled,
                        reminderDaysBefore = card.reminderDaysBefore,
                        status = card.status,
                    )
                }
            }
            loading = false
        }
    }

    @Composable
    override fun uiState(): EditCardState {
        val selectedBank = if (form.useCustomBank) null else catalogue.bank(form.bankId)
        val selectedProduct = if (form.useCustomProduct) null else catalogue.product(form.productId)
        return EditCardState(
            isEdit = accountId != null,
            loading = loading,
            form = form,
            bankQuery = bankQuery,
            bankResults = if (selectedBank == null && !form.useCustomBank) {
                catalogue.searchBanks(bankQuery).take(MAX_RESULTS).toImmutableList()
            } else {
                persistentListOf()
            },
            selectedBank = selectedBank,
            productQuery = productQuery,
            productResults = if (selectedBank != null && selectedProduct == null && !form.useCustomProduct) {
                catalogue.searchProducts(selectedBank.id, productQuery).take(MAX_RESULTS).toImmutableList()
            } else {
                persistentListOf()
            },
            selectedProduct = selectedProduct,
            bankHasProducts = selectedBank?.let { catalogue.productsOf(it.id).isNotEmpty() } == true,
            errors = errors,
            hasTransactions = hasTransactions,
            today = timeProvider.localDateNow(),
            catalogueVerifiedOn = catalogue.verifiedOn,
            saving = saving,
        )
    }

    override fun onEvent(event: EditCardEvent) {
        when (event) {
            is EditCardEvent.BankQuery -> bankQuery = event.query
            is EditCardEvent.SelectBank -> {
                form = form.copy(
                    bankId = event.bank.id,
                    useCustomBank = false,
                    productId = null,
                    useCustomProduct = false,
                )
                bankQuery = ""
                errors = errors - CardFormField.BANK
            }
            is EditCardEvent.UseCustomBank -> {
                form = form.copy(
                    bankId = null,
                    useCustomBank = true,
                    customBankName = event.name,
                    productId = null,
                    useCustomProduct = true,
                )
                errors = errors - CardFormField.BANK
            }
            EditCardEvent.ChangeBank -> {
                form = form.copy(bankId = null, useCustomBank = false, customBankName = "", productId = null)
            }
            is EditCardEvent.ProductQuery -> productQuery = event.query
            is EditCardEvent.SelectProduct -> selectProduct(event.product)
            EditCardEvent.UseCustomProduct -> form = form.copy(productId = null, useCustomProduct = true)
            EditCardEvent.ChangeProduct -> form = form.copy(productId = null, useCustomProduct = false)
            is EditCardEvent.Update -> form = event.transform(form)
            EditCardEvent.Save -> save()
        }
    }

    private fun selectProduct(product: CatalogueProduct) {
        val networks = product.networks.mapNotNull { n -> CardNetwork.entries.firstOrNull { it.name == n } }
        form = form.copy(
            productId = product.id,
            useCustomProduct = false,
            network = networks.singleOrNull() ?: form.network,
            nickname = form.nickname.ifBlank { product.name },
        )
        productQuery = ""
    }

    /**
     * Mirrors what ProductSection shows: the typed name is used in custom mode and also when the
     * chosen bank has no catalogue products (the screen shows the free-text field in that case).
     */
    private fun productName(): String? {
        val catalogueProduct = if (form.useCustomProduct) null else catalogue.product(form.productId)
        val bank = if (form.useCustomBank) null else catalogue.bank(form.bankId)
        val customFieldVisible = form.useCustomProduct || form.useCustomBank ||
            (bank != null && catalogue.productsOf(bank.id).isEmpty())
        return catalogueProduct?.name ?: form.customProductName.takeIf { customFieldVisible }
    }

    private fun save() {
        // In-flight guard: a second tap must not create a second card/account.
        if (saving) return
        saving = true
        viewModelScope.launch {
            var saved = false
            try {
                saved = saveCard()
            } finally {
                // Stay disabled after success: the screen is closing.
                if (!saved) saving = false
            }
        }
    }

    /** Returns true once the card was written. */
    private suspend fun saveCard(): Boolean {
        val id = accountId ?: AccountId(UUID.randomUUID())
        val result = CardFormValidator.validate(
            form = form.copy(
                nickname = form.nickname.ifBlank {
                    productName() ?: catalogue.bank(form.bankId)?.displayName?.let { "$it Card" }
                        ?: form.customBankName.takeIf { it.isNotBlank() }?.let { "$it Card" }.orEmpty()
                }
            ),
            accountId = id,
            productName = productName(),
            today = timeProvider.localDateNow(),
        )
        return when (result) {
            is CardFormResult.Invalid -> {
                errors = result.errors
                false
            }
            is CardFormResult.Valid -> {
                errors = emptyMap()
                val existing = existingAccount
                val account = Account(
                    id = id,
                    name = NotBlankTrimmedString.unsafe(result.nickname),
                    // Changing the currency of a card with history would reinterpret its amounts.
                    asset = if (existing != null && hasTransactions) existing.asset else result.currency,
                    color = ColorInt(result.card.appearance.accountColor().toArgb()),
                    icon = existing?.icon,
                    includeInBalance = result.includeInBalance,
                    orderNum = existing?.orderNum ?: (accountRepository.findMaxOrderNum() + 1),
                    type = AccountType.CREDIT_CARD,
                )
                moveSavedStatements(id, result.card.statementDay)
                // Save the card details first so the account never appears without them.
                creditCardRepository.save(result.card)
                accountRepository.save(account)
                // Idempotent (KEEP); covers cards added before the app was restarted after onboarding.
                reminderScheduler.schedule()
                reminderScheduler.refreshNow()
                navigation.back()
                if (existing == null) navigation.navigateTo(CreditCardDetailsScreen(id.value))
                true
            }
        }
    }

    /**
     * Saved bank-statement values are keyed by the old schedule's closing dates; when the
     * statement day changes they're moved to the new schedule instead of silently detaching.
     */
    private suspend fun moveSavedStatements(id: AccountId, newStatementDay: Int?) {
        val oldStatementDay = creditCardRepository.findByAccountId(id)?.statementDay
        if (newStatementDay == null || newStatementDay == oldStatementDay) return
        val saved = creditCardRepository.findStatementOverrides(id)
        if (saved.isEmpty()) return
        creditCardRepository.replaceStatementOverrides(id, StatementOverrideRemapper.remap(saved, newStatementDay))
    }

    /** Readable fallback for a bank id no longer in the catalogue, e.g. "hdfc-bank" -> "Hdfc Bank". */
    private fun bankNameFromId(id: String): String =
        id.split('-').filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    companion object {
        private const val MAX_RESULTS = 8
    }
}
