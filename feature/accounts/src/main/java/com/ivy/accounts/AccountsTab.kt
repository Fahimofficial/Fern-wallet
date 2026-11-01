package com.ivy.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.creditcards.ui.CreditCardTile
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountType
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.navigation.CreditCardDetailsScreen
import com.ivy.navigation.EditCreditCardScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.ReorderButton
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.wallet.ui.theme.dynamicContrast
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Composable
fun BoxWithConstraintsScope.AccountsTab() {
    val viewModel: AccountsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: AccountsState,
    onEvent: (AccountsEvent) -> Unit = {}
) {
    val nav = navigation()
    val ivyContext = com.ivy.legacy.ivyWalletCtx()
    var listState = rememberLazyListState()
    if (!state.accountsData.isEmpty()) {
        listState = rememberScrollPositionListState(
            key = "accounts_lazy_column",
            initialFirstVisibleItemIndex = ivyContext.accountsListState?.firstVisibleItemIndex ?: 0,
            initialFirstVisibleItemScrollOffset = ivyContext.accountsListState?.firstVisibleItemScrollOffset
                ?: 0
        )
    }
    // Cards are listed in their own section below; filter once per data change, not per frame.
    val regularAccounts = remember(state.accountsData) {
        state.accountsData.filter { it.account.type != AccountType.CREDIT_CARD }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .horizontalSwipeListener(
                sensitivity = 200,
                state = rememberSwipeListenerState(),
                onSwipeLeft = {
                    ivyContext.selectMainTab(com.ivy.legacy.data.model.MainTab.HOME)
                },
                onSwipeRight = {
                    ivyContext.selectMainTab(com.ivy.legacy.data.model.MainTab.HOME)
                }
            ),
        state = listState
    ) {
        item {
            Spacer(Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(24.dp))

                Column {
                    Text(
                        text = stringResource(R.string.accounts),
                        style = UI.typo.b1.style(
                            color = UI.colors.pureInverse,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                Spacer(Modifier.weight(1f))

                ReorderButton {
                    onEvent(
                        AccountsEvent.OnReorderModalVisible(reorderVisible = true)
                    )
                }

                Spacer(Modifier.width(24.dp))
            }
            if (!state.hideTotalBalance) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    IncomeExpensesRow(
                        currency = state.baseCurrency,
                        incomeLabel = stringResource(id = R.string.total_balance),
                        income = state.totalBalanceWithoutExcluded.toDoubleOrNull() ?: 0.00,
                        expensesLabel = stringResource(id = R.string.total_balance_excluded),
                        expenses = state.totalBalanceWithExcluded.toDoubleOrNull() ?: 0.00
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        items(
            items = regularAccounts,
            key = { it.account.id.value },
            contentType = { "account" },
        ) {
            Spacer(Modifier.height(16.dp))
            AccountCard(
                baseCurrency = state.baseCurrency,
                accountData = it,
                compactModeEnabled = state.compactAccountsModeEnabled,
                onBalanceClick = {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = it.account.id.value,
                            categoryId = null
                        )
                    )
                }
            ) {
                nav.navigateTo(
                    TransactionsScreen(
                        accountId = it.account.id.value,
                        categoryId = null
                    )
                )
            }
        }

        creditCardsSection(
            state = state,
            onAddCard = { nav.navigateTo(EditCreditCardScreen(accountId = null)) },
            onCardClick = { nav.navigateTo(CreditCardDetailsScreen(it)) },
        )

        item {
            Spacer(Modifier.height(150.dp)) // scroll hack
        }
    }

    ReorderModalSingleType(
        visible = state.reorderVisible,
        initialItems = state.accountsData,
        dismiss = {
            onEvent(AccountsEvent.OnReorderModalVisible(reorderVisible = false))
        },
        onReordered = {
            onEvent(AccountsEvent.OnReorder(reorderedList = it))
        }
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.account.name.value,
            style = UI.typo.b1.style(
                color = item.account.color.value.toComposeColor(),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun LazyListScope.creditCardsSection(
    state: AccountsState,
    onAddCard: () -> Unit,
    onCardClick: (UUID) -> Unit,
) {
    item(key = "credit_cards_header") {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.padding(start = 24.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = stringResource(R.string.cc_section_title),
                    style = UI.typo.b1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.ExtraBold)
                )
                val total = state.cardsOutstandingTotal
                if (total != null && !state.hideTotalBalance) {
                    Text(
                        text = "${stringResource(R.string.cc_total_outstanding)}: " +
                            "${total.format(state.baseCurrency)} ${state.baseCurrency}",
                        style = UI.typo.nC.style(color = UI.colors.gray)
                    )
                }
            }
            Text(
                modifier = Modifier
                    .clip(UI.shapes.rFull)
                    .clickable(role = Role.Button, onClick = onAddCard)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .testTag("add_credit_card"),
                text = "+ " + stringResource(R.string.cc_add_card_short),
                style = UI.typo.b2.style(color = Ivy, fontWeight = FontWeight.Bold)
            )
        }
        if (state.creditCards.isEmpty()) {
            Text(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                text = stringResource(R.string.cc_no_cards_hint),
                style = UI.typo.nC.style(color = UI.colors.gray)
            )
        }
    }
    items(state.creditCards, key = { "card-${it.accountId}" }, contentType = { "credit_card" }) { tile ->
        Spacer(Modifier.height(12.dp))
        CreditCardTile(
            tile = tile,
            today = state.today,
            hideAmounts = false,
            onClick = { onCardClick(tile.accountId) }
        )
    }
}

@Composable
private fun AccountCard(
    baseCurrency: String,
    accountData: AccountData,
    compactModeEnabled: Boolean,
    onBalanceClick: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .border(1.dp, UI.colors.medium, UI.shapes.r4)
            .clickable(
                onClick = onClick
            )
    ) {
        val account = accountData.account
        val contrastColor = findContrastTextColor(account.color.value.toComposeColor())
        val currency = account.asset.code

        AccountHeader(
            accountData = accountData,
            currency = currency,
            baseCurrency = baseCurrency,
            contrastColor = contrastColor,
            onBalanceClick = onBalanceClick
        )

        if (!compactModeEnabled) {
            Spacer(Modifier.height(12.dp))

            IncomeExpensesRow(
                currency = currency,
                incomeLabel = stringResource(R.string.month_income),
                income = accountData.monthlyIncome,
                expensesLabel = stringResource(R.string.month_expenses),
                expenses = accountData.monthlyExpenses
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AccountHeader(
    accountData: AccountData,
    currency: String,
    baseCurrency: String,
    contrastColor: Color,
    onBalanceClick: () -> Unit
) {
    val account = accountData.account

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(account.color.value.toComposeColor(), UI.shapes.r4Top)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            ItemIconSDefaultIcon(
                iconName = account.icon?.id,
                defaultIcon = R.drawable.ic_custom_account_s,
                tint = contrastColor
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = account.name.value,
                style = UI.typo.b1.style(
                    color = contrastColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            if (!account.includeInBalance) {
                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.excluded),
                    style = UI.typo.c.style(
                        color = account.color.value.toComposeColor().dynamicContrast()
                    )
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        BalanceRow(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
                    onBalanceClick()
                },
            textColor = contrastColor,
            currency = currency,
            balance = accountData.balance,

            balanceFontSize = 30.sp,
            currencyFontSize = 30.sp,

            currencyUpfront = false
        )

        if (currency != baseCurrency && accountData.balanceBaseCurrency != null) {
            BalanceRowMini(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
                        onBalanceClick()
                    }
                    .testTag("baseCurrencyEquivalent"),
                textColor = account.color.value.toComposeColor().dynamicContrast(),
                currency = baseCurrency,
                balance = accountData.balanceBaseCurrency!!,
                currencyUpfront = false
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Generic sample data for previews and README screenshots (no real accounts or amounts). */
@Suppress("MagicNumber")
private fun sampleAccountsState(compact: Boolean): AccountsState {
    fun account(
        name: String,
        color: Long,
        icon: String?,
        includeInBalance: Boolean = true,
        currency: String = "USD",
    ) = Account(
        id = AccountId(UUID.nameUUIDFromBytes(name.toByteArray())),
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(color.toInt()),
        asset = AssetCode.unsafe(currency),
        icon = icon?.let { IconAsset.unsafe(it) },
        includeInBalance = includeInBalance,
        orderNum = 0.0,
    )
    return AccountsState(
        baseCurrency = "USD",
        accountsData = persistentListOf(
            AccountData(
                account = account("Everyday", 0xFF3F51B5, null),
                balance = 3482.15,
                balanceBaseCurrency = null,
                monthlyExpenses = 1640.32,
                monthlyIncome = 4250.0
            ),
            AccountData(
                // Foreign-currency account: covers the converted-balance row (BalanceRowMini).
                account = account("Savings", 0xFFF29F30, null, currency = "EUR"),
                balance = 7590.0,
                balanceBaseCurrency = 8250.0,
                monthlyExpenses = 0.0,
                monthlyIncome = 460.0
            ),
            AccountData(
                account = account("Cash", 0xFF00897B, "cash"),
                balance = 186.5,
                balanceBaseCurrency = null,
                monthlyExpenses = 228.0,
                monthlyIncome = 0.0
            ),
            AccountData(
                account = account("Travel card", 0xFFD81B60, null, includeInBalance = false),
                balance = 562.0,
                balanceBaseCurrency = null,
                monthlyExpenses = 0.0,
                monthlyIncome = 0.0
            ),
        ),
        totalBalanceWithExcluded = "12480.65",
        totalBalanceWithExcludedText = "USD 12,480.65",
        totalBalanceWithoutExcluded = "11918.65",
        totalBalanceWithoutExcludedText = "USD 11,918.65",
        reorderVisible = false,
        compactAccountsModeEnabled = compact,
        hideTotalBalance = false
    )
}

@Preview
@Composable
private fun PreviewAccountsTabCompactModeDisabled(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        UI(state = sampleAccountsState(compact = false))
    }
}

@Preview
@Composable
private fun PreviewAccountsTabCompactModeEnabled(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        UI(state = sampleAccountsState(compact = true))
    }
}

/** For screen shot testing **/
@Composable
fun AccountsTabNonCompactUITest(dark: Boolean) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCompactModeDisabled(theme)
}

/** For screen shot testing **/
@Composable
fun AccountsTabCompactUITest(dark: Boolean) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCompactModeEnabled(theme)
}
