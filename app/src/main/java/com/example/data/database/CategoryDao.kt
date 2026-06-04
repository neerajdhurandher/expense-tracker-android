package com.example.data.database

import androidx.room.*
import com.example.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Delete
    suspend fun deleteCategory(category: Category)

    // ── Sync queries ──

    @Query("SELECT * FROM categories WHERE syncStatus != 0")
    suspend fun getUnsyncedCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE syncStatus = 0 AND isDeleted = 0")
    suspend fun getSyncedCategories(): List<Category>

    @Query("UPDATE categories SET syncStatus = :status WHERE name = :name")
    suspend fun updateSyncStatus(name: String, status: Int)

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun hardDeleteByName(name: String)
}
