package com.odevtakip.app.data

import androidx.room.TypeConverter

/**
 * Veritabanı ile Kotlin tipleri arasında köprü.
 *
 * Tüm dönüştürücüler **tek yönlü** düşünülür: yazma her zaman
 * yazdıranın kendi biçimini üretir, okuma ise gelen metni olabildiğince
 * bağışlayıcı çözer. Böylece elle düzenlenmiş ya da eski bir satır,
 * okuma anında çökertmez.
 */
class Converters {

    /** [Durum] enum'unu veritabanında metin olarak saklar. */
    @TypeConverter
    fun durumdanMetne(durum: Durum): String = durum.name

    @TypeConverter
    fun metnedenDuruma(value: String?): Durum = Durum.guvenliDeger(value)

    /** [DenemeTuru] enum'unu veritabanında metin olarak saklar. */
    @TypeConverter
    fun turdenMetne(tur: DenemeTuru): String = tur.name

    @TypeConverter
    fun metnedenTure(value: String?): DenemeTuru = DenemeTuru.guvenliDeger(value)

    /**
     * Ders bazlı sonuçları [sonuclariYaz] biçimiyle saklar.
     *
     * Kolon tek bir `TEXT`'tir: ders başına sütun açmak, her yeni tür için
     * şema değişikliği demek olurdu; metin ise türden **bağımsız** kalır.
     */
    @TypeConverter
    fun sonuclardanMetne(sonuclar: List<DersSonucu>): String = sonuclariYaz(sonuclar)

    @TypeConverter
    fun metnedenSonuclara(value: String?): List<DersSonucu> =
        sonuclariCoz(value.orEmpty())
}
