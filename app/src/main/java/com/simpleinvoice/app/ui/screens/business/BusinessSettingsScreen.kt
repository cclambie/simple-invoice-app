package com.simpleinvoice.app.ui.screens.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleinvoice.app.data.model.PaymentAccount
import com.simpleinvoice.app.ui.nav.AppMenu
import com.simpleinvoice.app.data.model.TaxIdType
import com.simpleinvoice.app.ui.components.CurrencyPicker
import com.simpleinvoice.app.util.asDecimalInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSettingsScreen(
    viewModel: BusinessSettingsViewModel = viewModel(factory = BusinessSettingsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.justSaved) {
        if (state.justSaved) {
            snackbarHostState.showSnackbar("Business details saved")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Details") },
                actions = { AppMenu() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("These details appear on every invoice you create.")

            OutlinedTextField(
                value = state.businessName,
                onValueChange = viewModel::updateBusinessName,
                label = { Text("Business name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.address,
                onValueChange = viewModel::updateAddress,
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::updatePhone,
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Text("Tax registration")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaxIdType.entries.forEach { type ->
                    FilterChip(
                        selected = state.taxIdType == type,
                        onClick = { viewModel.updateTaxIdType(type) },
                        label = { Text(type.label) }
                    )
                }
            }
            if (state.taxIdType == TaxIdType.OTHER) {
                OutlinedTextField(
                    value = state.taxIdCustomLabel,
                    onValueChange = viewModel::updateTaxIdCustomLabel,
                    label = { Text("Tax type label") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = state.taxIdNumber,
                onValueChange = viewModel::updateTaxIdNumber,
                label = { Text("Tax registration number") },
                modifier = Modifier.fillMaxWidth()
            )
            CurrencyPicker(
                label = "Default currency",
                selected = state.currencyCode,
                onSelect = { it?.let(viewModel::updateCurrency) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.defaultTaxRatePercent,
                onValueChange = { value -> value.asDecimalInput()?.let(viewModel::updateDefaultTaxRate) },
                label = { Text("Default tax rate (%) applied to new invoices") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = state.defaultDueDays,
                onValueChange = { value -> if (value.length <= 3 && value.all(Char::isDigit)) viewModel.updateDefaultDueDays(value) },
                label = { Text("Default payment terms (days until due)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Divider()
            Text("Payment details", style = MaterialTheme.typography.titleLarge)
            Text(
                "Printed at the bottom of each invoice. Bank details are kept per currency, so an " +
                    "invoice shows the account for its own currency.",
                style = MaterialTheme.typography.bodyMedium
            )
            CurrencyPicker(
                label = "Bank account for",
                selected = state.paymentCurrency,
                onSelect = { it?.let(viewModel::selectPaymentCurrency) },
                modifier = Modifier.fillMaxWidth()
            )
            val account = state.currentPaymentAccount
            val isEuro = state.paymentCurrency == "EUR"
            OutlinedTextField(
                value = account.accountName,
                onValueChange = viewModel::updateAccountName,
                label = { Text("Account name") },
                placeholder = { Text(state.businessName.ifBlank { "Name on the account" }) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            OutlinedTextField(
                value = account.bankCode,
                onValueChange = viewModel::updateBankCode,
                label = { Text(PaymentAccount.bankCodeLabel(state.paymentCurrency)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = if (isEuro) {
                    KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii)
                } else {
                    KeyboardOptions(keyboardType = KeyboardType.Number)
                }
            )
            OutlinedTextField(
                value = account.accountNumber,
                onValueChange = viewModel::updateAccountNumber,
                label = { Text(PaymentAccount.accountNumberLabel(state.paymentCurrency)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = if (isEuro) {
                    KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii)
                } else {
                    KeyboardOptions(keyboardType = KeyboardType.Number)
                }
            )
            if (state.currenciesWithPaymentDetails.isNotEmpty()) {
                Text(
                    "Bank details entered for: ${state.currenciesWithPaymentDetails.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text("International payments (optional)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.internationalPaymentService,
                onValueChange = viewModel::updateInternationalService,
                label = { Text("International payment service") },
                placeholder = { Text("e.g. PayPal, Wise, Revolut") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PayPal", "Wise", "Revolut").forEach { service ->
                    AssistChip(onClick = { viewModel.updateInternationalService(service) }, label = { Text(service) })
                }
            }
            OutlinedTextField(
                value = state.internationalPaymentLink,
                onValueChange = viewModel::updateInternationalLink,
                label = { Text("International payment link or account") },
                placeholder = { Text("e.g. paypal.me/yourname or you@email.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}
