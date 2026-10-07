package com.simpleinvoice.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Bank details printed on invoices issued in [currencyCode]. One account per currency. */
@Entity(tableName = "payment_accounts")
data class PaymentAccount(
    @PrimaryKey val currencyCode: String,
    val accountName: String = "",
    /** BSB, sort code, routing number or BIC depending on the currency; see [bankCodeLabel]. */
    val bankCode: String = "",
    /** Account number, or IBAN for euro accounts; see [accountNumberLabel]. */
    val accountNumber: String = ""
) {
    val isBlank: Boolean
        get() = accountName.isBlank() && bankCode.isBlank() && accountNumber.isBlank()

    companion object {
        /** What local banks call the branch/bank identifier for a currency. */
        fun bankCodeLabel(currencyCode: String): String = when (currencyCode) {
            "AUD" -> "BSB"
            "GBP" -> "Sort code"
            "USD" -> "Routing number (ABA)"
            "EUR" -> "BIC / SWIFT"
            "CAD" -> "Transit / institution number"
            "INR" -> "IFSC"
            else -> "Bank / branch code"
        }

        fun accountNumberLabel(currencyCode: String): String =
            if (currencyCode == "EUR") "IBAN" else "Account number"
    }
}
