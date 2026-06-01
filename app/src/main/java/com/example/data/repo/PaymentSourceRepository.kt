package com.example.data.repo

import com.example.data.database.PaymentSourceDao
import com.example.data.model.PaymentSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaymentSourceRepository(private val dao: PaymentSourceDao) {

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
        dao.insertPaymentSource(source)
    }

    suspend fun deletePaymentSource(source: PaymentSource) {
        dao.deletePaymentSource(source)
    }
}

