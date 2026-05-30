package com.example.data.repo

import com.example.data.database.CategoryDao
import com.example.data.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Note: using first() on Room flow retrieves the current snapshot
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
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}
