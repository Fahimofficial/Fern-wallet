package com.ivy.domain.creditcard

import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Maps the ordinary transactions of a card account to card movements. This is the same data
 * the account balance is computed from, so `outstanding == -accountBalance` always holds.
 */
object CardLedger {
    fun entries(
        cardAccountId: AccountId,
        transactions: List<Transaction>,
        toLocalDate: (Instant) -> LocalDate,
    ): List<CardLedgerEntry> = transactions
        .distinctBy { it.id }
        .filter { it.settled }
        .mapNotNull { trn -> entry(cardAccountId, trn, toLocalDate) }

    private fun entry(
        card: AccountId,
        trn: Transaction,
        toLocalDate: (Instant) -> LocalDate,
    ): CardLedgerEntry? {
        val amountAndKind: Pair<Double, CardLedgerEntry.Kind>? = when (trn) {
            is Expense -> (trn.value.amount.value to CardLedgerEntry.Kind.CHARGE).takeIf { trn.account == card }
            is Income -> (trn.value.amount.value to CardLedgerEntry.Kind.CREDIT).takeIf { trn.account == card }
            is Transfer -> when {
                // A transfer from the card to itself has no effect on what is owed.
                trn.fromAccount == card && trn.toAccount == card -> null
                trn.toAccount == card -> trn.toValue.amount.value to CardLedgerEntry.Kind.PAYMENT
                trn.fromAccount == card -> trn.fromValue.amount.value to CardLedgerEntry.Kind.CHARGE
                else -> null
            }
        }
        return amountAndKind?.let { (amount, kind) ->
            CardLedgerEntry(
                transactionId = trn.id,
                date = toLocalDate(trn.time),
                amount = BigDecimal.valueOf(amount),
                kind = kind,
            )
        }
    }
}
