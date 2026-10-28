@file:Suppress("InvalidPackageDeclaration") // matches the sibling reminder workers

package com.ivy.wallet.domain.deprecated.logic.notification

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeProvider
import com.ivy.domain.AppStarter
import com.ivy.domain.creditcard.CardReminder
import com.ivy.domain.creditcard.CardReminderPlanner
import com.ivy.domain.creditcard.CreditCardOverviewUseCase
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Daily local check for credit-card payment reminders. No network or server involved. */
@HiltWorker
class CreditCardReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val overviewUseCase: CreditCardOverviewUseCase,
    private val notificationService: NotificationService,
    private val sharedPrefs: SharedPrefs,
    private val appStarter: AppStarter,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val notificationsOn = sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true)
        val today = timeProvider.localDateNow()
        overviewUseCase.loadAll().forEach { item ->
            val notificationId = notificationId(item.account.id.value.toString())
            if (CardReminderPlanner.shouldDismiss(item.card, item.overview)) {
                notificationService.dismissNotification(notificationId)
                return@forEach
            }
            val reminder = CardReminderPlanner.reminderFor(item.card, item.overview, today)
                ?: return@forEach
            val key = reminder.dedupKey(item.account.id.value.toString())
            if (!notificationsOn || sharedPrefs.getBoolean(key, false)) return@forEach

            show(
                notificationId = notificationId,
                cardName = item.account.name.value,
                currency = item.account.asset.code,
                reminder = reminder,
            )
            sharedPrefs.putBoolean(key, true)
        }
        Result.success()
    }

    private fun show(notificationId: Int, cardName: String, currency: String, reminder: CardReminder) {
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
                    notificationId,
                    appStarter.getRootIntent(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        notificationService.showNotification(notification, notificationId)
    }

    private fun notificationId(cardId: String): Int = NOTIFICATION_ID_BASE + (cardId.hashCode() and ID_MASK)

    companion object {
        private const val NOTIFICATION_ID_BASE = 10_000
        private const val ID_MASK = 0xFFFF
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

    companion object {
        private const val UNIQUE_WORK_NAME = "credit_card_reminder_work_v1"
        private const val REPEAT_HOURS = 12L
        private const val INITIAL_DELAY_MINUTES = 15L
    }
}
