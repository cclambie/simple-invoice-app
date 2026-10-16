package com.craiglambie.invoicemaker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "payment_logs",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("invoiceId"), Index("timestamp")]
)
data class PaymentLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val amountCurrency: String,
    val reference: String,
    val clientName: String,
    val source: String = "tasker",
    val invoiceId: Long? = null,
    val timestamp: Instant = Instant.now(),
    val notes: String = ""
)
