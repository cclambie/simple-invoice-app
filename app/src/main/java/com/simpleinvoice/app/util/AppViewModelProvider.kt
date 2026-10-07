package com.simpleinvoice.app.util

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.simpleinvoice.app.SimpleInvoiceApp

/** Resolves the [SimpleInvoiceApp] instance from ViewModel creation extras. */
fun CreationExtras.simpleInvoiceApp(): SimpleInvoiceApp {
    val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
    return application as SimpleInvoiceApp
}
