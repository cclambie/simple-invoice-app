package com.simpleinvoice.app.ui.screens.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpleinvoice.app.data.model.Client
import com.simpleinvoice.app.data.model.Invoice
import com.simpleinvoice.app.data.model.InvoiceLineItem
import com.simpleinvoice.app.data.model.SavedItem
import com.simpleinvoice.app.repository.BusinessProfileRepository
import com.simpleinvoice.app.repository.ClientRepository
import com.simpleinvoice.app.repository.InvoiceRepository
import com.simpleinvoice.app.repository.SavedItemRepository
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
    val dueDate: LocalDate = LocalDate.now().plusDays(7),
    val taxRatePercent: String = "0",
    val currencyCode: String = "",
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
    private val savedItemRepository: SavedItemRepository,
    private val invoiceId: Long
) : ViewModel() {

    private var businessCurrencyCode: String = ""

    private val _uiState = MutableStateFlow(InvoiceEditUiState())
    val uiState: StateFlow<InvoiceEditUiState> = _uiState.asStateFlow()

    val clients: StateFlow<List<Client>> = clientRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedItems: StateFlow<List<SavedItem>> = savedItemRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (invoiceId == 0L) {
                val nextNumber = invoiceRepository.nextInvoiceNumber()
                val business = businessProfileRepository.get()
                businessCurrencyCode = business.currencyCode
                val today = LocalDate.now()
                _uiState.value = InvoiceEditUiState(
                    invoiceNumber = nextNumber,
                    issueDate = today,
                    dueDate = today.plusDays(business.defaultDueDays.toLong()),
                    taxRatePercent = formatNumber(business.defaultTaxRatePercent),
                    currencyCode = business.currencyCode,
                    isLoading = false
                )
            } else {
                businessCurrencyCode = businessProfileRepository.get().currencyCode
                invoiceRepository.getWithDetails(invoiceId)?.let { details ->
                    _uiState.value = InvoiceEditUiState(
                        id = details.invoice.id,
                        invoiceNumber = details.invoice.invoiceNumber,
                        clientId = details.invoice.clientId,
                        issueDate = details.invoice.issueDate,
                        dueDate = details.invoice.dueDate,
                        taxRatePercent = formatNumber(details.invoice.taxRatePercent),
                        currencyCode = details.invoice.currencyCode,
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
    /** Selecting a client switches the invoice to that client's billing currency. */
    fun updateClient(clientId: Long) {
        val client = clients.value.find { it.id == clientId }
        _uiState.value = _uiState.value.copy(
            clientId = clientId,
            currencyCode = client?.currencyCode ?: businessCurrencyCode.ifEmpty { _uiState.value.currencyCode }
        )
    }
    fun updateCurrency(value: String) { _uiState.value = _uiState.value.copy(currencyCode = value) }
    /** Moves the due date along with the issue date so the payment terms stay the same. */
    fun updateIssueDate(date: LocalDate) {
        val state = _uiState.value
        val termsDays = java.time.temporal.ChronoUnit.DAYS.between(state.issueDate, state.dueDate)
        _uiState.value = state.copy(issueDate = date, dueDate = date.plusDays(termsDays))
    }
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

    /** Adds a saved item as a line, filling the first blank line if there is one. */
    fun addSavedItem(item: SavedItem) {
        val draft = LineItemDraft(description = item.description, unitPriceText = formatNumber(item.unitPrice))
        val lines = _uiState.value.lineItems
        val blankIndex = lines.indexOfFirst { it.description.isBlank() && it.unitPriceText.isBlank() }
        _uiState.value = _uiState.value.copy(
            lineItems = if (blankIndex >= 0) {
                lines.mapIndexed { i, line -> if (i == blankIndex) draft.copy(key = line.key, quantityText = line.quantityText) else line }
            } else {
                lines + draft
            }
        )
    }

    /** Stores a line's description and unit price so it can be reused on later invoices. */
    fun saveLineForReuse(key: String) {
        val line = _uiState.value.lineItems.find { it.key == key } ?: return
        if (line.description.isBlank()) return
        viewModelScope.launch { savedItemRepository.save(line.description.trim(), line.unitPrice) }
    }

    fun deleteSavedItem(item: SavedItem) {
        viewModelScope.launch { savedItemRepository.delete(item) }
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
                    currencyCode = state.currencyCode,
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
                InvoiceEditViewModel(app.invoiceRepository, app.clientRepository, app.businessProfileRepository, app.savedItemRepository, invoiceId)
            }
        }
    }
}
