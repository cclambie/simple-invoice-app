package com.simpleinvoice.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the user's business details.
 * [id] is always [SINGLETON_ID] so there is exactly one profile.
 */
@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val businessName: String = "",
    val address: String = "",
    val email: String = "",
    val phone: String = "",
    val taxIdType: TaxIdType = TaxIdType.GST,
    val taxIdCustomLabel: String = "",
    val taxIdNumber: String = "",
    val defaultTaxRatePercent: Double = 0.0
) {
    /** Label to show next to the tax number, e.g. "GST", "VAT", or a custom label. */
    val effectiveTaxIdLabel: String
        get() = if (taxIdType == TaxIdType.OTHER && taxIdCustomLabel.isNotBlank()) {
            taxIdCustomLabel
        } else {
            taxIdType.label
        }

    companion object {
        const val SINGLETON_ID = 1
    }
}
