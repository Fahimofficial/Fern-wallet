package com.ivy.data.repository.mapper

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountType
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.CurrencyRepository
import javax.inject.Inject

class AccountMapper @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    suspend fun AccountEntity.toDomain(): Either<String, Account> = either {
        ensure(!isDeleted) { "Account is deleted" }

        Account(
            id = AccountId(id),
            name = NotBlankTrimmedString.from(name).bind(),
            asset = currency?.let(AssetCode::from)?.getOrNull()
                ?: currencyRepository.getBaseCurrency(),
            color = ColorInt(color),
            icon = icon?.let(IconAsset::from)?.getOrNull(),
            includeInBalance = includeInBalance,
            orderNum = orderNum,
            type = accountTypeFromEntity(type),
        )
    }

    fun Account.toEntity(): AccountEntity {
        return AccountEntity(
            name = name.value,
            currency = asset.code,
            color = color.value,
            icon = icon?.id,
            orderNum = orderNum,
            includeInBalance = includeInBalance,
            id = id.value,
            type = type.name,
            isSynced = true, // TODO: Delete this
        )
    }
}

/** Unknown values (e.g. from a newer app version) fall back to an ordinary asset account. */
fun accountTypeFromEntity(value: String?): AccountType =
    AccountType.entries.firstOrNull { it.name == value } ?: AccountType.ASSET
