package com.ivy.creditcards.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.creditcards.ui.CardUiMapper
import com.ivy.creditcards.ui.CardVisualData
import com.ivy.data.model.AccountId
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.repository.CreditCardRepository
import com.ivy.domain.creditcard.CardOverview
import com.ivy.domain.creditcard.CardStatement
import com.ivy.domain.creditcard.CreditCardOverviewUseCase
import com.ivy.domain.creditcard.CreditCardWithOverview
import com.ivy.domain.creditcard.DeleteCreditCardUseCase
import com.ivy.navigation.CreditCardDetailsScreen
import com.ivy.navigation.CreditCardStatementScreen
import com.ivy.navigation.EditCreditCardScreen
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.PayCreditCardScreen
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.deprecated.logic.notification.CreditCardNotifications
import com.ivy.wallet.domain.deprecated.logic.notification.CreditCardReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

enum class HistoryFilter { ALL, CURRENT_CYCLE, LAST_30_DAYS, LAST_90_DAYS }

enum class RowKind { CHARGE, CREDIT, PAYMENT, TRANSFER_OUT }

@Immutable
@Suppress("DataClassTypedIDs") // raw UUID keeps the UI model stable for Compose
data class CardTransactionRow(
    val id: UUID,
    val title: String,
    val category: String?,
    val date: LocalDate,
    val amount: BigDecimal,
    val kind: RowKind,
)

@Immutable
data class CardDetailsState(
    val loading: Boolean,
    val notFound: Boolean,
    val name: String,
    val currency: String,
    val visual: CardVisualData?,
    val overview: CardOverview?,
    val status: CardStatus,
    val statementDayConfigured: Boolean,
    val dueRuleConfigured: Boolean,
    val transactions: ImmutableList<CardTransactionRow>,
    val transactionCount: Int,
    val query: String,
    val filter: HistoryFilter,
    val today: LocalDate,
)

sealed interface CardDetailsEvent {
    data class Search(val query: String) : CardDetailsEvent
    data class Filter(val filter: HistoryFilter) : CardDetailsEvent
    data object Pay : CardDetailsEvent
    data object AddCharge : CardDetailsEvent
    data object AddCredit : CardDetailsEvent
    data object Edit : CardDetailsEvent
    data class OpenStatement(val statement: CardStatement) : CardDetailsEvent

    @Suppress("DataClassTypedIDs") // navigation takes raw UUIDs
    data class OpenTransaction(val id: UUID) : CardDetailsEvent
    data class SetStatus(val status: CardStatus) : CardDetailsEvent

    /** [paymentTitle] names preserved bill payments that had no title of their own. */
    data class Delete(val paymentTitle: String) : CardDetailsEvent
}

