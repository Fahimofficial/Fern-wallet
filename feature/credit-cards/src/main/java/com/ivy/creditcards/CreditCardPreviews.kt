package com.ivy.creditcards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.creditcards.details.CardDetailsState
import com.ivy.creditcards.details.CardTransactionRow
import com.ivy.creditcards.details.CreditCardDetailsUi
import com.ivy.creditcards.details.HistoryFilter
import com.ivy.creditcards.details.RowKind
import com.ivy.creditcards.edit.CardForm
import com.ivy.creditcards.edit.DueRuleKind
import com.ivy.creditcards.edit.EditCardState
import com.ivy.creditcards.edit.EditCreditCardUi
import com.ivy.creditcards.pay.PayCreditCardUi
import com.ivy.creditcards.pay.PaySource
import com.ivy.creditcards.pay.PayState
import com.ivy.creditcards.statement.CreditCardStatementUi
import com.ivy.creditcards.statement.StatementState
import com.ivy.creditcards.ui.CardVisualData
import com.ivy.creditcards.ui.CreditCardTile
import com.ivy.creditcards.ui.CreditCardTileUi
import com.ivy.creditcards.ui.DueSummary
import com.ivy.data.creditcard.CatalogueBank
import com.ivy.data.model.AccountId
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardNetwork
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.domain.creditcard.CardLedgerEntry
import com.ivy.domain.creditcard.CardOverview
import com.ivy.domain.creditcard.CreditCardCalculator
import com.ivy.legacy.IvyWalletPreview
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Made-up sample data for previews and screenshot tests (no real card or bank data). */
@Suppress("MagicNumber")
internal object CardSamples {
    val today: LocalDate = LocalDate.of(2026, 3, 10)
    private val id = UUID(42, 1)

    val card = CreditCard(
        accountId = AccountId(id),
        bankId = null,
        customBankName = "Sample Bank",
        productId = null,
        productName = "Everyday Rewards",
        network = CardNetwork.RUPAY,
        lastFour = "4821",
        creditLimit = BigDecimal("150000"),
        statementDay = 18,
        dueDateRule = DueDateRule.DaysAfterStatement(20),
        openingOutstanding = BigDecimal("12000"),
        openingDate = LocalDate.of(2026, 1, 5),
        status = CardStatus.ACTIVE,
        appearance = CardAppearance.FERN,
        remindersEnabled = true,
        reminderDaysBefore = 3,
    )

    private fun e(date: String, amount: String, kind: CardLedgerEntry.Kind): CardLedgerEntry =
        CardLedgerEntry(null, LocalDate.parse(date), BigDecimal(amount), kind)

    val overview: CardOverview = CreditCardCalculator.overview(
        card = card,
        ledger = listOf(
            e("2026-01-20", "4850.50", CardLedgerEntry.Kind.CHARGE),
            e("2026-02-02", "12000", CardLedgerEntry.Kind.PAYMENT),
            e("2026-02-11", "18230.75", CardLedgerEntry.Kind.CHARGE),
            e("2026-02-14", "1200", CardLedgerEntry.Kind.CREDIT),
            e("2026-03-01", "5000", CardLedgerEntry.Kind.PAYMENT),
            e("2026-03-04", "2340", CardLedgerEntry.Kind.CHARGE),
        ),
        overrides = emptyList(),
        today = today,
    )

    val visual = CardVisualData(
        nickname = "Everyday card",
        issuerName = "Sample Bank",
        productName = "Everyday Rewards",
        network = CardNetwork.RUPAY,
        lastFour = "4821",
        appearance = CardAppearance.FERN,
    )

    val rows = listOf(
        CardTransactionRow(
            UUID(1, 1),
            "Groceries",
            "Groceries",
            LocalDate.parse("2026-03-04"),
            BigDecimal("2340"),
            RowKind.CHARGE
        ),
        CardTransactionRow(
            UUID(1, 2),
            "Card payment",
            null,
            LocalDate.parse("2026-03-01"),
            BigDecimal("5000"),
            RowKind.PAYMENT
        ),
        CardTransactionRow(
            UUID(1, 3),
            "Refund: headphones",
            "Shopping",
            LocalDate.parse("2026-02-14"),
            BigDecimal("1200"),
            RowKind.CREDIT
        ),
        CardTransactionRow(
            UUID(1, 4),
            "Flights",
            "Travel",
            LocalDate.parse("2026-02-11"),
            BigDecimal("18230.75"),
            RowKind.CHARGE
        ),
    )

    fun tiles(): List<CreditCardTileUi> = listOf(
        tile(UUID(9, 1), visual, overview),
        tile(
            UUID(9, 2),
            visual.copy(
                nickname = "Travel card",
                issuerName = "Example Bank",
                productName = null,
                network = CardNetwork.VISA,
                lastFour = "0093",
                appearance = CardAppearance.MIDNIGHT
            ),
            CreditCardCalculator.overview(
                card = card.copy(
                    creditLimit = BigDecimal("60000"),
                    statementDay = 2,
                    openingOutstanding = BigDecimal.ZERO
                ),
                ledger = listOf(e("2026-01-25", "51000", CardLedgerEntry.Kind.CHARGE)),
                overrides = emptyList(),
                today = today,
            )
        ),
    )

