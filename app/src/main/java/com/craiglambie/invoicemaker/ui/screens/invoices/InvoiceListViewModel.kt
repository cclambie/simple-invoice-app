package com.craiglambie.invoicemaker.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.craiglambie.invoicemaker.data.model.InvoiceWithDetails
import com.craiglambie.invoicemaker.repository.InvoiceRepository
import com.craiglambie.invoicemaker.repository.PaymentLogRepository
import com.craiglambie.invoicemaker.util.simpleInvoiceApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InvoiceListViewModel(
    private val invoiceRepository: InvoiceRepository,
    paymentLogRepository: PaymentLogRepository
) : ViewModel() {

    val invoices: StateFlow<List<InvoiceWithDetails>> = invoiceRepository.observeAllWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total of the logged payments linked to each invoice, keyed by invoice id. */
    val receivedByInvoice: StateFlow<Map<Long, Double>> = paymentLogRepository.observeAll()
        .map { logs ->
            logs.filter { it.invoiceId != null }
                .groupBy { it.invoiceId!! }
                .mapValues { (_, linked) -> linked.sumOf { it.amount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun togglePaid(item: InvoiceWithDetails) {
        viewModelScope.launch {
            invoiceRepository.setPaid(item.invoice, !item.invoice.isPaid)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = simpleInvoiceApp()
                InvoiceListViewModel(app.invoiceRepository, app.paymentLogRepository)
            }
        }
    }
}
