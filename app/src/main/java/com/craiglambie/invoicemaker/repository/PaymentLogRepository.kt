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

    /**
     * Links a logged payment to an invoice, or unlinks it when [invoiceId] is null. The invoice is
     * marked paid once the payments linked to it add up to its total, so part payments leave it
     * unpaid. Unlinking never changes an invoice's paid flag.
     */
    suspend fun linkToInvoice(logId: Long, invoiceId: Long?) {
        db.withTransaction {
            paymentLogDao.setInvoice(logId, invoiceId)
            if (invoiceId == null) return@withTransaction
            val details = invoiceRepository.getWithDetails(invoiceId) ?: return@withTransaction
            val received = paymentLogDao.getForInvoice(invoiceId).sumOf { it.amount }
            if (!details.invoice.isPaid && received >= details.total - PAID_TOLERANCE) {
                invoiceRepository.setPaid(details.invoice, paid = true)
            }
        }
    }

    private companion object {
        /** Half a cent, so rounding in the total doesn't leave an invoice a fraction short. */
        const val PAID_TOLERANCE = 0.005
    }

    data class PaymentLogResult(
        val success: Boolean,
        val invoiceMatched: Boolean,
        val matchedInvoiceId: Long? = null,
        val message: String
    )
}
