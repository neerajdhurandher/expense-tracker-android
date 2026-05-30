package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repo.AuthRepository
import com.example.data.repo.CategoryRepository
import com.example.data.repo.ExpenseRepository

class ExpenseApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var expenseRepository: ExpenseRepository
        private set
    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        expenseRepository = ExpenseRepository(database.expenseDao())
        categoryRepository = CategoryRepository(database.categoryDao())
        authRepository = AuthRepository(this)
    }
}
