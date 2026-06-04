package com.example.data.database

import androidx.room.*
import com.example.data.model.SourceBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceBudgetDao {
    @Query("SELECT * FROM source_budgets WHERE yearMonth = :yearMonth AND isDeleted = 0")
    fun getBudgetsByMonth(yearMonth: String): Flow<List<SourceBudget>>

    @Query("SELECT * FROM source_budgets WHERE sourceName = :sourceName AND yearMonth = :yearMonth AND isDeleted = 0")
    suspend fun getBudget(sourceName: String, yearMonth: String): SourceBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: SourceBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<SourceBudget>)

    @Query("DELETE FROM source_budgets WHERE sourceName = :sourceName AND yearMonth = :yearMonth")
    suspend fun deleteBudget(sourceName: String, yearMonth: String)

    @Query("SELECT * FROM source_budgets WHERE yearMonth = :yearMonth AND carryOver = 1 AND isDeleted = 0")
    suspend fun getCarryOverBudgets(yearMonth: String): List<SourceBudget>

    @Query("SELECT * FROM source_budgets WHERE yearMonth = :yearMonth AND isDeleted = 0")
    suspend fun getBudgetsByMonthList(yearMonth: String): List<SourceBudget>

    // ── Sync queries ──

    @Query("SELECT * FROM source_budgets WHERE syncStatus != 0")
    suspend fun getUnsyncedBudgets(): List<SourceBudget>

    @Query("SELECT * FROM source_budgets WHERE syncStatus = 0 AND isDeleted = 0")
    suspend fun getSyncedBudgets(): List<SourceBudget>

    @Query("UPDATE source_budgets SET syncStatus = :status WHERE sourceName = :sourceName AND yearMonth = :yearMonth")
    suspend fun updateSyncStatus(sourceName: String, yearMonth: String, status: Int)

    @Query("DELETE FROM source_budgets WHERE sourceName = :sourceName AND yearMonth = :yearMonth")
    suspend fun hardDelete(sourceName: String, yearMonth: String)
}
