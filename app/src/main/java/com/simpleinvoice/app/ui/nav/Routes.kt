package com.simpleinvoice.app.ui.nav

object Routes {
    const val INVOICE_LIST = "invoices"
    const val CLIENT_LIST = "clients"
    const val REPORTS = "reports"
    const val BUSINESS_SETTINGS = "business"

    const val BACKUP = "backup"

    const val INVOICE_EDIT = "invoice_edit/{invoiceId}"
    const val INVOICE_DETAIL = "invoice_detail/{invoiceId}"
    const val CLIENT_EDIT = "client_edit/{clientId}"

    fun invoiceEdit(invoiceId: Long) = "invoice_edit/$invoiceId"
    fun invoiceDetail(invoiceId: Long) = "invoice_detail/$invoiceId"
    fun clientEdit(clientId: Long) = "client_edit/$clientId"
}

/** Top-level destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(val route: String, val label: String) {
    Invoices(Routes.INVOICE_LIST, "Invoices"),
    Clients(Routes.CLIENT_LIST, "Clients"),
    Reports(Routes.REPORTS, "Reports"),
    Business(Routes.BUSINESS_SETTINGS, "Business")
}
