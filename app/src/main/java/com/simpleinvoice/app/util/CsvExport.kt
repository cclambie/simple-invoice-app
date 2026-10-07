package com.simpleinvoice.app.util

import com.simpleinvoice.app.data.model.InvoiceWithDetails
import java.io.OutputStream
import java.util.Locale

/**
 * Writes one row per invoice as CSV for spreadsheets and accountants. Dates are ISO
 * (yyyy-mm-dd) and amounts are plain numbers with the currency in its own column.
 */
fun writeInvoicesCsv(invoices: List<InvoiceWithDetails>, out: OutputStream) {
    val header = listOf(
        "Invoice number", "Client", "Contact", "Issue date", "Due date", "Currency",
        "Subtotal", "Tax rate %", "Tax", "Total", "Status", "Paid date"
    )
    val rows = invoices.map { item ->
        val invoice = item.invoice
        listOf(
            invoice.invoiceNumber,
            item.client?.name.orEmpty(),
            item.client?.contactName.orEmpty(),
            invoice.issueDate.toString(),
            invoice.dueDate.toString(),
            invoice.currencyCode,
            amount(item.subtotal),
            invoice.taxRatePercent.toString(),
            amount(item.taxAmount),
            amount(item.total),
            if (invoice.isPaid) "Paid" else "Unpaid",
            invoice.paidDate?.toString().orEmpty()
        )
    }
    out.bufferedWriter(Charsets.UTF_8).use { writer ->
        writer.write("\uFEFF") // BOM so Excel reads non-ASCII names and symbols correctly
        (listOf(header) + rows).forEach { row ->
            writer.write(row.joinToString(",", transform = ::escape))
            writer.write("\r\n")
        }
    }
}

private fun amount(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

private fun escape(field: String): String =
    if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + field.replace("\"", "\"\"") + "\""
    } else {
        field
    }
