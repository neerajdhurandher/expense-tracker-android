package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.Expense
import com.example.data.model.SyncStatus
import com.example.notification.ExpenseNotifier
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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

                    // Detect payment source using DB-backed smartKeywords
                    val db = AppDatabase.getDatabase(context)
                    val paymentSource = try {
                        val sources = runBlocking { db.paymentSourceDao().getAllPaymentSourcesList() }
                        val detected = SmsParser.detectPaymentSource(body, sources)
                        Log.i(TAG, "[$index] Payment Source: $detected")
                        detected
                    } catch (e: Exception) {
                        Log.w(TAG, "[$index] Failed to detect payment source, defaulting to UPI", e)
                        "UPI"
                    }

                    val parsedWithSource = parsed.copy(paymentSource = paymentSource)

                    // Insert untracked expense to DB immediately
                    val expenseId = try {
                        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
                        val yearMonthStr = sdf.format(Date(parsed.occurredAt))

                        val expense = Expense(
                            name = parsed.merchant ?: sender,
                            amount = parsed.amount,
                            category = category,
                            source = "sms",
                            rawSms = parsed.rawSms,
                            sender = parsed.sender,
                            occurredAt = parsed.occurredAt,
                            createdAt = System.currentTimeMillis(),
                            yearMonth = yearMonthStr,
                            paymentSource = paymentSource,
                            isTracked = false, // Untracked until user acts
                            firestoreId = UUID.randomUUID().toString(),
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = SyncStatus.PENDING
                        )
                        val id = runBlocking { db.expenseDao().insertExpenseAndGetId(expense) }
                        Log.i(TAG, "[$index] Expense saved as untracked — ID: $id")
                        id
                    } catch (e: Exception) {
                        Log.e(TAG, "[$index] Failed to save untracked expense", e)
                        -1L
                    }

                    try {
                        ExpenseNotifier.showExpenseNotification(context, parsedWithSource, category, expenseId)
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
