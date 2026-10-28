package com.ivy.creditcards.pay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.creditcards.edit.CardFormValidator
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountType
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.domain.creditcard.CreditCardOverviewUseCase
import com.ivy.domain.creditcard.CreditCardWithOverview
import com.ivy.navigation.Navigation
import com.ivy.navigation.PayCreditCardScreen
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.deprecated.logic.currency.ExchangeRatesLogic
import com.ivy.wallet.domain.deprecated.logic.notification.CreditCardReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

enum class PayError { AMOUNT_INVALID, SOURCE_REQUIRED, SOURCE_AMOUNT_INVALID, DATE_IN_FUTURE, SAVE_FAILED }

@Immutable
@Suppress("DataClassTypedIDs") // raw UUID keeps the UI model stable for Compose
data class PaySource(val id: UUID, val name: String, val currency: String)

@Immutable
data class PayState(
    val loading: Boolean,
    val cardName: String,
    val currency: String,
    val outstanding: BigDecimal,
    val remainingDue: BigDecimal?,
    val minimumDueRemaining: BigDecimal?,
    val sources: ImmutableList<PaySource>,
    val source: PaySource?,
    val amount: String,
    val sourceAmount: String,
    val date: LocalDate,
    val note: String,
    /** Credit balance that this payment would create (null if none). */
    val overpayment: BigDecimal?,
    val errors: Set<PayError>,
    val today: LocalDate,
    /** True while a payment is being written; the save button is disabled. */
    val saving: Boolean,
    /** True while the debit amount in the source currency is being converted. */
    val converting: Boolean,
)

sealed interface PayEvent {
    data class Amount(val value: String) : PayEvent
    data class SourceAmount(val value: String) : PayEvent
    data class Source(val source: PaySource) : PayEvent
    data class Date(val date: LocalDate) : PayEvent
    data class Note(val note: String) : PayEvent
    data class Save(val defaultTitle: String) : PayEvent
}

