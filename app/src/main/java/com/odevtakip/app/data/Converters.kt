package com.odevtakip.app.data

import androidx.room.TypeConverter

/** [Durum] enum'unu veritabanında metin olarak saklar. */
class Converters {

    @TypeConverter
    fun durumdanMetne(durum: Durum): String = durum.name

    @TypeConverter
    fun metnedenDuruma(value: String?): Durum = Durum.guvenliDeger(value)
}
