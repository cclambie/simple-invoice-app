package com.simpleinvoice.app.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpleinvoice.app.data.model.Client
import com.simpleinvoice.app.repository.ClientRepository
import com.simpleinvoice.app.util.simpleInvoiceApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClientEditUiState(
    val id: Long = 0,
    val name: String = "",
    val contactName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val currencyCode: String? = null,
    val importedFromContactId: String? = null,
    val isLoading: Boolean = false,
    val isNew: Boolean = true
) {
    val isValid: Boolean get() = name.isNotBlank()
}

class ClientEditViewModel(
    private val clientRepository: ClientRepository,
    private val clientId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientEditUiState(isLoading = clientId != 0L))
    val uiState: StateFlow<ClientEditUiState> = _uiState.asStateFlow()

    init {
        if (clientId != 0L) {
            viewModelScope.launch {
                clientRepository.getById(clientId)?.let { client ->
                    _uiState.value = ClientEditUiState(
                        id = client.id,
                        name = client.name,
                        contactName = client.contactName,
                        email = client.email,
                        phone = client.phone,
                        address = client.address,
                        notes = client.notes,
                        currencyCode = client.currencyCode,
                        importedFromContactId = client.importedFromContactId,
                        isLoading = false,
                        isNew = false
                    )
                }
            }
        }
    }

    fun updateName(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun updateContactName(value: String) { _uiState.value = _uiState.value.copy(contactName = value) }
    fun updateEmail(value: String) { _uiState.value = _uiState.value.copy(email = value) }
    fun updatePhone(value: String) { _uiState.value = _uiState.value.copy(phone = value) }
    fun updateAddress(value: String) { _uiState.value = _uiState.value.copy(address = value) }
    fun updateNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun updateCurrency(value: String?) { _uiState.value = _uiState.value.copy(currencyCode = value) }

    /**
     * Prefills the form from a picked device contact. The contact is a person, so they become
     * the contact name, and also the client name if none has been entered yet.
     */
    fun applyContact(name: String, email: String, phone: String, contactId: String) {
        _uiState.value = _uiState.value.copy(
            name = _uiState.value.name.ifBlank { name },
            contactName = name,
            email = email.ifBlank { _uiState.value.email },
            phone = phone.ifBlank { _uiState.value.phone },
            importedFromContactId = contactId
        )
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            clientRepository.save(
                Client(
                    id = state.id,
                    name = state.name.trim(),
                    contactName = state.contactName.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim(),
                    address = state.address.trim(),
                    notes = state.notes.trim(),
                    currencyCode = state.currencyCode,
                    importedFromContactId = state.importedFromContactId
                )
            )
            onSaved()
        }
    }

    companion object {
        fun factory(clientId: Long) = viewModelFactory {
            initializer {
                val app = simpleInvoiceApp()
                ClientEditViewModel(app.clientRepository, clientId)
            }
        }
    }
}
