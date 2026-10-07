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

class InvoiceListViewModel(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    val invoices: StateFlow<List<InvoiceWithDetails>> = invoiceRepository.observeAllWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun togglePaid(item: InvoiceWithDetails) {
        viewModelScope.launch {
            invoiceRepository.setPaid(item.invoice, !item.invoice.isPaid)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { InvoiceListViewModel(simpleInvoiceApp().invoiceRepository) }
        }
    }
}
