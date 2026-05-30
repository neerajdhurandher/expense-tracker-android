package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.data.database.AppDatabase
import com.example.data.model.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuickSaveReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ExpenseTracker.QuickSave"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ExpenseNotifier.ACTION_QUICK_SAVE -> handleQuickSave(context, intent)
            ExpenseNotifier.ACTION_SKIP -> handleSkip(context)
            else -> Log.d(TAG, "Unknown action: ${intent.action}")
        }
    }

    private fun handleQuickSave(context: Context, intent: Intent) {
        val amount = intent.getDoubleExtra("amount", 0.0)
        val merchant = intent.getStringExtra("merchant") ?: "Unknown"
        val sender = intent.getStringExtra("sender") ?: "SMS"
        val rawSms = intent.getStringExtra("rawSms") ?: ""
        val occurredAt = intent.getLongExtra("occurredAt", System.currentTimeMillis())
        val category = intent.getStringExtra("category") ?: "Other"

        Log.i(TAG, "Quick Save triggered — ₹$amount at $merchant ($category)")

        if (amount <= 0.0) {
            Log.w(TAG, "Invalid amount, skipping save")
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val yearMonthStr = sdf.format(Date(occurredAt))

        val expense = Expense(
            name = merchant,
            amount = amount,
            category = category,
            source = "sms",
            rawSms = rawSms,
            sender = sender,
            occurredAt = occurredAt,
            createdAt = System.currentTimeMillis(),
            yearMonth = yearMonthStr
        )

        // Save in background using a coroutine
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.expenseDao().insertExpense(expense)
                Log.i(TAG, "✅ Expense saved — ₹$amount at $merchant")

                // Show toast on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Saved: ₹$amount at $merchant", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save expense", e)
            } finally {
                // Dismiss the notification
                dismissNotification(context)
                pendingResult.finish()
            }
        }
    }

    private fun handleSkip(context: Context) {
        Log.d(TAG, "Skip action — dismissing notification")
        dismissNotification(context)
    }

    private fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ExpenseNotifier.NOTIFICATION_ID)
    }
}

