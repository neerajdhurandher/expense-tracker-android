package com.example.data.repo

import com.example.data.database.ExpenseDao
import com.example.data.model.Expense
import com.example.data.model.SyncStatus
import com.example.data.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val syncEngine: SyncEngine? = null
) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    // Tracked-only flows (for totals & history)
    val allTrackedExpenses: Flow<List<Expense>> = expenseDao.getAllTrackedExpenses()

    // Untracked flows (for pending section)
    val allUntrackedExpenses: Flow<List<Expense>> = expenseDao.getAllUntrackedExpenses()

    fun getExpensesByMonth(yearMonth: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByMonth(yearMonth)
    }

    fun getTrackedExpensesByMonth(yearMonth: String): Flow<List<Expense>> {
        return expenseDao.getTrackedExpensesByMonth(yearMonth)
    }

    fun getUntrackedExpensesByMonth(yearMonth: String): Flow<List<Expense>> {
        return expenseDao.getUntrackedExpensesByMonth(yearMonth)
    }

    suspend fun insertExpense(expense: Expense) {
        val withSync = expense.copy(
            firestoreId = if (expense.firestoreId.isBlank()) UUID.randomUUID().toString() else expense.firestoreId,
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        expenseDao.insertExpense(withSync)
        tryPush { it.pushExpenseIfOnline(withSync) }
    }

    suspend fun insertExpenseAndGetId(expense: Expense): Long {
        val withSync = expense.copy(
            firestoreId = if (expense.firestoreId.isBlank()) UUID.randomUUID().toString() else expense.firestoreId,
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        val id = expenseDao.insertExpenseAndGetId(withSync)
        tryPush { it.pushExpenseIfOnline(withSync.copy(id = id)) }
        return id
    }

    suspend fun deleteExpenseById(id: Long) {
        val expense = expenseDao.getExpenseById(id) ?: return
        val deleted = expense.copy(
            isDeleted = true,
            syncStatus = SyncStatus.DELETED,
            updatedAt = System.currentTimeMillis()
        )
        expenseDao.updateExpense(deleted)
        tryPush { it.pushExpenseIfOnline(deleted) }
    }

    suspend fun updateExpense(expense: Expense) {
        val updated = expense.copy(
            syncStatus = SyncStatus.MODIFIED,
            updatedAt = System.currentTimeMillis()
        )
        expenseDao.updateExpense(updated)
        tryPush { it.pushExpenseIfOnline(updated) }
    }

    suspend fun getExpenseById(id: Long): Expense? {
        return expenseDao.getExpenseById(id)
    }

    suspend fun markAsTracked(id: Long) {
        expenseDao.markAsTracked(id)
        val expense = expenseDao.getExpenseById(id)
        if (expense != null) {
            val updated = expense.copy(syncStatus = SyncStatus.MODIFIED, updatedAt = System.currentTimeMillis())
            expenseDao.updateExpense(updated)
            tryPush { it.pushExpenseIfOnline(updated) }
        }
    }

    /** Fire-and-forget push to Firestore. Fails silently if offline. */
    private fun tryPush(action: suspend (SyncEngine) -> Unit) {
        val engine = syncEngine ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try { action(engine) } catch (_: Exception) { }
        }
    }
}
