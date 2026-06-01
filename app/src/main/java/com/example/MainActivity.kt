package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.Expense
import com.example.notification.ExpenseNotifier
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.SignInScreen
import com.example.ui.categories.ManageCategoriesScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.graph.GraphScreen
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ExpenseApp
        val authViewModel: AuthViewModel by viewModels { AuthViewModel.Factory(app.authRepository) }
        val homeViewModel: HomeViewModel by viewModels { HomeViewModel.Factory(app.expenseRepository, app.categoryRepository, app.paymentSourceRepository) }

        // Handle standard quick save notification intent tasks
        handleNotificationIntents(intent, app)

        setContent {
            MyApplicationTheme {
                val currentUser by authViewModel.currentUser.collectAsState()
                val navController = rememberNavController()

                // Check SMS and Notification permissions at entry
                PermissionRequestWrapper()

                LaunchedEffect(currentUser) {
                    if (currentUser == null) {
                        navController.navigate("signin") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (currentUser == null) "signin" else "home",
                        enterTransition = { fadeIn(animationSpec = tween(250)) },
                        exitTransition = { fadeOut(animationSpec = tween(250)) }
                    ) {
                        composable("signin") {
                            SignInScreen(
                                viewModel = authViewModel,
                                onSignInSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("signin") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            val user = currentUser
                            if (user != null) {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    userName = user.displayName,
                                    userEmail = user.email,
                                    onNavigateToGraph = { navController.navigate("graph") },
                                    onNavigateToCategories = { navController.navigate("categories") },
                                    onSignOut = {
                                        authViewModel.signOut {
                                            navController.navigate("signin") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = AccentYellow)
                                }
                            }
                        }

                        composable("graph") {
                            GraphScreen(
                                viewModel = homeViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("categories") {
                            ManageCategoriesScreen(
                                viewModel = homeViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleNotificationIntents(intent: android.content.Intent?, app: ExpenseApp) {
        if (intent == null) return
        val action = intent.action

        if (action == "QUICK_ADD_EXPENSE" || action == ExpenseNotifier.ACTION_QUICK_SAVE) {
            val amount = intent.getDoubleExtra("amount", 0.0)
            val merchant = intent.getStringExtra("merchant") ?: "Unknown"
            val sender = intent.getStringExtra("sender") ?: "SMS"
            val rawSms = intent.getStringExtra("rawSms") ?: ""
            val occurredAt = intent.getLongExtra("occurredAt", System.currentTimeMillis())
            val category = intent.getStringExtra("category") ?: "Other"
            val paymentSource = intent.getStringExtra("paymentSource") ?: "UPI"

            if (amount > 0.0) {
                lifecycleScope.launch {
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
                        yearMonth = yearMonthStr,
                        paymentSource = paymentSource
                    )
                    app.expenseRepository.insertExpense(expense)
                    Toast.makeText(this@MainActivity, "Saved: ₹$amount at $merchant", Toast.LENGTH_LONG).show()

                    // Cancel notification
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.cancel(ExpenseNotifier.NOTIFICATION_ID)
                }
            }
        } else if (action == ExpenseNotifier.ACTION_EDIT_EXPENSE) {
            // Extract SMS data and pass to HomeViewModel for pre-filled edit form
            val amount = intent.getDoubleExtra("amount", 0.0)
            val merchant = intent.getStringExtra("merchant") ?: "Unknown"
            val sender = intent.getStringExtra("sender") ?: "SMS"
            val rawSms = intent.getStringExtra("rawSms") ?: ""
            val occurredAt = intent.getLongExtra("occurredAt", System.currentTimeMillis())
            val category = intent.getStringExtra("category") ?: "Other"
            val paymentSource = intent.getStringExtra("paymentSource") ?: "UPI"

            if (amount > 0.0) {
                val homeViewModel: HomeViewModel by viewModels {
                    HomeViewModel.Factory(app.expenseRepository, app.categoryRepository, app.paymentSourceRepository)
                }
                homeViewModel.setPendingSmsExpense(merchant, amount, category, rawSms, sender, occurredAt, paymentSource)

                // Cancel notification
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(ExpenseNotifier.NOTIFICATION_ID)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestWrapper() {
    val smsPermissionState = rememberPermissionState(android.Manifest.permission.RECEIVE_SMS)
    val readSmsPermissionState = rememberPermissionState(android.Manifest.permission.READ_SMS)
    val notificationPermissionState = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)

    LaunchedEffect(Unit) {
        if (!smsPermissionState.status.isGranted) {
            smsPermissionState.launchPermissionRequest()
        }
        if (!readSmsPermissionState.status.isGranted) {
            readSmsPermissionState.launchPermissionRequest()
        }
        if (!notificationPermissionState.status.isGranted) {
            notificationPermissionState.launchPermissionRequest()
        }
    }
}
