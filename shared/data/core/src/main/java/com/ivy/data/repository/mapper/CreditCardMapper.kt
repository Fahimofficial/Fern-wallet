package com.ivy.data.repository.mapper

import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.db.entity.CreditCardStatementEntity
import com.ivy.data.model.AccountId
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardNetwork
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.data.model.creditcard.DueDateRule
import com.ivy.data.model.creditcard.StatementOverride
import java.time.LocalDate
import javax.inject.Inject

private const val DueDaysAfter = "DAYS_AFTER_STATEMENT"
private const val DueDayOfMonth = "DAY_OF_MONTH"
private const val MaxDayOfMonth = 31
private const val MaxDaysAfter = 60
private const val LastFourLength = 4

/**
 * Maps card rows to the domain. Invalid stored values (e.g. edited backups, newer app versions)
 * degrade to "unknown" instead of failing, so a card can always be opened and fixed.
 */
class CreditCardMapper @Inject constructor() {
    fun CreditCardEntity.toDomain(): CreditCard = CreditCard(
        accountId = AccountId(accountId),
        bankId = bankId?.takeIf { it.isNotBlank() },
        customBankName = customBankName?.trim()?.takeIf { it.isNotBlank() },
        productId = productId?.takeIf { it.isNotBlank() },
        productName = productName?.trim()?.takeIf { it.isNotBlank() },
        network = CardNetwork.entries.firstOrNull { it.name == network },
        lastFour = lastFour?.takeIf { isValidLastFour(it) },
        creditLimit = creditLimit?.takeIf { it > 0.0 }?.toBigDecimal(),
        statementDay = statementDay?.takeIf { it in 1..MaxDayOfMonth },
        dueDateRule = dueRule(dueRuleType, dueRuleValue),
        openingOutstanding = openingOutstanding.toBigDecimal(),
        openingDate = LocalDate.ofEpochDay(openingDateEpochDay),
        status = CardStatus.entries.firstOrNull { it.name == status } ?: CardStatus.ACTIVE,
        appearance = CardAppearance.entries.firstOrNull { it.name == appearance }
            ?: CardAppearance.FERN,
        remindersEnabled = remindersEnabled,
        reminderDaysBefore = reminderDaysBefore.coerceIn(0, MaxDaysAfter),
    )

    fun CreditCard.toEntity(): CreditCardEntity = CreditCardEntity(
        accountId = accountId.value,
        bankId = bankId,
        customBankName = customBankName,
        productId = productId,
        productName = productName,
        network = network?.name,
        lastFour = lastFour,
        creditLimit = creditLimit?.toDouble(),
        statementDay = statementDay,
        dueRuleType = when (dueDateRule) {
            is DueDateRule.DaysAfterStatement -> DueDaysAfter
            is DueDateRule.DayOfMonth -> DueDayOfMonth
            null -> null
        },
        dueRuleValue = when (val rule = dueDateRule) {
            is DueDateRule.DaysAfterStatement -> rule.days
            is DueDateRule.DayOfMonth -> rule.day
            null -> null
        },
        openingOutstanding = openingOutstanding.toDouble(),
        openingDateEpochDay = openingDate.toEpochDay(),
        status = status.name,
        appearance = appearance.name,
        remindersEnabled = remindersEnabled,
        reminderDaysBefore = reminderDaysBefore,
    )

    fun CreditCardStatementEntity.toDomain(): StatementOverride = StatementOverride(
        accountId = AccountId(accountId),
        nominalClosingDate = LocalDate.ofEpochDay(nominalClosingEpochDay),
        actualClosingDate = actualClosingEpochDay?.let(LocalDate::ofEpochDay),
        actualDueDate = actualDueEpochDay?.let(LocalDate::ofEpochDay),
        issuerStatementBalance = issuerStatementBalance?.toBigDecimal(),
        minimumDue = minimumDue?.takeIf { it >= 0.0 }?.toBigDecimal(),
    )

    fun StatementOverride.toEntity(): CreditCardStatementEntity = CreditCardStatementEntity(
        accountId = accountId.value,
        nominalClosingEpochDay = nominalClosingDate.toEpochDay(),
        actualClosingEpochDay = actualClosingDate?.toEpochDay(),
        actualDueEpochDay = actualDueDate?.toEpochDay(),
        issuerStatementBalance = issuerStatementBalance?.toDouble(),
        minimumDue = minimumDue?.toDouble(),
    )

    private fun dueRule(type: String?, value: Int?): DueDateRule? = when {
        value == null -> null
        type == DueDaysAfter && value in 0..MaxDaysAfter -> DueDateRule.DaysAfterStatement(value)
        type == DueDayOfMonth && value in 1..MaxDayOfMonth -> DueDateRule.DayOfMonth(value)
        else -> null
    }

    companion object {
        fun isValidLastFour(value: String): Boolean =
            value.length == LastFourLength && value.all { it in '0'..'9' }
    }
}
