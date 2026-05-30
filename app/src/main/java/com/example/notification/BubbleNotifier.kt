package com.example.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.data.model.ParsedSms
import com.example.ui.bubble.BubbleActivity

object BubbleNotifier {
    private const val CHANNEL_ID = "expense_sms_notifications"
    private const val CHANNEL_NAME = "Expense Alerts"
    private const val NOTIFICATION_ID = 4001
    private const val TAG = "ExpenseTracker.BubbleNotifier"

    @SuppressLint("MissingPermission")
    fun showBubbleNotification(context: Context, parsedSms: ParsedSms, category: String) {
        Log.d(TAG, "showBubbleNotification called — amount: ${parsedSms.amount}, merchant: ${parsedSms.merchant}")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create or update notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channel if bubbles were not enabled (fixes stale channel config)
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !existingChannel.canBubble()) {
                Log.w(TAG, "Existing channel has bubbles disabled, deleting and recreating")
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a new banking SMS is detected to capture expenses."
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created/updated: $CHANNEL_ID")
        }

        // Check if notifications are enabled
        val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        Log.d(TAG, "Notifications enabled: $areNotificationsEnabled")
        if (!areNotificationsEnabled) {
            Log.e(TAG, "❌ Notifications are DISABLED for this app — bubble cannot show")
            return
        }

        // Target Activity for Bubble
        val targetIntent = Intent(context, BubbleActivity::class.java).apply {
            putExtra("amount", parsedSms.amount)
            putExtra("merchant", parsedSms.merchant)
            putExtra("sender", parsedSms.sender)
            putExtra("rawSms", parsedSms.rawSms)
            putExtra("occurredAt", parsedSms.occurredAt)
            putExtra("category", category)
        }
        
        val bubbleIntent = PendingIntent.getActivity(
            context,
            0,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Shortcut setup (Required for Android 11+ Bubbles)
        val contactId = "merchant_${parsedSms.merchant?.replace(" ", "_") ?: "unknown"}"
        val person = Person.Builder()
            .setName(parsedSms.merchant ?: "Expense Scanner")
            .setBot(true)
            .build()

        val shortcut = ShortcutInfoCompat.Builder(context, contactId)
            .setShortLabel(parsedSms.merchant ?: "Expense")
            .setLongLabel("Capture transaction from ${parsedSms.merchant}")
            .setPerson(person)
            .setIntent(Intent(context, MainActivity::class.java).apply { action = Intent.ACTION_VIEW })
            .setLongLived(true)
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        // Bubble Metadata
        val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(bubbleIntent, 
            IconCompat.createWithResource(context, android.R.drawable.stat_notify_chat)
        )
            .setDesiredHeight(480)
            .setAutoExpandBubble(true)
            .setSuppressNotification(true)
            .build()

        // Content Intents
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("amount", parsedSms.amount)
            putExtra("merchant", parsedSms.merchant)
            putExtra("sender", parsedSms.sender)
            putExtra("rawSms", parsedSms.rawSms)
            putExtra("occurredAt", parsedSms.occurredAt)
            putExtra("category", category)
            action = "OPEN_ADD_EXPENSE"
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            1,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick add action intent
        val quickAddIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("amount", parsedSms.amount)
            putExtra("merchant", parsedSms.merchant)
            putExtra("sender", parsedSms.sender)
            putExtra("rawSms", parsedSms.rawSms)
            putExtra("occurredAt", parsedSms.occurredAt)
            putExtra("category", category)
            action = "QUICK_ADD_EXPENSE"
        }
        val quickAddPendingIntent = PendingIntent.getActivity(
            context,
            2,
            quickAddIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification builder
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("New Expense Detected: ₹${parsedSms.amount}")
            .setContentText("Spent at ${parsedSms.merchant}. Tap to capture or dismiss.")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(contactId)
            .setBubbleMetadata(bubbleMetadata)
            .setContentIntent(contentPendingIntent)
            .setStyle(NotificationCompat.MessagingStyle(person)
                .addMessage("Detected dynamic transaction of ₹${parsedSms.amount} at ${parsedSms.merchant}", System.currentTimeMillis(), person)
            )
            .addAction(
                android.R.drawable.ic_menu_add,
                "QUICK SAVE",
                quickAddPendingIntent
            )
            .setAutoCancel(true)

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
            Log.i(TAG, "✅ Notification posted successfully (ID: $NOTIFICATION_ID)")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException — POST_NOTIFICATIONS permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to post notification", e)
        }
    }
}
