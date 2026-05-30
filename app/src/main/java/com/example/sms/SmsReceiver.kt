package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.notification.ExpenseNotifier

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ExpenseTracker.SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "━━━ onReceive triggered ━━━")
        Log.d(TAG, "Action: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d(TAG, "SMS message count: ${messages.size}")

            for ((index, message) in messages.withIndex()) {
                val body = message.messageBody
                val sender = message.originatingAddress ?: "UNKNOWN"
                val timestamp = message.timestampMillis

                Log.d(TAG, "[$index] From: $sender")
                Log.d(TAG, "[$index] Body: $body")
                Log.d(TAG, "[$index] Timestamp: $timestamp")

                if (body == null) {
                    Log.w(TAG, "[$index] Body is null, skipping")
                    continue
                }

                val parsed = SmsParser.parse(body, sender, timestamp)
                if (parsed != null) {
                    Log.i(TAG, "[$index] ✅ EXPENSE DETECTED — Amount: ₹${parsed.amount}, Merchant: ${parsed.merchant}")
                    val category = CategoryClassifier.classify(parsed.merchant ?: "")
                    Log.i(TAG, "[$index] Category: $category")
                    try {
                        ExpenseNotifier.showExpenseNotification(context, parsed, category)
                        Log.i(TAG, "[$index] Notification sent successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "[$index] Failed to show notification", e)
                    }
                } else {
                    Log.d(TAG, "[$index] ❌ Not an expense SMS (no debit keywords or excluded)")
                }
            }
        } else {
            Log.d(TAG, "Ignoring non-SMS action: ${intent.action}")
        }

        Log.d(TAG, "━━━ onReceive completed ━━━")
    }
}
