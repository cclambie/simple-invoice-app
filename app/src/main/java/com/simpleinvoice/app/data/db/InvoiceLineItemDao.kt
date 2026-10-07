package com.simpleinvoice.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simpleinvoice.app.data.model.InvoiceLineItem

@Dao
interface InvoiceLineItemDao {
    @Query("SELECT * FROM invoice_line_items WHERE invoiceId = :invoiceId")
    suspend fun getForInvoice(invoiceId: Long): List<InvoiceLineItem>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<InvoiceLineItem>): List<Long>

    @Delete
    suspend fun delete(item: InvoiceLineItem)

    @Query("DELETE FROM invoice_line_items WHERE invoiceId = :invoiceId")
    suspend fun deleteForInvoice(invoiceId: Long)
}
