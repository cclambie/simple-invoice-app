package com.simpleinvoice.app.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

/** Currencies offered in pickers; any other ISO 4217 code already in use is still shown. */
val commonCurrencyCodes = listOf("AUD", "GBP", "USD", "EUR", "NZD", "CAD", "SGD", "HKD", "JPY", "CHF", "ZAR", "INR")

/** The currency of the device's region, used as the starting business currency. */
fun deviceCurrencyCode(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull() ?: "USD"

/** Formats an amount in the given ISO 4217 currency, e.g. "A$1,234.50" or "£1,234.50". */
fun Double.asCurrency(currencyCode: String): String =
    NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        runCatching { currency = Currency.getInstance(currencyCode) }
    }.format(this)

/** "AUD – Australian Dollar" */
fun currencyLabel(currencyCode: String): String =
    runCatching { "$currencyCode – ${Currency.getInstance(currencyCode).displayName}" }.getOrDefault(currencyCode)

/** A quantity or percentage without a pointless ".0": 1.0 -> "1", 2.5 -> "2.5". */
fun Double.asPlainNumber(): String =
    if (this == Math.floor(this) && !isInfinite()) toLong().toString() else toString()

fun LocalDate.asDisplayDate(): String = format(dateFormatter)

private val decimalInput = Regex("""\d*\.?\d*""")

/**
 * Normalises text typed into a numeric field: a comma decimal separator becomes a dot.
 * Returns null if the text isn't a plain non-negative decimal, so the edit can be rejected.
 */
fun String.asDecimalInput(): String? = replace(',', '.').takeIf { decimalInput.matches(it) }
