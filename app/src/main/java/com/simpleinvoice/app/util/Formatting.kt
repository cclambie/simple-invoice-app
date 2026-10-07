package com.simpleinvoice.app.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

fun Double.asCurrency(): String = currencyFormat.format(this)

fun LocalDate.asDisplayDate(): String = format(dateFormatter)
