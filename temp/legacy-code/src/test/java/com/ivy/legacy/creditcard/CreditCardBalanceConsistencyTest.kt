package com.ivy.legacy.creditcard

import arrow.core.nonEmptyListOf
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.Transaction
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.data.temp.migration.getAccountId
import com.ivy.domain.creditcard.CardLedger
import com.ivy.domain.creditcard.CreditCardCalculator
import com.ivy.legacy.domain.pure.transaction.AccountValueFunctions
import com.ivy.wallet.domain.pure.transaction.foldTransactions
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * The card screens (CreditCardCalculator) and the rest of the app (account balance via
 * AccountValueFunctions, used by Home, Accounts, widgets) must agree:
 * outstanding == -(account balance + opening outstanding adjustment).
 *
 * Also checks that ordinary accounts are unaffected: a bank account's balance from the same
 * ledger is the classic income - expense + transfers in - transfers out.
 */
class CreditCardBalanceConsistencyTest {
    private val card = AccountId(UUID(9, 1))
    private val bank = AccountId(UUID(9, 2))
    private val meta = TransactionMetadata(recurringRuleId = null, paidForDateTime = null, loanRecordId = null)
    private val start = Instant.parse("2026-01-01T00:00:00Z")

    private fun v(cents: Int) = PositiveValue(PositiveDouble.unsafe(cents / 100.0), AssetCode.unsafe("INR"))

    private fun trnArb(): Arb<(Int) -> Transaction> = Arb.choice(
        Arb.int(1..500_000).map { c -> { i: Int -> expense(i, card, c) } },
        Arb.int(1..500_000).map { c -> { i: Int -> income(i, card, c) } },
        Arb.int(1..500_000).map { c -> { i: Int -> transfer(i, bank, card, c) } },
        Arb.int(1..500_000).map { c -> { i: Int -> transfer(i, card, bank, c) } },
        Arb.int(1..500_000).map { c -> { i: Int -> expense(i, bank, c) } },
        Arb.constant { i: Int -> transfer(i, card, card, 100) },
    )

    @Test
    fun `card outstanding always equals the negated account balance`() = runTest {
        checkAll(200, Arb.list(trnArb(), 0..40), Arb.int(-100_000..100_000)) { builders, openingCents ->
            val transactions = builders.mapIndexed { i, build -> build(i) }
            val opening = BigDecimal(openingCents).movePointLeft(2)

            // Same pre-filter as AccTrnsAct: the card's own transactions + transfers into it.
            val cardTransactions = transactions.filter { trn ->
                trn.getAccountId() == card.value || (trn is Transfer && trn.toAccount == card)
            }
            val accountBalance = foldTransactions(
                transactions = cardTransactions,
                arg = card.value,
                valueFunctions = nonEmptyListOf(AccountValueFunctions::balance),
            ).head - opening // CalcAccBalanceAct subtracts the opening outstanding

            val overview = CreditCardCalculator.overview(
                card = creditCard(opening),
                ledger = CardLedger.entries(card, transactions) { it.atZone(ZoneOffset.UTC).toLocalDate() },
                overrides = emptyList(),
                today = LocalDate.parse("2027-01-01"),
            )

            overview.outstanding shouldBeEqualComparingTo accountBalance.negate()
        }
    }

    @Test
    fun `ordinary account balance is unchanged`() {
        val transactions = listOf(
            income(1, bank, 100_000), // +1000
            expense(2, bank, 25_050), // -250.50
            transfer(3, bank, card, 40_000), // card payment -400
            transfer(4, card, bank, 10_000), // +100
        )

        val balance = foldTransactions(
            transactions = transactions.filter { trn ->
                trn.getAccountId() == bank.value || (trn is Transfer && trn.toAccount == bank)
            },
            arg = bank.value,
            valueFunctions = nonEmptyListOf(AccountValueFunctions::balance),
        ).head

        balance shouldBeEqualComparingTo BigDecimal("449.50")
    }

    private fun time(i: Int) = start.plusSeconds(i * 3_600L * 7)
    private fun id(i: Int) = TransactionId(UUID(3, i.toLong()))

    private fun expense(i: Int, account: AccountId, cents: Int) = Expense(
        id = id(i), title = null, description = null, category = null, time = time(i), settled = true,
        metadata = meta, tags = emptyList(), value = v(cents), account = account,
    )

    private fun income(i: Int, account: AccountId, cents: Int) = Income(
        id = id(i), title = null, description = null, category = null, time = time(i), settled = true,
        metadata = meta, tags = emptyList(), value = v(cents), account = account,
    )

    private fun transfer(i: Int, from: AccountId, to: AccountId, cents: Int) = Transfer(
        id = id(i), title = null, description = null, category = null, time = time(i), settled = true,
        metadata = meta, tags = emptyList(), fromAccount = from, fromValue = v(cents),
        toAccount = to, toValue = v(cents),
    )

    private fun creditCard(opening: BigDecimal) = CreditCard(
        accountId = card, bankId = null, customBankName = "Test Bank", productId = null,
        productName = null, network = null, lastFour = null, creditLimit = BigDecimal("100000"),
        statementDay = 15, dueDateRule = null, openingOutstanding = opening,
        openingDate = LocalDate.parse("2025-12-01"), status = CardStatus.ACTIVE,
        appearance = CardAppearance.FERN, remindersEnabled = false, reminderDaysBefore = 0,
    )
}
