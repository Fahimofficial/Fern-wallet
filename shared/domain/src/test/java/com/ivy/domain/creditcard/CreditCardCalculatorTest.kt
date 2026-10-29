package com.ivy.domain.creditcard

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.data.model.AccountId
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.data.model.creditcard.StatementOverride
import com.ivy.domain.creditcard.CardLedgerEntry.Kind.CHARGE
import com.ivy.domain.creditcard.CardLedgerEntry.Kind.CREDIT
import com.ivy.domain.creditcard.CardLedgerEntry.Kind.PAYMENT
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RunWith(TestParameterInjector::class)
class CreditCardCalculatorTest {

    private val accountId = AccountId(UUID(0, 1))

    private fun card(
        limit: String? = "50000",
        statementDay: Int? = 15,
        dueRule: DueDateRule? = DueDateRule.DaysAfterStatement(20),
        opening: String = "0",
        openingDate: LocalDate = LocalDate.of(2026, 1, 1),
    ) = CreditCard(
        accountId = accountId,
        bankId = "hdfc-bank",
        customBankName = null,
        productId = null,
        productName = "Test card",
        network = null,
        lastFour = "1234",
        creditLimit = limit?.let(::BigDecimal),
        statementDay = statementDay,
        dueDateRule = dueRule,
        openingOutstanding = BigDecimal(opening),
        openingDate = openingDate,
        status = CardStatus.ACTIVE,
        appearance = CardAppearance.FERN,
        remindersEnabled = true,
        reminderDaysBefore = 3,
    )

    private fun e(date: String, amount: String, kind: CardLedgerEntry.Kind) = CardLedgerEntry(
        transactionId = null,
        date = LocalDate.parse(date),
        amount = BigDecimal(amount),
        kind = kind,
    )

    private fun calc(
        card: CreditCard = card(),
        ledger: List<CardLedgerEntry>,
        today: String,
        overrides: List<StatementOverride> = emptyList(),
    ) = CreditCardCalculator.overview(card, ledger, overrides, LocalDate.parse(today))

    // region Dates
    enum class ClosingCase(val month: String, val day: Int, val expected: String) {
        DAY_28_FEB(month = "2026-02", day = 28, expected = "2026-02-28"),
        DAY_29_FEB(month = "2026-02", day = 29, expected = "2026-02-28"),
        DAY_29_LEAP_FEB(month = "2028-02", day = 29, expected = "2028-02-29"),
        DAY_30_FEB(month = "2026-02", day = 30, expected = "2026-02-28"),
        DAY_31_FEB(month = "2026-02", day = 31, expected = "2026-02-28"),
        DAY_31_LEAP_FEB(month = "2028-02", day = 31, expected = "2028-02-29"),
        DAY_31_APR(month = "2026-04", day = 31, expected = "2026-04-30"),
        DAY_31_MAY(month = "2026-05", day = 31, expected = "2026-05-31"),
        DAY_30_JAN(month = "2026-01", day = 30, expected = "2026-01-30"),
    }

    @Test
    fun `closing day is clamped to short months`(@TestParameter case: ClosingCase) {
        CreditCardCalculator.nominalClosingDate(YearMonth.parse(case.month), case.day) shouldBe
            LocalDate.parse(case.expected)
    }

    enum class DueCase(val closing: String, val rule: DueDateRule, val expected: String) {
        DAYS_AFTER(closing = "2026-01-15", rule = DueDateRule.DaysAfterStatement(20), expected = "2026-02-04"),
        DAYS_AFTER_ACROSS_LEAP_DAY(
            closing = "2028-02-15",
            rule = DueDateRule.DaysAfterStatement(15),
            expected = "2028-03-01"
        ),
        DAY_OF_MONTH_LATER_SAME_MONTH(
            closing = "2026-01-05",
            rule = DueDateRule.DayOfMonth(25),
            expected = "2026-01-25"
        ),
        DAY_OF_MONTH_NEXT_MONTH(closing = "2026-01-20", rule = DueDateRule.DayOfMonth(5), expected = "2026-02-05"),
        DAY_OF_MONTH_SAME_DAY_GOES_NEXT(
            closing = "2026-01-05",
            rule = DueDateRule.DayOfMonth(5),
            expected = "2026-02-05"
        ),
        DAY_31_INTO_FEB(closing = "2026-01-31", rule = DueDateRule.DayOfMonth(31), expected = "2026-02-28"),
    }

