package com.simpleinvoice.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The client's business (or personal) name shown as the invoice recipient. */
    val name: String,
    /** Optional person to address the invoice to at the client. */
    @ColumnInfo(defaultValue = "")
    val contactName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    /** ISO 4217 code this client is billed in; null means the business default currency. */
    val currencyCode: String? = null,
    /** Set when this client was imported from the device contact book, for reference only. */
    val importedFromContactId: String? = null
)
