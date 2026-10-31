// Screen arguments are saved across process death (see NavigationState). Most screens have
// optional arguments with defaults; that is their public API, so the rule doesn't fit here.
@file:Suppress("DataClassDefaultValues")
@file:UseSerializers(KSerializerUUID::class)

package com.ivy.navigation

import com.ivy.base.kotlinxserilzation.KSerializerUUID
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import java.util.UUID

@Serializable
data object MainScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object OnboardingScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data class CSVScreen(
    val launchedFromOnboarding: Boolean
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data class EditTransactionScreen(
    val initialTransactionId: UUID?,
    val type: TransactionType,
    // extras
    val accountId: UUID? = null,
    val categoryId: UUID? = null
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data class TransactionsScreen(
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    val unspecifiedCategory: Boolean? = false,
    val transactionType: TransactionType? = null,
    val accountIdFilterList: List<UUID> = persistentListOf(),
    // Not saved across process death; see NavigationState.isRestorable.
    @Transient val transactions: List<Transaction> = persistentListOf()
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data class PieChartStatisticScreen(
    val type: TransactionType,
    val filterExcluded: Boolean = true,
    // Not saved across process death; see NavigationState.isRestorable.
    @Transient val accountList: ImmutableList<UUID> = persistentListOf(),
    @Transient val transactions: ImmutableList<Transaction> = persistentListOf(),
    val treatTransfersAsIncomeExpense: Boolean = false
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data class EditPlannedScreen(
    val plannedPaymentRuleId: UUID?,
    val type: TransactionType,
    val amount: Double? = null,
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    val title: String? = null,
    val description: String? = null,
) : Screen {
    override val isLegacy: Boolean
        get() = true

    fun mandatoryFilled(): Boolean {
        return amount != null && amount > 0.0 &&
            accountId != null
    }
}

@Serializable
data object BalanceScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object PlannedPaymentsScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object CategoriesScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object SettingsScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data class ImportScreen(
    val launchedFromOnboarding: Boolean
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object ReportScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object BudgetScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object LoansScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object SearchScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data class LoanDetailsScreen(
    val loanId: UUID
) : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object ExchangeRatesScreen : Screen {
    override val isLegacy: Boolean
        get() = true
}

@Serializable
data object FeaturesScreen : Screen

@Serializable
data object AttributionsScreen : Screen

@Serializable
data object ContributorsScreen : Screen

@Serializable
data object ReleasesScreen : Screen

@Serializable
data object DisclaimerScreen : Screen

/** Credit-card details: outstanding, statements, history, payment and settings. */
@Suppress("DataClassTypedIDs") // screens take raw UUIDs like the existing ones
@Serializable
data class CreditCardDetailsScreen(val accountId: UUID) : Screen

/** Add (accountId == null) or edit a credit card. */
@Suppress("DataClassTypedIDs") // screens take raw UUIDs like the existing ones
@Serializable
data class EditCreditCardScreen(val accountId: UUID?) : Screen

/** Record a bill payment (transfer from another account into the card). */
@Suppress("DataClassTypedIDs") // screens take raw UUIDs like the existing ones
@Serializable
data class PayCreditCardScreen(val accountId: UUID) : Screen

/** One closed statement, identified by its nominal closing date (epoch day). */
@Suppress("DataClassTypedIDs") // screens take raw UUIDs like the existing ones
@Serializable
data class CreditCardStatementScreen(val accountId: UUID, val nominalClosingEpochDay: Long) : Screen