    @Test
    fun `due date rules`(@TestParameter case: DueCase) {
        CreditCardCalculator.dueDate(LocalDate.parse(case.closing), case.rule) shouldBe
            LocalDate.parse(case.expected)
    }
    // endregion

    @Test
    fun `purchase increases debt and reduces available credit`() {
        val result = calc(ledger = listOf(e("2026-01-05", "1200.50", CHARGE)), today = "2026-01-10")

        result.outstanding shouldBeEqualComparingTo BigDecimal("1200.50")
        result.availableCredit!! shouldBeEqualComparingTo BigDecimal("48799.50")
        result.utilisation!! shouldBeEqualComparingTo BigDecimal("0.0240")
        result.unbilledCharges shouldBeEqualComparingTo BigDecimal("1200.50")
        result.statements shouldHaveSize 0
    }

    @Test
    fun `payment reduces debt and is never a charge`() {
        val result = calc(
            ledger = listOf(e("2026-01-05", "1000", CHARGE), e("2026-01-08", "400", PAYMENT)),
            today = "2026-01-10",
        )

        result.outstanding shouldBeEqualComparingTo BigDecimal("600")
        result.unbilledCharges shouldBeEqualComparingTo BigDecimal("1000")
    }

    @Test
    fun `statement contains only its period and transaction on closing date belongs to it`() {
        val result = calc(
            ledger = listOf(
                e("2026-01-15", "100", CHARGE), // on closing date -> Jan statement
                e("2026-01-16", "50", CHARGE), // next cycle
            ),
            today = "2026-01-20",
        )

        val jan = result.latestStatement!!
        jan.closingDate shouldBe LocalDate.parse("2026-01-15")
        jan.charges shouldBeEqualComparingTo BigDecimal("100")
        jan.statementBalance shouldBeEqualComparingTo BigDecimal("100")
        jan.dueDate shouldBe LocalDate.parse("2026-02-04")
        result.unbilledCharges shouldBeEqualComparingTo BigDecimal("50")
        result.outstanding shouldBeEqualComparingTo BigDecimal("150")
        result.currentCycle shouldBe CycleWindow(LocalDate.parse("2026-01-16"), LocalDate.parse("2026-02-15"))
    }

    @Test
    fun `statement is still open on its closing date`() {
        val result = calc(ledger = listOf(e("2026-01-10", "100", CHARGE)), today = "2026-01-15")

        result.statements shouldHaveSize 0
        result.currentCycle!!.closingDate shouldBe LocalDate.parse("2026-01-15")
    }

    @Test
    fun `payment after closing reduces remaining due but not the statement balance`() {
        val ledger = listOf(e("2026-01-10", "1000", CHARGE), e("2026-01-25", "300", PAYMENT))
        val result = calc(ledger = ledger, today = "2026-01-26")

        val jan = result.latestStatement!!
        jan.statementBalance shouldBeEqualComparingTo BigDecimal("1000")
        jan.paidSinceClosing shouldBeEqualComparingTo BigDecimal("300")
        jan.remainingDue shouldBeEqualComparingTo BigDecimal("700")
        jan.status shouldBe StatementStatus.PARTIALLY_PAID
        result.outstanding shouldBeEqualComparingTo BigDecimal("700")
    }

