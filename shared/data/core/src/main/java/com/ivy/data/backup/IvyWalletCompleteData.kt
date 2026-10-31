package com.ivy.data.backup

import androidx.annotation.Keep
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.BudgetEntity
import com.ivy.data.db.entity.CategoryEntity
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.db.entity.CreditCardStatementEntity
import com.ivy.data.db.entity.LoanEntity
import com.ivy.data.db.entity.LoanRecordEntity
import com.ivy.data.db.entity.PlannedPaymentRuleEntity
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.db.entity.TagAssociationEntity
import com.ivy.data.db.entity.TagEntity
import com.ivy.data.db.entity.TransactionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Suppress("DataClassDefaultValues")
data class IvyWalletCompleteData(
    /**
     * Backup format. Missing in backups made before it was added (= [LEGACY_FORMAT_VERSION]).
     * Bump [CURRENT_FORMAT_VERSION] only for changes older app versions can't read correctly;
     * they refuse newer formats instead of importing them partially.
     */
    @SerialName("formatVersion")
    val formatVersion: Int = LEGACY_FORMAT_VERSION,
    @SerialName("accounts")
    val accounts: List<AccountEntity> = emptyList(),
    @SerialName("budgets")
    val budgets: List<BudgetEntity> = emptyList(),
    @SerialName("categories")
    val categories: List<CategoryEntity> = emptyList(),
    @SerialName("loanRecords")
    val loanRecords: List<LoanRecordEntity> = emptyList(),
    @SerialName("loans")
    val loans: List<LoanEntity> = emptyList(),
    @SerialName("plannedPaymentRules")
    val plannedPaymentRules: List<PlannedPaymentRuleEntity> = emptyList(),
    @SerialName("settings")
    val settings: List<SettingsEntity> = emptyList(),
    @SerialName("transactions")
    val transactions: List<TransactionEntity> = emptyList(),
    @SerialName("sharedPrefs")
    val sharedPrefs: HashMap<String, String> = HashMap(),
    @SerialName("tags")
    val tags: List<TagEntity> = emptyList(),
    @SerialName("tagAssociations")
    val tagAssociations: List<TagAssociationEntity> = emptyList(),
    // Added with credit cards (DB v131). Older backups simply don't have these keys.
    @SerialName("creditCards")
    val creditCards: List<CreditCardEntity> = emptyList(),
    @SerialName("creditCardStatements")
    val creditCardStatements: List<CreditCardStatementEntity> = emptyList(),
) {
    companion object {
        const val LEGACY_FORMAT_VERSION = 1
        const val CURRENT_FORMAT_VERSION = 2
    }
}
