package com.ivy.wallet.domain.pure.account

import com.ivy.data.model.AccountType
import com.ivy.legacy.datamodel.Account

/**
 * Accounts whose transactions count towards income/expense statistics.
 * "Exclude from balance" on a credit card only removes its debt from the total balance:
 * purchases made with the card are still real spending, so cards are always kept here.
 */
fun filterExcluded(accounts: List<Account>): List<Account> =
    accounts.filter { it.includeInBalance || it.type == AccountType.CREDIT_CARD }

fun accountCurrency(account: Account, baseCurrency: String): String =
    account.currency ?: baseCurrency