    @Test
    fun `payment before closing reduces that statement`() {
        val ledger = listOf(e("2026-01-05", "1000", CHARGE), e("2026-01-12", "1000", PAYMENT))
        val jan = calc(ledger = ledger, today = "2026-01-20").latestStatement!!

        jan.payments shouldBeEqualComparingTo BigDecimal("1000")
        jan.statementBalance shouldBeEqualComparingTo BigDecimal("0")
        jan.status shouldBe StatementStatus.NOTHING_DUE
        jan.remainingDue shouldBeEqualComparingTo BigDecimal("0")
    }

    @Test
    fun `full payment marks the statement paid`() {
        val ledger = listOf(e("2026-01-10", "1000", CHARGE), e("2026-02-01", "1000", PAYMENT))
        val jan = calc(ledger = ledger, today = "2026-02-02").latestStatement!!

        jan.status shouldBe StatementStatus.PAID
        jan.remainingDue shouldBeEqualComparingTo BigDecimal("0")
    }

    enum class StatusCase(val today: String, val paid: String?, val expected: StatementStatus) {
        BEFORE_DUE(today = "2026-02-01", paid = null, expected = StatementStatus.UPCOMING),
        ON_DUE_DATE(today = "2026-02-04", paid = null, expected = StatementStatus.DUE_TODAY),
        AFTER_DUE(today = "2026-02-05", paid = null, expected = StatementStatus.OVERDUE),
        PARTIAL_AFTER_DUE(today = "2026-02-05", paid = "100", expected = StatementStatus.OVERDUE),
        PARTIAL_BEFORE_DUE(today = "2026-02-01", paid = "100", expected = StatementStatus.PARTIALLY_PAID),
        LATE_FULL_PAYMENT(today = "2026-02-07", paid = "1000", expected = StatementStatus.PAID),
    }

    @Test
    fun `payment status around the due date`(@TestParameter case: StatusCase) {
        val ledger = listOfNotNull(
            e("2026-01-10", "1000", CHARGE),
            case.paid?.let { e("2026-01-20", it, PAYMENT) },
        )
        // Nothing else happens until today (today stays in the cycle that closes Feb 15)
        val result = calc(ledger = ledger, today = case.today)

        result.latestStatement!!.status shouldBe case.expected
    }

    @Test
    fun `refunds fees and interest`() {
        val ledger = listOf(
            e("2026-01-02", "5000", CHARGE), // purchase
            e("2026-01-04", "1500", CREDIT), // refund
            e("2026-01-06", "499", CHARGE), // annual fee
            e("2026-01-14", "120.75", CHARGE), // interest
            e("2026-01-14", "50", CREDIT), // cashback
        )
        val jan = calc(ledger = ledger, today = "2026-01-20").latestStatement!!

        jan.charges shouldBeEqualComparingTo BigDecimal("5619.75")
        jan.credits shouldBeEqualComparingTo BigDecimal("1550")
        jan.statementBalance shouldBeEqualComparingTo BigDecimal("4069.75")
    }

    @Test
    fun `multiple cycles carry the unpaid balance`() {
        val ledger = listOf(
            e("2026-01-10", "1000", CHARGE),
            e("2026-01-25", "200", PAYMENT),
            e("2026-02-10", "500", CHARGE),
        )
        val result = calc(ledger = ledger, today = "2026-02-20")

        result.statements shouldHaveSize 2
        val (feb, jan) = result.statements
        jan.statementBalance shouldBeEqualComparingTo BigDecimal("1000")
        jan.status shouldBe StatementStatus.CARRIED_FORWARD
        feb.previousBalance shouldBeEqualComparingTo BigDecimal("1000")
        feb.payments shouldBeEqualComparingTo BigDecimal("200")
        feb.charges shouldBeEqualComparingTo BigDecimal("500")
        feb.statementBalance shouldBeEqualComparingTo BigDecimal("1300")
        feb.periodStart shouldBe LocalDate.parse("2026-01-16")
        feb.isLatest shouldBe true
    }

