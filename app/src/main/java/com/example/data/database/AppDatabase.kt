package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.PaymentSource

@Database(entities = [Expense::class, Category::class, PaymentSource::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentSourceDao(): PaymentSourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add paymentSource column to expenses with default "UPI"
                db.execSQL("ALTER TABLE expenses ADD COLUMN paymentSource TEXT NOT NULL DEFAULT 'UPI'")

                // Create payment_sources table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS payment_sources (
                        name TEXT NOT NULL PRIMARY KEY,
                        color TEXT NOT NULL,
                        smartKeywords TEXT NOT NULL DEFAULT '',
                        isCustom INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
