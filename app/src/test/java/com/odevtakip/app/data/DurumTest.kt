package com.odevtakip.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Durum geçiş kurallarının birim testleri.
 *
 * Bunlar saf (pure) fonksiyonlar olduğu için cihaz gerekmez.
 * Room DAO testleri Faz 1.5'te instrumented test olarak eklenecek.
 */
class DurumTest {

    private val simdi = 1_700_000_000_000L // sabit "şimdi" değeri

    private fun odev(
        sonTarih: Long,
        durum: Durum = Durum.BEKLIYOR,
    ) = Odev(
        id = 1,
        baslik = "Matematik ödevi",
        sonTarih = sonTarih,
        durum = durum,
    )

    // ---- gercekDurum ----

    @Test
    fun `teslim tarihine zaman varsa bekleyen sayilir`() {
        assertEquals(Durum.BEKLIYOR, odev(sonTarih = simdi + 60_000).gercekDurum(simdi))
    }

    @Test
    fun `teslim tarihi gectiyse gecikti sayilir`() {
        assertEquals(Durum.GECEKTI, odev(sonTarih = simdi - 1).gercekDurum(simdi))
    }

    @Test
    fun `tamamlanmis odev tarih gecse de tamamli kalir`() {
        val odev = odev(sonTarih = simdi - 86_400_000, durum = Durum.TAMAMLANDI)
        assertEquals(Durum.TAMAMLANDI, odev.gercekDurum(simdi))
    }

    @Test
    fun `gecmis odev tamamlanirsa tamamli olur`() {
        // Kullanıcı süresi geçmiş bir ödevi tamamlayabilmeli.
        val odev = odev(sonTarih = simdi - 86_400_000, durum = Durum.GECEKTI)
        val tamamlandi = odev.copy(durum = Durum.TAMAMLANDI)
        assertEquals(Durum.TAMAMLANDI, tamamlandi.gercekDurum(simdi))
    }

    @Test
    fun `teslim tarihi tam simdi ise gecikti sayilir`() {
        // Sınır: sonTarih <= simdi → gecikti. (Aynı saniyede teslim kabul edilmez.)
        assertEquals(Durum.GECEKTI, odev(sonTarih = simdi).gercekDurum(simdi))
    }

    @Test
    fun `gecmiste kalmis bekleyen odev listede gecikti gorunur`() {
        // DB'de hâlâ BEKLIYOR yazsa bile (ör. cihaz kapalıyken tarih geçti)
        // arayüz hesaplamayla doğru durumu göstermeli.
        val odev = odev(sonTarih = simdi - 1, durum = Durum.BEKLIYOR)
        assertEquals(Durum.GECEKTI, odev.gercekDurum(simdi))
    }

    // ---- Durum.guvenliDeger ----

    @Test
    fun `bilinen durum metni dogru cozumlenir`() {
        assertEquals(Durum.GECEKTI, Durum.guvenliDeger("GECEKTI"))
        assertEquals(Durum.TAMAMLANDI, Durum.guvenliDeger("TAMAMLANDI"))
        assertEquals(Durum.BEKLIYOR, Durum.guvenliDeger("BEKLIYOR"))
    }

    @Test
    fun `bilinmeyen veya bos durum metni varsayilana doner`() {
        assertEquals(Durum.BEKLIYOR, Durum.guvenliDeger("OLMAYAN_DURUM"))
        assertEquals(Durum.BEKLIYOR, Durum.guvenliDeger(null))
        assertEquals(Durum.BEKLIYOR, Durum.guvenliDeger(""))
    }

    @Test
    fun `durum metne cevrilince geri ayni doner`() {
        // Converters round-trip: DB'ye yazıp okumak kaybı olmamalı.
        Durum.entries.forEach { durum ->
            assertEquals(durum, Durum.guvenliDeger(durum.name))
        }
    }
}
