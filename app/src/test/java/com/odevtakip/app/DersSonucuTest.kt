package com.odevtakip.app

import com.odevtakip.app.data.DersSonucu
import com.odevtakip.app.data.sonuclariCoz
import com.odevtakip.app.data.sonuclariYaz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ders bazlı sonuçların veritabanı biçimi: [sonuclariYaz] / [sonuclariCoz].
 *
 * Biçim `"Türkçe=35/4;Sosyal=12/3"` şeklindedir; Room bunu tek bir
 * `TEXT` kolonuna yazar. Bu testler iki şeyi kilitler:
 *
 *  1. **Gidiş-dönüş kayıpsızdır**: yazıp okuyan aynı listeyi alır.
 *  2. **Bozuk satır bütün kaydı yutamaz**: hatalı parça atlanır,
 *     geri kalan satırlar okunur.
 */
class DersSonucuTest {

    @Test
    fun gidisDonusKayipsizdir() {
        val orijinal = listOf(
            DersSonucu("Türkçe", dogru = 35, yanlis = 4),
            DersSonucu("Sosyal", dogru = 12, yanlis = 3),
            DersSonucu("Matematik", dogru = 30, yanlis = 2),
            DersSonucu("Fen", dogru = 15, yanlis = 3),
        )

        assertEquals(orijinal, sonuclariCoz(sonuclariYaz(orijinal)))
    }

    @Test
    fun bosListeBosMetinYazilir() {
        assertEquals("", sonuclariYaz(emptyList()))
    }

    @Test
    fun bosMetinBosListeCozulur() {
        assertEquals(emptyList<DersSonucu>(), sonuclariCoz(""))
    }

    @Test
    fun tekSatirYazilir() {
        assertEquals(
            "Türkçe=35/4",
            sonuclariYaz(listOf(DersSonucu("Türkçe", dogru = 35, yanlis = 4))),
        )
    }

    @Test
    fun sifirDegerlerYazilir() {
        // "0" yazılır, satır atlanmaz: kayıt "bu derste soru yok"
        // bilgisini kaybetmez.
        assertEquals("Fizik=0/0", sonuclariYaz(listOf(DersSonucu("Fizik"))))
        assertEquals(listOf(DersSonucu("Fizik")), sonuclariCoz("Fizik=0/0"))
    }

    @Test
    fun bozukSatirlarAtlanir() {
        // Ara satır bozuk; ilk ve son satır yine okunur.
        val sonuclar = sonuclariCoz("Türkçe=35/4;bozuk;Sosyal=12/3")

        assertEquals(
            listOf(
                DersSonucu("Türkçe", dogru = 35, yanlis = 4),
                DersSonucu("Sosyal", dogru = 12, yanlis = 3),
            ),
            sonuclar,
        )
    }

    @Test
    fun eksikDegerliSatirAtlanir() {
        assertTrue(sonuclariCoz("Türkçe=35").isEmpty())
        assertTrue(sonuclariCoz("Türkçe=").isEmpty())
        assertTrue(sonuclariCoz("=35/4").isEmpty())
    }

    @Test
    fun sayisalOlmayanDegerlerAtlanir() {
        assertTrue(sonuclariCoz("Türkçe=otuz/4").isEmpty())
        assertTrue(sonuclariCoz("Türkçe=35/üç").isEmpty())
    }

    @Test
    fun negatifSayilarSifirlanir() {
        // Elle düzenlenmiş bir kaydın toplamı eksiye düşemesin.
        assertEquals(
            listOf(DersSonucu("Türkçe", dogru = 0, yanlis = 4)),
            sonuclariCoz("Türkçe=-5/4"),
        )
    }

    @Test
    fun turkceKarakterlerKorunur() {
        val ad = "Din Kültürü"
        assertEquals(
            listOf(DersSonucu(ad, dogru = 3, yanlis = 1)),
            sonuclariCoz(sonuclariYaz(listOf(DersSonucu(ad, dogru = 3, yanlis = 1)))),
        )
    }
}
