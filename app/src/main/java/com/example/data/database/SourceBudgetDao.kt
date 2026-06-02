package com.example.data.database

import androidx.room.*
import com.example.data.model.SourceBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceBudgetDao {
    @Query("SELECT * FROM source_budgets WHERE yearMonth = :yearMonth")
    fun getBudgetsByMonth(yearMonth: String): Flow<List<SourceBudget>>

    @Query("SELECT * FROM source_budgets WHERE sourceName = :sourceName AND yearMonth = :yearMonth")
    suspend fun getBudget(sourceName: String, yearMonth: String): SourceBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: SourceBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<SourceBudget>)

    @Query("DELETE FROM source_budgets WHERE sourceName = :sourceName AND yearMonth = :yearMonth")
    suspend fun deleteBudget(sourceName: String, yearMonth: String)

    @Query("SELECT * FROM source_budgets WHERE yearMonth = :yearMonth AND carryOver = 1")
    suspend fun getCarryOverBudgets(yearMonth: String): List<SourceBudget>

    @Query("SELECT * FROM source_budgets WHERE yearMonth = :yearMonth")
    suspend fun getBudgetsByMonthList(yearMonth: String): List<SourceBudget>
}

