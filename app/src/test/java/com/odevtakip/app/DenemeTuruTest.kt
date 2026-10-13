package com.odevtakip.app

import com.odevtakip.app.data.DenemeTuru
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deneme türlerinin **soru sayıları** ve kota kuralı.
 *
 * Sayılar ÖSYM'nin resmî dağılımıdır ve uygulamanın en sabit verisidir:
 * TYT 120 (Türkçe 40, Sosyal 20, Matematik 40, Fen 20), AYT sayısal 80
 * (Matematik 40, Fizik 14, Kimya 13, Biyoloji 13). Bir sayı burada
 * değişirse form, detay ve tür paneli **hepsi** değişir; bu yüzden tek
 * test dosyası bu sözleşmeyi kilitler.
 *
 * Saf veri + saf işlev olduğu için cihaz gerekmez.
 */
class DenemeTuruTest {

    // ---- Toplamlar ----

    @Test
    fun tytToplamSoru120dir() {
        assertEquals(120, DenemeTuru.TYT.toplamSoru)
    }

    @Test
    fun aytToplamSoru80dir() {
        // AYT'nin tamamı 160 sorudur (Edebiyat-Sosyal 1 + Sosyal 2 +
        // Matematik + Fen); uygulama yalnızca sayısal adayın çözdüğü
        // 80 soruluk kısmı destekler.
        assertEquals(80, DenemeTuru.AYT.toplamSoru)
    }

    @Test
    fun toplamlarKotalarinToplamidir() {
        DenemeTuru.entries.forEach { tur ->
            assertEquals(
                "toplam, kotaların toplamıyla uyuşmalı",
                tur.dersleri.sumOf { it.soru },
                tur.toplamSoru,
            )
        }
    }

    // ---- TYT kotaları ----

    @Test
    fun tytDersleriSirasiylaDurur() {
        assertEquals(
            listOf("Türkçe", "Sosyal", "Matematik", "Fen"),
            DenemeTuru.TYT.dersleri.map { it.ders },
        )
    }

    @Test
    fun tytKotalari() {
        assertEquals(40, DenemeTuru.TYT.kotasi("Türkçe"))
        assertEquals(20, DenemeTuru.TYT.kotasi("Sosyal"))
        assertEquals(40, DenemeTuru.TYT.kotasi("Matematik"))
        assertEquals(20, DenemeTuru.TYT.kotasi("Fen"))
    }

    @Test
    fun sosyalDortTesteBolunur() {
        val alt = DenemeTuru.TYT.dersleri.first { it.ders == "Sosyal" }.altDagilim
        assertTrue("Tarih 5" in alt!!)
        assertTrue("Coğrafya 5" in alt)
        assertTrue("Felsefe 5" in alt)
        assertTrue("Din Kültürü 5" in alt)
    }

    @Test
    fun fenUcDerseBolunur() {
        // Fizik 7, Kimya 7, Biyoloji 6 — toplam 20.
        val alt = DenemeTuru.TYT.dersleri.first { it.ders == "Fen" }.altDagilim
        assertTrue("Fizik 7" in alt!!)
        assertTrue("Kimya 7" in alt)
        assertTrue("Biyoloji 6" in alt)
    }

    @Test
    fun turkcedeBolunmeYoktur() {
        assertNull(DenemeTuru.TYT.dersleri.first { it.ders == "Türkçe" }.altDagilim)
    }

    // ---- Yapraklar (girdi satırları) ----

    @Test
    fun tytYapraklariSiraIleDurur() {
        // Form ve kayıt bu sırayı izler: bölünmemiş ders tek satır,
        // bölünmüş dersler alt testleriyle.
        assertEquals(
            listOf(
                "Türkçe",
                "Tarih", "Coğrafya", "Felsefe", "Din Kültürü",
                "Matematik",
                "Fizik", "Kimya", "Biyoloji",
            ),
            DenemeTuru.TYT.yapraklar.map { it.ders },
        )
    }

    @Test
    fun aytYapraklariDersleriIleAynidir() {
        // AYT'de bölünme yok: girdi satırları ile dersler aynıdır.
        assertEquals(
            DenemeTuru.AYT.dersleri.map { it.ders },
            DenemeTuru.AYT.yapraklar.map { it.ders },
        )
    }

    @Test
    fun yaprakKotalariToplamSoruIleEsittir() {
        // Girdi satırları kilitli kotaların tek kaynağıdır: satırların
        // soru sayısı toplanıp sınav toplamına eşit olmalıdır — yoksa
        // form, "120 soru" derken satırlar başka türlü toplanırdı.
        DenemeTuru.entries.forEach { tur ->
            assertEquals(
                "${tur.name}: yaprak kotaları toplamı",
                tur.toplamSoru,
                tur.yapraklar.sumOf { it.soru },
            )
        }
    }

    @Test
    fun altKotalarAnaKotayaEsittir() {
        // Sosyal 20 = 5+5+5+5, Fen 20 = 7+7+6. Ana başlıktaki
        // göstergenin satırlarla çelişmemesi bu eşitliğe bağlı.
        val sosyal = DenemeTuru.TYT.dersleri.first { it.ders == "Sosyal" }
        val fen = DenemeTuru.TYT.dersleri.first { it.ders == "Fen" }

        assertEquals(sosyal.soru, sosyal.altlar.sumOf { it.soru })
        assertEquals(fen.soru, fen.altlar.sumOf { it.soru })
    }

