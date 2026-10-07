package com.simpleinvoice.app.ui.screens.invoices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.simpleinvoice.app.ui.nav.AppMenu
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleinvoice.app.data.model.InvoiceWithDetails
import com.simpleinvoice.app.ui.theme.Green40
import com.simpleinvoice.app.ui.theme.Green90
import com.simpleinvoice.app.ui.theme.Red40
import com.simpleinvoice.app.ui.theme.Red90
import com.simpleinvoice.app.util.asCurrency
import com.simpleinvoice.app.util.asDisplayDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    onAddInvoice: () -> Unit,
    onOpenInvoice: (Long) -> Unit,
    viewModel: InvoiceListViewModel = viewModel(factory = InvoiceListViewModel.Factory)
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoices") }, actions = { AppMenu() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddInvoice) {
                Icon(Icons.Filled.Add, contentDescription = "New invoice")
            }
        }
    ) { padding ->
        if (invoices.isEmpty()) {
            EmptyInvoicesMessage(padding)
        } else {
            LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                items(invoices, key = { it.invoice.id }) { item ->
                    InvoiceRow(item = item, onClick = { onOpenInvoice(item.invoice.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyInvoicesMessage(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text("No invoices yet. Tap + to create your first invoice.")
    }
}

@Composable
private fun InvoiceRow(item: InvoiceWithDetails, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.invoice.invoiceNumber, style = MaterialTheme.typography.titleLarge)
                PaidStatusChip(isPaid = item.invoice.isPaid)
            }
            Text(item.client?.name ?: "No client", style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Due ${item.invoice.dueDate.asDisplayDate()}")
                Text(item.total.asCurrency(item.currencyCode), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun PaidStatusChip(isPaid: Boolean) {
    val containerColor = if (isPaid) Green90 else Red90
    val labelColor = if (isPaid) Green40 else Red40
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(if (isPaid) "Paid" else "Unpaid", color = labelColor, style = MaterialTheme.typography.labelLarge)
    }
}
