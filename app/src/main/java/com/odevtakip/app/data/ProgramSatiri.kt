package com.odevtakip.app.data

import androidx.room.Entity
import com.odevtakip.app.util.gunKisaAdi
import com.odevtakip.app.util.gunUzunAdi
import java.time.LocalDate

/**
 * Bir okul gününde okunan ders saati sayısı.
 *
 * Program **haftalık**dır: kullanıcı haftanın gününü seçip o günün
 * [GUNLUK_DERS_SAYISI] satırını doldurur; her hafta aynı düzen tekrarlanır.
 */
const val GUNLUK_DERS_SAYISI: Int = 8

/**
 * Haftanın günü.
 *
 * Sıra, ISO-8601'in gün sırasıyla aynıdır: `0 = pazartesi … 6 = pazar`.
 * Hem veritabanındaki `gun` kolonu hem de arayüzdeki çip sırası bu değeri
 * kullanır; ayrı bir indeks tablosu tutulmaz.
 *
 * Gün adları [REFERANS] tarihinden üretilir — böylece Türkçe biçimlendirme
 * tek yerde (`util.Tarih`) kalır ve program ekranı takvimle aynı dili konuşur.
 */
enum class HaftaGunu(val sira: Int) {
    PAZARTESI(0),
    SALI(1),
    CARSAMBA(2),
    PERSEMBE(3),
    CUMA(4),
    CUMARTESI(5),
    PAZAR(6);

    companion object {
        /** 1 Ocak 2024 pazartesidir; gün adları bu referans günün üstünden okunur. */
        private val REFERANS = LocalDate.of(2024, 1, 1)

        /**
         * Sıra değerini güne çevirir.
         *
         * Aralık dışı bir değer (negatif ya da 7 ve sonrası) sessizce
         * pazartesye düşer. Dışarıdan girdi her zaman [den] üzerinden
         * geçirildiği için arayüzde bilinmeyen bir gün oluşmaz.
         */
        fun den(sira: Int): HaftaGunu = entries.getOrElse(sira) { PAZARTESI }

        /** Bugünün hafta içi sırası (0 = pazartesi). */
        fun bugun(): HaftaGunu = den(LocalDate.now().dayOfWeek.value - 1)
    }

    /** Adı biçimsel olarak okunacak tarih karşılığı. */
    private val tarih: LocalDate get() = REFERANS.plusDays(sira.toLong())

    /** "Pzt" gibi kısa gün adı — program çipleri. */
    fun kisaAdi(): String = tarih.gunKisaAdi()

    /** "Pazartesi" gibi uzun gün adı — seçili günün başlığı. */
    fun uzunAdi(): String = tarih.gunUzunAdi()
}

/**
 * Haftanın bir gününde bir ders saati.
 *
 * Tablo bir **ızgaradır**: `(gun, sira)` çifti tekildir; ikinci kez yazılan
 * kayıt mevcut satırı değiştirir ([com.odevtakip.app.data.ProgramDao]).
 *
 * [ders] o saatte okunan dersin **adını** tutar, kimlik değil —
 * [Odev.ders] ile aynı gerekçe: bir ders listeden silinirse programdaki
 * yazısı yerinde kalır, ekran boşlukla değil o yazıyla karşılaşır.
 * Saate hiç ders atanmadıysa satır silinmez, yalnızca `ders` boş bırakılır;
 * böylece ekran her açılışta 8 satırı aynı düzende çizebilir.
 *
 * @property gun ISO günü: 0 = pazartesi … 6 = pazar.
 * @property sira Ders saatinin sırası, 1'den başlar.
 * @property ders Atanan ders adı; atanmadıysa `""`.
 */
@Entity(tableName = "program", primaryKeys = ["gun", "sira"])
data class ProgramSatiri(
    val gun: Int,
    val sira: Int,
    val ders: String = "",
) {
    /** Bu ders saatine henüz ders atanmadı mı? */
    val bosMu: Boolean get() = ders.isBlank()
}
