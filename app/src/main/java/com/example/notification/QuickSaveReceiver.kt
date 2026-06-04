package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.data.database.AppDatabase
import com.example.data.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickSaveReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ExpenseTracker.QuickSave"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ExpenseNotifier.ACTION_QUICK_SAVE -> handleQuickSave(context, intent)
            ExpenseNotifier.ACTION_SKIP -> handleSkip(context, intent)
            else -> Log.d(TAG, "Unknown action: ${intent.action}")
        }
    }

    private fun handleQuickSave(context: Context, intent: Intent) {
        val expenseId = intent.getLongExtra("expenseId", -1L)
        val notificationId = intent.getIntExtra("notificationId", ExpenseNotifier.NOTIFICATION_ID)

        Log.i(TAG, "Quick Save triggered — expenseId: $expenseId")

        if (expenseId <= 0L) {
            Log.w(TAG, "Invalid expenseId, cannot quick save")
            dismissNotification(context, notificationId)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.expenseDao().markAsTracked(expenseId)
                db.expenseDao().updateSyncStatus(expenseId, SyncStatus.MODIFIED)
                val expense = db.expenseDao().getExpenseById(expenseId)
                Log.i(TAG, "✅ Expense #$expenseId marked as tracked — ₹${expense?.amount} at ${expense?.name}")

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Saved: ₹${expense?.amount} at ${expense?.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to track expense #$expenseId", e)
            } finally {
                dismissNotification(context, notificationId)
                pendingResult.finish()
            }
        }
    }

    private fun handleSkip(context: Context, intent: Intent) {
        val expenseId = intent.getLongExtra("expenseId", -1L)
        val notificationId = intent.getIntExtra("notificationId", ExpenseNotifier.NOTIFICATION_ID)

        Log.d(TAG, "Skip action — expenseId: $expenseId")

        if (expenseId <= 0L) {
            dismissNotification(context, notificationId)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val expense = db.expenseDao().getExpenseById(expenseId)
                if (expense != null) {
                    val deleted = expense.copy(
                        isDeleted = true,
                        syncStatus = SyncStatus.DELETED,
                        updatedAt = System.currentTimeMillis()
                    )
                    db.expenseDao().updateExpense(deleted)
                }
                Log.d(TAG, "Skipped & soft-deleted untracked expense #$expenseId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete skipped expense #$expenseId", e)
            } finally {
                dismissNotification(context, notificationId)
                pendingResult.finish()
            }
        }
    }

    private fun dismissNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}

