package com.ivy.creditcards

import com.ivy.creditcards.edit.CardForm
import com.ivy.creditcards.edit.CardFormError
import com.ivy.creditcards.edit.CardFormField
import com.ivy.creditcards.edit.CardFormResult
import com.ivy.creditcards.edit.CardFormValidator
import com.ivy.creditcards.edit.DueRuleKind
import com.ivy.creditcards.pay.PayCreditCardViewModel
import com.ivy.data.model.AccountId
import com.ivy.data.model.creditcard.DueDateRule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class CardFormValidatorTest {

    private val today = LocalDate.of(2026, 3, 10)
    private val accountId = AccountId(UUID(0, 3))

    private val valid = CardForm(
        customBankName = "Sample Bank",
        useCustomBank = true,
        nickname = "Everyday card",
        currency = "inr",
        lastFour = "4821",
        creditLimit = "150000",
        statementDay = 18,
        dueRuleKind = DueRuleKind.DAYS_AFTER_STATEMENT,
        dueRuleValue = "20",
        openingOutstanding = "12000.50",
        openingDate = today,
    )

    private fun validate(form: CardForm) = CardFormValidator.validate(form, accountId, null, today)

    private fun errorsOf(form: CardForm): Map<CardFormField, CardFormError> =
        (validate(form) as CardFormResult.Invalid).errors

    @Test
    fun `valid form produces a card with parsed values`() {
        val result = validate(valid).shouldBeInstanceOf<CardFormResult.Valid>()
        result.currency.code shouldBe "INR"
        result.card.creditLimit?.compareTo(BigDecimal("150000")) shouldBe 0
        result.card.openingOutstanding.compareTo(BigDecimal("12000.50")) shouldBe 0
        result.card.dueDateRule shouldBe DueDateRule.DaysAfterStatement(20)
        result.card.customBankName shouldBe "Sample Bank"
    }

    @Test
    fun `opening credit balance is stored as a negative outstanding`() {
        val result = validate(valid.copy(openingIsCreditBalance = true)).shouldBeInstanceOf<CardFormResult.Valid>()
        result.card.openingOutstanding.compareTo(BigDecimal("-12000.50")) shouldBe 0
    }

    @Test
    fun `nickname, bank and currency are required`() {
        val errors = errorsOf(valid.copy(nickname = " ", useCustomBank = false, bankId = null, currency = " "))
        errors[CardFormField.NICKNAME] shouldBe CardFormError.NICKNAME_REQUIRED
        errors[CardFormField.BANK] shouldBe CardFormError.BANK_REQUIRED
        errors[CardFormField.CURRENCY] shouldBe CardFormError.CURRENCY_INVALID
    }

    @Test
    fun `only four digits are accepted as last four`() {
        errorsOf(valid.copy(lastFour = "12a4"))[CardFormField.LAST_FOUR] shouldBe CardFormError.LAST_FOUR_INVALID
        errorsOf(valid.copy(lastFour = "123"))[CardFormField.LAST_FOUR] shouldBe CardFormError.LAST_FOUR_INVALID
        validate(valid.copy(lastFour = "")).shouldBeInstanceOf<CardFormResult.Valid>().card.lastFour shouldBe null
    }

    @Test
    fun `credit limit must be positive when given`() {
        errorsOf(valid.copy(creditLimit = "0"))[CardFormField.LIMIT] shouldBe CardFormError.LIMIT_INVALID
        errorsOf(valid.copy(creditLimit = "abc"))[CardFormField.LIMIT] shouldBe CardFormError.LIMIT_INVALID
        validate(valid.copy(creditLimit = "")).shouldBeInstanceOf<CardFormResult.Valid>().card.creditLimit shouldBe null
    }

    @Test
    fun `due rule is range checked and needs a statement day`() {
        errorsOf(valid.copy(dueRuleValue = "0"))[CardFormField.DUE_RULE] shouldBe CardFormError.DUE_RULE_INVALID
        val dayOfMonth = valid.copy(dueRuleKind = DueRuleKind.DAY_OF_MONTH, dueRuleValue = "32")
        errorsOf(dayOfMonth)[CardFormField.DUE_RULE] shouldBe CardFormError.DUE_RULE_INVALID
        errorsOf(valid.copy(statementDay = null))[CardFormField.DUE_RULE] shouldBe
            CardFormError.DUE_RULE_NEEDS_STATEMENT_DAY
    }

    @Test
    fun `opening outstanding cannot be negative or dated in the future`() {
        errorsOf(valid.copy(openingOutstanding = "-5"))[CardFormField.OPENING] shouldBe CardFormError.OPENING_INVALID
        errorsOf(valid.copy(openingDate = today.plusDays(1)))[CardFormField.OPENING_DATE] shouldBe
            CardFormError.OPENING_DATE_IN_FUTURE
    }

    @Test
    fun `overpayment is the part of a payment above what is owed`() {
        PayCreditCardViewModel.overpayment(BigDecimal("1000"), BigDecimal("1500")) shouldBe null
        PayCreditCardViewModel.overpayment(BigDecimal("1500"), BigDecimal("1500")) shouldBe null
        PayCreditCardViewModel.overpayment(BigDecimal("2000"), BigDecimal("1500"))
            ?.compareTo(BigDecimal("500")) shouldBe 0
        // A card already in credit: the whole payment adds to the credit balance.
        PayCreditCardViewModel.overpayment(BigDecimal("300"), BigDecimal("-200"))
            ?.compareTo(BigDecimal("300")) shouldBe 0
    }
}
