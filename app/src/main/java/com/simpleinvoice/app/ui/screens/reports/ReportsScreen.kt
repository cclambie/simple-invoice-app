package com.simpleinvoice.app.ui.screens.reports

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleinvoice.app.SimpleInvoiceApp
import com.simpleinvoice.app.plan.Tier
import com.simpleinvoice.app.ui.components.DateField
import com.simpleinvoice.app.ui.nav.AppMenu
import com.simpleinvoice.app.util.asCurrency
import com.simpleinvoice.app.util.currencyLabel
import com.simpleinvoice.app.util.asDisplayDate
import com.simpleinvoice.app.util.writeInvoicesCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onOpenPlans: () -> Unit,
    viewModel: ReportsViewModel = viewModel(factory = ReportsViewModel.Factory)
) {
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tier by (context.applicationContext as SimpleInvoiceApp).planRepository.tier.collectAsStateWithLifecycle()

    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val report = summary ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)!!.use { writeInvoicesCsv(report.invoices, it) }
                }.isSuccess
            }
            Toast.makeText(context, if (ok) "CSV exported" else "Couldn't export CSV", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }, actions = { AppMenu() }) }
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
                OutlinedButton(
                    onClick = {
                        val withinOneMonth = report.endDate.isBefore(report.startDate.plusMonths(1))
                        when {
                            !tier.canExportCsv -> {
                                Toast.makeText(context, "Upgrade to Premium to export reports", Toast.LENGTH_SHORT).show()
                                onOpenPlans()
                            }
                            !tier.csvExportUnlimited && !withinOneMonth -> Toast.makeText(
                                context, "Upgrade to get more than 1 month export at a time", Toast.LENGTH_LONG
                            ).show()
                            else -> exportCsv.launch("invoices-${report.startDate}-to-${report.endDate}.csv")
                        }
                    },
                    enabled = report.invoices.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Text(if (tier == Tier.FREE) " Export CSV (Premium)" else " Export CSV")
                }
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
                                    Text(item.total.asCurrency(item.currencyCode))
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
            StatRow("Invoices", "${report.invoiceCount} (${report.paidCount} paid, ${report.unpaidCount} unpaid)")
            val totals = report.totalsByCurrency
            totals.forEach { t ->
                if (totals.size > 1) {
                    Text(currencyLabel(t.currencyCode), style = MaterialTheme.typography.labelLarge)
                }
                StatRow("Total invoiced", t.invoiced.asCurrency(t.currencyCode))
                StatRow("Total paid", t.paid.asCurrency(t.currencyCode))
                StatRow("Total outstanding", t.outstanding.asCurrency(t.currencyCode))
            }
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
