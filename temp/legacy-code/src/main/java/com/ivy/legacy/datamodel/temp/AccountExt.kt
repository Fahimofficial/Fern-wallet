package com.ivy.legacy.datamodel.temp

import com.ivy.data.repository.mapper.appearance
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.repository.mapper.accountTypeFromEntity
import com.ivy.legacy.datamodel.Account

fun AccountEntity.toLegacyDomain(): Account = Account(
    name = name,
    currency = currency,
    color = color,
    icon = icon,
    orderNum = orderNum,
    includeInBalance = includeInBalance,
    type = accountTypeFromEntity(type),
    appearance = appearance(),
    isSynced = isSynced,
    isDeleted = isDeleted,
    id = id
)
