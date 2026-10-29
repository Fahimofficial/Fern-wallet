package com.ivy.domain.creditcard

import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import com.ivy.domain.creditcard.CardLedgerEntry.Kind
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class CardLedgerTest {
    private val card = AccountId(UUID(1, 1))
    private val bank = AccountId(UUID(1, 2))
    private val time = Instant.parse("2026-01-10T10:00:00Z")
    private val meta = TransactionMetadata(recurringRuleId = null, paidForDateTime = null, loanRecordId = null)
    private fun v(amount: Double, asset: String = "INR") =
        PositiveValue(PositiveDouble.unsafe(amount), AssetCode.unsafe(asset))
    private fun id(n: Long) = TransactionId(UUID(2, n))

    private fun expense(n: Long, account: AccountId, amount: Double, settled: Boolean = true) = Expense(
        id = id(n), title = null, description = null, category = null, time = time, settled = settled,
        metadata = meta, tags = emptyList(), value = v(amount), account = account,
    )

    private fun income(n: Long, account: AccountId, amount: Double) = Income(
        id = id(n), title = null, description = null, category = null, time = time, settled = true,
        metadata = meta, tags = emptyList(), value = v(amount), account = account,
    )

    private fun transfer(n: Long, from: AccountId, to: AccountId, fromAmount: Double, toAmount: Double) = Transfer(
        id = id(n), title = null, description = null, category = null, time = time, settled = true,
        metadata = meta, tags = emptyList(), fromAccount = from, fromValue = v(fromAmount, "USD"),
        toAccount = to, toValue = v(toAmount),
    )

    @Test
    fun `maps card transactions to ledger kinds`() {
        val transactions = listOf(
            expense(1, card, 1000.0), // purchase
            income(2, card, 200.0), // refund
            transfer(3, bank, card, fromAmount = 6.0, toAmount = 500.0), // payment in card currency
            transfer(4, card, bank, fromAmount = 300.0, toAmount = 3.5), // cash out of the card
            transfer(5, card, card, fromAmount = 1.0, toAmount = 1.0), // no-op
            expense(6, bank, 50.0), // other account
            expense(7, card, 70.0, settled = false), // planned, not happened yet
            expense(1, card, 1000.0), // duplicate row from two queries
        )

        val entries = CardLedger.entries(card, transactions) { it.atZone(ZoneOffset.UTC).toLocalDate() }

        entries.map { it.kind to it.amount } shouldBe listOf(
            Kind.CHARGE to BigDecimal("1000.0"),
            Kind.CREDIT to BigDecimal("200.0"),
            Kind.PAYMENT to BigDecimal("500.0"),
            Kind.CHARGE to BigDecimal("300.0"),
        )
        entries.first().date shouldBe LocalDate.parse("2026-01-10")
    }

    @Test
    fun `local date uses the provided time zone`() {
        val late = expense(1, card, 10.0).copy(time = Instant.parse("2026-01-31T20:00:00Z"))
        val ist = java.time.ZoneId.of("Asia/Kolkata")

        val entry = CardLedger.entries(card, listOf(late)) { it.atZone(ist).toLocalDate() }.single()

        entry.date shouldBe LocalDate.parse("2026-02-01") // 01:30 IST on Feb 1
    }
}
