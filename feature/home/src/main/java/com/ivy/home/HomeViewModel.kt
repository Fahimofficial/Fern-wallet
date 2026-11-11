package com.ivy.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.domain.usecase.exchange.SyncExchangeRatesUseCase
import com.ivy.frp.fixUnit
import com.ivy.frp.then
import com.ivy.frp.thenInvokeAfter
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.home.customerjourney.CustomerJourneyCardsProvider
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.BufferInfo
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.data.model.toUTCCloseTimeRange
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.datamodel.Settings
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.domain.action.settings.UpdateSettingsAct
import com.ivy.legacy.domain.action.viewmodel.home.ShouldHideIncomeAct
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.ioThread
import com.ivy.navigation.BalanceScreen
import com.ivy.navigation.MainScreen
import com.ivy.navigation.Navigation
import com.ivy.ui.ComposeViewModel
import com.ivy.wallet.domain.action.account.AccountsAct
import com.ivy.wallet.domain.action.global.StartDayOfMonthAct
import com.ivy.wallet.domain.action.settings.CalcBufferDiffAct
import com.ivy.wallet.domain.action.settings.SettingsAct
import com.ivy.wallet.domain.action.transaction.HistoryWithDateDivsAct
import com.ivy.wallet.domain.action.viewmodel.home.HasTrnsAct
import com.ivy.wallet.domain.action.viewmodel.home.OverdueAct
import com.ivy.wallet.domain.action.viewmodel.home.ShouldHideBalanceAct
import com.ivy.wallet.domain.action.viewmodel.home.UpcomingAct
import com.ivy.wallet.domain.action.viewmodel.home.UpdateAccCacheAct
import com.ivy.wallet.domain.action.viewmodel.home.UpdateCategoriesCacheAct
import com.ivy.wallet.domain.action.wallet.CalcIncomeExpenseAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import com.ivy.wallet.domain.deprecated.logic.PlannedPaymentsLogic
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import com.ivy.data.DataObserver
import com.ivy.data.datasource.SupportCardsDataSource
import com.ivy.data.datasource.SupportCardsPrefs
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.refreshWidget
import com.ivy.data.model.TransactionId
import com.ivy.data.repository.TransactionRepository
import com.ivy.ui.R
import com.ivy.ui.undo.UndoController
import com.ivy.widget.balance.WalletBalanceWidgetReceiver
import com.ivy.data.DataWriteEvent

