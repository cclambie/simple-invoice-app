package com.simpleinvoice.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    /** Set when this client was imported from the device contact book, for reference only. */
    val importedFromContactId: String? = null
)
