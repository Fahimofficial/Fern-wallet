package com.ivy.data.model

import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.sync.Identifiable
import com.ivy.data.model.sync.UniqueId
import java.util.UUID

@JvmInline
value class AccountId(override val value: UUID) : UniqueId

@Suppress("DataClassDefaultValues") // type defaults to ASSET so existing accounts stay unchanged
data class Account(
    override val id: AccountId,
    val name: NotBlankTrimmedString,
    val asset: AssetCode,
    val color: ColorInt,
    val icon: IconAsset?,
    val includeInBalance: Boolean,
    override val orderNum: Double,
    /** ASSET for cash/bank accounts; CREDIT_CARD for liability (card) accounts. */
    val type: AccountType = AccountType.ASSET,
) : Identifiable<AccountId>, Reorderable

/**
 * Kind of account.
 *
 * The balance of every account is derived from the same transaction ledger:
 * income and transfers in increase it, expenses and transfers out decrease it.
 * For a [CREDIT_CARD] the balance is therefore the negated outstanding amount
 * (balance -1,000 means 1,000 is owed on the card).
 */
enum class AccountType {
    ASSET,
    CREDIT_CARD,
}
