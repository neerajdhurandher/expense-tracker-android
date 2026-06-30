package com.example

import android.app.Application
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.repo.AuthRepository
import com.example.data.repo.BudgetRepository
import com.example.data.repo.CategoryRepository
import com.example.data.repo.ExpenseRepository
import com.example.data.repo.PaymentSourceRepository
import com.example.data.sync.ConnectivityMonitor
import com.example.data.sync.SyncEngine
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExpenseApp : Application() {
    companion object {
        private const val TAG = "ExpenseTracker.Heartbeat"
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set
    lateinit var expenseRepository: ExpenseRepository
        private set
    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var paymentSourceRepository: PaymentSourceRepository
        private set
    lateinit var budgetRepository: BudgetRepository
        private set
    lateinit var syncEngine: SyncEngine
        private set
    lateinit var connectivityMonitor: ConnectivityMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "══════ ExpenseApp started ══════")

        database = AppDatabase.getDatabase(this)
        authRepository = AuthRepository(this)

        // Initialize sync infrastructure
        connectivityMonitor = ConnectivityMonitor(this)
        connectivityMonitor.startMonitoring()

        syncEngine = SyncEngine(
            firestore = FirebaseFirestore.getInstance(),
            expenseDao = database.expenseDao(),
            categoryDao = database.categoryDao(),
            paymentSourceDao = database.paymentSourceDao(),
            sourceBudgetDao = database.sourceBudgetDao()
        )

        // Initialize repositories with SyncEngine
        expenseRepository = ExpenseRepository(database.expenseDao(), syncEngine)
        categoryRepository = CategoryRepository(database.categoryDao(), syncEngine)
        paymentSourceRepository = PaymentSourceRepository(database.paymentSourceDao(), syncEngine)
        budgetRepository = BudgetRepository(database.sourceBudgetDao(), database.expenseDao(), syncEngine)

        Log.i(TAG, "Repositories initialized with SyncEngine. SmsReceiver registered via manifest.")

        // Auto-sync when connectivity is restored
        appScope.launch {
            connectivityMonitor.isOnline.collect { online ->
                if (online && authRepository.currentUser.value != null) {
                    Log.i(TAG, "Network restored — triggering sync")
                    syncEngine.performFullSync()
                }
            }
        }

        // Heartbeat logger — logs every 1 minute to confirm process is alive
        startHeartbeatLogger()
    }

    private fun startHeartbeatLogger() {
        appScope.launch {
            var tick = 0L
            while (true) {
                tick++
                Log.d(TAG, "💓 Heartbeat #$tick — SmsReceiver is active (process alive for ${tick} min)")
                delay(60_000L) // 1 minute
            }
        }
    }
}
