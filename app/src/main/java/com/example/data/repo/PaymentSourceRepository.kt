package com.example.data.repo

import com.example.data.database.PaymentSourceDao
import com.example.data.model.PaymentSource
import com.example.data.model.SyncStatus
import com.example.data.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaymentSourceRepository(
    private val dao: PaymentSourceDao,
    private val syncEngine: SyncEngine? = null
) {

    val allPaymentSources: Flow<List<PaymentSource>> = dao.getAllPaymentSources()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val current = dao.getAllPaymentSources().first()
                if (current.isEmpty()) {
                    val defaults = listOf(
                        PaymentSource("Cash", "#51CF66", smartKeywords = ""),
                        PaymentSource("UPI", "#4DABF7", smartKeywords = "upi,vpa,phonepe,gpay,bhim,paytm,razorpay"),
                        PaymentSource("Credit Card", "#FF6B6B", smartKeywords = "credit card,cc ,creditcard")
                    )
                    dao.insertPaymentSources(defaults)
                }
            } catch (e: Exception) {
                // Handle or log gracefully
            }
        }
    }

    suspend fun insertPaymentSource(source: PaymentSource) {
        val withSync = source.copy(
            syncStatus = SyncStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertPaymentSource(withSync)
        tryPush { it.pushPaymentSourceIfOnline(withSync) }
    }

    suspend fun deletePaymentSource(source: PaymentSource) {
        val deleted = source.copy(
            isDeleted = true,
            syncStatus = SyncStatus.DELETED,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertPaymentSource(deleted) // Upsert with soft delete flag
        tryPush { it.pushPaymentSourceIfOnline(deleted) }
    }

    private fun tryPush(action: suspend (SyncEngine) -> Unit) {
        val engine = syncEngine ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try { action(engine) } catch (_: Exception) { }
        }
    }
}
