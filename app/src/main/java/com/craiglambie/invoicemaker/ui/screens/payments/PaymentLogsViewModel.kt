package com.craiglambie.invoicemaker.ui.screens.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.craiglambie.invoicemaker.data.model.PaymentLog
import com.craiglambie.invoicemaker.repository.PaymentLogRepository
import com.craiglambie.invoicemaker.util.simpleInvoiceApp
import kotlinx.coroutines.flow.Flow

class PaymentLogsViewModel(private val paymentLogRepository: PaymentLogRepository) : ViewModel() {
    val paymentLogs: Flow<List<PaymentLog>> = paymentLogRepository.observeAll()

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras.simpleInvoiceApp()
                return PaymentLogsViewModel(app.paymentLogRepository) as T
            }
        }
    }
}
