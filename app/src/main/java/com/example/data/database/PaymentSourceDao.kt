package com.example.data.database

import androidx.room.*
import com.example.data.model.PaymentSource
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentSourceDao {
    @Query("SELECT * FROM payment_sources ORDER BY name ASC")
    fun getAllPaymentSources(): Flow<List<PaymentSource>>

    @Query("SELECT * FROM payment_sources ORDER BY name ASC")
    suspend fun getAllPaymentSourcesList(): List<PaymentSource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentSource(source: PaymentSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentSources(sources: List<PaymentSource>)

    @Delete
    suspend fun deletePaymentSource(source: PaymentSource)
}

