package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE yearMonth = :yearMonth ORDER BY occurredAt DESC")
    fun getExpensesByMonth(yearMonth: String): Flow<List<Expense>>

    // Tracked-only queries (for totals & history display)
    @Query("SELECT * FROM expenses WHERE isTracked = 1 ORDER BY occurredAt DESC")
    fun getAllTrackedExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE yearMonth = :yearMonth AND isTracked = 1 ORDER BY occurredAt DESC")
    fun getTrackedExpensesByMonth(yearMonth: String): Flow<List<Expense>>

    // Untracked queries (for pending section)
    @Query("SELECT * FROM expenses WHERE isTracked = 0 ORDER BY occurredAt DESC")
    fun getAllUntrackedExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE yearMonth = :yearMonth AND isTracked = 0 ORDER BY occurredAt DESC")
    fun getUntrackedExpensesByMonth(yearMonth: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    // Insert and return the auto-generated ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseAndGetId(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    // Mark an untracked expense as tracked
    @Query("UPDATE expenses SET isTracked = 1 WHERE id = :id")
    suspend fun markAsTracked(id: Long)
}
