package com.simpleinvoice.app.ui.screens.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleinvoice.app.data.model.TaxIdType

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
        topBar = { TopAppBar(title = { Text("Business Details") }) },
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
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::updatePhone,
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth()
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
            OutlinedTextField(
                value = state.defaultTaxRatePercent,
                onValueChange = viewModel::updateDefaultTaxRate,
                label = { Text("Default tax rate (%) applied to new invoices") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}