    @Test
    fun kotaAraliklariYapraklarlaEslesir() {
        // Aralıklar bitişik ve sıralıdır; form "bu başlık şu satırların
        // toplamıdır" derken bu aralığı kullanır.
        assertEquals(
            listOf(0..0, 1..4, 5..5, 6..8),
            DenemeTuru.TYT.kotaAraliklari,
        )
        assertEquals(
            listOf(0..0, 1..1, 2..2, 3..3),
            DenemeTuru.AYT.kotaAraliklari,
        )

        // Her aralık, ilgili kotaların yaprak sayısına birebir uymalı.
        DenemeTuru.entries.forEach { tur ->
            tur.dersleri.forEachIndexed { i, kota ->
                val beklenen = kota.altlar.size.coerceAtLeast(1)
                assertEquals(
                    "${tur.name}/${kota.ders}: aralık genişliği",
                    beklenen,
                    tur.kotaAraliklari[i].count(),
                )
            }
        }
    }

    // ---- AYT kotaları ----

    @Test
    fun aytDersleriSirasiylaDurur() {
        assertEquals(
            listOf("Matematik", "Fizik", "Kimya", "Biyoloji"),
            DenemeTuru.AYT.dersleri.map { it.ders },
        )
    }

    @Test
    fun aytKotalari() {
        assertEquals(40, DenemeTuru.AYT.kotasi("Matematik"))
        assertEquals(14, DenemeTuru.AYT.kotasi("Fizik"))
        assertEquals(13, DenemeTuru.AYT.kotasi("Kimya"))
        assertEquals(13, DenemeTuru.AYT.kotasi("Biyoloji"))
    }

    @Test
    fun aytdeSosyalVeyaTurkceYoktur() {
        // Sayısal aday Edebiyat/Sosyal testlerini çözmez.
        assertNull(DenemeTuru.AYT.kotasi("Sosyal"))
        assertNull(DenemeTuru.AYT.kotasi("Türkçe"))
    }

    @Test
    fun turlerAyniDersiFarkliKotaylaTasir() {
        // "Matematik" iki türde de vardır; eşleşmenin **türle** yapıldığı
        // burada görünür: kaydın türü olmadan kota bilinemez.
        assertEquals(40, DenemeTuru.TYT.kotasi("Matematik"))
        assertEquals(40, DenemeTuru.AYT.kotasi("Matematik"))
    }

    // ---- Görünüm metinleri ----

    @Test
    fun dersOzetiKotalardanUretilir() {
        assertEquals(
            "Türkçe 40, Sosyal 20, Matematik 40, Fen 20",
            DenemeTuru.TYT.dersOzeti,
        )
        assertEquals(
            "Matematik 40, Fizik 14, Kimya 13, Biyoloji 13",
            DenemeTuru.AYT.dersOzeti,
        )
    }

    @Test
    fun yalnizcaBolunmusDerslerinAltDagilimiVardir() {
        assertEquals(2, DenemeTuru.TYT.altDagilimlar.size)
        assertTrue(DenemeTuru.AYT.altDagilimlar.isEmpty())
    }

    // ---- Depo çözümlemesi ----

    @Test
    fun guvenliDegerBilinenAdiTanir() {
        assertEquals(DenemeTuru.TYT, DenemeTuru.guvenliDeger("TYT"))
        assertEquals(DenemeTuru.AYT, DenemeTuru.guvenliDeger("AYT"))
    }

    @Test
    fun guvenliDegerBilinmeyeniTytYapar() {
        // Bozuk/elle yazılmış bir satır kaydı okunamaz yapmaz.
        assertEquals(DenemeTuru.TYT, DenemeTuru.guvenliDeger("BILINMEYEN"))
        assertEquals(DenemeTuru.TYT, DenemeTuru.guvenliDeger(null))
        assertEquals(DenemeTuru.TYT, DenemeTuru.guvenliDeger(""))
    }

    // ---- Kota kuralı ----

    @Test
    fun kotaSinirdaKabulEdilir() {
        val turkce = DenemeTuru.TYT.dersleri.first { it.ders == "Türkçe" }

        // 40 soruluk derste 40 cevap (40 doğru + 0 yanlış) hâlâ geçerli.
        assertFalse(turkce.asildiMi(dogru = 40, yanlis = 0))
        assertFalse(turkce.asildiMi(dogru = 35, yanlis = 5))
        assertFalse(turkce.asildiMi(dogru = 0, yanlis = 0))
    }

    @Test
    fun kotaAsilirsaHataDoner() {
        val turkce = DenemeTuru.TYT.dersleri.first { it.ders == "Türkçe" }

        assertTrue(turkce.asildiMi(dogru = 36, yanlis = 5))
        assertTrue(turkce.asildiMi(dogru = 41, yanlis = 0))
    }

    @Test
    fun kotaKuraliKucukKotalardaDaGecerlidir() {
        val biyoloji = DenemeTuru.AYT.dersleri.first { it.ders == "Biyoloji" }

        assertFalse(biyoloji.asildiMi(dogru = 12, yanlis = 1))
        assertTrue(biyoloji.asildiMi(dogru = 12, yanlis = 2))
    }
}
