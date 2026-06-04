package com.example.data.database

import androidx.room.*
import com.example.data.model.PaymentSource
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentSourceDao {
    @Query("SELECT * FROM payment_sources WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllPaymentSources(): Flow<List<PaymentSource>>

    @Query("SELECT * FROM payment_sources WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllPaymentSourcesList(): List<PaymentSource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentSource(source: PaymentSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentSources(sources: List<PaymentSource>)

    @Delete
    suspend fun deletePaymentSource(source: PaymentSource)

    // ── Sync queries ──

    @Query("SELECT * FROM payment_sources WHERE syncStatus != 0")
    suspend fun getUnsyncedPaymentSources(): List<PaymentSource>

    @Query("SELECT * FROM payment_sources WHERE syncStatus = 0 AND isDeleted = 0")
    suspend fun getSyncedPaymentSources(): List<PaymentSource>

    @Query("UPDATE payment_sources SET syncStatus = :status WHERE name = :name")
    suspend fun updateSyncStatus(name: String, status: Int)

    @Query("SELECT * FROM payment_sources WHERE name = :name LIMIT 1")
    suspend fun getPaymentSourceByName(name: String): PaymentSource?

    @Query("DELETE FROM payment_sources WHERE name = :name")
    suspend fun hardDeleteByName(name: String)
}
