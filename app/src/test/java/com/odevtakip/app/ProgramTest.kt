package com.odevtakip.app

import com.odevtakip.app.data.GUNLUK_DERS_SAYISI
import com.odevtakip.app.data.HaftaGunu
import com.odevtakip.app.data.ProgramSatiri
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ders programının saf kısımlarının birim testleri (Faz 14).
 *
 * Burada veritabanı değil, ızgaranın **eşleme kuralları** sınanır: sıra →
 * gün dönüşümü, Türkçe gün adları ve bir saatin boş olup olmadığı. Ekranın
 * çizimi Compose testiyle değil, elle gözle doğrulanır — projenin genel
 * kuralıyla aynı.
 */
class ProgramTest {

    // ---- Gün sırası ----

    @Test
    fun `sira ile gun birbirine donusur`() {
        HaftaGunu.entries.forEach { gun ->
            assertEquals(gun, HaftaGunu.den(gun.sira))
        }
    }

    @Test
    fun `pazartesi sifirdir ve pazar yedide biter`() {
        assertEquals(0, HaftaGunu.PAZARTESI.sira)
        assertEquals(6, HaftaGunu.PAZAR.sira)
    }

    @Test
    fun `arlik disindaki sira sessizce pazartesye duser`() {
        assertEquals(HaftaGunu.PAZARTESI, HaftaGunu.den(-1))
        assertEquals(HaftaGunu.PAZARTESI, HaftaGunu.den(7))
        assertEquals(HaftaGunu.PAZARTESI, HaftaGunu.den(99))
    }

    @Test
    fun `bugunun sirasi yerel tarihle ayni`() {
        assertEquals(LocalDate.now().dayOfWeek.value - 1, HaftaGunu.bugun().sira)
    }

    @Test
    fun `gun sirasi iso haftanin sirasiyla eslesir`() {
        // 1 Ocak 2024 pazartesidir; her gün ISO sırasını taşır.
        val referans = LocalDate.of(2024, 1, 1)
        HaftaGunu.entries.forEach { gun ->
            val tarih = referans.plusDays(gun.sira.toLong())
            assertEquals(gun, HaftaGunu.den(tarih.dayOfWeek.value - 1))
        }
    }

    // ---- Türkçe gün adları ----

    @Test
    fun `gun uzun adlari turkcedir`() {
        assertEquals("Pazartesi", HaftaGunu.PAZARTESI.uzunAdi())
        assertEquals("Salı", HaftaGunu.SALI.uzunAdi())
        assertEquals("Çarşamba", HaftaGunu.CARSAMBA.uzunAdi())
        assertEquals("Cumartesi", HaftaGunu.CUMARTESI.uzunAdi())
    }

    @Test
    fun `kisa adlar bos degil ve birbirinden farkli`() {
        val kisalar = HaftaGunu.entries.map { it.kisaAdi() }

        assertTrue(kisalar.none { it.isBlank() })
        assertEquals(HaftaGunu.entries.size, kisalar.distinct().size)
    }

    // ---- Saat satırı ----

    @Test
    fun `bos satir ders adina gore belirlenir`() {
        assertTrue(ProgramSatiri(gun = 0, sira = 1).bosMu)
        assertTrue(ProgramSatiri(gun = 0, sira = 1, ders = "").bosMu)
        assertTrue(ProgramSatiri(gun = 0, sira = 1, ders = "   ").bosMu)
        assertFalse(ProgramSatiri(gun = 0, sira = 1, ders = "Matematik").bosMu)
    }

    @Test
    fun `gunluk ders sayisi sekizdir`() {
        assertEquals(8, GUNLUK_DERS_SAYISI)
    }

    @Test
    fun `bir gunun tum saatleri tek tek numaralandirilir`() {
        val gun = HaftaGunu.SALI.sira
        val satirlar = (1..GUNLUK_DERS_SAYISI).map { sira ->
            ProgramSatiri(gun = gun, sira = sira, ders = if (sira == 3) "Fizik" else "")
        }

        assertEquals(GUNLUK_DERS_SAYISI, satirlar.size)
        assertEquals(1, satirlar.first().sira)
        assertEquals(GUNLUK_DERS_SAYISI, satirlar.last().sira)
        assertEquals(1, satirlar.count { !it.bosMu })
        assertEquals("Fizik", satirlar.single { !it.bosMu }.ders)
        assertEquals(gun, satirlar.first().gun)
    }
}
