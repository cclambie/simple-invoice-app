package com.simpleinvoice.app.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpleinvoice.app.data.model.BusinessProfile
import com.simpleinvoice.app.data.model.InvoiceWithDetails
import com.simpleinvoice.app.data.model.PaymentAccount
import com.simpleinvoice.app.repository.BusinessProfileRepository
import com.simpleinvoice.app.repository.InvoiceRepository
import com.simpleinvoice.app.util.simpleInvoiceApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InvoiceDetailViewModel(
    private val invoiceRepository: InvoiceRepository,
    businessProfileRepository: BusinessProfileRepository,
    invoiceId: Long
) : ViewModel() {

    val invoice: StateFlow<InvoiceWithDetails?> = invoiceRepository.observeWithDetails(invoiceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val business: StateFlow<BusinessProfile> = businessProfileRepository.observe()
        .map { it ?: BusinessProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessProfile())

    /** Bank details keyed by currency code. */
    val paymentAccounts: StateFlow<Map<String, PaymentAccount>> = businessProfileRepository.observePaymentAccounts()
        .map { accounts -> accounts.associateBy { it.currencyCode } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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
            initializer {
                val app = simpleInvoiceApp()
                InvoiceDetailViewModel(app.invoiceRepository, app.businessProfileRepository, invoiceId)
            }
        }
    }
}
