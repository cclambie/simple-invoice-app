package com.craiglambie.invoicemaker.ui.screens.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.craiglambie.invoicemaker.data.model.InvoiceWithDetails
import com.craiglambie.invoicemaker.data.model.PaymentLog
import com.craiglambie.invoicemaker.repository.InvoiceRepository
import com.craiglambie.invoicemaker.repository.PaymentLogRepository
import com.craiglambie.invoicemaker.util.simpleInvoiceApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PaymentLogsViewModel(
    private val paymentLogRepository: PaymentLogRepository,
    invoiceRepository: InvoiceRepository
) : ViewModel() {
    val paymentLogs: Flow<List<PaymentLog>> = paymentLogRepository.observeAll()
    val invoices: Flow<List<InvoiceWithDetails>> = invoiceRepository.observeAllWithDetails()

    /** Links the payment to the invoice, or unlinks it when [invoiceId] is null. */
    fun link(log: PaymentLog, invoiceId: Long?) {
        viewModelScope.launch { paymentLogRepository.linkToInvoice(log.id, invoiceId) }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras.simpleInvoiceApp()
                return PaymentLogsViewModel(app.paymentLogRepository, app.invoiceRepository) as T
            }
        }
    }
}
