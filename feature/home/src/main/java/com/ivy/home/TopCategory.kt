package com.ivy.home

import androidx.compose.runtime.Immutable
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category

/** This month's most frequent expense category, for the Quick-access insight. */
@Immutable
data class TopCategory(val name: String, val count: Int)

/**
 * Counts expenses per category instead of summing them: amounts in different currencies can't
 * be added without conversion, and a count is honest as is. Ties go to the category seen first.
 */
fun topExpenseCategory(history: List<TransactionHistoryItem>, categories: List<Category>): TopCategory? {
    val counts = history.asSequence()
        .filterIsInstance<Transaction>()
        .filter { it.type == TransactionType.EXPENSE }
        .mapNotNull { it.categoryId }
        .groupingBy { it }
        .eachCount()
    return counts.maxByOrNull { it.value }?.let { (id, count) ->
        categories.find { it.id.value == id }?.let { TopCategory(name = it.name.value, count = count) }
    }
}
