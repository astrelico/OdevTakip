package com.odevtakip.app.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tarih/saat işlemleri için yardımcılar.
 *
 * Tüm hesaplar cihazın yerel saat diliminde yapılır. `java.time` kullandığımız
 * için DST geçişlerinde gün sınırı doğru hesaplanır (minSdk 24 için
 * core library desugaring etkinleştirildi).
 */

private val yerelSaatDilimi: ZoneId get() = ZoneId.systemDefault()
private val tr: Locale = Locale.forLanguageTag("tr")

private val tarihBicim = DateTimeFormatter.ofPattern("d MMM yyyy", tr)
private val saatBicim = DateTimeFormatter.ofPattern("HH:mm", tr)

// Takvim ekranı için uzun biçimler.
private val gunKisaBicim = DateTimeFormatter.ofPattern("EEE", tr)
private val gunUzunBicim = DateTimeFormatter.ofPattern("EEEE", tr)
private val ayBicim = DateTimeFormatter.ofPattern("MMMM yyyy", tr)
private val uzunTarihBicim = DateTimeFormatter.ofPattern("d MMMM yyyy", tr)

/** Tarih yaklaşımlarını betimler; arayüz bunu metne çevirir. */
enum class TarihYaklasimi { BUGUN, YARIN, DUN, UZAK }

// ---- Dönüştürme ----

/** Verilen anın yerel tarihini alır. */
fun Long.yerelTarih(): LocalDate =
    Instant.ofEpochMilli(this).atZone(yerelSaatDilimi).toLocalDate()

/** Verilen anın yerel saat/dakika bilgisini alır. */
fun Long.yerelSaat(): LocalTime =
    Instant.ofEpochMilli(this).atZone(yerelSaatDilimi).toLocalTime()

/** Tarih + saat bilgisini epoch millis'e çevirir (yerel saat diliminde). */
fun tarihSaatiniDonustur(tarih: LocalDate, saat: LocalTime): Long =
    tarih.atTime(saat).atZone(yerelSaatDilimi).toInstant().toEpochMilli()

/** Tarin gün başlangıcını (00:00) epoch millis olarak alır. */
fun LocalDate.gunBaslangici(): Long =
    atStartOfDay(yerelSaatDilimi).toInstant().toEpochMilli()

// ---- Biçimlendirme ----

/** "3 Eki 2026" biçiminde yerel tarih metni üretir. */
fun Long.formatliTarih(): String = yerelTarih().format(tarihBicim)

/** "14:30" biçiminde yerel saat metni üretir. */
fun Long.formatliSaat(): String = yerelSaat().format(saatBicim)

/** "Cmt" biçiminde günün kısa adını üretir — takvim şeridi üstü. */
fun LocalDate.gunKisaAdi(): String = format(gunKisaBicim)

/** "Cumartesi" biçiminde günün uzun adını üretir. */
fun LocalDate.gunUzunAdi(): String = format(gunUzunBicim)

/** "Ekim 2026" biçiminde ay ve yılı üretir. */
fun LocalDate.ayAdiVeYili(): String = format(ayBicim)

/** "3 Ekim 2026" biçiminde tam tarih metni üretir. */
fun LocalDate.tarihMetni(): String = format(uzunTarihBicim)

// ---- Aylık takvim ızgarası ----

/**
 * [ay]ın aylık takvim ızgarasında görünecek hücreleri üretir.
 *
 * Hafta **pazartesi** başlar: okul ve iş haftası böyle sürer, pazarı ilk
 * sıraya almak hafta sonunu satırın ortasında bırakıp okumayı zorlaştırırdı.
 *
 * Dönen liste iki uçtaki **boş hücrelerle** (`null`) tamamlanır:
 *
 *  - Ayın ilk gününden önceki hücreler, o günün sütununa denk gelmesi için
 *    `null`'dır; böylece her sütunun günü aynı hafta gününde sabit kalır.
 *  - Son satır da `null` ile doldurulur; ızgara dikdörtgen olmazsa satırlar
 *    farklı genişlikte görünür.
 *
 * Uzunluk her zaman 7'nin katıdır (5 ya da 6 satır) ve dönen `null` olmayan
 * günler ayın **tamamını**, eksiksiz ve artan sırada içerir.
 *
 * Cihazın takvim ayarı (ilk günün hafta günü) burayı etkilemez — sabit
 * pazartesi başlangıcı, ay boyunca sütun kaymamasını garanti eder.
 */
fun ayinHucreleri(ay: YearMonth): List<LocalDate?> {
    val ilkGun = ay.atDay(1)

    // DayOfWeek: Pazartesi=1 … Pazar=7. Pazartesi'ye göre konum 0…6.
    val onDolgu = (ilkGun.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val gunSayisi = ay.lengthOfMonth()
    val hucreSayisi = ((onDolgu + gunSayisi + 6) / 7) * 7

    return List(hucreSayisi) { i ->
        val gunSirasi = i - onDolgu
        when {
            gunSirasi < 0 || gunSirasi >= gunSayisi -> null
            else -> ilkGun.plusDays(gunSirasi.toLong())
        }
    }
}

/**
 * Tarihin bugüne göre yaklaşımını döndürür.
 *
 * Arayüz bunu "Bugün"/"Yarın"/"Dün" gibi yerelleştirilmiş metne çevirir.
 */
fun tarihYaklasimi(millis: Long): TarihYaklasimi {
    val tarih = millis.yerelTarih()
    val bugun = LocalDate.now()
    return when {
        tarih == bugun -> TarihYaklasimi.BUGUN
        tarih == bugun.plusDays(1) -> TarihYaklasimi.YARIN
        tarih == bugun.minusDays(1) -> TarihYaklasimi.DUN
        else -> TarihYaklasimi.UZAK
    }
}
