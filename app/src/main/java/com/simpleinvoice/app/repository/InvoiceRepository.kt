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

    suspend fun getAllWithDetails(): List<InvoiceWithDetails> = invoiceDao.getAllWithDetails()

    suspend fun getWithDetails(id: Long): InvoiceWithDetails? = invoiceDao.getWithDetails(id)

    suspend fun getInRange(start: LocalDate, end: LocalDate): List<InvoiceWithDetails> =
        invoiceDao.getWithDetailsInRange(start.toEpochDay(), end.toEpochDay())

    /**
     * Increments the trailing number of the most recently created invoice, keeping its prefix
     * and zero padding (e.g. "008" -> "009", "INV-0041" -> "INV-0042").
     */
    suspend fun nextInvoiceNumber(): String {
        val latest = invoiceDao.latestInvoiceNumber()
            ?: return "INV-%04d".format(invoiceDao.count() + 1)
        val match = TRAILING_NUMBER.find(latest) ?: return "$latest-1"
        val digits = match.value
        val next = (digits.toBigInteger() + java.math.BigInteger.ONE).toString().padStart(digits.length, '0')
        return latest.substring(0, match.range.first) + next + latest.substring(match.range.last + 1)
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

    private companion object {
        /** The last run of digits in an invoice number, e.g. "0041" in "INV-0041-A". */
        val TRAILING_NUMBER = Regex("""\d+(?=\D*$)""")
    }
}
