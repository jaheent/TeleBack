package com.teleback.core.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncWorkScheduler {

    fun schedulePeriodicSync(
        context: Context,
        intervalHours: Int,
        requiresUnmetered: Boolean = true,
        requiresCharging: Boolean = false,
        requiresBatteryNotLow: Boolean = true
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requiresUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(requiresCharging)
            .setRequiresBatteryNotLow(requiresBatteryNotLow)
            .build()

        // PeriodicWorkRequest interval: minimum 15 mins. coerceAtLeast 1 hour
        val periodicRequest = PeriodicWorkRequestBuilder<TeleBackSyncWorker>(
            intervalHours.toLong().coerceAtLeast(1),
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TeleBackSyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TeleBackSyncWorker.WORK_NAME_PERIODIC)
    }

    fun enqueueOneTimeSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<TeleBackSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            TeleBackSyncWorker.WORK_NAME_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}
