package com.simpleinvoice.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("clientId")]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val clientId: Long,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val taxRatePercent: Double = 0.0,
    val notes: String = "",
    val isPaid: Boolean = false,
    val paidDate: LocalDate? = null
)
