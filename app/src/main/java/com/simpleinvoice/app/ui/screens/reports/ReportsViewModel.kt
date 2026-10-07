package com.simpleinvoice.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpleinvoice.app.data.model.InvoiceWithDetails
import com.simpleinvoice.app.repository.InvoiceRepository
import com.simpleinvoice.app.util.simpleInvoiceApp
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val invoices: List<InvoiceWithDetails> = emptyList()
) {
    val invoiceCount: Int get() = invoices.size
    val totalInvoiced: Double get() = invoices.sumOf { it.total }
    val totalPaid: Double get() = invoices.filter { it.invoice.isPaid }.sumOf { it.total }
    val totalOutstanding: Double get() = invoices.filterNot { it.invoice.isPaid }.sumOf { it.total }
    val paidCount: Int get() = invoices.count { it.invoice.isPaid }
    val unpaidCount: Int get() = invoiceCount - paidCount
}

class ReportsViewModel(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    private val _startDate = MutableStateFlow(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()))
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(LocalDate.now())
    val endDate: StateFlow<LocalDate> = _endDate.asStateFlow()

    private val _summary = MutableStateFlow<ReportSummary?>(null)
    val summary: StateFlow<ReportSummary?> = _summary.asStateFlow()

    init {
        generate()
    }

    fun setStartDate(date: LocalDate) {
        _startDate.value = date
        generate()
    }

    fun setEndDate(date: LocalDate) {
        _endDate.value = date
        generate()
    }

    private fun generate() {
        val start = _startDate.value
        val end = _endDate.value
        if (start.isAfter(end)) return
        viewModelScope.launch {
            val results = invoiceRepository.getInRange(start, end)
            _summary.value = ReportSummary(start, end, results)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ReportsViewModel(simpleInvoiceApp().invoiceRepository) }
        }
    }
}
