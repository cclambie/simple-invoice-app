package com.simpleinvoice.app.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpleinvoice.app.data.model.InvoiceWithDetails
import com.simpleinvoice.app.repository.InvoiceRepository
import com.simpleinvoice.app.util.simpleInvoiceApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InvoiceDetailViewModel(
    private val invoiceRepository: InvoiceRepository,
    invoiceId: Long
) : ViewModel() {

    val invoice: StateFlow<InvoiceWithDetails?> = invoiceRepository.observeWithDetails(invoiceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePaid() {
        val current = invoice.value ?: return
        viewModelScope.launch {
            invoiceRepository.setPaid(current.invoice, !current.invoice.isPaid)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = invoice.value ?: return
        viewModelScope.launch {
            invoiceRepository.delete(current.invoice)
            onDeleted()
        }
    }

    companion object {
        fun factory(invoiceId: Long) = viewModelFactory {
            initializer { InvoiceDetailViewModel(simpleInvoiceApp().invoiceRepository, invoiceId) }
        }
    }
}
