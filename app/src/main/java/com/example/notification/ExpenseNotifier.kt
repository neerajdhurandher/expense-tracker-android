package com.example.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.model.ParsedSms

object ExpenseNotifier {
    private const val CHANNEL_ID = "expense_alerts"
    private const val CHANNEL_NAME = "Expense Alerts"
    const val NOTIFICATION_ID = 4001
    private const val TAG = "ExpenseTracker.Notifier"

    // Intent actions
    const val ACTION_QUICK_SAVE = "com.example.ACTION_QUICK_SAVE"
    const val ACTION_EDIT_EXPENSE = "com.example.ACTION_EDIT_EXPENSE"
    const val ACTION_SKIP = "com.example.ACTION_SKIP"

    @SuppressLint("MissingPermission")
    fun showExpenseNotification(context: Context, parsedSms: ParsedSms, category: String, expenseId: Long = -1L) {
        Log.d(TAG, "Showing expense notification — ₹${parsedSms.amount} at ${parsedSms.merchant} ($category) [expenseId=$expenseId]")

        // Use expenseId as notification ID for uniqueness, fallback to default
        val notificationId = if (expenseId > 0) expenseId.toInt() else NOTIFICATION_ID

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel
        val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a bank debit SMS is detected."
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }

        // Check if notifications are enabled
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.e(TAG, "❌ Notifications are DISABLED — cannot show alert")
            return
        }

        // Quick Save action — handled by QuickSaveReceiver (no app launch needed)
        val quickSaveIntent = Intent(context, QuickSaveReceiver::class.java).apply {
            action = ACTION_QUICK_SAVE
            putExtra("expenseId", expenseId)
            putExtra("amount", parsedSms.amount)
            putExtra("merchant", parsedSms.merchant)
            putExtra("sender", parsedSms.sender)
            putExtra("rawSms", parsedSms.rawSms)
            putExtra("occurredAt", parsedSms.occurredAt)
            putExtra("category", category)
            putExtra("paymentSource", parsedSms.paymentSource)
            putExtra("notificationId", notificationId)
        }
        val quickSavePendingIntent = PendingIntent.getBroadcast(
            context, (1000 + notificationId), quickSaveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Edit action — opens MainActivity with pre-filled form
        val editIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_EDIT_EXPENSE
            putExtra("expenseId", expenseId)
            putExtra("amount", parsedSms.amount)
            putExtra("merchant", parsedSms.merchant)
            putExtra("sender", parsedSms.sender)
            putExtra("rawSms", parsedSms.rawSms)
            putExtra("occurredAt", parsedSms.occurredAt)
            putExtra("category", category)
            putExtra("paymentSource", parsedSms.paymentSource)
            putExtra("notificationId", notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val editPendingIntent = PendingIntent.getActivity(
            context, (2000 + notificationId), editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Skip action — deletes the untracked expense from DB
        val skipIntent = Intent(context, QuickSaveReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra("expenseId", expenseId)
            putExtra("notificationId", notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context, (3000 + notificationId), skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build heads-up notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("₹${parsedSms.amount} at ${parsedSms.merchant}")
            .setContentText("Category: $category • via ${parsedSms.paymentSource}")
            .setSubText("Expense Detected")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(editPendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "✅ SAVE", quickSavePendingIntent)
            .addAction(android.R.drawable.ic_menu_edit, "✏️ EDIT", editPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "SKIP", skipPendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i(TAG, "✅ Heads-up notification posted (ID: $notificationId) — ₹${parsedSms.amount} at ${parsedSms.merchant}")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException — POST_NOTIFICATIONS not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to post notification", e)
        }
    }
}