@HiltViewModel
class CreditCardDetailsViewModel @Inject constructor(
    private val overviewUseCase: CreditCardOverviewUseCase,
    private val creditCardRepository: CreditCardRepository,
    private val creditCardNotifications: CreditCardNotifications,
    private val reminderScheduler: CreditCardReminderScheduler,
    private val rowsBuilder: CardRowsBuilder,
    private val deleteCreditCardUseCase: DeleteCreditCardUseCase,
    private val mapper: CardUiMapper,
    private val timeProvider: TimeProvider,
    private val navigation: Navigation,
) : ComposeViewModel<CardDetailsState, CardDetailsEvent>() {

    private var deleting = false
    private var accountId: AccountId? = null
    private var loading by mutableStateOf(true)
    private var data by mutableStateOf<CreditCardWithOverview?>(null)
    private var rows by mutableStateOf<List<CardTransactionRow>>(emptyList())
    private var query by mutableStateOf("")
    private var filter by mutableStateOf(HistoryFilter.ALL)

    fun start(screen: CreditCardDetailsScreen) {
        accountId = AccountId(screen.accountId)
        reload()
    }

    private fun reload() {
        val id = accountId ?: return
        viewModelScope.launch {
            val loaded = overviewUseCase.load(id)
            data = loaded
            rows = loaded?.let { rowsBuilder.rows(it) }.orEmpty()
            loading = false
        }
    }

    @Composable
    override fun uiState(): CardDetailsState {
        val item = data
        val today = timeProvider.localDateNow()
        return CardDetailsState(
            loading = loading,
            notFound = !loading && item == null,
            name = item?.account?.name?.value.orEmpty(),
            currency = item?.account?.asset?.code.orEmpty(),
            visual = item?.let { mapper.visual(it.account.name.value, it.card) },
            overview = item?.overview,
            status = item?.card?.status ?: CardStatus.ACTIVE,
            statementDayConfigured = item?.card?.statementDay != null,
            dueRuleConfigured = item?.card?.dueDateRule != null,
            transactions = filteredRows(item?.overview, today),
            transactionCount = rows.size,
            query = query,
            filter = filter,
            today = today,
        )
    }

    private fun filteredRows(overview: CardOverview?, today: LocalDate): ImmutableList<CardTransactionRow> {
        if (rows.isEmpty()) return persistentListOf()
        val from: LocalDate? = when (filter) {
            HistoryFilter.ALL -> null
            HistoryFilter.CURRENT_CYCLE -> overview?.currentCycle?.start
            HistoryFilter.LAST_30_DAYS -> today.minusDays(DAYS_30)
            HistoryFilter.LAST_90_DAYS -> today.minusDays(DAYS_90)
        }
        val q = query.trim().lowercase()
        return rows.filter { row ->
            (from == null || !row.date.isBefore(from)) &&
                (q.isEmpty() || row.title.lowercase().contains(q) || row.category?.lowercase()?.contains(q) == true)
        }.toImmutableList()
    }

    override fun onEvent(event: CardDetailsEvent) {
        val id = accountId ?: return
        when (event) {
            is CardDetailsEvent.Search -> query = event.query
            is CardDetailsEvent.Filter -> filter = event.filter
            CardDetailsEvent.Pay -> navigation.navigateTo(PayCreditCardScreen(id.value))
            CardDetailsEvent.AddCharge -> navigation.navigateTo(
                EditTransactionScreen(initialTransactionId = null, type = TransactionType.EXPENSE, accountId = id.value)
            )
            CardDetailsEvent.AddCredit -> navigation.navigateTo(
                EditTransactionScreen(initialTransactionId = null, type = TransactionType.INCOME, accountId = id.value)
            )
            CardDetailsEvent.Edit -> navigation.navigateTo(EditCreditCardScreen(id.value))
            is CardDetailsEvent.OpenStatement -> navigation.navigateTo(
                CreditCardStatementScreen(id.value, event.statement.nominalClosingDate.toEpochDay())
            )
            is CardDetailsEvent.OpenTransaction -> navigation.navigateTo(
                EditTransactionScreen(initialTransactionId = event.id, type = TransactionType.EXPENSE)
            )
            is CardDetailsEvent.SetStatus -> setStatus(event.status)
            is CardDetailsEvent.Delete -> delete(id, event.paymentTitle)
        }
    }

    private fun setStatus(status: CardStatus) {
        val card = data?.card ?: return
        viewModelScope.launch {
            creditCardRepository.save(card.copy(status = status))
            // Closing removes the card's bill notifications at once; reopening re-checks them.
            if (status == CardStatus.CLOSED) creditCardNotifications.cancelForCard(card.accountId.value)
            reminderScheduler.refreshNow()
            reload()
        }
    }

    /**
     * Only reachable after an explicit confirmation. Bill payments made from other accounts stay
     * in those accounts (see [DeleteCreditCardUseCase]), as the confirmation text promises.
     */
    private fun delete(id: AccountId, paymentTitle: String) {
        if (deleting) return
        deleting = true
        viewModelScope.launch {
            creditCardNotifications.cancelForCard(id.value)
            deleteCreditCardUseCase.delete(id, fallbackTitle = paymentTitle)
            navigation.back()
        }
    }

    companion object {
        private const val DAYS_30 = 30L
        private const val DAYS_90 = 90L
    }
}
