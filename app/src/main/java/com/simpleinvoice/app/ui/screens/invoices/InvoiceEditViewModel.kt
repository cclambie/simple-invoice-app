package com.simpleinvoice.app.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpleinvoice.app.data.model.Client
import com.simpleinvoice.app.data.model.Invoice
import com.simpleinvoice.app.data.model.InvoiceLineItem
import com.simpleinvoice.app.repository.BusinessProfileRepository
import com.simpleinvoice.app.repository.ClientRepository
import com.simpleinvoice.app.repository.InvoiceRepository
import com.simpleinvoice.app.util.simpleInvoiceApp
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A single editable invoice line row. [key] is a stable UI identity, independent of the DB id. */
data class LineItemDraft(
    val key: String = UUID.randomUUID().toString(),
    val id: Long = 0,
    val description: String = "",
    val quantityText: String = "1",
    val unitPriceText: String = ""
) {
    val quantity: Double get() = quantityText.toDoubleOrNull() ?: 0.0
    val unitPrice: Double get() = unitPriceText.toDoubleOrNull() ?: 0.0
    val lineTotal: Double get() = quantity * unitPrice
}

data class InvoiceEditUiState(
    val id: Long = 0,
    val invoiceNumber: String = "",
    val clientId: Long? = null,
    val issueDate: LocalDate = LocalDate.now(),
    val dueDate: LocalDate = LocalDate.now().plusDays(14),
    val taxRatePercent: String = "0",
    val notes: String = "",
    val lineItems: List<LineItemDraft> = listOf(LineItemDraft()),
    val isPaid: Boolean = false,
    val paidDate: LocalDate? = null,
    val isLoading: Boolean = true
) {
    val subtotal: Double get() = lineItems.sumOf { it.lineTotal }
    val taxAmount: Double get() = subtotal * (taxRatePercent.toDoubleOrNull() ?: 0.0) / 100.0
    val total: Double get() = subtotal + taxAmount
    val isValid: Boolean
        get() = invoiceNumber.isNotBlank() && clientId != null &&
            lineItems.any { it.description.isNotBlank() }
}

class InvoiceEditViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    businessProfileRepository: BusinessProfileRepository,
    private val invoiceId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceEditUiState())
    val uiState: StateFlow<InvoiceEditUiState> = _uiState.asStateFlow()

    val clients: StateFlow<List<Client>> = clientRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (invoiceId == 0L) {
                val nextNumber = invoiceRepository.nextInvoiceNumber()
                val defaultTaxRate = businessProfileRepository.get().defaultTaxRatePercent
                _uiState.value = InvoiceEditUiState(
                    invoiceNumber = nextNumber,
                    taxRatePercent = formatNumber(defaultTaxRate),
                    isLoading = false
                )
            } else {
                invoiceRepository.getWithDetails(invoiceId)?.let { details ->
                    _uiState.value = InvoiceEditUiState(
                        id = details.invoice.id,
                        invoiceNumber = details.invoice.invoiceNumber,
                        clientId = details.invoice.clientId,
                        issueDate = details.invoice.issueDate,
                        dueDate = details.invoice.dueDate,
                        taxRatePercent = formatNumber(details.invoice.taxRatePercent),
                        notes = details.invoice.notes,
                        lineItems = details.lineItems.map {
                            LineItemDraft(
                                id = it.id,
                                description = it.description,
                                quantityText = formatNumber(it.quantity),
                                unitPriceText = formatNumber(it.unitPrice)
                            )
                        }.ifEmpty { listOf(LineItemDraft()) },
                        isPaid = details.invoice.isPaid,
                        paidDate = details.invoice.paidDate,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    fun updateInvoiceNumber(value: String) { _uiState.value = _uiState.value.copy(invoiceNumber = value) }
    fun updateClient(clientId: Long) { _uiState.value = _uiState.value.copy(clientId = clientId) }
    fun updateIssueDate(date: LocalDate) { _uiState.value = _uiState.value.copy(issueDate = date) }
    fun updateDueDate(date: LocalDate) { _uiState.value = _uiState.value.copy(dueDate = date) }
    fun updateTaxRate(value: String) { _uiState.value = _uiState.value.copy(taxRatePercent = value) }
    fun updateNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

    fun addLineItem() {
        _uiState.value = _uiState.value.copy(lineItems = _uiState.value.lineItems + LineItemDraft())
    }

    fun removeLineItem(key: String) {
        val remaining = _uiState.value.lineItems.filterNot { it.key == key }
        _uiState.value = _uiState.value.copy(
            lineItems = remaining.ifEmpty { listOf(LineItemDraft()) }
        )
    }

    fun updateLineItem(key: String, transform: (LineItemDraft) -> LineItemDraft) {
        _uiState.value = _uiState.value.copy(
            lineItems = _uiState.value.lineItems.map { if (it.key == key) transform(it) else it }
        )
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val clientId = state.clientId ?: return
        if (!state.isValid) return
        viewModelScope.launch {
            invoiceRepository.saveInvoice(
                Invoice(
                    id = state.id,
                    invoiceNumber = state.invoiceNumber.trim(),
                    clientId = clientId,
                    issueDate = state.issueDate,
                    dueDate = state.dueDate,
                    taxRatePercent = state.taxRatePercent.toDoubleOrNull() ?: 0.0,
                    notes = state.notes.trim(),
                    isPaid = state.isPaid,
                    paidDate = state.paidDate
                ),
                state.lineItems
                    .filter { it.description.isNotBlank() }
                    .map { InvoiceLineItem(id = it.id, invoiceId = state.id, description = it.description.trim(), quantity = it.quantity, unitPrice = it.unitPrice) }
            )
            onSaved()
        }
    }

    companion object {
        fun factory(invoiceId: Long) = viewModelFactory {
            initializer {
                val app = simpleInvoiceApp()
                InvoiceEditViewModel(app.invoiceRepository, app.clientRepository, app.businessProfileRepository, invoiceId)
            }
        }
    }
}
