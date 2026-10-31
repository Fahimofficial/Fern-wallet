package com.ivy.data.backup.auto

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val autoBackup: AutoBackupUseCase,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = when (autoBackup.backUpNow()) {
        null -> Result.success()
        // Possibly temporary (e.g. storage full): try again later, with backoff.
        AutoBackupFailure.WRITE_FAILED -> if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        // Needs the user; the next scheduled run tries again and Settings shows the problem.
        AutoBackupFailure.FOLDER_UNAVAILABLE, AutoBackupFailure.KEY_UNAVAILABLE -> Result.failure()
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}

class AutoBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun apply(frequency: AutoBackupFrequency) {
        val workManager = WorkManager.getInstance(context)
        val days = when (frequency) {
            AutoBackupFrequency.OFF -> {
                workManager.cancelUniqueWork(PERIODIC_WORK)
                return
            }
            AutoBackupFrequency.DAILY -> 1L
            AutoBackupFrequency.WEEKLY -> DAYS_IN_WEEK
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(days, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private companion object {
        const val PERIODIC_WORK = "fern_auto_backup"
        const val DAYS_IN_WEEK = 7L
    }
}
