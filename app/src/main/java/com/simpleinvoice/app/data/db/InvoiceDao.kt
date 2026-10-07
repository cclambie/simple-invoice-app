package com.simpleinvoice.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.simpleinvoice.app.data.model.Invoice
import com.simpleinvoice.app.data.model.InvoiceWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, id DESC")
    fun observeAllWithDetails(): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, id DESC")
    suspend fun getAllWithDetails(): List<InvoiceWithDetails>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeWithDetails(id: Long): Flow<InvoiceWithDetails?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getWithDetails(id: Long): InvoiceWithDetails?

    @Transaction
    @Query(
        "SELECT * FROM invoices WHERE issueDate >= :startEpochDay AND issueDate <= :endEpochDay " +
            "ORDER BY issueDate ASC, id ASC"
    )
    suspend fun getWithDetailsInRange(startEpochDay: Long, endEpochDay: Long): List<InvoiceWithDetails>

    @Query("SELECT COUNT(*) FROM invoices WHERE clientId = :clientId")
    suspend fun countForClient(clientId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(invoice: Invoice): Long

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun count(): Int

    @Query("SELECT invoiceNumber FROM invoices ORDER BY id DESC LIMIT 1")
    suspend fun latestInvoiceNumber(): String?
}
