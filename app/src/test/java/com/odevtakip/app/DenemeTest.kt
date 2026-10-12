package com.odevtakip.app

import com.odevtakip.app.data.Deneme
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [Deneme.net] hesabı ve deneme kaydının alan davranışı.
 *
 * Yalnızca **saf işlevler** test edilir: `net` bir formül, kalan alanlar da
 * veri tutma davranışıdır. Ekran ve gezinme testleri yok — bkz. proje notu.
 *
 * Dört seçenekli varsayım: `net = doğru − (yanlış ÷ 4)`.
 */
class DenemeTest {

    private fun deneme(
        dogru: Int = 0,
        yanlis: Int = 0,
        ad: String = "3. Deneme",
        tarih: Long = 0L,
    ) = Deneme(ad = ad, tarih = tarih, dogru = dogru, yanlis = yanlis)

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

    @Test
    fun netCiktiliPuanAlanindanBagimsizdir() {
        // Puan kullanıcının girdiği değer; net ondan türetilmez.
        val d = deneme(dogru = 30, yanlis = 4).copy(puan = 85.5)
        assertEquals(29.0, d.net, 0.0)
        assertEquals(85.5, d.puan!!, 0.0)
    }

    // ---- Varsayılan alanlar ----

    @Test
    fun puanGirilmezseNullKalir() {
        assertEquals(null, deneme().puan)
    }

    @Test
    fun dersBosOlabilir() {
        // Genel denemeler tüm dersleri kapsar; boş string "seçilmedi"dir.
        assertEquals("", deneme().ders)
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
