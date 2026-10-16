package com.craiglambie.invoicemaker.data.db

import androidx.room.TypeConverter
import com.craiglambie.invoicemaker.data.model.TaxIdType
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromTaxIdType(value: String?): TaxIdType = value?.let { TaxIdType.valueOf(it) } ?: TaxIdType.GST

    @TypeConverter
    fun toTaxIdType(type: TaxIdType?): String = (type ?: TaxIdType.GST).name

    @TypeConverter
    fun fromInstant(millis: Long?): Instant? = millis?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toInstant(instant: Instant?): Long? = instant?.toEpochMilli()
}
