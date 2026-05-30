package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.notification.BubbleNotifier

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val body = message.messageBody ?: continue
                val sender = message.originatingAddress ?: "UNKNOWN"
                val timestamp = message.timestampMillis

                val parsed = SmsParser.parse(body, sender, timestamp)
                if (parsed != null) {
                    val category = CategoryClassifier.classify(parsed.merchant ?: "")
                    BubbleNotifier.showBubbleNotification(context, parsed, category)
                }
            }
        }
    }
}
