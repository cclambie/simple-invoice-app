package com.simpleinvoice.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.simpleinvoice.app.data.model.BusinessProfile
import com.simpleinvoice.app.data.model.InvoiceWithDetails
import com.simpleinvoice.app.data.model.PaymentAccount
import java.io.File
import java.io.OutputStream

/** File name used when saving or sharing an invoice PDF, e.g. "Invoice-INV-0001.pdf". */
fun InvoiceWithDetails.pdfFileName(): String =
    "Invoice-" + invoice.invoiceNumber.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".pdf"

/**
 * Renders [details] as a standard A4 invoice and writes the PDF to [out]. [paymentAccount] is the
 * bank account for the invoice's currency, if one has been set up.
 */
fun writeInvoicePdf(
    details: InvoiceWithDetails,
    business: BusinessProfile,
    paymentAccount: PaymentAccount?,
    out: OutputStream
) {
    val document = PdfDocument()
    try {
        InvoicePdfRenderer(document, details, business, paymentAccount).render()
        document.writeTo(out)
    } finally {
        document.close()
    }
}

/**
 * Writes the invoice PDF to the app cache and opens the Android share sheet so it can be
 * attached to an email, message, cloud drive, etc. The client's email is prefilled when known.
 */
fun shareInvoicePdf(
    context: Context,
    details: InvoiceWithDetails,
    business: BusinessProfile,
    paymentAccount: PaymentAccount?
) {
    val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
    val file = File(dir, details.pdfFileName())
    file.outputStream().use { writeInvoicePdf(details, business, paymentAccount, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val from = business.businessName.ifBlank { null }
    val subject = "Invoice ${details.invoice.invoiceNumber}" + (from?.let { " from $it" } ?: "")
    val greeting = details.client?.contactName?.ifBlank { null }?.let { "Hi $it," } ?: "Hi,"
    val body = "$greeting\n\nPlease find attached invoice ${details.invoice.invoiceNumber} for " +
        "${details.total.asCurrency(details.currencyCode)}, due ${details.invoice.dueDate.asDisplayDate()}.\n\n" +
        "Thank you" + (from?.let { ",\n$it" } ?: "")

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        details.client?.email?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_EMAIL, arrayOf(it)) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Send invoice"))
}

private class InvoicePdfRenderer(
    private val document: PdfDocument,
    private val details: InvoiceWithDetails,
    private val business: BusinessProfile,
    private val paymentAccount: PaymentAccount?
) {
    private val invoice = details.invoice

    private fun Double.money(): String = asCurrency(details.currencyCode)

    private val text = paint(10f)
    private val textBold = paint(10f, bold = true)
    private val muted = paint(9f, color = MUTED)
    private val label = paint(9f, bold = true, color = MUTED)
    private val businessName = paint(18f, bold = true)
    private val title = paint(26f, bold = true, color = ACCENT)
    private val totalPaint = paint(12f, bold = true)
    private val line = Paint().apply { color = RULE; strokeWidth = 0.75f }
    private val headerFill = Paint().apply { color = HEADER_BG; style = Paint.Style.FILL }

    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = 0f

    fun render() {
        startPage()
        drawHeader()
        drawBillTo()
        drawItems()
        drawTotals()
        drawNotes()
        drawPaymentDetails()
        finishPage()
    }

    private fun drawHeader() {
        // Left: business details.
        var left = MARGIN + businessName.textSize
        canvas.drawText(business.businessName.ifBlank { "Your Business" }, MARGIN, left, businessName)
        left += 6f
        val businessLines = buildList {
            addAll(business.address.lines().filter { it.isNotBlank() })
            if (business.email.isNotBlank()) add(business.email)
            if (business.phone.isNotBlank()) add(business.phone)
            if (business.taxIdNumber.isNotBlank()) add("${business.effectiveTaxIdLabel}: ${business.taxIdNumber}")
        }
        businessLines.forEach { left += LINE; canvas.drawText(it, MARGIN, left, text) }

        // Right: title and invoice meta.
        var right = MARGIN + title.textSize
        drawRight("INVOICE", RIGHT, right, title)
        right += 8f
        listOf(
            "Invoice #" to invoice.invoiceNumber,
            "Issue date" to invoice.issueDate.asDisplayDate(),
            "Due date" to invoice.dueDate.asDisplayDate()
        ).forEach { (k, v) ->
            right += LINE
            drawRight(v, RIGHT, right, textBold)
            drawRight(k, RIGHT - 110f, right, muted)
        }
        if (invoice.isPaid) {
            right += LINE + 4f
            val paid = "PAID" + (invoice.paidDate?.let { " ${it.asDisplayDate()}" } ?: "")
            drawRight(paid, RIGHT, right, paint(11f, bold = true, color = PAID))
        }

        y = maxOf(left, right) + 28f
    }

    private fun drawBillTo() {
        val client = details.client
        canvas.drawText("BILL TO", MARGIN, y, label)
        y += LINE + 2f
        canvas.drawText(client?.name ?: "", MARGIN, y, paint(11f, bold = true))
        val lines = buildList {
            if (client != null) {
                if (client.contactName.isNotBlank()) add("Attn: ${client.contactName}")
                addAll(client.address.lines().filter { it.isNotBlank() })
                if (client.email.isNotBlank()) add(client.email)
                if (client.phone.isNotBlank()) add(client.phone)
            }
        }
        lines.forEach { y += LINE; canvas.drawText(it, MARGIN, y, text) }
        y += 28f
    }

    private fun drawItems() {
        drawTableHeader()
        details.lineItems.forEach { item ->
            val descLines = wrap(item.description, DESC_WIDTH, text)
            val rowHeight = descLines.size * LINE + ROW_PAD * 2
            if (y + rowHeight > BOTTOM) {
                finishPage()
                startPage()
                y = MARGIN
                drawTableHeader()
            }
            var lineY = y + ROW_PAD + text.textSize
            descLines.forEach { canvas.drawText(it, COL_DESC + 6f, lineY, text); lineY += LINE }
            val firstBaseline = y + ROW_PAD + text.textSize
            drawRight(item.quantity.asPlainNumber(), COL_QTY, firstBaseline, text)
            drawRight(item.unitPrice.money(), COL_PRICE, firstBaseline, text)
            drawRight(item.lineTotal.money(), RIGHT - 6f, firstBaseline, text)
            y += rowHeight
            canvas.drawLine(MARGIN, y, RIGHT, y, line)
        }
        y += 16f
    }

    private fun drawTableHeader() {
        val height = 22f
        canvas.drawRect(MARGIN, y, RIGHT, y + height, headerFill)
        val baseline = y + 15f
        canvas.drawText("DESCRIPTION", COL_DESC + 6f, baseline, label)
        drawRight("QTY", COL_QTY, baseline, label)
        drawRight("UNIT PRICE", COL_PRICE, baseline, label)
        drawRight("AMOUNT", RIGHT - 6f, baseline, label)
        y += height
    }

    private fun drawTotals() {
        ensureSpace(LINE * 4 + 12f)
        val labelX = RIGHT - 150f
        val taxLabel = if (business.taxIdNumber.isNotBlank()) business.effectiveTaxIdLabel else "Tax"
        y += LINE
        canvas.drawText("Subtotal", labelX, y, text)
        drawRight(details.subtotal.money(), RIGHT - 6f, y, text)
        y += LINE + 2f
        canvas.drawText("$taxLabel (${invoice.taxRatePercent.asPlainNumber()}%)", labelX, y, text)
        drawRight(details.taxAmount.money(), RIGHT - 6f, y, text)
        y += 8f
        canvas.drawLine(labelX, y, RIGHT, y, line)
        y += LINE + 4f
        canvas.drawText(if (invoice.isPaid) "Total" else "Total due", labelX, y, totalPaint)
        drawRight(details.total.asCurrency(details.currencyCode), RIGHT - 6f, y, totalPaint)
        y += 28f
    }

    private fun drawNotes() {
        if (invoice.notes.isBlank()) return
        ensureSpace(LINE * 3)
        canvas.drawText("NOTES", MARGIN, y, label)
        y += LINE + 2f
        wrap(invoice.notes, RIGHT - MARGIN, text).forEach {
            ensureSpace(LINE)
            canvas.drawText(it, MARGIN, y, text)
            y += LINE
        }
    }

    private fun drawPaymentDetails() {
        val rows = mutableListOf<Pair<String, String>>()
        paymentAccount?.takeUnless { it.isBlank }?.let { account ->
            account.accountName.ifBlank { business.businessName }.takeIf { it.isNotBlank() }?.let { rows += "Account name" to it }
            if (account.bankCode.isNotBlank()) rows += PaymentAccount.bankCodeLabel(account.currencyCode) to account.bankCode
            if (account.accountNumber.isNotBlank()) {
                rows += PaymentAccount.accountNumberLabel(account.currencyCode) to account.accountNumber
            }
        }
        val service = business.internationalPaymentService
        val link = business.internationalPaymentLink
        when {
            link.isNotBlank() -> rows += service.ifBlank { "International" } to link
            service.isNotBlank() -> rows += "International" to service
        }
        if (rows.isEmpty()) return
        rows += "Reference" to invoice.invoiceNumber

        y += 12f
        ensureSpace(LINE * (rows.size + 2))
        canvas.drawText("PAYMENT DETAILS", MARGIN, y, label)
        y += LINE + 2f
        rows.forEach { (name, value) ->
            canvas.drawText(name, MARGIN, y, muted)
            canvas.drawText(value, MARGIN + 130f, y, textBold)
            y += LINE
        }
    }

    private fun ensureSpace(height: Float) {
        if (y + height > BOTTOM) {
            finishPage()
            startPage()
            y = MARGIN + LINE
        }
    }

    private fun startPage() {
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
    }

    private fun finishPage() {
        val footer = "Invoice ${invoice.invoiceNumber} · Page $pageNumber"
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 40f, RIGHT, PAGE_HEIGHT - 40f, line)
        drawRight(footer, RIGHT, PAGE_HEIGHT - 26f, muted)
        document.finishPage(page)
    }

    private fun drawRight(value: String, x: Float, baseline: Float, paint: Paint) {
        canvas.drawText(value, x - paint.measureText(value), baseline, paint)
    }

    /** Word-wraps [value] to [width], honouring explicit line breaks and splitting over-long words. */
    private fun wrap(value: String, width: Float, paint: Paint): List<String> =
        value.lines().flatMap { paragraph ->
            if (paragraph.isBlank()) return@flatMap listOf("")
            val out = mutableListOf<String>()
            var current = ""
            paragraph.split(" ").filter { it.isNotEmpty() }.forEach { word ->
                var w = word
                while (paint.measureText(w) > width) {
                    if (current.isNotEmpty()) { out += current; current = "" }
                    val fit = paint.breakText(w, true, width, null).coerceAtLeast(1)
                    out += w.substring(0, fit)
                    w = w.substring(fit)
                }
                val candidate = if (current.isEmpty()) w else "$current $w"
                if (paint.measureText(candidate) <= width) {
                    current = candidate
                } else {
                    out += current
                    current = w
                }
            }
            if (current.isNotEmpty()) out += current
            out
        }


    private companion object {
        // A4 in PostScript points.
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48f
        const val RIGHT = PAGE_WIDTH - MARGIN
        const val BOTTOM = PAGE_HEIGHT - 60f
        const val LINE = 14f
        const val ROW_PAD = 7f

        const val COL_DESC = MARGIN
        const val DESC_WIDTH = 270f
        const val COL_QTY = 380f
        const val COL_PRICE = 465f

        val ACCENT = Color.rgb(0x1F, 0x3A, 0x5F)
        val MUTED = Color.rgb(0x6B, 0x72, 0x80)
        val RULE = Color.rgb(0xD1, 0xD5, 0xDB)
        val HEADER_BG = Color.rgb(0xF1, 0xF3, 0xF6)
        val PAID = Color.rgb(0x15, 0x80, 0x3D)

        fun paint(size: Float, bold: Boolean = false, color: Int = Color.rgb(0x11, 0x18, 0x27)) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
    }
}
