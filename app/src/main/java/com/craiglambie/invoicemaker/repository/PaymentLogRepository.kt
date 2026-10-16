package com.craiglambie.invoicemaker.repository

import androidx.room.withTransaction
import com.craiglambie.invoicemaker.data.db.AppDatabase
import com.craiglambie.invoicemaker.data.db.InvoiceDao
import com.craiglambie.invoicemaker.data.db.PaymentLogDao
import com.craiglambie.invoicemaker.data.model.Invoice
import com.craiglambie.invoicemaker.data.model.PaymentLog
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class PaymentLogRepository(
    private val db: AppDatabase,
    private val paymentLogDao: PaymentLogDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceRepository: InvoiceRepository
) {
    fun observeAll(): Flow<List<PaymentLog>> = paymentLogDao.observeAll()

    suspend fun getAll(): List<PaymentLog> = paymentLogDao.getAll()

    fun observeForInvoice(invoiceId: Long): Flow<List<PaymentLog>> = paymentLogDao.observeForInvoice(invoiceId)

    suspend fun getForInvoice(invoiceId: Long): List<PaymentLog> = paymentLogDao.getForInvoice(invoiceId)

    suspend fun recordPayment(log: PaymentLog) = paymentLogDao.insert(log)

    suspend fun processTaskerPayment(
        amount: Double,
        currency: String,
        reference: String,
        clientName: String,
        notes: String = ""
    ): PaymentLogResult {
        val allInvoices = invoiceRepository.getAllWithDetails()

        // Search for matching invoice by amount, client name, and reference
        val matchedInvoice = allInvoices.find { invoice ->
            val amountMatches = invoice.total == amount
            val clientMatches = invoice.client?.name?.contains(clientName, ignoreCase = true) == true ||
                    clientName.contains(invoice.client?.name ?: "", ignoreCase = true)
            val referenceMatches = invoice.invoice.invoiceNumber.contains(reference, ignoreCase = true)
            amountMatches && clientMatches && referenceMatches
        }

        val invoiceId = matchedInvoice?.invoice?.id

        val log = PaymentLog(
            amount = amount,
            amountCurrency = currency,
            reference = reference,
            clientName = clientName,
            source = "tasker",
            invoiceId = invoiceId,
            timestamp = Instant.now(),
            notes = notes
        )

        return db.withTransaction {
            paymentLogDao.insert(log)

            if (invoiceId != null && matchedInvoice != null && !matchedInvoice.invoice.isPaid) {
                invoiceRepository.setPaid(
                    matchedInvoice.invoice,
                    paid = true
                )
            }

            PaymentLogResult(
                success = true,
                invoiceMatched = invoiceId != null,
                matchedInvoiceId = invoiceId,
                message = when {
                    invoiceId != null -> "Payment matched and invoice marked paid"
                    else -> "Payment recorded but no matching invoice found"
                }
            )
        }
    }

    data class PaymentLogResult(
        val success: Boolean,
        val invoiceMatched: Boolean,
        val matchedInvoiceId: Long? = null,
        val message: String
    )
}
