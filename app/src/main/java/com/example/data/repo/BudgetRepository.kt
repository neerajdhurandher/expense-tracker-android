package com.example.data.repo

import com.example.data.database.ExpenseDao
import com.example.data.database.SourceBudgetDao
import com.example.data.model.SourceBudget
import com.example.data.model.SourceSpending
import com.example.data.model.SyncStatus
import com.example.data.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetRepository(
    private val budgetDao: SourceBudgetDao,
    private val expenseDao: ExpenseDao,
    private val syncEngine: SyncEngine? = null
) {
    /** Get budgets for a month as Flow */
    fun getBudgetsByMonth(yearMonth: String): Flow<List<SourceBudget>> {
        return budgetDao.getBudgetsByMonth(yearMonth)
    }

    /** Get spending by source for a month as Flow */
    fun getSpendingBySourceForMonth(yearMonth: String): Flow<List<SourceSpending>> {
        return expenseDao.getTrackedSpendingBySourceForMonth(yearMonth)
    }

    /** Set/update budget for a source in a month */
    suspend fun setBudget(sourceName: String, yearMonth: String, amount: Double, carryOver: Boolean) {
        val existing = budgetDao.getBudget(sourceName, yearMonth)
        val budget = SourceBudget(
            sourceName = sourceName,
            yearMonth = yearMonth,
            amount = amount,
            carryOver = carryOver,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        budgetDao.upsertBudget(budget)
        tryPush { it.pushBudgetIfOnline(budget) }
    }

    /** Copy budgets from one month to another */
    suspend fun copyBudgetsFromMonth(fromYearMonth: String, toYearMonth: String) {
        val sourceBudgets = budgetDao.getBudgetsByMonthList(fromYearMonth)
        if (sourceBudgets.isEmpty()) return

        val copiedBudgets = sourceBudgets.map { budget ->
            SourceBudget(
                sourceName = budget.sourceName,
                yearMonth = toYearMonth,
                amount = budget.amount,
                carryOver = budget.carryOver,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
        }
        budgetDao.upsertBudgets(copiedBudgets)
        // Push each new budget
        for (b in copiedBudgets) {
            tryPush { it.pushBudgetIfOnline(b) }
        }
    }

    /**
     * Get carry-over amounts from the previous month.
     * Returns map of sourceName → carry-over amount (only positive values).
     */
    suspend fun getCarryOverFromPreviousMonth(currentYearMonth: String): Map<String, Double> {
        val prevYearMonth = calculatePreviousMonth(currentYearMonth)
        val carryOverBudgets = budgetDao.getCarryOverBudgets(prevYearMonth)
        if (carryOverBudgets.isEmpty()) return emptyMap()

        val prevSpending = expenseDao.getTrackedSpendingBySourceForMonthList(prevYearMonth)
        val spendingMap = prevSpending.associate { it.paymentSource to it.total }

        return carryOverBudgets.associate { budget ->
            val spent = spendingMap[budget.sourceName] ?: 0.0
            val remaining = budget.amount - spent
            budget.sourceName to maxOf(0.0, remaining)
        }.filter { it.value > 0.0 }
    }

    /** Delete a budget */
    suspend fun deleteBudget(sourceName: String, yearMonth: String) {
        val existing = budgetDao.getBudget(sourceName, yearMonth)
        if (existing != null) {
            val deleted = existing.copy(
                isDeleted = true,
                syncStatus = SyncStatus.DELETED,
                updatedAt = System.currentTimeMillis()
            )
            budgetDao.upsertBudget(deleted)
            tryPush { it.pushBudgetIfOnline(deleted) }
        } else {
            budgetDao.deleteBudget(sourceName, yearMonth)
        }
    }

    /** Calculate previous month string from "yyyy-MM" format */
    private fun calculatePreviousMonth(yearMonth: String): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(yearMonth) ?: return yearMonth
        cal.add(Calendar.MONTH, -1)
        return sdf.format(cal.time)
    }

    /** Calculate previous month string (public utility) */
    fun getPreviousMonth(yearMonth: String): String {
        return calculatePreviousMonth(yearMonth)
    }

    private fun tryPush(action: suspend (SyncEngine) -> Unit) {
        val engine = syncEngine ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try { action(engine) } catch (_: Exception) { }
        }
    }
}
