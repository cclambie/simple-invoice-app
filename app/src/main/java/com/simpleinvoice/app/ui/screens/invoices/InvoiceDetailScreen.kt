package com.simpleinvoice.app.ui.screens.invoices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.simpleinvoice.app.util.asCurrency
import com.simpleinvoice.app.util.asDisplayDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: InvoiceDetailViewModel = viewModel(factory = InvoiceDetailViewModel.factory(invoiceId))
) {
    val details by viewModel.invoice.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.invoice?.invoiceNumber ?: "Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit invoice")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete invoice")
                    }
                }
            )
        }
    ) { padding ->
        val item = details ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(item.client?.name ?: "No client", style = MaterialTheme.typography.titleLarge)
            Text("Issued ${item.invoice.issueDate.asDisplayDate()} · Due ${item.invoice.dueDate.asDisplayDate()}")

            Divider()

            item.lineItems.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(line.description)
                        Text("${line.quantity} × ${line.unitPrice.asCurrency()}", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(line.lineTotal.asCurrency())
                }
            }

            Divider()

            SummaryRow("Subtotal", item.subtotal.asCurrency())
            SummaryRow("Tax (${item.invoice.taxRatePercent}%)", item.taxAmount.asCurrency())
            SummaryRow("Total", item.total.asCurrency(), emphasize = true)

            if (item.invoice.notes.isNotBlank()) {
                Divider()
                Text("Notes", style = MaterialTheme.typography.labelLarge)
                Text(item.invoice.notes)
            }

            if (item.invoice.isPaid) {
                Text("Paid on ${item.invoice.paidDate?.asDisplayDate() ?: "-"}")
                OutlinedButton(onClick = viewModel::togglePaid, modifier = Modifier.fillMaxWidth()) {
                    Text("Mark as Unpaid")
                }
            } else {
                Button(onClick = viewModel::togglePaid, modifier = Modifier.fillMaxWidth()) {
                    Text("Mark as Paid")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete invoice?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
        Text(value, style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
    }
}
