package com.simpleinvoice.app.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleinvoice.app.ui.components.DateField
import com.simpleinvoice.app.util.asCurrency
import com.simpleinvoice.app.util.asDisplayDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel(factory = ReportsViewModel.Factory)
) {
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DateField(
                    label = "From",
                    date = startDate,
                    onDateSelected = viewModel::setStartDate,
                    modifier = Modifier.weight(1f)
                )
                DateField(
                    label = "To",
                    date = endDate,
                    onDateSelected = viewModel::setEndDate,
                    modifier = Modifier.weight(1f)
                )
            }

            val report = summary
            if (report == null) {
                Text("Loading…")
            } else if (startDate.isAfter(endDate)) {
                Text("The from date must be before the to date.")
            } else {
                SummaryCard(report)
                Divider()
                Text("Invoices in period", style = MaterialTheme.typography.titleLarge)
                if (report.invoices.isEmpty()) {
                    Text("No invoices in this date range.")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(report.invoices, key = { it.invoice.id }) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(item.invoice.invoiceNumber)
                                    Text(
                                        "${item.client?.name ?: "No client"} · ${item.invoice.issueDate.asDisplayDate()}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Text(item.total.asCurrency())
                                    Text(
                                        if (item.invoice.isPaid) "Paid" else "Unpaid",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(report: ReportSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatRow("Invoices", report.invoiceCount.toString())
            StatRow("Total invoiced", report.totalInvoiced.asCurrency())
            StatRow("Total paid (${report.paidCount})", report.totalPaid.asCurrency())
            StatRow("Total outstanding (${report.unpaidCount})", report.totalOutstanding.asCurrency())
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
