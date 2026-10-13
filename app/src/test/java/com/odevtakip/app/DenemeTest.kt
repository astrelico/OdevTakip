package com.odevtakip.app

import com.odevtakip.app.data.Deneme
import com.odevtakip.app.data.DenemeTuru
import com.odevtakip.app.data.DersSonucu
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [Deneme] toplamları ve alan davranışı.
 *
 * Yalnızca **saf işlevler** test edilir: `dogru`, `yanlis` ve `net`
 * formüldür, kalan alanlar veri tutma davranışıdır. Ekran ve gezinme
 * testleri yok — bkz. proje notu.
 *
 * Dört seçenekli varsayım: `net = doğru − (yanlış ÷ 4)`.
 *
 * Kayıt artık ders bazlı sonuçlardan (`sonuclar`) kurulur; toplamlar
 * ayrıca yazılmaz. Bu yüzden testlerin kurucusu, tek bir dersin
 * sonucunu verip toplamın **ondan** türetilmesini bekler.
 */
class DenemeTest {

    private fun deneme(
        dogru: Int = 0,
        yanlis: Int = 0,
        ad: String = "3. Deneme",
        tarih: Long = 0L,
    ) = Deneme(
        ad = ad,
        tarih = tarih,
        sonuclar = listOf(DersSonucu(ders = "Türkçe", dogru = dogru, yanlis = yanlis)),
    )

    // ---- Net hesabı ----

    @Test
    fun hepsiDogruIseNetDogruSayisinaEsittir() {
        assertEquals(40.0, deneme(dogru = 40).net, 0.0)
    }

    @Test
    fun hicBiriDogruDegilseNetSifirdir() {
        assertEquals(0.0, deneme(dogru = 0, yanlis = 0).net, 0.0)
    }

    @Test
    fun yanlislarDortteBirOranindadogruyuDusurur() {
        // 20 doğru, 8 yanlış → 20 − 2 = 18
        assertEquals(18.0, deneme(dogru = 20, yanlis = 8).net, 0.0)
    }

    @Test
    fun yanlisSayisiDortunKatindaykenOndalikDogar() {
        // 10 doğru, 1 yanlış → 10 − 0.25 = 9.75
        assertEquals(9.75, deneme(dogru = 10, yanlis = 1).net, 0.0)
    }

    @Test
    fun yanlisDogrudanFazlaOluncaNetEksiyeDuser() {
        // 0 doğru, 8 yanlış → 0 − 2 = −2
        assertEquals(-2.0, deneme(dogru = 0, yanlis = 8).net, 0.0)
    }

    @Test
    fun sifirDogruSifirYanlisNetSifirdir() {
        assertEquals(0.0, deneme().net, 0.0)
    }

    @Test
    fun netAyniKayittaTutarliddir() {
        // Aynı nesne iki kez okunsa da sonuç değişmez; kolon olarak
        // saklanmadığının kanıtı.
        val d = deneme(dogru = 25, yanlis = 12)
        assertEquals(d.net, d.net, 0.0)
        assertEquals(22.0, d.net, 0.0)
    }

    // ---- Toplamlar derslerden türetilir ----

    @Test
    fun toplamlarSonuclarinToplamidir() {
        val d = Deneme(
            ad = "TYT",
            tarih = 0L,
            sonuclar = listOf(
                DersSonucu("Türkçe", dogru = 30, yanlis = 4),
                DersSonucu("Sosyal", dogru = 12, yanlis = 3),
                DersSonucu("Matematik", dogru = 28, yanlis = 6),
                DersSonucu("Fen", dogru = 10, yanlis = 5),
            ),
        )

        assertEquals(80, d.dogru)
        assertEquals(18, d.yanlis)
        assertEquals(80 - 18 / 4.0, d.net, 0.0)
    }

    @Test
    fun sonucYoksaToplamlarSifirdir() {
        // Boş kayıt (ör. elle oluşturulmuş) hata üretmez.
        val d = Deneme(ad = "boş", tarih = 0L)
        assertEquals(0, d.dogru)
        assertEquals(0, d.yanlis)
        assertEquals(0.0, d.net, 0.0)
    }

    // ---- Varsayılan alanlar ----

    @Test
    fun turVarsayilanTytDir() {
        // Formda tür hep seçilir; varsayılan yalnızca savunma amaçlıdır.
        assertEquals(DenemeTuru.TYT, deneme().tur)
    }

    @Test
    fun aciklamaBosOlabilir() {
        assertEquals("", deneme().aciklama)
    }

    @Test
    fun yeniKayittaKimlikSifirdir() {
        // Room autoGenerate kimliği yazar; oluştururken verilmemeli.
        assertEquals(0L, deneme().id)
    }

    @Test
    fun yeniKayittaOlusturmaTarihiKendiAnidir() {
        val once = System.currentTimeMillis()
        val d = Deneme(ad = "x", tarih = 0L)
        val sonra = System.currentTimeMillis()

        assert(d.olusturmaTarihi in once..sonra)
    }

    @Test
    fun kimlikDegistirinceKayitKopyalanir() {
        val d = deneme(dogru = 10, yanlis = 4)
        val kopya = d.copy(id = 7)

        assertEquals(7L, kopya.id)
        assertEquals(d.net, kopya.net, 0.0)
        assertEquals(d.ad, kopya.ad)
    }
}
