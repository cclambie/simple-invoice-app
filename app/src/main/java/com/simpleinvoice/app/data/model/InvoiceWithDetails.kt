package com.simpleinvoice.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * An [Invoice] joined with its [Client] and [InvoiceLineItem]s for display purposes.
 */
data class InvoiceWithDetails(
    @Embedded val invoice: Invoice,
    @Relation(parentColumn = "clientId", entityColumn = "id")
    val client: Client?,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val lineItems: List<InvoiceLineItem>
) {
    val currencyCode: String
        get() = invoice.currencyCode

    val subtotal: Double
        get() = lineItems.sumOf { it.lineTotal }

    val taxAmount: Double
        get() = subtotal * invoice.taxRatePercent / 100.0

    val total: Double
        get() = subtotal + taxAmount
}
