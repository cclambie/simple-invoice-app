package com.simpleinvoice.app.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpleinvoice.app.data.model.Client
import com.simpleinvoice.app.repository.ClientRepository
import com.simpleinvoice.app.repository.InvoiceRepository
import com.simpleinvoice.app.util.simpleInvoiceApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientListViewModel(
    private val clientRepository: ClientRepository,
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    val clients: StateFlow<List<Client>> = clientRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteClient(client: Client, onBlocked: () -> Unit, onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (invoiceRepository.hasInvoicesForClient(client.id)) {
                onBlocked()
            } else {
                clientRepository.delete(client)
                onDeleted()
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = simpleInvoiceApp()
                ClientListViewModel(app.clientRepository, app.invoiceRepository)
            }
        }
    }
}
