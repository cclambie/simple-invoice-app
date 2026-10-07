package com.simpleinvoice.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simpleinvoice.app.data.model.BusinessProfile
import com.simpleinvoice.app.data.model.Client
import com.simpleinvoice.app.data.model.Invoice
import com.simpleinvoice.app.data.model.InvoiceLineItem
import com.simpleinvoice.app.data.model.PaymentAccount
import com.simpleinvoice.app.data.model.SavedItem
import com.simpleinvoice.app.util.deviceCurrencyCode

/** Current schema version; also used to reject backups made by a newer app version. */
const val DATABASE_VERSION = 6

/** File name of the database in the app's private databases directory. */
const val DATABASE_NAME = "simple_invoice.db"

@Database(
    entities = [BusinessProfile::class, Client::class, Invoice::class, InvoiceLineItem::class, SavedItem::class, PaymentAccount::class],
    version = DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceLineItemDao(): InvoiceLineItemDao
    abstract fun savedItemDao(): SavedItemDao
    abstract fun paymentAccountDao(): PaymentAccountDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN contactName TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "description TEXT NOT NULL, " +
                        "unitPrice REAL NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE business_profile ADD COLUMN defaultDueDays INTEGER NOT NULL DEFAULT 7")
            }
        }

        // Schema-identical to v3; v3 briefly declared a different default for defaultDueDays.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        /** Adds currencies; existing invoices and the business default take the device's currency. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val code = deviceCurrencyCode().filter(Char::isLetter).take(3).ifEmpty { "USD" }
                db.execSQL("ALTER TABLE business_profile ADD COLUMN currencyCode TEXT NOT NULL DEFAULT '$code'")
                db.execSQL("ALTER TABLE invoices ADD COLUMN currencyCode TEXT NOT NULL DEFAULT '$code'")
                db.execSQL("ALTER TABLE clients ADD COLUMN currencyCode TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS payment_accounts (" +
                        "currencyCode TEXT NOT NULL, " +
                        "accountName TEXT NOT NULL, " +
                        "bankCode TEXT NOT NULL, " +
                        "accountNumber TEXT NOT NULL, " +
                        "PRIMARY KEY(currencyCode))"
                )
                db.execSQL("ALTER TABLE business_profile ADD COLUMN internationalPaymentService TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN internationalPaymentLink TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
            }
    }
}