    @Test
    fun `statement day 31 cycles through short months`() {
        val ledger = listOf(
            e("2026-01-31", "100", CHARGE), // Jan 31 statement
            e("2026-02-28", "200", CHARGE), // Feb 28 statement (closing clamped)
            e("2026-03-01", "300", CHARGE), // Mar 31 statement
        )
        val result = calc(card = card(statementDay = 31), ledger = ledger, today = "2026-04-05")

        result.statements.map { it.closingDate to it.charges.toPlainString() } shouldBe listOf(
            LocalDate.parse("2026-03-31") to "300",
            LocalDate.parse("2026-02-28") to "200",
            LocalDate.parse("2026-01-31") to "100",
        )
        result.statements.first().periodStart shouldBe LocalDate.parse("2026-03-01")
    }

    @Test
    fun `leap year closing on day 29`() {
        val ledger = listOf(e("2028-02-29", "100", CHARGE), e("2028-03-01", "50", CHARGE))
        val result = calc(
            card = card(statementDay = 29, openingDate = LocalDate.parse("2028-02-01")),
            ledger = ledger,
            today = "2028-03-02",
        )

        result.latestStatement!!.closingDate shouldBe LocalDate.parse("2028-02-29")
        result.latestStatement!!.charges shouldBeEqualComparingTo BigDecimal("100")
        result.unbilledCharges shouldBeEqualComparingTo BigDecimal("50")
    }

    @Test
    fun `overpayment creates a credit balance`() {
        val ledger = listOf(e("2026-01-05", "1000", CHARGE), e("2026-01-08", "1500", PAYMENT))
        val result = calc(ledger = ledger, today = "2026-01-10")

        result.outstanding shouldBeEqualComparingTo BigDecimal("-500")
        result.creditBalance shouldBeEqualComparingTo BigDecimal("500")
        result.availableCredit!! shouldBeEqualComparingTo BigDecimal("50500")
        result.utilisation!! shouldBeEqualComparingTo BigDecimal("0")
    }

    @Test
    fun `over limit gives negative available credit`() {
        val result = calc(
            card = card(limit = "1000"),
            ledger = listOf(e("2026-01-05", "1200", CHARGE)),
            today = "2026-01-10",
        )

        result.availableCredit!! shouldBeEqualComparingTo BigDecimal("-200")
        result.utilisation!! shouldBeEqualComparingTo BigDecimal("1.2000")
    }

    @Test
    fun `opening outstanding counts once and is part of the first statement`() {
        val result = calc(
            card = card(opening = "2500", openingDate = LocalDate.parse("2026-01-03")),
            ledger = listOf(e("2026-01-10", "100", CHARGE)),
            today = "2026-01-20",
        )

        result.outstanding shouldBeEqualComparingTo BigDecimal("2600")
        val jan = result.latestStatement!!
        jan.openingBalance shouldBeEqualComparingTo BigDecimal("2500")
        jan.charges shouldBeEqualComparingTo BigDecimal("100")
        jan.statementBalance shouldBeEqualComparingTo BigDecimal("2600")
    }

    @Test
    fun `opening credit balance`() {
        val result = calc(card = card(opening = "-300"), ledger = emptyList(), today = "2026-01-10")

        result.outstanding shouldBeEqualComparingTo BigDecimal("-300")
        result.creditBalance shouldBeEqualComparingTo BigDecimal("300")
    }

