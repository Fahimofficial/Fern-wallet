package com.ivy.home

import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

class TopCategoryTest {
    private val food = category("Food")
    private val rent = category("Rent")

    @Test
    fun `most frequent expense category wins, income and uncategorised ignored`() {
        val history = listOf(
            trn(TransactionType.EXPENSE, food),
            trn(TransactionType.EXPENSE, food),
            trn(TransactionType.EXPENSE, rent),
            trn(TransactionType.INCOME, rent),
            trn(TransactionType.INCOME, rent),
            trn(TransactionType.EXPENSE, null),
        )
        topExpenseCategory(history, listOf(food, rent)) shouldBe TopCategory("Food", 2)
    }

    @Test
    fun `no categorised expenses gives no insight`() {
        topExpenseCategory(listOf(trn(TransactionType.EXPENSE, null)), listOf(food)).shouldBeNull()
    }

    private fun category(name: String) = Category(
        name = NotBlankTrimmedString.unsafe(name),
        color = ColorInt(0),
        icon = null,
        id = CategoryId(UUID.randomUUID()),
        orderNum = 0.0,
    )

    private fun trn(type: TransactionType, category: Category?) = Transaction(
        accountId = UUID.randomUUID(),
        type = type,
        amount = BigDecimal.TEN,
        categoryId = category?.id?.value,
    )
}
