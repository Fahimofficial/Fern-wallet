package com.ivy.domain.creditcard

import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import java.math.BigDecimal
import java.time.LocalDate

/** A payment reminder that should be shown today. */
data class CardReminder(
    val kind: Kind,
    val dueDate: LocalDate,
    val remainingDue: BigDecimal,
    val minimumDueRemaining: BigDecimal?,
) {
    enum class Kind { BEFORE_DUE, DUE_TODAY }
}

/** Stable key for de-duplication: one reminder of each kind per statement due date. */
fun CardReminder.dedupKey(cardId: String): String = "cc_reminder_${cardId}_${dueDate}_$kind"

/**
 * Decides which reminder (if any) is due today. It is recomputed from current data every run,
 * so editing a card, its statement dates or recording a payment automatically updates or
 * cancels reminders. Missed days (e.g. phone off) still produce one "before due" reminder.
 */
object CardReminderPlanner {
    fun reminderFor(card: CreditCard, overview: CardOverview, today: LocalDate): CardReminder? {
        val statement = overview.latestStatement?.takeIf { it.remainingDue.signum() > 0 }
        val due = statement?.dueDate
        val active = card.remindersEnabled && card.status != CardStatus.CLOSED
        if (!active || statement == null || due == null) return null
        return reminderKind(card, due, today)?.let { kind ->
            CardReminder(
                kind = kind,
                dueDate = due,
                remainingDue = statement.remainingDue,
                minimumDueRemaining = statement.minimumDueRemaining,
            )
        }
    }

    private fun reminderKind(card: CreditCard, due: LocalDate, today: LocalDate): CardReminder.Kind? {
        val windowStart = due.minusDays(card.reminderDaysBefore.toLong())
        return when {
            today == due -> CardReminder.Kind.DUE_TODAY
            card.reminderDaysBefore > 0 && !today.isBefore(windowStart) && today.isBefore(due) ->
                CardReminder.Kind.BEFORE_DUE
            else -> null
        }
    }

    /** True when an earlier reminder for this card should be removed (bill paid / disabled). */
    fun shouldDismiss(card: CreditCard, overview: CardOverview): Boolean {
        val statement = overview.latestStatement
        return !card.remindersEnabled || card.status == CardStatus.CLOSED ||
            statement == null || statement.remainingDue.signum() <= 0
    }
}
