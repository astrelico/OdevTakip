package com.odevtakip.app

import com.odevtakip.app.data.DenemeTuru
import com.odevtakip.app.data.DersSonucu
import com.odevtakip.app.data.kotaAltSonuclari
import com.odevtakip.app.data.kotaSonucu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Ana dal çözümlemesi: [kotaSonucu] ve [kotaAltSonuclari].
 *
 * Kayıt yalnızca **yaprak** satırları taşır (Tarih, Fizik …); Sosyal ve
 * Fen'un toplamları bunlardan türetilir. Ama eski kayıtlar ana dal
 * düzeyinde yazılmış olabilir (`Sosyal=12/3`) — o kayıtlar da doğru
 * görünmelidir. Bu iki işlev, ekranın (detay kartı) ve dolayısıyla
 * kullanıcının gördüğü tek doğruluk kaynağıdır.
 *
 * Saf işlev olduğu için cihaz gerekmez.
 */
class KotaSonucuTest {

    private val sosyal = DenemeTuru.TYT.dersleri.first { it.ders == "Sosyal" }
    private val turkce = DenemeTuru.TYT.dersleri.first { it.ders == "Türkçe" }

    // ---- Yaprak biçimindeki kayıt ----

    @Test
    fun yaprakKaydindanAnaDalToplanir() {
        // Bugün girilen biçim: yalnızca alt testler kayıtlı.
        val sonuclar = listOf(
            DersSonucu("Türkçe", dogru = 35, yanlis = 4),
            DersSonucu("Tarih", dogru = 3, yanlis = 1),
            DersSonucu("Coğrafya", dogru = 4, yanlis = 1),
            DersSonucu("Felsefe", dogru = 3, yanlis = 1),
            DersSonucu("Din Kültürü", dogru = 2, yanlis = 0),
        )

        val sonuc = kotaSonucu(sosyal, sonuclar)

        assertEquals("Sosyal", sonuc.ders)
        assertEquals(12, sonuc.dogru)
        assertEquals(3, sonuc.yanlis)
    }

    @Test
    fun yaprakKaydindaEksikAltTestSifirlaDoldurulur() {
        // Kullanıcı Fen'de yalnızca Fizik'i girmiş: diğer alt testler
        // "soru çözülmedi"dir (0/0), kayıp değildir.
        val sonuclar = listOf(DersSonucu("Fizik", dogru = 5, yanlis = 1))

        val fen = DenemeTuru.TYT.dersleri.first { it.ders == "Fen" }
        val sonuc = kotaSonucu(fen, sonuclar)

        assertEquals(5, sonuc.dogru)
        assertEquals(1, sonuc.yanlis)
    }

    // ---- Eski biçimdeki kayıt ----

    @Test
    fun eskiKayitAnaDalDuzeyindenOkunur() {
        // Eski kayıtta Sosyal'ın kendisi yazılıdır; alt satırlar yoktur.
        val sonuclar = listOf(
            DersSonucu("Türkçe", dogru = 35, yanlis = 4),
            DersSonucu("Sosyal", dogru = 12, yanlis = 3),
        )

        val sonuc = kotaSonucu(sosyal, sonuclar)

        assertEquals(12, sonuc.dogru)
        assertEquals(3, sonuc.yanlis)
    }

    @Test
    fun eskiKayittaAltSatirlarCizilmez() {
        // Eski kaydın alt testleri sıfırla görünürse kaydın eski
        // bütünlüğü ezerdi — bu yüzden satır hiç çizilmez.
        val sonuclar = listOf(DersSonucu("Sosyal", dogru = 12, yanlis = 3))

        assertNull(kotaAltSonuclari(sosyal, sonuclar))
    }

    @Test
    fun yaprakKaydindaAltSatirlarCizilir() {
        val sonuclar = listOf(
            DersSonucu("Tarih", dogru = 3, yanlis = 1),
            DersSonucu("Coğrafya", dogru = 4, yanlis = 1),
        )

        val altlar = checkNotNull(kotaAltSonuclari(sosyal, sonuclar))

        // Sıra kota sırasıdır; kaydı olmayan alt test 0/0 taşır.
        assertEquals(
            listOf("Tarih", "Coğrafya", "Felsefe", "Din Kültürü"),
            altlar.map { it.ders },
        )
        assertEquals(3, altlar[0].dogru)
        assertEquals(4, altlar[1].dogru)
        assertEquals(0, altlar[2].dogru)
        assertEquals(0, altlar[3].yanlis)
    }

    // ---- Bölünmemiş ders ----

    @Test
    fun bolunmemisDesteTekBasinaOkunur() {
        val sonuclar = listOf(DersSonucu("Türkçe", dogru = 35, yanlis = 4))

        val sonuc = kotaSonucu(turkce, sonuclar)

        assertEquals(35, sonuc.dogru)
        assertEquals(4, sonuc.yanlis)
        // Bölünmeyen dersin alt satırı yoktur.
        assertNull(kotaAltSonuclari(turkce, sonuclar))
    }

    @Test
    fun kayitYoksaSifirDoner() {
        // "Bu derste soru çözülmemiş" 0/0'dır; null değil.
        val sonuc = kotaSonucu(turkce, emptyList())

        assertEquals(0, sonuc.dogru)
        assertEquals(0, sonuc.yanlis)
        assertNull(kotaAltSonuclari(sosyal, emptyList()))
    }
}
