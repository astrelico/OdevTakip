package com.odevtakip.app

import com.odevtakip.app.data.Ders
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.dersAdiVarMi
import com.odevtakip.app.data.temizDersAdi
import com.odevtakip.app.ui.OdevFiltresi
import com.odevtakip.app.ui.suzulVeSirala
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ders fazının saf karar noktaları: ad temizliği, yinelenen engeli ve
 * ders filtresinin liste üzerindeki etkisi.
 *
 * Hepsi saf fonksiyon olduğundan testler JVM'de koşar; Room/DAO tarafı
 * `OdevRepositoryTest` (instrumented) ile kapsanır.
 */
class DersTest {

    private val gelecek = System.currentTimeMillis() + 5 * 86_400_000L
    private val gecmis = System.currentTimeMillis() - 86_400_000L

    private fun odev(
        baslik: String,
        ders: String = "Matematik",
        sonTarih: Long = gelecek,
        durum: Durum = Durum.BEKLIYOR,
    ) = Odev(baslik = baslik, ders = ders, sonTarih = sonTarih, durum = durum)

    // ---- Ad temizliği ----

    @Test
    fun `ders adindaki bosluklar atilir`() {
        assertEquals("Matematik", "  Matematik  ".temizDersAdi())
        assertEquals("Matematik", "Matematik".temizDersAdi())
        assertEquals("", "   ".temizDersAdi())
    }

    // ---- Yinelenen engeli ----

    @Test
    fun `ayni ders buyuk kucuk harf farkiyla tekrar eklenemez`() {
        val liste = listOf(Ders(id = 1, ad = "Matematik"))

        assertTrue(liste.dersAdiVarMi("Matematik"))
        assertTrue(liste.dersAdiVarMi("matematik"))
        assertTrue(liste.dersAdiVarMi("  MATEMATIK  "))
    }

    @Test
    fun `farkli bir ders eklenebilir`() {
        val liste = listOf(Ders(id = 1, ad = "Matematik"), Ders(id = 2, ad = "Türkçe"))

        assertFalse(liste.dersAdiVarMi("Fen Bilimleri"))
        assertFalse(liste.dersAdiVarMi("Türkcce"))
    }

    @Test
    fun `bos veya bosluklu ad listede sayilmaz`() {
        val liste = listOf(Ders(id = 1, ad = "Matematik"))

        // Yoksa kullanıcı yalnızca boşluk yazıp "zaten var" hatası alırdı.
        assertFalse(liste.dersAdiVarMi(""))
        assertFalse(liste.dersAdiVarMi("   "))
        assertFalse(emptyList<Ders>().dersAdiVarMi("Matematik"))
    }

    // ---- Ders filtresi ----

    @Test
    fun `ders filtresi yalnizca o dersteki odevleri birakir`() {
        val liste = listOf(
            odev("Mat1", ders = "Matematik"),
            odev("Mat2", ders = "Matematik"),
            odev("Tur1", ders = "Türkçe"),
        )

        val sonuc = liste.suzulVeSirala(
            filtre = OdevFiltresi.TUMU,
            tamamlananlariGizle = false,
            dersFiltresi = "Matematik",
        )

        assertEquals(listOf("Mat1", "Mat2"), sonuc.map { it.baslik })
    }

    @Test
    fun `ders filtresi yokken tum odevler kalir`() {
        val liste = listOf(
            odev("Mat1", ders = "Matematik"),
            odev("Tur1", ders = "Türkçe"),
            odev("Derssiz", ders = ""),
        )

        val sonuc = liste.suzulVeSirala(
            filtre = OdevFiltresi.TUMU,
            tamamlananlariGizle = false,
            dersFiltresi = null,
        )

        assertEquals(3, sonuc.size)
    }

    @Test
    fun `ders secilmemis odevler baska bir dersin filtresinde gorunmez`() {
        // Faz 12'den önceki kayıtların dersi boştur; "Matematik" filtresi
        // bunları da süzmezse filtre işe yaramazdı.
        val liste = listOf(
            odev("Derssiz", ders = ""),
            odev("Mat1", ders = "Matematik"),
        )

        val sonuc = liste.suzulVeSirala(
            filtre = OdevFiltresi.TUMU,
            tamamlananlariGizle = false,
            dersFiltresi = "Matematik",
        )

        assertEquals(listOf("Mat1"), sonuc.map { it.baslik })
    }

    @Test
    fun `ders filtresi eslesmeyince bos liste doner`() {
        val liste = listOf(odev("Tur1", ders = "Türkçe"))

        val sonuc = liste.suzulVeSirala(
            filtre = OdevFiltresi.TUMU,
            tamamlananlariGizle = false,
            dersFiltresi = "Matematik",
        )

        assertTrue(sonuc.isEmpty())
    }

    @Test
    fun `ders filtresi gizleme ve durum filtresiyle birlikte calisir`() {
        val simdi = System.currentTimeMillis()
        val liste = listOf(
            odev("MatBitmis", ders = "Matematik", sonTarih = simdi, durum = Durum.TAMAMLANDI),
            odev("MatAcik", ders = "Matematik", sonTarih = simdi),
            odev("TurAcik", ders = "Türkçe", sonTarih = simdi + 60_000L),
            odev("MatGeciken", ders = "Matematik", sonTarih = gecmis),
        )

        // Üç süzgeç aynı anda: ders + gizleme + "bugün".
        val sonuc = liste.suzulVeSirala(
            filtre = OdevFiltresi.BUGUN,
            tamamlananlariGizle = true,
            dersFiltresi = "Matematik",
        )

        assertEquals(listOf("MatAcik"), sonuc.map { it.baslik })
    }

    @Test
    fun `tamamlanan odev ders filtresinde en sonda durur`() {
        // Filtre sıralamayı bozmamalı: biten iş, bekleyen işin arasına girmez.
        val liste = listOf(
            odev("Bitmis", ders = "Matematik", sonTarih = gecmis, durum = Durum.TAMAMLANDI),
            odev("Bekleyen", ders = "Matematik", sonTarih = gelecek),
        )

        val sonuc = liste.suzulVeSirala(
            filtre = OdevFiltresi.TUMU,
            tamamlananlariGizle = false,
            dersFiltresi = "Matematik",
        )

        assertEquals(listOf("Bekleyen", "Bitmis"), sonuc.map { it.baslik })
    }
}
