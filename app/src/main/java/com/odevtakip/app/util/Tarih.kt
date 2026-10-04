package com.odevtakip.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
private val tr: Locale = Locale("tr")

private val tarihBicim = DateTimeFormatter.ofPattern("d MMM yyyy", tr)
private val saatBicim = DateTimeFormatter.ofPattern("HH:mm", tr)

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
