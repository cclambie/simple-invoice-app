package com.simpleinvoice.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A reusable line item that can be added to any invoice. */
@Entity(tableName = "saved_items")
data class SavedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val unitPrice: Double
)