    private fun tile(id: UUID, visual: CardVisualData, overview: CardOverview): CreditCardTileUi {
        val s = overview.latestStatement
        return CreditCardTileUi(
            accountId = id,
            visual = visual,
            currency = "INR",
            outstanding = overview.outstanding,
            creditLimit = overview.creditLimit,
            availableCredit = overview.availableCredit,
            utilisation = overview.utilisation?.toFloat()?.coerceIn(0f, 1f),
            statementClosing = overview.currentCycle?.closingDate,
            due = s?.let {
                DueSummary(
                    status = it.status,
                    dueDate = it.dueDate,
                    remaining = it.remainingDue,
                    daysLeft = it.dueDate?.let { d -> ChronoUnit.DAYS.between(today, d) },
                )
            },
            status = CardStatus.ACTIVE,
            orderNum = 0.0,
        )
    }
}

private fun theme(isDark: Boolean): Theme = if (isDark) Theme.DARK else Theme.LIGHT

/** For screenshot testing */
@Composable
fun CreditCardTilesUiTest(isDark: Boolean, modifier: Modifier = Modifier) {
    IvyWalletPreview(theme(isDark)) {
        Column(modifier) {
            Spacer(Modifier.height(24.dp))
            CardSamples.tiles().forEach {
                CreditCardTile(tile = it, today = CardSamples.today, hideAmounts = false, onClick = {})
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** For screenshot testing */
@Composable
fun CreditCardDetailsUiTest(isDark: Boolean) {
    IvyWalletPreview(theme(isDark)) {
        CreditCardDetailsUi(
            state = CardDetailsState(
                loading = false,
                notFound = false,
                name = CardSamples.visual.nickname,
                currency = "INR",
                visual = CardSamples.visual,
                overview = CardSamples.overview,
                status = CardStatus.ACTIVE,
                statementDayConfigured = true,
                dueRuleConfigured = true,
                transactions = CardSamples.rows.toImmutableList(),
                transactionCount = CardSamples.rows.size,
                query = "",
                filter = HistoryFilter.ALL,
                today = CardSamples.today,
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun EditCreditCardUiTest(isDark: Boolean) {
    IvyWalletPreview(theme(isDark)) {
        EditCreditCardUi(
            state = EditCardState(
                isEdit = false,
                loading = false,
                form = CardForm(
                    useCustomBank = true,
                    customBankName = "Sample Bank",
                    useCustomProduct = true,
                    customProductName = "Everyday Rewards",
                    nickname = "Everyday card",
                    currency = "INR",
                    network = CardNetwork.RUPAY,
                    lastFour = "4821",
                    creditLimit = "150000",
                    statementDay = 18,
                    dueRuleKind = DueRuleKind.DAYS_AFTER_STATEMENT,
                    dueRuleValue = "20",
                    openingDate = CardSamples.today,
                ),
                bankQuery = "",
                bankResults = persistentListOf(),
                selectedBank = null,
                productQuery = "",
                productResults = persistentListOf(),
                selectedProduct = null,
                bankHasProducts = false,
                errors = emptyMap(),
                hasTransactions = false,
                today = CardSamples.today,
                catalogueVerifiedOn = "2026-10-27",
                saving = false,
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing: bank search step. */
@Composable
fun EditCreditCardBankSearchUiTest(isDark: Boolean) {
    IvyWalletPreview(theme(isDark)) {
        EditCreditCardUi(
            state = EditCardState(
                isEdit = false,
                loading = false,
                form = CardForm(currency = "INR", openingDate = CardSamples.today),
                bankQuery = "",
                bankResults = listOf(
                    CatalogueBank(
                        "sample-a",
                        "Sample Bank Ltd.",
                        "Sample Bank",
                        category = "PRIVATE_SECTOR",
                        creditCardIssuer = true
                    ),
                    CatalogueBank(
                        "sample-b",
                        "Example Bank Ltd.",
                        "Example Bank",
                        category = "PUBLIC_SECTOR",
                        creditCardIssuer = false
                    ),
                ).toImmutableList(),
                selectedBank = null,
                productQuery = "",
                productResults = persistentListOf(),
                selectedProduct = null,
                bankHasProducts = false,
                errors = emptyMap(),
                hasTransactions = false,
                today = CardSamples.today,
                catalogueVerifiedOn = "2026-10-27",
                saving = false,
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@Suppress("MagicNumber") // sample account ids
@Composable
fun PayCreditCardUiTest(isDark: Boolean) {
    IvyWalletPreview(theme(isDark)) {
        val statement = CardSamples.overview.latestStatement
        PayCreditCardUi(
            state = PayState(
                loading = false,
                notFound = false,
                cardName = CardSamples.visual.nickname,
                currency = "INR",
                outstanding = CardSamples.overview.outstanding,
                remainingDue = statement?.remainingDue,
                minimumDueRemaining = null,
                sources = persistentListOf(
                    PaySource(UUID(5, 1), "Salary account", "INR"),
                    PaySource(UUID(5, 2), "Savings", "INR"),
                ),
                source = PaySource(UUID(5, 1), "Salary account", "INR"),
                amount = statement?.remainingDue?.toPlainString().orEmpty(),
                sourceAmount = "",
                date = CardSamples.today,
                note = "",
                overpayment = null,
                errors = emptySet(),
                today = CardSamples.today,
                saving = false,
                converting = false,
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun CreditCardStatementUiTest(isDark: Boolean) {
    IvyWalletPreview(theme(isDark)) {
        CreditCardStatementUi(
            state = StatementState(
                loading = false,
                cardName = CardSamples.visual.nickname,
                currency = "INR",
                statement = CardSamples.overview.latestStatement,
                rows = CardSamples.rows.drop(2).toImmutableList(),
                issuerForm = null,
                invalidAmount = false,
                today = CardSamples.today,
            ),
            onEvent = {}
        )
    }
}
