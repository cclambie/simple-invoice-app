package com.simpleinvoice.app.ui.screens.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simpleinvoice.app.data.model.PaymentAccount
import com.simpleinvoice.app.data.model.TaxIdType
import com.simpleinvoice.app.repository.BusinessProfileRepository
import com.simpleinvoice.app.util.simpleInvoiceApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BusinessSettingsUiState(
    val businessName: String = "",
    val address: String = "",
    val email: String = "",
    val phone: String = "",
    val taxIdType: TaxIdType = TaxIdType.GST,
    val taxIdCustomLabel: String = "",
    val taxIdNumber: String = "",
    val defaultTaxRatePercent: String = "0",
    val defaultDueDays: String = "7",
    val currencyCode: String = com.simpleinvoice.app.util.deviceCurrencyCode(),
    /** Bank details keyed by currency code. */
    val paymentAccounts: Map<String, PaymentAccount> = emptyMap(),
    /** The currency whose bank details are being edited. */
    val paymentCurrency: String = currencyCode,
    val internationalPaymentService: String = "",
    val internationalPaymentLink: String = "",
    val isLoading: Boolean = true,
    val justSaved: Boolean = false
) {
    val currentPaymentAccount: PaymentAccount
        get() = paymentAccounts[paymentCurrency] ?: PaymentAccount(currencyCode = paymentCurrency)

    /** Currencies that have bank details entered, for the summary line. */
    val currenciesWithPaymentDetails: List<String>
        get() = paymentAccounts.values.filterNot { it.isBlank }.map { it.currencyCode }.sorted()
}

class BusinessSettingsViewModel(
    private val repository: BusinessProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessSettingsUiState())
    val uiState: StateFlow<BusinessSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = repository.get()
            val accounts = repository.getPaymentAccounts().associateBy { it.currencyCode }
            _uiState.value = BusinessSettingsUiState(
                businessName = profile.businessName,
                address = profile.address,
                email = profile.email,
                phone = profile.phone,
                taxIdType = profile.taxIdType,
                taxIdCustomLabel = profile.taxIdCustomLabel,
                taxIdNumber = profile.taxIdNumber,
                defaultTaxRatePercent = formatRate(profile.defaultTaxRatePercent),
                defaultDueDays = profile.defaultDueDays.toString(),
                currencyCode = profile.currencyCode,
                paymentAccounts = accounts,
                paymentCurrency = profile.currencyCode,
                internationalPaymentService = profile.internationalPaymentService,
                internationalPaymentLink = profile.internationalPaymentLink,
                isLoading = false
            )
        }
    }

    private fun formatRate(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    fun updateBusinessName(value: String) { _uiState.value = _uiState.value.copy(businessName = value, justSaved = false) }
    fun updateAddress(value: String) { _uiState.value = _uiState.value.copy(address = value, justSaved = false) }
    fun updateEmail(value: String) { _uiState.value = _uiState.value.copy(email = value, justSaved = false) }
    fun updatePhone(value: String) { _uiState.value = _uiState.value.copy(phone = value, justSaved = false) }
    fun updateTaxIdType(value: TaxIdType) { _uiState.value = _uiState.value.copy(taxIdType = value, justSaved = false) }
    fun updateTaxIdCustomLabel(value: String) { _uiState.value = _uiState.value.copy(taxIdCustomLabel = value, justSaved = false) }
    fun updateTaxIdNumber(value: String) { _uiState.value = _uiState.value.copy(taxIdNumber = value, justSaved = false) }
    fun updateDefaultTaxRate(value: String) { _uiState.value = _uiState.value.copy(defaultTaxRatePercent = value, justSaved = false) }

    fun updateDefaultDueDays(value: String) { _uiState.value = _uiState.value.copy(defaultDueDays = value, justSaved = false) }

    fun updateCurrency(value: String) { _uiState.value = _uiState.value.copy(currencyCode = value, justSaved = false) }

    fun selectPaymentCurrency(value: String) { _uiState.value = _uiState.value.copy(paymentCurrency = value) }
    fun updateAccountName(value: String) = editPaymentAccount { it.copy(accountName = value) }
    fun updateBankCode(value: String) = editPaymentAccount { it.copy(bankCode = value) }
    fun updateAccountNumber(value: String) = editPaymentAccount { it.copy(accountNumber = value) }
    fun updateInternationalService(value: String) {
        _uiState.value = _uiState.value.copy(internationalPaymentService = value, justSaved = false)
    }
    fun updateInternationalLink(value: String) {
        _uiState.value = _uiState.value.copy(internationalPaymentLink = value, justSaved = false)
    }

    private fun editPaymentAccount(transform: (PaymentAccount) -> PaymentAccount) {
        val state = _uiState.value
        val updated = transform(state.currentPaymentAccount)
        _uiState.value = state.copy(paymentAccounts = state.paymentAccounts + (updated.currencyCode to updated), justSaved = false)
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.save(
                com.simpleinvoice.app.data.model.BusinessProfile(
                    businessName = state.businessName.trim(),
                    address = state.address.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim(),
                    taxIdType = state.taxIdType,
                    taxIdCustomLabel = state.taxIdCustomLabel.trim(),
                    taxIdNumber = state.taxIdNumber.trim(),
                    defaultTaxRatePercent = state.defaultTaxRatePercent.toDoubleOrNull() ?: 0.0,
                    defaultDueDays = state.defaultDueDays.toIntOrNull() ?: 7,
                    currencyCode = state.currencyCode,
                    internationalPaymentService = state.internationalPaymentService.trim(),
                    internationalPaymentLink = state.internationalPaymentLink.trim()
                )
            )
            repository.savePaymentAccounts(
                state.paymentAccounts.values.map {
                    it.copy(accountName = it.accountName.trim(), bankCode = it.bankCode.trim(), accountNumber = it.accountNumber.trim())
                }
            )
            _uiState.value = _uiState.value.copy(justSaved = true)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { BusinessSettingsViewModel(simpleInvoiceApp().businessProfileRepository) }
        }
    }
}
