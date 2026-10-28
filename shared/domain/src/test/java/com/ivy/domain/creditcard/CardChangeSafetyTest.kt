package com.ivy.domain.creditcard

import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.data.model.creditcard.StatementOverride
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.domain.creditcard.CardLedgerEntry.Kind.CHARGE
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Deleting a card and changing its statement day must not change other data. */
class CardChangeSafetyTest {
    private val cardId = AccountId(UUID(0, 1))
    private val bankId = AccountId(UUID(0, 2))
    private val inr = AssetCode.unsafe("INR")
    private val usd = AssetCode.unsafe("USD")

    private fun transfer(
        from: AccountId,
        to: AccountId,
        fromAmount: Double,
        toAmount: Double,
        settled: Boolean = true,
        title: String? = null,
    ) = Transfer(
        id = TransactionId(UUID.randomUUID()),
        title = title?.let(NotBlankTrimmedString::unsafe),
        description = null,
        category = null,
        time = Instant.parse("2026-02-01T10:00:00Z"),
        settled = settled,
        metadata = TransactionMetadata(recurringRuleId = null, paidForDateTime = null, loanRecordId = null),
        tags = emptyList(),
        fromAccount = from,
        fromValue = PositiveValue(PositiveDouble.unsafe(fromAmount), usd),
        toAccount = to,
        toValue = PositiveValue(PositiveDouble.unsafe(toAmount), inr),
    )

    // region Delete card
    @Test
    fun `a bill payment becomes an expense of the paying account with its own amount`() {
        val payment = transfer(from = bankId, to = cardId, fromAmount = 60.0, toAmount = 5000.0, title = "Bill")
        val kept = DeleteCreditCardUseCase.preservedSides(listOf(payment), emptyList(), null)

        val expense = kept.single().shouldBeInstanceOf<Expense>()
        expense.id shouldBe payment.id
        expense.account shouldBe bankId
        expense.value shouldBe payment.fromValue // what actually left the bank, in its currency
        expense.time shouldBe payment.time
        expense.title?.value shouldBe "Bill"
    }

    @Test
    fun `money moved off the card becomes income of the receiving account`() {
        val out = transfer(from = cardId, to = bankId, fromAmount = 100.0, toAmount = 100.0)
        val kept = DeleteCreditCardUseCase.preservedSides(emptyList(), listOf(out), NotBlankTrimmedString.unsafe("X"))

        val income = kept.single().shouldBeInstanceOf<Income>()
        income.account shouldBe bankId
        income.value shouldBe out.toValue
        income.title?.value shouldBe "X"
    }

    @Test
    fun `planned unpaid transfers are not preserved`() {
        val planned = transfer(from = bankId, to = cardId, fromAmount = 1.0, toAmount = 1.0, settled = false)
        DeleteCreditCardUseCase.preservedSides(listOf(planned), emptyList(), null) shouldHaveSize 0
    }
    // endregion

    // region Statement day change
    private fun card(statementDay: Int) = CreditCard(
        accountId = cardId,
        bankId = null,
        customBankName = "Sample Bank",
        productId = null,
        productName = null,
        network = null,
        lastFour = null,
        creditLimit = null,
        statementDay = statementDay,
        dueDateRule = DueDateRule.DaysAfterStatement(20),
        openingOutstanding = BigDecimal.ZERO,
        openingDate = LocalDate.of(2026, 1, 1),
        status = CardStatus.ACTIVE,
        appearance = CardAppearance.FERN,
        remindersEnabled = true,
        reminderDaysBefore = 3,
    )

    private val janOverride = StatementOverride(
        accountId = cardId,
        nominalClosingDate = LocalDate.of(2026, 1, 15),
        actualClosingDate = null,
        actualDueDate = LocalDate.of(2026, 2, 6),
        issuerStatementBalance = BigDecimal("1234"),
        minimumDue = BigDecimal("100"),
    )

    @Test
    fun `saved statement values survive a statement day change from 15 to 18`() {
        val ledger = listOf(
            CardLedgerEntry(null, LocalDate.parse("2026-01-10"), BigDecimal("1000"), CHARGE),
            CardLedgerEntry(null, LocalDate.parse("2026-02-10"), BigDecimal("500"), CHARGE),
        )
        val today = LocalDate.of(2026, 2, 25)
        val before = CreditCardCalculator.overview(card(15), ledger, listOf(janOverride), today)
            .statements.single { it.closingDate == LocalDate.of(2026, 1, 15) }

        val moved = StatementOverrideRemapper.remap(listOf(janOverride), newStatementDay = 18)
        moved.single().nominalClosingDate shouldBe LocalDate.of(2026, 1, 18)
        val after = CreditCardCalculator.overview(card(18), ledger, moved, today)
            .statements.single { it.issuerStatementBalance != null }

        after.closingDate shouldBe before.closingDate
        after.dueDate shouldBe before.dueDate
        after.issuerStatementBalance!! shouldBeEqualComparingTo BigDecimal("1234")
        after.minimumDue!! shouldBeEqualComparingTo BigDecimal("100")
        after.remainingDue shouldBeEqualComparingTo before.remainingDue
    }

    @Test
    fun `moved statements keep unique keys in order`() {
        val a = janOverride.copy(nominalClosingDate = LocalDate.of(2026, 1, 28))
        val b = janOverride.copy(nominalClosingDate = LocalDate.of(2026, 2, 2), actualClosingDate = null)
        val moved = StatementOverrideRemapper.remap(listOf(b, a), newStatementDay = 1)
        moved.map { it.nominalClosingDate }.distinct() shouldHaveSize 2
        moved.map { it.actualClosingDate } shouldBe listOf(LocalDate.of(2026, 1, 28), LocalDate.of(2026, 2, 2))
    }
    // endregion
}
