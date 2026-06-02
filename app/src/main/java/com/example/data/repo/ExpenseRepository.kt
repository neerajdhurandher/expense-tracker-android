package com.example.data.repo

import com.example.data.database.ExpenseDao
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

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
        expenseDao.insertExpense(expense)
    }

    suspend fun insertExpenseAndGetId(expense: Expense): Long {
        return expenseDao.insertExpenseAndGetId(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun getExpenseById(id: Long): Expense? {
        return expenseDao.getExpenseById(id)
    }

    suspend fun markAsTracked(id: Long) {
        expenseDao.markAsTracked(id)
    }
}
