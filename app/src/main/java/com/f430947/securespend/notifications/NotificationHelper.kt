package com.f430947.securespend.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * Helper object for creating the notification channel and sending budget-limit alerts.
 * Uses NotificationManager to inform the user when daily spending exceeds the configured limit (L5).
 */
object NotificationHelper {

    private const val CHANNEL_ID = "budget_alerts"
    private const val CHANNEL_NAME = "Budget Alerts"
    private const val NOTIFICATION_ID = 1001

    /** Must be called once (e.g. in MainActivity.onCreate) to register the notification channel. */
    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when the daily spending limit is exceeded"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /** Posts a notification informing the user that their daily budget limit has been exceeded. */
    fun sendBudgetLimitNotification(context: Context, total: Double, limit: Double) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Daily Budget Limit Exceeded")
            .setContentText("You have spent £%.2f today (limit: £%.2f)".format(total, limit))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