@Stable
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ivyContext: IvyWalletCtx,
    private val nav: Navigation,
    private val plannedPaymentsLogic: PlannedPaymentsLogic,
    private val customerJourneyLogic: CustomerJourneyCardsProvider,
    private val historyWithDateDivsAct: HistoryWithDateDivsAct,
    private val calcIncomeExpenseAct: CalcIncomeExpenseAct,
    private val calcWalletBalanceAct: CalcWalletBalanceAct,
    private val settingsAct: SettingsAct,
    private val accountsAct: AccountsAct,
    private val categoryRepository: CategoryRepository,
    private val calcBufferDiffAct: CalcBufferDiffAct,
    private val upcomingAct: UpcomingAct,
    private val overdueAct: OverdueAct,
    private val hasTrnsAct: HasTrnsAct,
    private val startDayOfMonthAct: StartDayOfMonthAct,
    private val shouldHideBalanceAct: ShouldHideBalanceAct,
    private val shouldHideIncomeAct: ShouldHideIncomeAct,
    private val updateSettingsAct: UpdateSettingsAct,
    private val updateAccCacheAct: UpdateAccCacheAct,
    private val updateCategoriesCacheAct: UpdateCategoriesCacheAct,
    private val syncExchangeRatesUseCase: SyncExchangeRatesUseCase,
    private val transactionMapper: TransactionMapper,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val dataObserver: DataObserver,
    private val transactionRepo: TransactionRepository,
    private val undoController: UndoController,
    private val sharedPrefs: SharedPrefs,
    private val supportCardsDataSource: SupportCardsDataSource,
    @ApplicationContext appContext: Context,
) : ComposeViewModel<HomeState, HomeEvent>() {
    // When the app was last installed or updated. The one-time cards come back whenever it changes,
    // which a version code can't promise: test builds and re-installs keep the same one.
    @Suppress("DEPRECATION") // the Flags overload needs API 33; this works on every supported version
    private val installStamp = appContext.packageManager
        .getPackageInfo(appContext.packageName, 0).lastUpdateTime

    init {
        // e.g. a deleted transaction restored by Undo while Home is already showing.
        viewModelScope.launch {
            dataObserver.writeEvents.collect { event ->
                if (event is DataWriteEvent.TransactionsChanged) reload()
            }
        }
    }

    private var currentTheme by mutableStateOf(Theme.AUTO)
    private var name by mutableStateOf("")
    private var period by mutableStateOf(ivyContext.selectedPeriod)
    private var baseData by mutableStateOf(
        AppBaseData(
            baseCurrency = "",
            accounts = persistentListOf(),
            categories = persistentListOf()
        )
    )
    private var history by mutableStateOf<ImmutableList<TransactionHistoryItem>>(persistentListOf())
    private var stats by mutableStateOf(IncomeExpensePair.zero())
    private var balance by mutableStateOf(BigDecimal.ZERO)
    private var buffer by mutableStateOf(
        BufferInfo(
            amount = BigDecimal.ZERO,
            bufferDiff = BigDecimal.ZERO,
        )
    )
    private var upcoming by mutableStateOf(
        LegacyDueSection(
            trns = persistentListOf(),
            stats = IncomeExpensePair.zero(),
            expanded = false,
        )
    )
    private var overdue by mutableStateOf(
        LegacyDueSection(
            trns = persistentListOf(),
            stats = IncomeExpensePair.zero(),
            expanded = false,
        )
    )
    private var customerJourneyCards by
    mutableStateOf<ImmutableList<CustomerJourneyCardModel>>(persistentListOf())
    private var hideBalance by mutableStateOf(false)
    private var hideIncome by mutableStateOf(false)
    private var expanded by mutableStateOf(true)
    private var celebrateSavingsGoal by mutableStateOf(false)

    /** null until the stored values are read, so a dismissed card never flashes in. */
    private var supportCardsPrefs by mutableStateOf<SupportCardsPrefs?>(null)

    init {
        viewModelScope.launch { supportCardsDataSource.prefs.collect { supportCardsPrefs = it } }
    }

    @Composable
    override fun uiState(): HomeState {
        LaunchedEffect(Unit) {
            start()
        }

        return HomeState(
            theme = getTheme(),
            name = getName(),
            period = getPeriod(),
            baseData = getBaseData(),
            history = getHistory(),
            stats = getStats(),
            balance = getBalance(),
            buffer = getBuffer(),
            upcoming = getUpcoming(),
            overdue = getOverdue(),
            customerJourneyCards = getCustomerJourneyCards(),
            hideBalance = getHideBalance(),
            expanded = getExpanded(),
            hideIncome = getHideIncome(),
            celebrateSavingsGoal = celebrateSavingsGoal,
            supportCards = supportCardsUi(supportCardsPrefs, installStamp),
        )
    }

    @Composable
    private fun getTheme(): Theme {
        return currentTheme
    }

    @Composable
    private fun getName(): String {
        return name
    }

    @Composable
    private fun getPeriod(): TimePeriod {
        return period
    }

    @Composable
    private fun getBaseData(): AppBaseData {
        return baseData
    }

    @Composable
    private fun getHistory(): ImmutableList<TransactionHistoryItem> {
        return history
    }

    @Composable
    private fun getStats(): IncomeExpensePair {
        return stats
    }

    @Composable
    private fun getBalance(): BigDecimal {
        return balance
    }

    @Composable
    private fun getBuffer(): BufferInfo {
        return buffer
    }

    @Composable
    private fun getUpcoming(): LegacyDueSection {
        return upcoming
    }

    @Composable
    private fun getOverdue(): LegacyDueSection {
        return overdue
    }

    @Composable
    private fun getCustomerJourneyCards(): ImmutableList<CustomerJourneyCardModel> {
        return customerJourneyCards
    }

    @Composable
    private fun getHideBalance(): Boolean {
        return hideBalance
    }

    @Composable
    private fun getExpanded(): Boolean {
        return expanded
    }

    @Composable
    private fun getHideIncome(): Boolean {
        return hideIncome
    }

    // One branch per event: the complexity is the dispatch table, not logic.
    @Suppress("CyclomaticComplexMethod")
    override fun onEvent(event: HomeEvent) {
        viewModelScope.launch {
            when (event) {
                HomeEvent.BalanceClick -> onBalanceClick()
                HomeEvent.HiddenBalanceClick -> onHiddenBalanceClick()
                HomeEvent.HiddenIncomeClick -> onHiddenIncomeClick()
                is HomeEvent.PayOrGetPlanned -> payOrGetPlanned(event.transaction)
                is HomeEvent.SkipPlanned -> skipPlanned(event.transaction)
                is HomeEvent.SkipAllPlanned -> skipAllPlanned(event.transactions)
                is HomeEvent.SetPeriod -> setPeriod(event.period)
                HomeEvent.SelectNextMonth -> onSelectNextMonth()
                HomeEvent.SelectPreviousMonth -> onSelectPreviousMonth()
                is HomeEvent.SetUpcomingExpanded -> setUpcomingExpanded(event.expanded)
                is HomeEvent.SetOverdueExpanded -> setOverdueExpanded(event.expanded)
                is HomeEvent.SetBuffer -> setBuffer(event.buffer)
                is HomeEvent.SetCurrency -> setCurrency(event.currency).fixUnit()
                HomeEvent.SwitchTheme -> switchTheme()
                is HomeEvent.DismissCustomerJourneyCard -> dismissCustomerJourneyCard(event.card)
                is HomeEvent.SetExpanded -> setExpanded(event.expanded)
                HomeEvent.Refresh -> start()
                is HomeEvent.DeleteTransaction -> deleteTransaction(event.transaction)
                HomeEvent.ToggleHideBalance -> toggleHideBalance()
                HomeEvent.SavingsGoalCelebrated -> savingsGoalCelebrated()
                HomeEvent.DismissHelpCard -> supportCardsDataSource.dismissHelpCard(installStamp)
                HomeEvent.DismissSupportCard -> supportCardsDataSource.dismissSupportCard(installStamp)
            }
        }
    }

    private suspend fun start() {
        suspend {
            val startDay = startDayOfMonthAct(Unit)
            ivyContext.initSelectedPeriodInMemory(
                startDayOfMonth = startDay
            )
        } thenInvokeAfter ::reload
    }

    // -----------------------------------------------------------------------------------
    private suspend fun reload(
        timePeriod: TimePeriod = ivyContext.selectedPeriod
    ) = suspend {
        val settings = settingsAct(Unit)
        val hideBalance = shouldHideBalanceAct(Unit)
        val hideIncome = shouldHideIncomeAct(Unit)

        currentTheme = settings.theme
        name = settings.name
        period = timePeriod
        this.hideBalance = hideBalance
        this.hideIncome = hideIncome

        // This method is used to restore the theme when user imports locally backed up data
        ivyContext.switchTheme(theme = settings.theme)

        Pair(
            settings,
            period.toRange(
                startDateOfMonth = ivyContext.startDayOfMonth,
                timeConverter = timeConverter,
                timeProvider = timeProvider
            ).toUTCCloseTimeRange()
        )
    } then ::loadAppBaseData then ::loadIncomeExpenseBalance then
            ::loadBuffer then ::loadTrnHistory then
            ::loadDueTrns thenInvokeAfter ::loadCustomerJourney

    private suspend fun loadAppBaseData(
        input: Pair<Settings, ClosedTimeRange>
    ): Triple<Settings, ClosedTimeRange, List<Account>> =
        suspend {} then accountsAct then updateAccCacheAct then { accounts ->
            accounts
        } then { accounts ->
            val retrievedCategories = categoryRepository.findAll()
            val categories = updateCategoriesCacheAct(retrievedCategories)
            accounts to categories
        } thenInvokeAfter { (accounts, categories) ->
            val (settings, timeRange) = input

            baseData = AppBaseData(
                baseCurrency = settings.baseCurrency,
                categories = categories.toImmutableList(),
                accounts = accounts.toImmutableList()
            )

            Triple(settings, timeRange, accounts)
        }

    private suspend fun loadIncomeExpenseBalance(
        input: Triple<Settings, ClosedTimeRange, List<Account>>
    ): Triple<Settings, ClosedTimeRange, BigDecimal> {
        val (settings, timeRange, accounts) = input

        val incomeExpense = calcIncomeExpenseAct(
            CalcIncomeExpenseAct.Input(
                baseCurrency = settings.baseCurrency,
                accounts = accounts,
                range = timeRange
            )
        )

        val balanceAmount = calcWalletBalanceAct(
            CalcWalletBalanceAct.Input(baseCurrency = settings.baseCurrency)
        )

        balance = balanceAmount
        stats = incomeExpense

        return Triple(settings, timeRange, balanceAmount)
    }

    private suspend fun loadBuffer(
        input: Triple<Settings, ClosedTimeRange, BigDecimal>
    ): Pair<String, ClosedTimeRange> {
        val (settings, timeRange, balance) = input

        buffer = BufferInfo(
            amount = settings.bufferAmount,
            bufferDiff = calcBufferDiffAct(
                CalcBufferDiffAct.Input(
                    balance = balance,
                    buffer = settings.bufferAmount
                )
            )
        )

        val goal = settings.bufferAmount
        val celebrated = ioThread { sharedPrefs.getString(SharedPrefs.SAVINGS_GOAL_CELEBRATED, null) }
        celebrateSavingsGoal = goal > BigDecimal.ZERO && balance >= goal &&
            celebrated != goal.stripTrailingZeros().toPlainString()

        return settings.baseCurrency to timeRange
    }

    private suspend fun savingsGoalCelebrated() {
        celebrateSavingsGoal = false
        val goal = buffer.amount.stripTrailingZeros().toPlainString()
        ioThread { sharedPrefs.putString(SharedPrefs.SAVINGS_GOAL_CELEBRATED, goal) }
    }

    private suspend fun loadTrnHistory(
        input: Pair<String, ClosedTimeRange>
    ): Pair<String, ClosedTimeRange> {
        val (baseCurrency, timeRange) = input

        history = historyWithDateDivsAct(
            HistoryWithDateDivsAct.Input(
                range = timeRange,
                baseCurrency = baseCurrency
            )
        )

        return baseCurrency to timeRange
    }

    private suspend fun loadDueTrns(
        input: Pair<String, ClosedTimeRange>
    ): Unit = suspend {
        UpcomingAct.Input(baseCurrency = input.first, range = input.second)
    } then upcomingAct then { result ->
        upcoming = LegacyDueSection(
            trns = with(transactionMapper) {
                result.upcomingTrns.map {
                    it.toEntity().toLegacyDomain()
                }.toImmutableList()
            },
            stats = result.upcoming,
            expanded = upcoming.expanded
        )
    } then {
        OverdueAct.Input(baseCurrency = input.first, toRange = input.second.to)
    } then overdueAct thenInvokeAfter { result ->
        overdue = LegacyDueSection(
            trns = with(transactionMapper) {
                result.overdueTrns.map {
                    it.toEntity().toLegacyDomain()
                }.toImmutableList()
            },
            stats = result.overdue,
            expanded = overdue.expanded
        )
    }

    private suspend fun loadCustomerJourney(unit: Unit) {
        customerJourneyCards = ioThread {
            customerJourneyLogic.loadCards().toImmutableList()
        }
    }
// -----------------------------------------------------------------

    private fun setUpcomingExpanded(expanded: Boolean) {
        upcoming = upcoming.copy(expanded = expanded)
    }

    private fun setOverdueExpanded(expanded: Boolean) {
        overdue = overdue.copy(expanded = expanded)
    }

    private suspend fun onBalanceClick() {
        val hasTransactions = hasTrnsAct(Unit)
        if (hasTransactions) {
            // has transactions show him "Balance" screen
            nav.navigateTo(BalanceScreen)
        } else {
            // doesn't have transactions lead him to adjust balance
            ivyContext.selectMainTab(MainTab.ACCOUNTS)
            nav.navigateTo(MainScreen)
        }
    }

    // Same restorable delete + Undo as the edit screen.
    private suspend fun deleteTransaction(transaction: Transaction) {
        val deleted = transactionRepo.deleteByIdRestorable(TransactionId(transaction.id))
        refreshWidget(WalletBalanceWidgetReceiver::class.java)
        reload()
        if (deleted != null) {
            undoController.show(text = R.string.transaction_deleted) {
                deleted.restore()
                refreshWidget(WalletBalanceWidgetReceiver::class.java)
                dataObserver.post(DataWriteEvent.TransactionsChanged)
            }
        }
    }

    private suspend fun toggleHideBalance() {
        hideBalance = !hideBalance
        ioThread { sharedPrefs.putBoolean(SharedPrefs.HIDE_CURRENT_BALANCE, hideBalance) }
    }

    private suspend fun onHiddenBalanceClick() {
        hideBalance = false

        // Showing Balance fow 5s
        delay(5000)

        hideBalance = true
    }

    private suspend fun onHiddenIncomeClick() {
        hideIncome = false

        // Showing Balance fow 5s
        delay(5000)

        hideIncome = true
    }

    private fun switchTheme() {
        viewModelScope.launch {
            settingsAct.getSettingsWithNextTheme().run {
                updateSettingsAct(this)
                ivyContext.switchTheme(this.theme)
                currentTheme = this.theme
            }
        }
    }

    private fun setBuffer(newBuffer: Double) {
        viewModelScope.launch {
            val currentSettings =
                settingsAct.getSettings().copy(bufferAmount = newBuffer.toBigDecimal())
            updateSettingsAct(currentSettings)
            buffer = buffer.copy(amount = currentSettings.bufferAmount)
        }
    }

    private suspend fun setCurrency(newCurrency: String) = settingsAct then {
        it.copy(
            baseCurrency = newCurrency
        )
    } then updateSettingsAct then {
        // update exchange rates from POV of the new base currency
        AssetCode.from(newCurrency).onRight {
            syncExchangeRatesUseCase.sync(it)
        }
    } then {
        reload()
    }

    private suspend fun payOrGetPlanned(transaction: Transaction) {
        plannedPaymentsLogic.payOrGetLegacy(
            transaction = transaction,
            skipTransaction = false
        ) {
            reload()
        }
    }

    private suspend fun skipPlanned(transaction: Transaction) {
        plannedPaymentsLogic.payOrGetLegacy(
            transaction = transaction,
            skipTransaction = true
        ) {
            reload()
        }
    }

    private suspend fun skipAllPlanned(transactions: List<Transaction>) {
        plannedPaymentsLogic.payOrGetLegacy(
            transactions = transactions,
            skipTransaction = true
        ) {
            reload()
        }
    }

    private suspend fun dismissCustomerJourneyCard(card: CustomerJourneyCardModel) = suspend {
        customerJourneyLogic.dismissCard(card)
    } thenInvokeAfter {
        reload()
    }

    private suspend fun onSelectNextMonth() {
        val month = period.month
        val year = period.year ?: dateNowUTC().year
        val period = month?.incrementMonthPeriod(ivyContext, 1L, year = year)
        if (period != null) {
            setPeriod(period)
        }
    }

    private suspend fun onSelectPreviousMonth() {
        val month = period.month
        val year = period.year ?: dateNowUTC().year
        val period = month?.incrementMonthPeriod(ivyContext, -1L, year = year)
        if (period != null) {
            setPeriod(period)
        }
    }

    private suspend fun setPeriod(period: TimePeriod) {
        reload(period)
    }

    @JvmName("setExpandedMethod")
    private fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
    }
}
