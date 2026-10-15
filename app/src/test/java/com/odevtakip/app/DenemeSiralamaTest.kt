package com.odevtakip.app

import com.odevtakip.app.data.Deneme
import com.odevtakip.app.data.DersSonucu
import com.odevtakip.app.ui.DenemeSiralamasi
import com.odevtakip.app.ui.denemeleriSirala
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deneme sıralamasının saf karar noktaları.
 *
 * Ekran yalnızca [denemeleriSirala]ın döndürdüğü sırayı çizer; buradaki bir
 * sözleşme ihlali doğrudan yanlış listeye dönüşür. Varsayılan
 * [DenemeSiralamasi.TARIH_YENI]nin ayrıca test edilmesi gerekir: DAO da
 * aynı sırayı verir, ama akışın gelen sıraya güvenmeyip sözü kendisinde
 * tuttuğunu bilmek isteriz.
 */
class DenemeSiralamaTest {

    private fun deneme(
        ad: String,
        tarih: Long = 1_000L,
        id: Long = 0L,
        olusturmaTarihi: Long = 1_000L,
        dogru: Int = 0,
        yanlis: Int = 0,
    ) = Deneme(
        id = id,
        ad = ad,
        tarih = tarih,
        olusturmaTarihi = olusturmaTarihi,
        sonuclar = listOf(DersSonucu(ders = "Matematik", dogru = dogru, yanlis = yanlis)),
    )

    /** Deneme adlarını olduğu gibi döndürür — okunuruk için. */
    private fun List<Deneme>.adlar() = map { it.ad }

    // ---- Tarih ----

    @Test
    fun `varsayilan siralama tarihi azalan kurar`() {
        val liste = listOf(
            deneme("Eski", tarih = 100L, id = 1L),
            deneme("Yeni", tarih = 300L, id = 3L),
            deneme("Orta", tarih = 200L, id = 2L),
        )

        assertEquals(
            listOf("Yeni", "Orta", "Eski"),
            liste.denemeleriSirala().adlar(),
        )
    }

    @Test
    fun `esit tarihde id azalan siralar`() {
        // Aynı güne yazılan iki denemenin sırası DAO'da id ile belli olur;
        // varsayılan sıralama bunu elle de kurar.
        val liste = listOf(
            deneme("A", tarih = 100L, id = 1L),
            deneme("B", tarih = 100L, id = 3L),
            deneme("C", tarih = 100L, id = 2L),
        )

        assertEquals(
            listOf("B", "C", "A"),
            liste.denemeleriSirala().adlar(),
        )
    }

    @Test
    fun `tarih eski once siralar`() {
        val liste = listOf(
            deneme("Yeni", tarih = 300L, id = 3L),
            deneme("Eski", tarih = 100L, id = 1L),
            deneme("Orta", tarih = 200L, id = 2L),
        )

        val sonuc = liste.denemeleriSirala(DenemeSiralamasi.TARIH_ESKI)

        assertEquals(listOf("Eski", "Orta", "Yeni"), sonuc.adlar())
    }

    @Test
    fun `son eklenen once siralar`() {
        // Eklenme sırası, sınav tarihinden bağımsızdır: geriye dönük girilen
        // bir kayıt "en yeni"dir ama tarihi eskidir — iki seçenek ayrı durur.
        val liste = listOf(
            deneme("Ilk", olusturmaTarihi = 1_000L, tarih = 300L),
            deneme("Son", olusturmaTarihi = 3_000L, tarih = 100L),
            deneme("Orta", olusturmaTarihi = 2_000L, tarih = 200L),
        )

        val sonuc = liste.denemeleriSirala(DenemeSiralamasi.EKLENEN_YENI)

        assertEquals(listOf("Son", "Orta", "Ilk"), sonuc.adlar())
    }

    // ---- Ada göre ----

