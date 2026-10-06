package com.odevtakip.app

import com.odevtakip.app.data.GUNLUK_DERS_SAYISI
import com.odevtakip.app.data.HaftaGunu
import com.odevtakip.app.data.ProgramSatiri
import com.odevtakip.app.data.gununDersSayisi
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ders programının saf kısımlarının birim testleri (Faz 14–15).
 *
 * Burada veritabanı değil, ızgaranın **eşleme kuralları** sınanır: sıra →
 * gün dönüşümü, Türkçe gün adları, bir saatin boş olup olmadığı ve satır
 * sayısının nasıl türrediği. Ekranın çizimi Compose testiyle değil, elle
 * gözle doğrulanır — projenin genel kuralıyla aynı.
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

    // ---- Satır sayısı (Faz 15: ders ekleme) ----

    @Test
    fun `kayit yoksa gun sekiz satirla acilir`() {
        assertEquals(GUNLUK_DERS_SAYISI, gununDersSayisi(gun = 0, program = emptyList()))
    }

    @Test
    fun `satirlar sekizden az olsa bile alt sinir sekizde kalir`() {
        // Ders yalnızca 3. saate atanmış olabilir; ekran yine de sekiz
        // satır çizer, eksik saatler "Ders seç" olarak görünür.
        val program = listOf(ProgramSatiri(gun = 0, sira = 3, ders = "Fizik"))

        assertEquals(GUNLUK_DERS_SAYISI, gununDersSayisi(gun = 0, program = program))
    }

    @Test
    fun `eklenen dokuzuncu satir sayiyi artirir`() {
        val program = (1..9).map { ProgramSatiri(gun = 0, sira = it, ders = "") }

        assertEquals(9, gununDersSayisi(gun = 0, program = program))
    }

    @Test
    fun `satir sayisi yalnizca secili gunun satirlariyla olcur`() {
        // Pazartesi 10 satıra uzatılmış olsa bile salı sekizde kalır:
        // sayı günü özeldir, haftaya yayılmaz.
        val program = buildList {
            (1..10).forEach { add(ProgramSatiri(gun = HaftaGunu.PAZARTESI.sira, sira = it)) }
            add(ProgramSatiri(gun = HaftaGunu.SALI.sira, sira = 1, ders = "Matematik"))
        }

        assertEquals(10, gununDersSayisi(gun = HaftaGunu.PAZARTESI.sira, program = program))
        assertEquals(GUNLUK_DERS_SAYISI, gununDersSayisi(gun = HaftaGunu.SALI.sira, program = program))
        assertEquals(GUNLUK_DERS_SAYISI, gununDersSayisi(gun = HaftaGunu.PAZAR.sira, program = program))
    }

    @Test
    fun `bos satir da sayilir cunku o saat var demektir`() {
        // "Ders ekle" ile açılan satır henüz dolu değil ama varlığı
        // sayıyı taşır; dolu satırla aynı muameleyi görmelidir.
        val program = (1..8).map { ProgramSatiri(gun = 0, sira = it, ders = "Matematik") } +
            ProgramSatiri(gun = 0, sira = 9, ders = "")

        assertEquals(9, gununDersSayisi(gun = 0, program = program))
    }

    @Test
    fun `kaldirmadan sonra sayi eski sinirina doner`() {
        val dokuz = (1..9).map { ProgramSatiri(gun = 0, sira = it) }
        val sekiz = dokuz.filterNot { it.sira == 9 }

        assertEquals(9, gununDersSayisi(gun = 0, program = dokuz))
        assertEquals(GUNLUK_DERS_SAYISI, gununDersSayisi(gun = 0, program = sekiz))
    }
}
