package com.craiglambie.invoicemaker.ui.screens.payments

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.craiglambie.invoicemaker.data.model.InvoiceWithDetails
import com.craiglambie.invoicemaker.data.model.PaymentLog
import com.craiglambie.invoicemaker.util.asCurrency
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentLogsScreen(onBack: () -> Unit) {
    val viewModel: PaymentLogsViewModel = viewModel(factory = PaymentLogsViewModel.Factory)
    val paymentLogs by viewModel.paymentLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    val invoices by viewModel.invoices.collectAsStateWithLifecycle(initialValue = emptyList())
    var linking by remember { mutableStateOf<PaymentLog?>(null) }

    val invoicesById = invoices.associateBy { it.invoice.id }
    val receivedByInvoice = paymentLogs.filter { it.invoiceId != null }
        .groupBy { it.invoiceId }.mapValues { (_, logs) -> logs.sumOf { it.amount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (paymentLogs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No payment records yet",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 32.dp)
                )
                Text(
                    "Payments sent from Tasker will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(paymentLogs) { log ->
                    val invoice = log.invoiceId?.let { invoicesById[it] }
                    PaymentLogItem(log, invoice, receivedByInvoice[log.invoiceId] ?: 0.0) { linking = log }
                    Divider(modifier = Modifier.padding(vertical = 0.dp))
                }
            }
        }

        linking?.let { log ->
            LinkInvoiceDialog(
                log = log,
                invoices = invoices,
                receivedByInvoice = receivedByInvoice,
                onLink = { viewModel.link(log, it); linking = null },
                onDismiss = { linking = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaymentLogItem(
    log: PaymentLog,
    invoice: InvoiceWithDetails?,
    received: Double,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.clientName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Ref: ${log.reference}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = log.amount.asCurrency(log.amountCurrency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    invoice == null -> "⚠ No match · long-press to link to an invoice"
                    received >= invoice.total - 0.005 ->
                        "✓ ${invoice.invoice.invoiceNumber} paid in full"
                    else -> "◐ ${invoice.invoice.invoiceNumber} part paid: " +
                        "${received.asCurrency(invoice.currencyCode)} of ${invoice.total.asCurrency(invoice.currencyCode)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (invoice != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatTime(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (log.notes.isNotEmpty()) {
            Text(
                text = log.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun LinkInvoiceDialog(
    log: PaymentLog,
    invoices: List<InvoiceWithDetails>,
    receivedByInvoice: Map<Long?, Double>,
    onLink: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // Unpaid invoices only, with this payer's own invoices first
    val candidates = invoices
        .filter { !it.invoice.isPaid || it.invoice.id == log.invoiceId }
        .sortedByDescending { inv ->
            val name = inv.client?.name.orEmpty()
            name.isNotEmpty() && (log.clientName.contains(name, true) || name.contains(log.clientName, true))
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link ${log.amount.asCurrency(log.amountCurrency)} to an invoice") },
        text = {
            if (candidates.isEmpty()) {
                Text("There are no unpaid invoices.")
            } else {
                LazyColumn {
                    items(candidates) { inv ->
                        val balance = inv.total - (receivedByInvoice[inv.invoice.id] ?: 0.0)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLink(inv.invoice.id) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                "${inv.invoice.invoiceNumber} · ${inv.client?.name.orEmpty()}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Total ${inv.total.asCurrency(inv.currencyCode)} · " +
                                    "due ${balance.asCurrency(inv.currencyCode)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            if (log.invoiceId != null) {
                TextButton(onClick = { onLink(null) }) { Text("Unlink") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTime(instant: Instant): String {
    val zoneId = ZoneId.systemDefault()
    val zonedDateTime = instant.atZone(zoneId)
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(zoneId)
    return zonedDateTime.format(formatter)
}
