package com.ivy.data.repository.mapper

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.AccountType
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardNetwork
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.data.repository.CurrencyRepository
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@RunWith(TestParameterInjector::class)
class CreditCardMapperTest {
    private val mapper = CreditCardMapper()
    private val id = UUID(7, 7)

    private fun card(rule: DueDateRule?) = CreditCard(
        accountId = AccountId(id),
        bankId = "axis-bank",
        customBankName = null,
        productId = "axis-bank-x",
        productName = "X Credit Card",
        network = CardNetwork.VISA,
        lastFour = "0042",
        creditLimit = BigDecimal("200000.0"),
        statementDay = 28,
        dueDateRule = rule,
        openingOutstanding = BigDecimal("1500.25"),
        openingDate = LocalDate.parse("2026-03-01"),
        status = CardStatus.INACTIVE,
        appearance = CardAppearance.SUNSET,
        remindersEnabled = true,
        reminderDaysBefore = 2,
    )

    enum class RuleCase(val rule: DueDateRule?) {
        NONE(null),
        DAYS_AFTER(DueDateRule.DaysAfterStatement(20)),
        DAY_OF_MONTH(DueDateRule.DayOfMonth(5)),
    }

    @Test
    fun `round trips every field`(@TestParameter case: RuleCase) {
        val card = card(case.rule)

        val restored = with(mapper) { card.toEntity().toDomain() }

        restored shouldBe card
    }

    @Test
    fun `invalid stored values degrade safely`() {
        val entity = CreditCardEntity(
            accountId = id,
            network = "UNKNOWN_NETWORK",
            lastFour = "12a4",
            creditLimit = -5.0,
            statementDay = 42,
            dueRuleType = "DAYS_AFTER_STATEMENT",
            dueRuleValue = 400,
            openingDateEpochDay = 0,
            status = "FROM_THE_FUTURE",
            appearance = "NEON",
        )

        val card = with(mapper) { entity.toDomain() }

        card.network shouldBe null
        card.lastFour shouldBe null
        card.creditLimit shouldBe null
        card.statementDay shouldBe null
        card.dueDateRule shouldBe null
        card.status shouldBe CardStatus.ACTIVE
        card.appearance shouldBe CardAppearance.FERN
    }

    @Test
    fun `account type round trips and unknown values fall back to asset`() = runTest {
        val accountMapper = AccountMapper(mockk<CurrencyRepository>(relaxed = true))
        val entity = AccountEntity(name = "Card", currency = "INR", color = 1, type = "CREDIT_CARD", id = id)

        val account = with(accountMapper) { entity.toDomain() }.shouldBeRight()

        account.type shouldBe AccountType.CREDIT_CARD
        with(accountMapper) { account.toEntity() }.type shouldBe "CREDIT_CARD"
        with(accountMapper) { entity.copy(type = "SOMETHING_NEW").toDomain() }
            .shouldBeRight().type shouldBe AccountType.ASSET
    }
}
