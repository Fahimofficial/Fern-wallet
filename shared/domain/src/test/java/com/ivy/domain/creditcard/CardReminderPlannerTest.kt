package com.ivy.domain.creditcard

import com.ivy.data.model.AccountId
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.domain.creditcard.CardLedgerEntry.Kind.CHARGE
import com.ivy.domain.creditcard.CardLedgerEntry.Kind.PAYMENT
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Statement closes on the 15th and is due 20 days later: the Jan 15 statement is due Feb 4.
 * Reminders start 3 days before (Feb 1).
 */
class CardReminderPlannerTest {

    private fun card(
        remindersEnabled: Boolean = true,
        daysBefore: Int = 3,
        status: CardStatus = CardStatus.ACTIVE,
    ) = CreditCard(
        accountId = AccountId(UUID(0, 7)),
        bankId = null,
        customBankName = "Sample Bank",
        productId = null,
        productName = null,
        network = null,
        lastFour = null,
        creditLimit = BigDecimal("50000"),
        statementDay = 15,
        dueDateRule = DueDateRule.DaysAfterStatement(20),
        openingOutstanding = BigDecimal.ZERO,
        openingDate = LocalDate.of(2026, 1, 1),
        status = status,
        appearance = CardAppearance.FERN,
        remindersEnabled = remindersEnabled,
        reminderDaysBefore = daysBefore,
    )

    private fun e(date: String, amount: String, kind: CardLedgerEntry.Kind) = CardLedgerEntry(
        transactionId = null,
        date = LocalDate.parse(date),
        amount = BigDecimal(amount),
        kind = kind,
    )

    private val unpaid = listOf(e("2026-01-10", "1000", CHARGE))

    private fun reminder(
        today: String,
        card: CreditCard = card(),
        ledger: List<CardLedgerEntry> = unpaid,
    ): CardReminder? {
        val date = LocalDate.parse(today)
        val overview = CreditCardCalculator.overview(card, ledger, emptyList(), date)
        return CardReminderPlanner.reminderFor(card, overview, date)
    }

    @Test
    fun `no reminder before the reminder window`() {
        reminder("2026-01-31") shouldBe null
    }

    @Test
    fun `before-due reminder inside the window`() {
        val r = reminder("2026-02-01")
        r?.kind shouldBe CardReminder.Kind.BEFORE_DUE
        r?.dueDate shouldBe LocalDate.of(2026, 2, 4)
        r?.remainingDue?.compareTo(BigDecimal("1000")) shouldBe 0
    }

    @Test
    fun `due-today reminder on the due date`() {
        reminder("2026-02-04")?.kind shouldBe CardReminder.Kind.DUE_TODAY
    }

    @Test
    fun `due-today reminder still fires when days-before is zero`() {
        reminder("2026-02-04", card = card(daysBefore = 0))?.kind shouldBe CardReminder.Kind.DUE_TODAY
        reminder("2026-02-03", card = card(daysBefore = 0)) shouldBe null
    }

    @Test
    fun `no reminder once the statement is paid, and old ones are dismissed`() {
        val paid = unpaid + e("2026-01-20", "1000", PAYMENT)
        reminder("2026-02-02", ledger = paid) shouldBe null
        val c = card()
        val overview = CreditCardCalculator.overview(c, paid, emptyList(), LocalDate.of(2026, 2, 2))
        CardReminderPlanner.shouldDismiss(c, overview) shouldBe true
    }

    @Test
    fun `no reminder when reminders are off or the card is closed`() {
        reminder("2026-02-02", card = card(remindersEnabled = false)) shouldBe null
        reminder("2026-02-02", card = card(status = CardStatus.CLOSED)) shouldBe null
    }

    @Test
    fun `dedup key is stable per card, due date and kind`() {
        val r = reminder("2026-02-01")!!
        r.dedupKey("card") shouldBe "cc_reminder_card_2026-02-04_BEFORE_DUE"
    }
}
