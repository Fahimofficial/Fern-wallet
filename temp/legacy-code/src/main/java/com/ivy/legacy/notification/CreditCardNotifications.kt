@file:Suppress("InvalidPackageDeclaration") // matches the sibling reminder workers

package com.ivy.wallet.domain.deprecated.logic.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.ivy.wallet.android.notification.IvyNotificationChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Posts and removes credit-card bill notifications.
 *
 * Each notification is tagged with its card (`cc_<cardId>`) and identified by the statement's
 * due date, so all of a card's notifications can be found and removed again, even after the
 * card itself has been deleted.
 */
class CreditCardNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: NotificationManager?
        get() = context.getSystemService(NotificationManager::class.java)

    /**
     * True only when a posted reminder would actually be shown: app notifications are allowed
     * (including the Android 13+ runtime permission) and the card channel isn't turned off.
     */
    fun canPost(): Boolean {
        val m = manager
        if (m == null || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = IvyNotificationChannel.CREDIT_CARD_DUE.create(context)
        // Creating an existing channel is a no-op and keeps the user's settings.
        m.createNotificationChannel(channel)
        val current = m.getNotificationChannel(channel.id)
        val groupBlocked = current?.group?.let { m.getNotificationChannelGroup(it)?.isBlocked } == true
        return current != null && current.importance != NotificationManager.IMPORTANCE_NONE && !groupBlocked
    }

    /** Returns true only when the system accepted the notification. */
    fun post(cardId: UUID, dueDate: LocalDate, notification: Notification): Boolean {
        val m = manager ?: return false
        return try {
            m.notify(tag(cardId), id(dueDate), notification)
            true
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "Couldn't post card reminder", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w(LOG_TAG, "Couldn't post card reminder", e)
            false
        }
    }

    /** Removes a card's notifications except those for the given statement due dates. */
    fun cancelForCardExcept(cardId: UUID, keepDueDates: Set<LocalDate>) {
        val keepIds = keepDueDates.map(::id).toSet()
        val cardTag = tag(cardId)
        val m = manager ?: return
        m.activeNotifications
            .filter { it.tag == cardTag && it.id !in keepIds }
            .forEach { m.cancel(it.tag, it.id) }
    }

    /** Removes every notification of a card; call before deleting it. */
    fun cancelForCard(cardId: UUID) {
        cancelWhere { it == tag(cardId) }
    }

    /** Removes the notifications of cards that no longer exist (deleted, data wiped, restored backup). */
    fun cancelOrphans(existingCardIds: Set<UUID>) {
        val keep = existingCardIds.map(::tag).toSet()
        cancelWhere { it.startsWith(TAG_PREFIX) && it !in keep }
    }

    /** Removes all credit-card notifications, e.g. when wallet data is cleared. */
    fun cancelAll() {
        cancelWhere { it.startsWith(TAG_PREFIX) }
    }

    private fun cancelWhere(matches: (String) -> Boolean) {
        val m = manager ?: return
        m.activeNotifications
            .filter { n -> n.tag?.let(matches) == true }
            .forEach { n -> m.cancel(n.tag, n.id) }
    }

    /** Stable PendingIntent request code for a card's notification. */
    fun requestCode(cardId: UUID, dueDate: LocalDate): Int = (tag(cardId) + dueDate).hashCode()

    private fun tag(cardId: UUID): String = "$TAG_PREFIX$cardId"

    private fun id(dueDate: LocalDate): Int = dueDate.toEpochDay().toInt()

    companion object {
        private const val TAG_PREFIX = "cc_"
        private const val LOG_TAG = "CardReminders"
    }
}
