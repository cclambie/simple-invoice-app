package com.simpleinvoice.app.repository

import androidx.room.withTransaction
import com.simpleinvoice.app.data.db.AppDatabase
import com.simpleinvoice.app.data.db.InvoiceDao
import com.simpleinvoice.app.data.db.InvoiceLineItemDao
import com.simpleinvoice.app.data.model.Invoice
import com.simpleinvoice.app.data.model.InvoiceLineItem
import com.simpleinvoice.app.data.model.InvoiceWithDetails
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class InvoiceRepository(
    private val db: AppDatabase,
    private val invoiceDao: InvoiceDao,
    private val lineItemDao: InvoiceLineItemDao
) {
    fun observeAllWithDetails(): Flow<List<InvoiceWithDetails>> = invoiceDao.observeAllWithDetails()

    fun observeWithDetails(id: Long): Flow<InvoiceWithDetails?> = invoiceDao.observeWithDetails(id)

    suspend fun getWithDetails(id: Long): InvoiceWithDetails? = invoiceDao.getWithDetails(id)

    suspend fun getInRange(start: LocalDate, end: LocalDate): List<InvoiceWithDetails> =
        invoiceDao.getWithDetailsInRange(start.toEpochDay(), end.toEpochDay())

    suspend fun nextInvoiceNumber(): String {
        val count = invoiceDao.count()
        return "INV-%04d".format(count + 1)
    }

    /** Saves an invoice and replaces its line items in a single transaction. */
    suspend fun saveInvoice(invoice: Invoice, lineItems: List<InvoiceLineItem>): Long =
        db.withTransaction {
            val invoiceId = if (invoice.id == 0L) {
                invoiceDao.insert(invoice)
            } else {
                invoiceDao.update(invoice)
                invoice.id
            }
            lineItemDao.deleteForInvoice(invoiceId)
            if (lineItems.isNotEmpty()) {
                lineItemDao.insertAll(lineItems.map { it.copy(id = 0, invoiceId = invoiceId) })
            }
            invoiceId
        }

    suspend fun setPaid(invoice: Invoice, paid: Boolean) {
        invoiceDao.update(invoice.copy(isPaid = paid, paidDate = if (paid) LocalDate.now() else null))
    }

    suspend fun delete(invoice: Invoice) = invoiceDao.delete(invoice)

    suspend fun hasInvoicesForClient(clientId: Long): Boolean = invoiceDao.countForClient(clientId) > 0
}
