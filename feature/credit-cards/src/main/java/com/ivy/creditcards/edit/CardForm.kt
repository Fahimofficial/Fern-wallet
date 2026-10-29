package com.ivy.creditcards.edit

import androidx.compose.runtime.Immutable
import com.ivy.data.model.AccountId
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardNetwork
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.mapper.CreditCardMapper
import com.ivy.legacy.utils.normalizeAmount
import java.math.BigDecimal
import java.time.LocalDate

enum class DueRuleKind { NONE, DAYS_AFTER_STATEMENT, DAY_OF_MONTH }

/** Raw form input (strings exactly as typed). */
@Immutable
@Suppress("DataClassDefaultValues", "DataClassTypedIDs") // defaults describe an empty form; ids are catalogue slugs
data class CardForm(
    val bankId: String? = null,
    val customBankName: String = "",
    val useCustomBank: Boolean = false,
    val productId: String? = null,
    val customProductName: String = "",
    val useCustomProduct: Boolean = false,
    val nickname: String = "",
    val currency: String = "",
    val network: CardNetwork? = null,
    val lastFour: String = "",
    val creditLimit: String = "",
    val statementDay: Int? = null,
    val dueRuleKind: DueRuleKind = DueRuleKind.NONE,
    val dueRuleValue: String = "",
    val openingOutstanding: String = "",
    val openingIsCreditBalance: Boolean = false,
    val openingDate: LocalDate = LocalDate.now(),
    val appearance: CardAppearance = CardAppearance.FERN,
    val includeInBalance: Boolean = true,
    val remindersEnabled: Boolean = true,
    val reminderDaysBefore: Int = DEFAULT_REMINDER_DAYS,
    val status: CardStatus = CardStatus.ACTIVE,
) {
    companion object {
        const val DEFAULT_REMINDER_DAYS = 3
        const val MAX_REMINDER_DAYS = 10
        const val MAX_DAYS_AFTER_STATEMENT = 60
        const val MAX_DAY_OF_MONTH = 31
    }
}

enum class CardFormError {
    NICKNAME_REQUIRED,
    CURRENCY_INVALID,
    BANK_REQUIRED,
    LAST_FOUR_INVALID,
    LIMIT_INVALID,
    DUE_RULE_INVALID,
    DUE_RULE_NEEDS_STATEMENT_DAY,
    OPENING_INVALID,
    OPENING_DATE_IN_FUTURE,
}

/** Result of validation: either field errors or the values ready to save. */
sealed interface CardFormResult {
    data class Invalid(val errors: Map<CardFormField, CardFormError>) : CardFormResult
    data class Valid(
        val nickname: String,
        val currency: AssetCode,
        val card: CreditCard,
        val includeInBalance: Boolean,
    ) : CardFormResult
}

enum class CardFormField { NICKNAME, CURRENCY, BANK, LAST_FOUR, LIMIT, DUE_RULE, OPENING, OPENING_DATE }

object CardFormValidator {
    fun parseAmount(text: String): BigDecimal? =
        text.trim().takeIf { it.isNotEmpty() }?.normalizeAmount()?.toBigDecimalOrNull()

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun validate(
        form: CardForm,
        accountId: AccountId,
        productName: String?,
        today: LocalDate,
    ): CardFormResult {
        val errors = mutableMapOf<CardFormField, CardFormError>()

        val nickname = form.nickname.trim()
        if (nickname.isEmpty()) errors[CardFormField.NICKNAME] = CardFormError.NICKNAME_REQUIRED

        val currency = AssetCode.from(form.currency.trim().uppercase()).getOrNull()
        if (currency == null) errors[CardFormField.CURRENCY] = CardFormError.CURRENCY_INVALID

        val customBank = form.customBankName.trim().takeIf { form.useCustomBank && it.isNotEmpty() }
        val bankId = form.bankId.takeIf { !form.useCustomBank }
        if (bankId == null && customBank == null) errors[CardFormField.BANK] = CardFormError.BANK_REQUIRED

        val lastFour = form.lastFour.trim().takeIf { it.isNotEmpty() }
        if (lastFour != null && !CreditCardMapper.isValidLastFour(lastFour)) {
            errors[CardFormField.LAST_FOUR] = CardFormError.LAST_FOUR_INVALID
        }

        val limit = form.creditLimit.trim().takeIf { it.isNotEmpty() }?.let { parseAmount(it) }
        val limitInvalid = form.creditLimit.isNotBlank() && (limit == null || limit.signum() <= 0)
        if (limitInvalid) errors[CardFormField.LIMIT] = CardFormError.LIMIT_INVALID

        val dueValue = form.dueRuleValue.trim().toIntOrNull()
        val dueRule = when (form.dueRuleKind) {
            DueRuleKind.NONE -> null
            DueRuleKind.DAYS_AFTER_STATEMENT ->
                dueValue?.takeIf { it in 1..CardForm.MAX_DAYS_AFTER_STATEMENT }?.let(DueDateRule::DaysAfterStatement)
            DueRuleKind.DAY_OF_MONTH ->
                dueValue?.takeIf { it in 1..CardForm.MAX_DAY_OF_MONTH }?.let(DueDateRule::DayOfMonth)
        }
        when {
            form.dueRuleKind != DueRuleKind.NONE && dueRule == null ->
                errors[CardFormField.DUE_RULE] = CardFormError.DUE_RULE_INVALID
            dueRule != null && form.statementDay == null ->
                errors[CardFormField.DUE_RULE] = CardFormError.DUE_RULE_NEEDS_STATEMENT_DAY
        }

        val opening = if (form.openingOutstanding.isBlank()) BigDecimal.ZERO else parseAmount(form.openingOutstanding)
        if (opening == null || opening.signum() < 0) errors[CardFormField.OPENING] = CardFormError.OPENING_INVALID
        if (form.openingDate.isAfter(today)) {
            errors[CardFormField.OPENING_DATE] = CardFormError.OPENING_DATE_IN_FUTURE
        }

        if (errors.isNotEmpty() || currency == null || opening == null) return CardFormResult.Invalid(errors)

        return CardFormResult.Valid(
            nickname = nickname,
            currency = currency,
            includeInBalance = form.includeInBalance,
            card = CreditCard(
                accountId = accountId,
                bankId = bankId,
                customBankName = customBank,
                productId = form.productId.takeIf { !form.useCustomProduct && bankId != null },
                productName = productName?.trim()?.takeIf { it.isNotEmpty() },
                network = form.network,
                lastFour = lastFour,
                creditLimit = limit,
                statementDay = form.statementDay,
                dueDateRule = dueRule,
                openingOutstanding = if (form.openingIsCreditBalance) opening.negate() else opening,
                openingDate = form.openingDate,
                status = form.status,
                appearance = form.appearance,
                remindersEnabled = form.remindersEnabled,
                reminderDaysBefore = form.reminderDaysBefore.coerceIn(0, CardForm.MAX_REMINDER_DAYS),
            )
        )
    }
}
