package com.teleback.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.teleback.MainActivity
import com.teleback.R

object NotificationHelper {
    const val CHANNEL_ID_TASK = "teleback_task_channel"
    const val CHANNEL_NAME_TASK = "TeleBack Tasks"
    const val CHANNEL_ID_SYNC = "teleback_sync_channel"
    const val CHANNEL_NAME_SYNC = "TeleBack Data Sync"

    const val NOTIFICATION_ID_TASK = 2001
    const val NOTIFICATION_ID_PROGRESS = 1002

    fun notifyTaskCompleted(context: Context) {
        notify(context, "TeleBack", "Task Completed")
    }

    fun buildOngoingNotification(context: Context, text: String): android.app.Notification {
        createSyncChannel(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setContentTitle("TeleBack Cloud Sync")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateProgressNotification(context: Context, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        try {
            manager.notify(NOTIFICATION_ID_PROGRESS, buildOngoingNotification(context, text))
        } catch (_: SecurityException) {
        }
    }

    fun cancelOngoingNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.cancel(NOTIFICATION_ID_PROGRESS)
    }

    private fun createSyncChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SYNC,
                CHANNEL_NAME_SYNC,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays background sync progress"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun notify(context: Context, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_TASK,
                CHANNEL_NAME_TASK,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Task alerts"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASK)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            manager.notify(NOTIFICATION_ID_TASK, notification)
        } catch (_: SecurityException) {
            // Ignored if runtime permission was denied
        }
    }
}
