package com.simpleinvoice.app.ui.screens.invoices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleinvoice.app.data.model.Client
import com.simpleinvoice.app.ui.components.DateField
import com.simpleinvoice.app.util.asCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditScreen(
    invoiceId: Long,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    onAddClient: () -> Unit,
    viewModel: InvoiceEditViewModel = viewModel(factory = InvoiceEditViewModel.factory(invoiceId))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (invoiceId == 0L) "New Invoice" else "Edit Invoice") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
            OutlinedTextField(
                value = state.invoiceNumber,
                onValueChange = viewModel::updateInvoiceNumber,
                label = { Text("Invoice number") },
                modifier = Modifier.fillMaxWidth()
            )

            ClientPicker(
                clients = clients,
                selectedClientId = state.clientId,
                onSelect = viewModel::updateClient,
                onAddClient = onAddClient
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DateField(
                    label = "Issue date",
                    date = state.issueDate,
                    onDateSelected = viewModel::updateIssueDate,
                    modifier = Modifier.weight(1f)
                )
                DateField(
                    label = "Due date",
                    date = state.dueDate,
                    onDateSelected = viewModel::updateDueDate,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.taxRatePercent,
                onValueChange = viewModel::updateTaxRate,
                label = { Text("Tax rate (%)") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()
            Text("Line items", style = MaterialTheme.typography.titleLarge)

            state.lineItems.forEach { line ->
                LineItemRow(
                    line = line,
                    canRemove = state.lineItems.size > 1,
                    onDescriptionChange = { value -> viewModel.updateLineItem(line.key) { it.copy(description = value) } },
                    onQuantityChange = { value -> viewModel.updateLineItem(line.key) { it.copy(quantityText = value) } },
                    onUnitPriceChange = { value -> viewModel.updateLineItem(line.key) { it.copy(unitPriceText = value) } },
                    onRemove = { viewModel.removeLineItem(line.key) }
                )
            }

            OutlinedButton(onClick = viewModel::addLineItem, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" Add line item")
            }

            Divider()
            SummaryRow("Subtotal", state.subtotal.asCurrency())
            SummaryRow("Tax", state.taxAmount.asCurrency())
            SummaryRow("Total", state.total.asCurrency(), emphasize = true)

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = state.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Invoice")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientPicker(
    clients: List<Client>,
    selectedClientId: Long?,
    onSelect: (Long) -> Unit,
    onAddClient: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = clients.find { it.id == selectedClientId }?.name ?: "Select a client"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Client") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            clients.forEach { client ->
                DropdownMenuItem(
                    text = { Text(client.name) },
                    onClick = {
                        onSelect(client.id)
                        expanded = false
                    }
                )
            }
            Divider()
            DropdownMenuItem(
                text = { Text("+ Add new client") },
                onClick = {
                    expanded = false
                    onAddClient()
                }
            )
        }
    }
}

@Composable
private fun LineItemRow(
    line: LineItemDraft,
    canRemove: Boolean,
    onDescriptionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = line.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove line item")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = line.quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text("Qty") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = line.unitPriceText,
                    onValueChange = onUnitPriceChange,
                    label = { Text("Unit price") },
                    modifier = Modifier.weight(1f)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text("Total", style = MaterialTheme.typography.labelLarge)
                    Text(line.lineTotal.asCurrency())
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
        Text(value, style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
    }
}
