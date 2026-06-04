package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Expense
import com.example.data.model.SourceSpending
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY occurredAt DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE yearMonth = :yearMonth AND isDeleted = 0 ORDER BY occurredAt DESC")
    fun getExpensesByMonth(yearMonth: String): Flow<List<Expense>>

    // Tracked-only queries (for totals & history display)
    @Query("SELECT * FROM expenses WHERE isTracked = 1 AND isDeleted = 0 ORDER BY occurredAt DESC")
    fun getAllTrackedExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE yearMonth = :yearMonth AND isTracked = 1 AND isDeleted = 0 ORDER BY occurredAt DESC")
    fun getTrackedExpensesByMonth(yearMonth: String): Flow<List<Expense>>

    // Untracked queries (for pending section)
    @Query("SELECT * FROM expenses WHERE isTracked = 0 AND isDeleted = 0 ORDER BY occurredAt DESC")
    fun getAllUntrackedExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE yearMonth = :yearMonth AND isTracked = 0 AND isDeleted = 0 ORDER BY occurredAt DESC")
    fun getUntrackedExpensesByMonth(yearMonth: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    // Insert and return the auto-generated ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseAndGetId(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    /** Hard delete — permanently removes from Room (used after Firestore confirms deletion) */
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun hardDeleteById(id: Long)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    // Mark an untracked expense as tracked
    @Query("UPDATE expenses SET isTracked = 1 WHERE id = :id")
    suspend fun markAsTracked(id: Long)

    // Get total tracked spending per source for a month (for budget calculation)
    @Query("""
        SELECT paymentSource, SUM(amount) as total 
        FROM expenses 
        WHERE yearMonth = :yearMonth AND isTracked = 1 AND isDeleted = 0
        GROUP BY paymentSource
    """)
    fun getTrackedSpendingBySourceForMonth(yearMonth: String): Flow<List<SourceSpending>>

    // Suspend version for one-shot queries (e.g. carry-over calculation)
    @Query("""
        SELECT paymentSource, SUM(amount) as total 
        FROM expenses 
        WHERE yearMonth = :yearMonth AND isTracked = 1 AND isDeleted = 0
        GROUP BY paymentSource
    """)
    suspend fun getTrackedSpendingBySourceForMonthList(yearMonth: String): List<SourceSpending>

    // ═══════════════════════════════════════════════════════════════
    // Sync queries
    // ═══════════════════════════════════════════════════════════════

    @Query("SELECT * FROM expenses WHERE syncStatus != 0")
    suspend fun getUnsyncedExpenses(): List<Expense>

    @Query("SELECT * FROM expenses WHERE syncStatus = 0 AND isDeleted = 0")
    suspend fun getSyncedExpenses(): List<Expense>

    @Query("UPDATE expenses SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: Int)

    @Query("UPDATE expenses SET syncStatus = :status WHERE firestoreId = :firestoreId")
    suspend fun updateSyncStatusByFirestoreId(firestoreId: String, status: Int)

    @Query("SELECT * FROM expenses WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getExpenseByFirestoreId(firestoreId: String): Expense?

    @Query("DELETE FROM expenses WHERE firestoreId = :firestoreId")
    suspend fun hardDeleteByFirestoreId(firestoreId: String)
}