@HiltViewModel
class PayCreditCardViewModel @Inject constructor(
    private val overviewUseCase: CreditCardOverviewUseCase,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val exchangeRatesLogic: ExchangeRatesLogic,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val navigation: Navigation,
    private val reminderScheduler: CreditCardReminderScheduler,
) : ComposeViewModel<PayState, PayEvent>() {

    private var data by mutableStateOf<CreditCardWithOverview?>(null)
    private var sources by mutableStateOf<List<PaySource>>(emptyList())
    private var source by mutableStateOf<PaySource?>(null)
    private var amount by mutableStateOf("")
    private var sourceAmount by mutableStateOf("")
    private var sourceAmountEdited = false
    private var date by mutableStateOf(LocalDate.now())
    private var note by mutableStateOf("")
    private var errors by mutableStateOf<Set<PayError>>(emptySet())
    private var saving by mutableStateOf(false)
    private var conversionJob: Job? = null
    private var converting by mutableStateOf(false)
    private var started = false

    fun start(screen: PayCreditCardScreen) {
        if (started) return
        started = true
        viewModelScope.launch {
            date = timeProvider.localDateNow()
            val loaded = overviewUseCase.load(AccountId(screen.accountId))
            data = loaded
            val closedCards = creditCardRepository.findAll().filter {
                it.status == CardStatus.CLOSED
            }.map { it.accountId }.toSet()
            sources = accountRepository.findAll()
                .filter { it.type == AccountType.ASSET && it.id !in closedCards }
                .map { it.toSource() }
            source = sources.firstOrNull { it.currency == loaded?.account?.asset?.code } ?: sources.firstOrNull()
            loaded?.overview?.latestStatement?.remainingDue?.takeIf { it.signum() > 0 }?.let {
                updateAmount(it.toPlainString())
            }
        }
    }

    private fun Account.toSource(): PaySource = PaySource(id.value, name.value, asset.code)

    @Composable
    override fun uiState(): PayState {
        val item = data
        val parsed = CardFormValidator.parseAmount(amount)
        val outstanding = item?.overview?.outstanding ?: BigDecimal.ZERO
        val statement = item?.overview?.latestStatement
        return PayState(
            loading = item == null,
            cardName = item?.account?.name?.value.orEmpty(),
            currency = item?.account?.asset?.code.orEmpty(),
            outstanding = outstanding,
            remainingDue = statement?.remainingDue?.takeIf { it.signum() > 0 },
            minimumDueRemaining = statement?.minimumDueRemaining?.takeIf { it.signum() > 0 },
            sources = sources.toImmutableList(),
            source = source,
            amount = amount,
            sourceAmount = sourceAmount,
            date = date,
            note = note,
            overpayment = parsed?.let { overpayment(it, outstanding) },
            errors = errors,
            today = timeProvider.localDateNow(),
            saving = saving,
            converting = converting,
        )
    }

    override fun onEvent(event: PayEvent) {
        when (event) {
            is PayEvent.Amount -> updateAmount(event.value)
            is PayEvent.SourceAmount -> {
                // A manual value wins over any conversion still in flight.
                cancelConversion()
                sourceAmount = event.value
                sourceAmountEdited = true
            }
            is PayEvent.Source -> {
                source = event.source
                sourceAmountEdited = false
                updateSourceAmount()
            }
            is PayEvent.Date -> date = event.date
            is PayEvent.Note -> note = event.note
            is PayEvent.Save -> save(event.defaultTitle)
        }
    }

    private fun updateAmount(value: String) {
        amount = value
        errors = errors - PayError.AMOUNT_INVALID
        if (!sourceAmountEdited) updateSourceAmount()
    }

    /** Pre-fills the amount debited from a source in another currency (still editable). */
    private fun updateSourceAmount() {
        // Only the latest request may write the result; an older one must never land last.
        cancelConversion()
        val card = data?.account ?: return
        val src = source
        val parsed = CardFormValidator.parseAmount(amount)
        // Clear the previous value so a stale debit amount can't be saved while converting.
        sourceAmount = ""
        if (src == null || parsed == null || src.currency == card.asset.code) return
        val requestedAmount = amount
        converting = true
        conversionJob = viewModelScope.launch {
            // A failed conversion leaves the field empty for the user to fill in; it must never
            // leave the save button stuck in the "converting" state.
            val converted = runCatching {
                val base = currencyRepository.getBaseCurrency().code
                exchangeRatesLogic.convertAmount(
                    baseCurrency = base,
                    amount = parsed.toDouble(),
                    fromCurrency = card.asset.code,
                    toCurrency = src.currency,
                )
            }.getOrNull()?.takeIf { it.isFinite() }
            val stillCurrent = !sourceAmountEdited && source == src && amount == requestedAmount
            if (stillCurrent) {
                sourceAmount = converted
                    ?.let { BigDecimal.valueOf(it).setScale(2, RoundingMode.HALF_UP).toPlainString() }
                    .orEmpty()
                converting = false
            }
        }
    }

    private fun cancelConversion() {
        conversionJob?.cancel()
        conversionJob = null
        converting = false
    }

    /** Validates the form, publishes errors, and returns the payment when it is valid. */
    private fun validatePayment(item: CreditCardWithOverview): PaymentInput? {
        val src = source
        val toAmount = CardFormValidator.parseAmount(amount)?.takeIf { it.signum() > 0 }
        val sameCurrency = src?.currency == item.account.asset.code
        val fromAmount = if (sameCurrency) {
            toAmount
        } else {
            CardFormValidator.parseAmount(sourceAmount)?.takeIf { it.signum() > 0 }
        }
        val newErrors = buildSet {
            if (toAmount == null) add(PayError.AMOUNT_INVALID)
            if (src == null) add(PayError.SOURCE_REQUIRED)
            if (src != null && !sameCurrency && fromAmount == null) add(PayError.SOURCE_AMOUNT_INVALID)
            if (date.isAfter(timeProvider.localDateNow())) add(PayError.DATE_IN_FUTURE)
        }
        errors = newErrors
        // No errors implies the source and both amounts are present.
        return if (newErrors.isEmpty()) {
            PaymentInput(requireNotNull(src), requireNotNull(toAmount), requireNotNull(fromAmount))
        } else {
            null
        }
    }

    private data class PaymentInput(
        val source: PaySource,
        val toAmount: BigDecimal,
        val fromAmount: BigDecimal,
    )

    private fun save(defaultTitle: String) {
        // In-flight guard: a second tap while saving must not record a second payment.
        val item = data?.takeIf { !saving && !converting } ?: return
        val input = validatePayment(item) ?: return
        val src = input.source
        val toAmount = input.toAmount
        val fromAmount = input.fromAmount

        saving = true
        viewModelScope.launch {
            var saved = false
            try {
                saved = persistPayment(item, src, toAmount, fromAmount, defaultTitle)
            } finally {
                // Stay disabled after success: the screen is closing.
                if (!saved) saving = false
            }
        }
    }

    private suspend fun persistPayment(
        item: CreditCardWithOverview,
        src: PaySource,
        toAmount: BigDecimal,
        fromAmount: BigDecimal,
        defaultTitle: String,
    ): Boolean {
        val sourceAccount = accountRepository.findById(AccountId(src.id))
        val toValue = PositiveDouble.from(toAmount.toDouble()).getOrNull()
        val fromValue = PositiveDouble.from(fromAmount.toDouble()).getOrNull()
        if (sourceAccount == null || toValue == null || fromValue == null) {
            errors = setOf(PayError.SAVE_FAILED)
            return false
        }
        val today = timeProvider.localDateNow()
        val time = with(timeConverter) {
            date.atTime(if (date == today) timeProvider.localNow().toLocalTime() else LocalTime.NOON).toUTC()
        }
        transactionRepository.save(
            Transfer(
                id = TransactionId(UUID.randomUUID()),
                title = NotBlankTrimmedString.from(defaultTitle).getOrNull(),
                description = NotBlankTrimmedString.from(note).getOrNull(),
                category = null,
                time = time,
                settled = true,
                metadata = TransactionMetadata(recurringRuleId = null, paidForDateTime = null, loanRecordId = null),
                tags = emptyList(),
                fromAccount = sourceAccount.id,
                fromValue = PositiveValue(fromValue, sourceAccount.asset),
                toAccount = item.account.id,
                toValue = PositiveValue(toValue, item.account.asset),
            )
        )
        // A paid bill's reminder disappears now rather than on the next periodic check.
        reminderScheduler.refreshNow()
        navigation.back()
        return true
    }

    companion object {
        /** How much of a payment would turn into a credit balance on the card. */
        fun overpayment(payment: BigDecimal, outstanding: BigDecimal): BigDecimal? =
            (payment - outstanding.max(BigDecimal.ZERO)).takeIf { it.signum() > 0 }
    }
}