    @Test
    fun `ada gore alfabetik siralar`() {
        // Küçük harf anahtarı Türkçe'ye duyarlıdır: "MATEMATİK" de "matematik"
        // gibi, "İngilizce" de "i" grubunda sıralanır.
        val liste = listOf(
            deneme("zulu"),
            deneme("Almanca"),
            deneme("İngilizce"),
            deneme("MATEMATİK"),
        )

        val sonuc = liste.denemeleriSirala(DenemeSiralamasi.AD_A_Z)

        assertEquals(
            listOf("Almanca", "İngilizce", "MATEMATİK", "zulu"),
            sonuc.adlar(),
        )
    }

    @Test
    fun `ada gore ters siralama zden aye gider`() {
        val liste = listOf(
            deneme("zulu"),
            deneme("Almanca"),
            deneme("İngilizce"),
            deneme("MATEMATİK"),
        )

        val sonuc = liste.denemeleriSirala(DenemeSiralamasi.AD_Z_A)

        assertEquals(
            listOf("zulu", "MATEMATİK", "İngilizce", "Almanca"),
            sonuc.adlar(),
        )
    }

    @Test
    fun `esit adda gelen sira korunur`() {
        // Aynı adlı iki deneme: alfabetik eşitlendiğinde sıra bozulmaz,
        // gelen (tarih azalan) düzen yerinde kalır.
        val liste = listOf(
            deneme("Aynı Ad", tarih = 300L),
            deneme("Aynı Ad", tarih = 100L),
        )

        assertEquals(
            listOf(300L, 100L),
            liste.denemeleriSirala(DenemeSiralamasi.AD_A_Z).map { it.tarih },
        )
    }

    // ---- Nete göre ----

    @Test
    fun `nete gore dusen siralar`() {
        val liste = listOf(
            deneme("Düşük", dogru = 20, yanlis = 20), // net 15
            deneme("Yüksek", dogru = 40, yanlis = 8), // net 38
            deneme("Orta", dogru = 30, yanlis = 12),  // net 27
        )

        val sonuc = liste.denemeleriSirala(DenemeSiralamasi.NET_DUSEN)

        assertEquals(listOf("Yüksek", "Orta", "Düşük"), sonuc.adlar())
    }

    @Test
    fun `nete gore artan siralar`() {
        val liste = listOf(
            deneme("Düşük", dogru = 20, yanlis = 20),
            deneme("Yüksek", dogru = 40, yanlis = 8),
            deneme("Orta", dogru = 30, yanlis = 12),
        )

        val sonuc = liste.denemeleriSirala(DenemeSiralamasi.NET_ARTAN)

        assertEquals(listOf("Düşük", "Orta", "Yüksek"), sonuc.adlar())
    }

    @Test
    fun `esit nette gelen sira korunur`() {
        // Net aynıysa kararlı sıralama devreye girer: DAO sırası (tarih
        // azalan) eşitler arasında aynen taşınır.
        val liste = listOf(
            deneme("Yeni tarihli", tarih = 300L, dogru = 10),
            deneme("Eski tarihli", tarih = 100L, dogru = 10),
        )

        val sonuc = liste.denemeleriSirala(DenemeSiralamasi.NET_DUSEN)

        assertEquals(listOf("Yeni tarihli", "Eski tarihli"), sonuc.adlar())
    }

    // ---- Bütünlük ----

    @Test
    fun `siralama hicbir denemeyi ekleyip cikarmaz`() {
        val liste = listOf(
            deneme("A", tarih = 100L),
            deneme("B", tarih = 200L, dogru = 30, yanlis = 10),
            deneme("C", tarih = 300L, olusturmaTarihi = 5_000L),
        )

        DenemeSiralamasi.entries.forEach { secenek ->
            assertEquals(
                "siralama=$secenek",
                liste.size,
                liste.denemeleriSirala(secenek).size,
            )
        }
    }

    @Test
    fun `bos liste her secenekte bos doner`() {
        DenemeSiralamasi.entries.forEach { secenek ->
            assertTrue(
                "siralama=$secenek",
                emptyList<Deneme>().denemeleriSirala(secenek).isEmpty(),
            )
        }
    }
}
