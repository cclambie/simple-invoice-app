package com.simpleinvoice.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.simpleinvoice.app.data.model.BusinessProfile
import com.simpleinvoice.app.data.model.Client
import com.simpleinvoice.app.data.model.Invoice
import com.simpleinvoice.app.data.model.InvoiceLineItem

@Database(
    entities = [BusinessProfile::class, Client::class, Invoice::class, InvoiceLineItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceLineItemDao(): InvoiceLineItemDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simple_invoice.db"
                ).build().also { instance = it }
            }
    }
}
