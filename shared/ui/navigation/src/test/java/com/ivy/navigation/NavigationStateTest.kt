package com.ivy.navigation

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

class NavigationStateTest {
    private val id = UUID.fromString("0f2c4a8e-1111-4a5b-9c3d-123456789abc")

    private val everyScreen: List<Screen> = listOf(
        MainScreen,
        OnboardingScreen,
        CSVScreen(launchedFromOnboarding = true),
        EditTransactionScreen(initialTransactionId = id, type = TransactionType.TRANSFER, accountId = id),
        EditTransactionScreen(initialTransactionId = null, type = TransactionType.EXPENSE),
        TransactionsScreen(accountId = id, transactionType = TransactionType.INCOME, accountIdFilterList = listOf(id)),
        PieChartStatisticScreen(type = TransactionType.EXPENSE, filterExcluded = false),
        EditPlannedScreen(plannedPaymentRuleId = null, type = TransactionType.INCOME, amount = 12.5, title = "Rent"),
        BalanceScreen,
        PlannedPaymentsScreen,
        CategoriesScreen,
        SettingsScreen,
        ImportScreen(launchedFromOnboarding = false),
        ReportScreen,
        BudgetScreen,
        LoansScreen,
        SearchScreen,
        LoanDetailsScreen(loanId = id),
        ExchangeRatesScreen,
        FeaturesScreen,
        AttributionsScreen,
        ContributorsScreen,
        ReleasesScreen,
        DisclaimerScreen,
        CreditCardDetailsScreen(accountId = id),
        EditCreditCardScreen(accountId = null),
        PayCreditCardScreen(accountId = id),
        CreditCardStatementScreen(accountId = id, nominalClosingEpochDay = 20_400),
    )

    @Test
    fun `every screen survives a save and restore`() {
        NavigationState.decode(NavigationState.encode(everyScreen)) shouldBe everyScreen
    }

    @Test
    fun `the saved stack stops before a screen opened with in-memory transactions`() {
        val trn = Transaction(accountId = id, type = TransactionType.EXPENSE, amount = BigDecimal.TEN)
        val stack = listOf(
            MainScreen,
            ReportScreen,
            TransactionsScreen(transactions = listOf(trn)),
            EditTransactionScreen(initialTransactionId = id, type = TransactionType.EXPENSE),
        )

        NavigationState.decode(NavigationState.encode(stack)) shouldBe listOf(MainScreen, ReportScreen)
        NavigationState.isRestorable(
            PieChartStatisticScreen(type = TransactionType.EXPENSE, accountList = persistentListOf(id))
        ) shouldBe false
    }

    @Test
    fun `missing or unreadable saved state restores nothing`() {
        NavigationState.decode(null) shouldBe null
        NavigationState.decode("") shouldBe null
        NavigationState.decode("[]") shouldBe null
        NavigationState.decode("""[{"screen":"com.ivy.navigation.RemovedScreen"}]""") shouldBe null
        NavigationState.decode("not json") shouldBe null
    }

    @Test
    fun `restore rebuilds the back stack`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)

        nav.restore(listOf(MainScreen, SettingsScreen, ExchangeRatesScreen))

        nav.currentScreen shouldBe ExchangeRatesScreen
        nav.snapshot() shouldBe listOf(MainScreen, SettingsScreen, ExchangeRatesScreen)
        nav.back() shouldBe true
        nav.currentScreen shouldBe SettingsScreen
        nav.back() shouldBe true
        nav.back() shouldBe false
        nav.currentScreen shouldBe MainScreen
    }
}
