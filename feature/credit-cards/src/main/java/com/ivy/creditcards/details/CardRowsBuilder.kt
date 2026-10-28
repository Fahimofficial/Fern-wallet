package com.ivy.creditcards.details

import com.ivy.base.time.TimeConverter
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transfer
import com.ivy.data.repository.CategoryRepository
import com.ivy.domain.creditcard.CreditCardWithOverview
import java.math.BigDecimal
import javax.inject.Inject

/** Turns a card's transactions into display rows (same classification as CardLedger). */
class CardRowsBuilder @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val timeConverter: TimeConverter,
) {
    suspend fun rows(item: CreditCardWithOverview): List<CardTransactionRow> {
        val categories = categoryRepository.findAll().associateBy({ it.id }, { it.name.value })
        val cardId = item.account.id
        return item.transactions.filter { it.settled }.mapNotNull { trn ->
            val (amount, kind) = when (trn) {
                is Expense -> trn.value.amount.value to RowKind.CHARGE
                is Income -> trn.value.amount.value to RowKind.CREDIT
                is Transfer -> when {
                    trn.toAccount == cardId && trn.fromAccount != cardId ->
                        trn.toValue.amount.value to RowKind.PAYMENT
                    trn.fromAccount == cardId && trn.toAccount != cardId ->
                        trn.fromValue.amount.value to RowKind.TRANSFER_OUT
                    else -> return@mapNotNull null
                }
            }
            val category = trn.category?.let(categories::get)
            CardTransactionRow(
                id = trn.id.value,
                title = trn.title?.value ?: category.orEmpty(),
                category = category,
                date = with(timeConverter) { trn.time.toLocalDate() },
                amount = BigDecimal.valueOf(amount),
                kind = kind,
            )
        }
    }
}
