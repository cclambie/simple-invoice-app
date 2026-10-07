package com.simpleinvoice.app

import android.app.Application
import com.simpleinvoice.app.data.db.AppDatabase
import com.simpleinvoice.app.repository.BusinessProfileRepository
import com.simpleinvoice.app.repository.ClientRepository
import com.simpleinvoice.app.repository.InvoiceRepository

class SimpleInvoiceApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var businessProfileRepository: BusinessProfileRepository
        private set
    lateinit var clientRepository: ClientRepository
        private set
    lateinit var invoiceRepository: InvoiceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        businessProfileRepository = BusinessProfileRepository(database.businessProfileDao())
        clientRepository = ClientRepository(database.clientDao())
        invoiceRepository = InvoiceRepository(database, database.invoiceDao(), database.invoiceLineItemDao())
    }
}
