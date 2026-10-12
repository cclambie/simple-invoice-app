package com.craiglambie.invoicemaker.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.craiglambie.invoicemaker.data.model.InvoiceWithDetails
import com.craiglambie.invoicemaker.repository.InvoiceRepository
import com.craiglambie.invoicemaker.util.simpleInvoiceApp
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
