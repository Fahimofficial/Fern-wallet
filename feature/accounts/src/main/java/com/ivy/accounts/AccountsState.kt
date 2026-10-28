package com.ivy.accounts

import androidx.compose.runtime.Immutable
import com.ivy.creditcards.ui.CreditCardTileUi
import com.ivy.legacy.data.model.AccountData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate

@Immutable
@Suppress("DataClassDefaultValues") // card fields default to empty for older state builders/previews
data class AccountsState(
    val baseCurrency: String,
    val accountsData: ImmutableList<AccountData>,
    val totalBalanceWithExcluded: String,
    val totalBalanceWithExcludedText: String,
    val totalBalanceWithoutExcluded: String,
    val totalBalanceWithoutExcludedText: String,
    val reorderVisible: Boolean,
    val compactAccountsModeEnabled: Boolean,
    val hideTotalBalance: Boolean,
    val creditCards: ImmutableList<CreditCardTileUi> = persistentListOf(),
    /** Total owed on all cards in the base currency (null when there are no cards). */
    val cardsOutstandingTotal: Double? = null,
    val today: LocalDate = LocalDate.now(),
)
