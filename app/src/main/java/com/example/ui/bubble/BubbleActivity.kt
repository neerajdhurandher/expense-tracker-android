package com.example.ui.bubble

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.ExpenseApp
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.ui.components.ExpenseFormSheet
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BubbleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the floating bubble layout fit comfortably as embedded
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

        val amount = intent.getDoubleExtra("amount", 0.0)
        val merchant = intent.getStringExtra("merchant") ?: "Unknown"
        val sender = intent.getStringExtra("sender") ?: "SMS"
        val rawSms = intent.getStringExtra("rawSms") ?: ""
        val occurredAt = intent.getLongExtra("occurredAt", System.currentTimeMillis())
        val initialCategory = intent.getStringExtra("category") ?: "Other"

        val app = application as ExpenseApp
        val expenseRepo = app.expenseRepository
        val categoryRepo = app.categoryRepository

        setContent {
            MyApplicationTheme {
                val categories by categoryRepo.allCategories.collectAsState(initial = emptyList())

                Scaffold(containerColor = DarkBg) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg)
                            .padding(innerPadding)
                    ) {
                        ExpenseFormSheet(
                            initialName = merchant,
                            initialAmount = if (amount > 0.0) amount else null,
                            initialCategory = initialCategory,
                            categories = categories,
                            onSave = { name, finalAmount, category ->
                                lifecycleScope.launch {
                                    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
                                    val yearMonthStr = sdf.format(Date(occurredAt))

                                    val expense = Expense(
                                        name = name,
                                        amount = finalAmount,
                                        category = category,
                                        source = "sms",
                                        rawSms = rawSms,
                                        sender = sender,
                                        occurredAt = occurredAt,
                                        createdAt = System.currentTimeMillis(),
                                        yearMonth = yearMonthStr
                                    )
                                    expenseRepo.insertExpense(expense)

                                    // Clear notification
                                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    notificationManager.cancel(4001)

                                    Toast.makeText(this@BubbleActivity, "Expense saved: ₹$finalAmount", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            },
                            onDismiss = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}
