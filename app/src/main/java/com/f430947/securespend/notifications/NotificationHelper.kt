package com.f430947.securespend.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import java.time.LocalDate

/**
 * Helper object for creating the notification channel and sending budget-limit alerts.
 * Uses NotificationManager to inform the user when daily spending exceeds the configured limit (L5).
 *
 * Notifications are de-duplicated so at most one budget alert fires per calendar day,
 * preventing spam when the user adds several expenses in quick succession.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "budget_alerts"
    private const val CHANNEL_NAME = "Budget Alerts"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_LAST_NOTIF_DATE = "last_budget_notification_date"

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

    /**
     * Posts a notification informing the user that their daily budget limit has been exceeded.
     * At most one notification is sent per calendar day to avoid repeated alerts.
     */
    fun sendBudgetLimitNotification(context: Context, total: Double, limit: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (prefs.getString(KEY_LAST_NOTIF_DATE, null) == today) return // already notified today

        prefs.edit().putString(KEY_LAST_NOTIF_DATE, today).apply()

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
