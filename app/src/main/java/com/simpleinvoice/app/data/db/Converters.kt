package com.simpleinvoice.app.data.db

import androidx.room.TypeConverter
import com.simpleinvoice.app.data.model.TaxIdType
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
}
