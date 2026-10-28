@file:Suppress("InvalidPackageDeclaration") // matches the sibling reminder workers

package com.ivy.wallet.domain.deprecated.logic.notification

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeProvider
import com.ivy.domain.AppStarter
import com.ivy.domain.creditcard.CardReminder
import com.ivy.domain.creditcard.CardReminderPlanner
import com.ivy.domain.creditcard.CreditCardOverviewUseCase
import com.ivy.domain.creditcard.CreditCardWithOverview
import com.ivy.domain.creditcard.dedupKey
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.wallet.android.notification.IvyNotificationChannel
import com.ivy.wallet.android.notification.NotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Daily local check for credit-card payment reminders. No network or server involved. */
@HiltWorker
class CreditCardReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val overviewUseCase: CreditCardOverviewUseCase,
    private val notificationService: NotificationService,
    private val cardNotifications: CreditCardNotifications,
    private val sharedPrefs: SharedPrefs,
    private val appStarter: AppStarter,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val notificationsOn = sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true)
        val today = timeProvider.localDateNow()
        val items = overviewUseCase.loadAll()
        // Deleted cards have no row left to clean up from, so sweep their notifications here too.
        cardNotifications.cancelOrphans(items.map { it.account.id.value }.toSet())
        items.forEach { item -> process(item, today, notificationsOn) }
        Result.success()
    }

    private fun process(item: CreditCardWithOverview, today: LocalDate, notificationsOn: Boolean) {
        val cardId = item.account.id.value
        cardNotifications.cancelForCardExcept(cardId, CardReminderPlanner.dueDatesToKeep(item.card, item.overview))
        val reminders = CardReminderPlanner.remindersFor(item.card, item.overview, today)
        // Nothing is marked as sent while notifications are blocked, so the reminder is
        // retried on a later run once they are allowed again (within the reminder window).
        if (!notificationsOn || reminders.isEmpty() || !cardNotifications.canPost()) return
        reminders.forEach { reminder ->
            val key = reminder.dedupKey(cardId.toString())
            if (sharedPrefs.getBoolean(key, false)) return@forEach
            val posted = show(
                cardId = cardId,
                cardName = item.account.name.value,
                currency = item.account.asset.code,
                reminder = reminder,
            )
            if (posted) sharedPrefs.putBoolean(key, true)
        }
    }

    private fun show(cardId: UUID, cardName: String, currency: String, reminder: CardReminder): Boolean {
        val amount = "${reminder.remainingDue.toDouble().format(currency)} $currency"
        val date = reminder.dueDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        val text = when (reminder.kind) {
            CardReminder.Kind.DUE_TODAY ->
                applicationContext.getString(R.string.cc_reminder_due_today, cardName, amount)
            CardReminder.Kind.BEFORE_DUE ->
                applicationContext.getString(R.string.cc_reminder_due_on, cardName, amount, date)
        }
        val notification = notificationService
            .defaultIvyNotification(
                channel = IvyNotificationChannel.CREDIT_CARD_DUE,
                priority = NotificationCompat.PRIORITY_HIGH
            )
            .setContentTitle(applicationContext.getString(R.string.cc_reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    cardNotifications.requestCode(cardId, reminder.dueDate),
                    appStarter.getRootIntent(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        return cardNotifications.post(cardId, reminder.dueDate, notification)
    }
}

class CreditCardReminderScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    /** Runs roughly once a day; KEEP so app restarts don't reset the schedule. */
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<CreditCardReminderWorker>(REPEAT_HOURS, TimeUnit.HOURS)
            .setInitialDelay(INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Runs the check once now, so a payment, edit or closure updates or removes the card's
     * notifications right away instead of on the next periodic run.
     */
    fun refreshNow() {
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            REFRESH_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CreditCardReminderWorker>().build()
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "credit_card_reminder_work_v1"
        private const val REFRESH_WORK_NAME = "credit_card_reminder_refresh"
        private const val REPEAT_HOURS = 12L
        private const val INITIAL_DELAY_MINUTES = 15L
    }
}
