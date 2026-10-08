package com.odevtakip.app

import com.odevtakip.app.util.ayinHucreleri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Aylık takvim ızgarasının saf hesabı — [ayinHucreleri].
 *
 * Seçilen örnekler bilinçli: farklı hafta gününde başlayan, farklı uzunlukta
 * olan, artik yıl ve "dolgu gerektirmeyen" sınırlar.
 */
class AyTakvimTest {

    private val ornekler = listOf(
        YearMonth.of(2026, 10), // perşembe başlangıç, 31 gün, 5 satır
        YearMonth.of(2024, 2), // artik yıl, 29 gün
        YearMonth.of(2027, 2), // pazartesi başlangıç, dolgu yok
        YearMonth.of(2027, 8), // pazar başlangıç, 6 satır
        YearMonth.of(2026, 4), // çarşamba başlangıç, 30 gün
        YearMonth.of(2025, 12),
    )

    @Test
    fun `uzunluk her ayda yedinin kati`() {
        ornekler.forEach { ay ->
            val hucreler = ayinHucreleri(ay)
            assertEquals("$ay: satır tamamlanmalı", 0, hucreler.size % 7)
            assertTrue("$ay: en az 4 satır olmalı", hucreler.size >= 28)
            assertTrue("$ay: en çok 6 satır olmalı", hucreler.size <= 42)
        }
    }

    @Test
    fun `ayin gunleri eksiksiz ve sirali gelir`() {
        ornekler.forEach { ay ->
            val dolu = ayinHucreleri(ay).filterNotNull()

            assertEquals("$ay: gün sayısı", ay.lengthOfMonth(), dolu.size)
            dolu.forEachIndexed { i, gun ->
                assertEquals("$ay: ${i + 1}. gün", ay.atDay(i + 1), gun)
            }
        }
    }

    @Test
    fun `dolgu null ister ve disariya tasmaz`() {
        ornekler.forEach { ay ->
            val hucreler = ayinHucreleri(ay)
            val ilkDolu = hucreler.indexOfFirst { it != null }
            val sonDolu = hucreler.indexOfLast { it != null }

            assertTrue("$ay: hiç dolu hücre yok", ilkDolu >= 0)
            assertEquals("$ay: ilk dolu", ay.atDay(1), hucreler[ilkDolu])
            assertEquals("$ay: son dolu", ay.atEndOfMonth(), hucreler[sonDolu])

            for (i in 0 until ilkDolu) {
                assertNull("$ay: baş dolgusu", hucreler[i])
            }
            for (i in sonDolu + 1 until hucreler.size) {
                assertNull("$ay: son dolgusu", hucreler[i])
            }
            // Dolgu bir haftayı geçemez; aksi hâlde ızgara satır taşırır.
            assertTrue("$ay: son dolgu", hucreler.size - sonDolu - 1 < 7)
        }
    }

    @Test
    fun `her sutun kendi hafta gununde sabit kalir`() {
        ornekler.forEach { ay ->
            val hucreler = ayinHucreleri(ay)

            for (i in hucreler.indices) {
                val gun = hucreler[i] ?: continue
                if (i % 7 == 0) {
                    assertEquals("$ay: ilk sütun pazartesi", DayOfWeek.MONDAY, gun.dayOfWeek)
                }
                if (i % 7 == 6) {
                    assertEquals("$ay: son sütun pazar", DayOfWeek.SUNDAY, gun.dayOfWeek)
                }
            }
        }
    }

    @Test
    fun `pazartesi ile baslayan ayin dolgusu yoktur`() {
        // 1 Şubat 2027 pazartesidir: dolgu olmadan tam dört satır.
        val hucreler = ayinHucreleri(YearMonth.of(2027, 2))

        assertEquals(28, hucreler.size)
        assertTrue(hucreler.none { it == null })
        assertEquals(LocalDate.of(2027, 2, 1), hucreler.first())
        assertEquals(LocalDate.of(2027, 2, 28), hucreler.last())
    }

    @Test
    fun `pazar ile baslayan ay alti satir kapar`() {
        // 1 Ağustos 2027 pazardır: altı baş dolgusu + 31 gün = 42 hücre.
        val hucreler = ayinHucreleri(YearMonth.of(2027, 8))

        assertEquals(42, hucreler.size)
        assertEquals(LocalDate.of(2027, 8, 1), hucreler[6])
        assertTrue(hucreler.take(6).all { it == null })
    }

    @Test
    fun `artik yil subati yirmi dokuz gununu verir`() {
        val hucreler = ayinHucreleri(YearMonth.of(2024, 2)).filterNotNull()

        assertEquals(29, hucreler.size)
        assertEquals(LocalDate.of(2024, 2, 29), hucreler.last())
    }
}
