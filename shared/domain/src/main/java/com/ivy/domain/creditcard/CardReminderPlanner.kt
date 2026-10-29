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
 * Decides which reminders are due today. It is recomputed from current data every run,
 * so editing a card, its statement dates or recording a payment automatically updates or
 * cancels reminders. Missed days (e.g. phone off) still produce one "before due" reminder.
 *
 * Every unpaid statement is checked, not only the latest: with a long due-date rule
 * (up to 60 days after closing) an older bill can still be ahead of its due date after
 * the next statement has closed.
 */
object CardReminderPlanner {
    /** Reminders to show today, earliest due date first (at most one per due date). */
    fun remindersFor(card: CreditCard, overview: CardOverview, today: LocalDate): List<CardReminder> {
        if (!isActive(card)) return emptyList()
        return overview.statements
            .filter { it.remainingDue.signum() > 0 }
            .mapNotNull { statement ->
                val due = statement.dueDate
                val kind = due?.let { reminderKind(card, it, today) }
                if (due == null || kind == null) {
                    null
                } else {
                    CardReminder(
                        kind = kind,
                        dueDate = due,
                        remainingDue = statement.remainingDue,
                        minimumDueRemaining = statement.minimumDueRemaining,
                    )
                }
            }
            .distinctBy { it.dueDate }
            .sortedBy { it.dueDate }
    }

    /** The most urgent reminder for today, if any. */
    fun reminderFor(card: CreditCard, overview: CardOverview, today: LocalDate): CardReminder? =
        remindersFor(card, overview, today).firstOrNull()

    private fun reminderKind(card: CreditCard, due: LocalDate, today: LocalDate): CardReminder.Kind? {
        val windowStart = due.minusDays(card.reminderDaysBefore.toLong())
        return when {
            today == due -> CardReminder.Kind.DUE_TODAY
            card.reminderDaysBefore > 0 && !today.isBefore(windowStart) && today.isBefore(due) ->
                CardReminder.Kind.BEFORE_DUE
            else -> null
        }
    }

    /**
     * Due dates whose notification may stay visible: unpaid statements of an active card.
     * Anything else (paid bills, due dates moved by an issuer override, reminders turned off,
     * closed cards) should be removed.
     */
    fun dueDatesToKeep(card: CreditCard, overview: CardOverview): Set<LocalDate> =
        if (!isActive(card)) {
            emptySet()
        } else {
            overview.statements.filter { it.remainingDue.signum() > 0 }.mapNotNull { it.dueDate }.toSet()
        }

    private fun isActive(card: CreditCard): Boolean = card.remindersEnabled && card.status != CardStatus.CLOSED
}
