package com.teleback.core.service

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.teleback.core.cli.LogLevel
import com.teleback.core.util.NotificationHelper
import com.teleback.core.util.SizeFormat
import com.teleback.data.local.AppPreferences
import com.teleback.domain.SyncManager
import kotlinx.coroutines.flow.first

class TeleBackSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME_PERIODIC = "teleback_periodic_sync"
        const val WORK_NAME_ONE_TIME = "teleback_one_time_sync"
        private const val LARGE_UPLOAD_THRESHOLD_BYTES = 100 * 1024 * 1024L // 100 MB
    }

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(context)
        val token = prefs.botToken.first()
        val chatId = prefs.chatId.first()
        val folderUri = prefs.folderUri.first()

        // If app hasn't been configured yet, fail silently
        if (token.isNullOrBlank() || chatId == null || folderUri.isNullOrBlank()) {
            return Result.failure()
        }

        val syncManager = SyncManager(context)

        return try {
            // Non-blocking folder change detection before initiating transfers
            val diff = syncManager.calculateDiff()

            if (diff.addedCount == 0 && diff.modifiedCount == 0 && diff.deletedCount == 0) {
                // Folder is already up to date; avoid unnecessary network requests
                return Result.success()
            }

            // Transition transparently to foreground execution for large file sets
            val isLargeUpload = diff.bytesToUpload >= LARGE_UPLOAD_THRESHOLD_BYTES
            if (isLargeUpload) {
                try {
                    setForeground(createForegroundInfo("Starting background sync (${SizeFormat.format(diff.bytesToUpload)})..."))
                } catch (_: Exception) {
                    SyncForegroundService.start(context)
                }
            } else {
                NotificationHelper.updateProgressNotification(context, "TeleBack: Scanning files for backup...")
            }

            // Perform backup with live progress updates
            val result = syncManager.runBackupOrSync(isForceFullBackup = false) { log ->
                if (log.level == LogLevel.SYNC || log.message.startsWith("[")) {
                    NotificationHelper.updateProgressNotification(context, log.message)
                }
            }

            NotificationHelper.cancelOngoingNotification(context)
            SyncForegroundService.stop(context)

            if (result.isSuccess) {
                val manifest = result.getOrNull()
                val totalFiles = manifest?.totalFiles ?: 0
                NotificationHelper.notify(
                    context,
                    "TeleBack Sync",
                    "Task Completed: $totalFiles file(s) synced to cloud"
                )
                Result.success()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown sync error"
                NotificationHelper.notify(
                    context,
                    "TeleBack Sync Failed",
                    errorMsg
                )
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            NotificationHelper.cancelOngoingNotification(context)
            SyncForegroundService.stop(context)
            NotificationHelper.notify(
                context,
                "TeleBack Sync Failed",
                e.localizedMessage ?: "Sync error occurred"
            )
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo("TeleBack background sync in progress...")
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val notification = NotificationHelper.buildOngoingNotification(context, text)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.NOTIFICATION_ID_PROGRESS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.NOTIFICATION_ID_PROGRESS, notification)
        }
    }
}