    @Test
    fun `issuer figures take precedence and are marked`() {
        val override = StatementOverride(
            accountId = accountId,
            nominalClosingDate = LocalDate.parse("2026-01-15"),
            actualClosingDate = LocalDate.parse("2026-01-17"),
            actualDueDate = LocalDate.parse("2026-02-06"),
            issuerStatementBalance = BigDecimal("1100"),
            minimumDue = BigDecimal("200"),
        )
        val ledger = listOf(
            e("2026-01-10", "1000", CHARGE),
            e("2026-01-17", "50", CHARGE), // inside the extended period
            e("2026-01-20", "150", PAYMENT),
        )
        val jan = calc(ledger = ledger, today = "2026-01-21", overrides = listOf(override)).latestStatement!!

        jan.closingDate shouldBe LocalDate.parse("2026-01-17")
        jan.closingDateFromIssuer shouldBe true
        jan.dueDate shouldBe LocalDate.parse("2026-02-06")
        jan.dueDateFromIssuer shouldBe true
        jan.statementBalance shouldBeEqualComparingTo BigDecimal("1050") // calculated
        jan.issuerStatementBalance!! shouldBeEqualComparingTo BigDecimal("1100")
        jan.amountDue shouldBeEqualComparingTo BigDecimal("1100")
        jan.remainingDue shouldBeEqualComparingTo BigDecimal("950")
        jan.minimumDueRemaining!! shouldBeEqualComparingTo BigDecimal("50")
    }

    @Test
    fun `editing or deleting a historical transaction recalculates statements`() {
        val original = listOf(e("2026-01-10", "1000", CHARGE), e("2026-02-10", "500", CHARGE))
        val edited = listOf(e("2026-01-10", "800", CHARGE), e("2026-02-10", "500", CHARGE))
        val deleted = listOf(e("2026-02-10", "500", CHARGE))

        calc(ledger = original, today = "2026-02-20").latestStatement!!.statementBalance
            .shouldBeEqualComparingTo(BigDecimal("1500"))
        calc(ledger = edited, today = "2026-02-20").latestStatement!!.statementBalance
            .shouldBeEqualComparingTo(BigDecimal("1300"))
        calc(ledger = deleted, today = "2026-02-20").latestStatement!!.statementBalance
            .shouldBeEqualComparingTo(BigDecimal("500"))
    }

    @Test
    fun `card without schedule or limit still tracks outstanding`() {
        val result = calc(
            card = card(limit = null, statementDay = null, dueRule = null),
            ledger = listOf(e("2026-01-05", "300", CHARGE), e("2026-02-05", "100", PAYMENT)),
            today = "2026-03-01",
        )

        result.outstanding shouldBeEqualComparingTo BigDecimal("200")
        result.availableCredit shouldBe null
        result.utilisation shouldBe null
        result.statements shouldHaveSize 0
    }

    @Test
    fun `decimal amounts don't drift`() {
        val ledger = (1..10).map { e("2026-01-0${(it % 9) + 1}", "0.10", CHARGE) }
        calc(ledger = ledger, today = "2026-01-10").outstanding shouldBeEqualComparingTo BigDecimal("1.00")
    }

    @Test
    fun `an older bill that isn't due yet keeps its own status and is the urgent one`() {
        // Jan 15 statement due Mar 1 (45 days); by Feb 20 the Feb 15 statement has closed too.
        val longRule = card(dueRule = DueDateRule.DaysAfterStatement(45))
        val ledger = listOf(e("2026-01-10", "1000", CHARGE), e("2026-02-10", "500", CHARGE))
        val result = calc(card = longRule, ledger = ledger, today = "2026-02-20")

        val (feb, jan) = result.statements
        jan.status shouldBe StatementStatus.UPCOMING
        feb.status shouldBe StatementStatus.UPCOMING
        result.urgentStatement?.closingDate shouldBe jan.closingDate
    }

    @Test
    fun `an older bill past its due date is carried forward and the latest one is urgent`() {
        val ledger = listOf(e("2026-01-10", "1000", CHARGE), e("2026-02-10", "500", CHARGE))
        val result = calc(ledger = ledger, today = "2026-02-20")

        val (feb, jan) = result.statements
        jan.status shouldBe StatementStatus.CARRIED_FORWARD
        result.urgentStatement shouldBe feb
    }
}
