package com.craiglambie.invoicemaker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.craiglambie.invoicemaker.data.model.PaymentLog
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentLogDao {
    @Insert
    suspend fun insert(log: PaymentLog): Long

    @Query("SELECT * FROM payment_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PaymentLog>>

    @Query("SELECT * FROM payment_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<PaymentLog>

    @Query("SELECT * FROM payment_logs WHERE invoiceId = :invoiceId ORDER BY timestamp DESC")
    fun observeForInvoice(invoiceId: Long): Flow<List<PaymentLog>>

    @Query("SELECT * FROM payment_logs WHERE invoiceId = :invoiceId ORDER BY timestamp DESC")
    suspend fun getForInvoice(invoiceId: Long): List<PaymentLog>

    @Query("SELECT COUNT(*) FROM payment_logs")
    suspend fun count(): Int
}
