package com.example.data.repo

import com.example.data.database.CategoryDao
import com.example.data.model.Category
import com.example.data.model.SyncStatus
import com.example.data.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val syncEngine: SyncEngine? = null
) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val current = categoryDao.getAllCategories().first()
                if (current.isEmpty()) {
                    val defaults = listOf(
                        Category("Food", "#FF6B6B"),
                        Category("Travel", "#4DABF7"),
                        Category("Groceries", "#51CF66"),
                        Category("Shopping", "#FCC419"),
                        Category("Bills", "#BE4BDB"),
                        Category("Entertainment", "#FF922B"),
                        Category("Health", "#20C997"),
                        Category("Other", "#ADB5BD")
                    )
                    categoryDao.insertCategories(defaults)
                }
            } catch (e: Exception) {
                // Handle or log gracefully
            }
        }
    }

    suspend fun insertCategory(category: Category) {
        val withSync = category.copy(
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.insertCategory(withSync)
        tryPush { it.pushCategoryIfOnline(withSync) }
    }

    suspend fun deleteCategory(category: Category) {
        val deleted = category.copy(
            isDeleted = true,
            syncStatus = SyncStatus.DELETED,
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.insertCategory(deleted) // Upsert with soft delete flag
        tryPush { it.pushCategoryIfOnline(deleted) }
    }

    private fun tryPush(action: suspend (SyncEngine) -> Unit) {
        val engine = syncEngine ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try { action(engine) } catch (_: Exception) { }
        }
    }
}
