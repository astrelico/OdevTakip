package com.odevtakip.app

import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.ui.dersDagilimi
import com.odevtakip.app.ui.durumDilimleri
import com.odevtakip.app.ui.istatistikHesapla
import com.odevtakip.app.ui.sonGunlerinTamamlanmasi
import com.odevtakip.app.util.tarihSaatiniDonustur
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * İstatistik hesaplarının birim testleri (Faz 16).
 *
 * Her test **sabit bir "şimdi"** ile çalışır: cihaz saati gece yarısını
 * geçse de sayılar değişmez. Sayımlar yalnızca saf fonksiyonlardan geçer,
 * ekran kodu test edilmez.
 */
class IstatistikTest {

    private val bugun = LocalDate.now()

    /** "Şimdi": bugün 12:00. */
    private val simdi = tarihSaatiniDonustur(bugun, LocalTime.NOON)

    private fun odev(
        sonTarih: LocalDate = bugun.plusDays(3),
        durum: Durum = Durum.BEKLIYOR,
        tamamlanma: LocalDate? = null,
        ders: String = "Matematik",
    ) = Odev(
        baslik = "Test ödevi",
        ders = ders,
        sonTarih = tarihSaatiniDonustur(sonTarih, LocalTime.of(23, 59)),
        durum = durum,
        tamamlanmaTarihi = tamamlanma?.let { tarihSaatiniDonustur(it, LocalTime.NOON) },
    )

    // ---- Özet ----

    @Test
    fun `uc durum toplami kayit sayisina esittir`() {
        val liste = listOf(
            odev(sonTarih = bugun.plusDays(5)),                       // bekleyen
            odev(sonTarih = bugun.minusDays(5)),                      // geciken
            odev(sonTarih = bugun.minusDays(1), durum = Durum.TAMAMLANDI),
        )

        val s = liste.istatistikHesapla(simdi)

        assertEquals(3, s.toplam)
        assertEquals(s.toplam, s.tamamlanan + s.geciken + s.bekleyen)
        assertEquals(1, s.tamamlanan)
        assertEquals(1, s.geciken)
        assertEquals(1, s.bekleyen)
    }

    @Test
    fun `teslim tarihi gecmis odev tamamlandiysa geciken sayilmaz`() {
        // Kural arayüzle aynı: tamamlama her şeyi ezer.
        val s = listOf(odev(sonTarih = bugun.minusDays(9), durum = Durum.TAMAMLANDI))
            .istatistikHesapla(simdi)

        assertEquals(0, s.geciken)
        assertEquals(1, s.tamamlanan)
    }

    @Test
    fun `bos liste sifir dondurur`() {
        val s = emptyList<Odev>().istatistikHesapla(simdi)

        assertEquals(0, s.toplam)
        assertEquals(0f, s.tamamlamaOrani, 0.0001f)
    }

    @Test
    fun `tamamlama orani tamamlanan bolum toplama esittir`() {
        val liste = (1..4).mapIndexed { i, _ ->
            odev(
                sonTarih = bugun.plusDays(i.toLong()),
                durum = if (i < 2) Durum.TAMAMLANDI else Durum.BEKLIYOR,
            )
        }

        assertEquals(0.5f, liste.istatistikHesapla(simdi).tamamlamaOrani, 0.0001f)
    }

    @Test
    fun `bugun teslim edilmemis odevler sayilir`() {
        val liste = listOf(
            odev(sonTarih = bugun),                                  // bugün, bekliyor
            odev(sonTarih = bugun, durum = Durum.TAMAMLANDI),        // bugün, bitti
            odev(sonTarih = bugun.plusDays(1)),                      // yarın
            odev(sonTarih = bugun.minusDays(2)),                     // gecikmiş
        )

        // Yalnızca bugünün ve bitmemiş olan: gecikmişler ayrı sayıda.
        assertEquals(1, liste.istatistikHesapla(simdi).bugunTeslim)
    }

    @Test
    fun `bugun tamamlanan odev bugun teslimden dusmez`() {
        val liste = listOf(
            odev(sonTarih = bugun, durum = Durum.TAMAMLANDI),
            odev(sonTarih = bugun),
        )

        assertEquals(1, liste.istatistikHesapla(simdi).bugunTeslim)
    }

    // ---- Pasta dilimleri ----

    @Test
    fun `dilimler hep ayni sirada ve uc adet dondurur`() {
        val dilimler = listOf(
            odev(sonTarih = bugun.plusDays(4)),
            odev(sonTarih = bugun.minusDays(4)),
            odev(sonTarih = bugun.minusDays(4), durum = Durum.TAMAMLANDI),
        ).durumDilimleri(simdi)

        assertEquals(
            listOf(Durum.TAMAMLANDI, Durum.GECEKTI, Durum.BEKLIYOR),
            dilimler.map { it.durum },
        )
        assertEquals(3, dilimler.size)
        assertEquals(1, dilimler[0].adet)
        assertEquals(1, dilimler[1].adet)
        assertEquals(1, dilimler[2].adet)
    }

    @Test
    fun `adeti sifir olan dilim de listelenir`() {
        // Çizim sırasında elenir ama sıra ve renk sözlüğü kaybolmasın diye
        // burada da döner: "Gecikti 0" arayüzde görünür kalmalıdır.
        val dilimler = listOf(odev(sonTarih = bugun.plusDays(6))).durumDilimleri(simdi)

        assertEquals(3, dilimler.size)
        assertEquals(0, dilimler.first { it.durum == Durum.GECEKTI }.adet)
    }

    @Test
    fun `dilim adetleri toplam kayit sayisina esittir`() {
        val liste = listOf(
            odev(sonTarih = bugun.plusDays(1)),
            odev(sonTarih = bugun.plusDays(2)),
            odev(sonTarih = bugun.minusDays(3)),
            odev(sonTarih = bugun.plusDays(4), durum = Durum.TAMAMLANDI),
            odev(sonTarih = bugun.plusDays(5), durum = Durum.TAMAMLANDI),
        )

        assertEquals(liste.size, liste.durumDilimleri(simdi).sumOf { it.adet })
    }

    // ---- Derslere göre ----

    @Test
    fun `ayni ders birden fazla kez toplanir`() {
        val liste = listOf(
            odev(ders = "Matematik"),
            odev(ders = "Matematik"),
            odev(ders = "  Matematik  "),
            odev(ders = "Fen"),
        )

        val dagilim = liste.dersDagilimi(simdi)

        assertEquals(2, dagilim.size)
        assertEquals("Matematik", dagilim.first().ders)
        assertEquals(3, dagilim.first().toplam)
    }

    @Test
    fun `dersler en cok odevden en aza siralanir`() {
        val liste = listOf(
            odev(ders = "Fen"),
            odev(ders = "Matematik"),
            odev(ders = "Matematik"),
            odev(ders = "Matematik"),
            odev(ders = "Türkçe"),
        )

        assertEquals(
            listOf("Matematik", "Fen", "Türkçe"),
            liste.dersDagilimi(simdi).map { it.ders },
        )
    }

    @Test
    fun `ders dagiliminin durum toplami kayit sayisina esittir`() {
        val liste = listOf(
            odev(ders = "Matematik", durum = Durum.TAMAMLANDI),
            odev(ders = "Matematik", sonTarih = bugun.minusDays(2)),
            odev(ders = "Fen", durum = Durum.TAMAMLANDI),
        )

        val matematik = liste.dersDagilimi(simdi).first { it.ders == "Matematik" }

        assertEquals(2, matematik.toplam)
        assertEquals(matematik.toplam, matematik.tamamlanan + matematik.geciken + matematik.bekleyen)
    }

    @Test
    fun `dersi bos kayit kendi grubunda toplanir`() {
        val liste = listOf(
            odev(ders = ""),
            odev(ders = ""),
            odev(ders = "Fen"),
        )

        val bos = liste.dersDagilimi(simdi).first { it.ders.isEmpty() }

        assertEquals(2, bos.toplam)
    }

    // ---- Son 7 gün ----

    @Test
    fun `liste hep yedi gun uzunlugundadir ve bugunde biter`() {
        val sayim = emptyList<Odev>().sonGunlerinTamamlanmasi(simdi)

        assertEquals(7, sayim.size)
        assertEquals(bugun, sayim.last().gun)
        assertEquals(bugun.minusDays(6), sayim.first().gun)
        assertTrue(sayim.all { it.adet == 0 })
    }

    @Test
    fun `gunler eskiden bugune siralanir`() {
        val gunler = emptyList<Odev>().sonGunlerinTamamlanmasi(simdi).map { it.gun }

        assertEquals(gunler.sorted(), gunler)
    }

    @Test
    fun `tamamlanma tarihinin gune gore sayilir`() {
        val liste = listOf(
            odev(durum = Durum.TAMAMLANDI, tamamlanma = bugun),
            odev(durum = Durum.TAMAMLANDI, tamamlanma = bugun),
            odev(durum = Durum.TAMAMLANDI, tamamlanma = bugun.minusDays(3)),
        )

        val sayim = liste.sonGunlerinTamamlanmasi(simdi)

        assertEquals(2, sayim.last().adet)
        assertEquals(1, sayim[7 - 1 - 3].adet)
        assertEquals(0, sayim.first().adet)
    }

    @Test
    fun `yedi gunden eski tamamlanma grafige girmez`() {
        val liste = listOf(
            odev(durum = Durum.TAMAMLANDI, tamamlanma = bugun.minusDays(10)),
            odev(durum = Durum.TAMAMLANDI, tamamlanma = bugun.minusDays(6)),
        )

        val sayim = liste.sonGunlerinTamamlanmasi(simdi)

        assertEquals(1, sayim.first().adet)
        assertEquals(0, sayim.last().adet)
        assertEquals(1, sayim.sumOf { it.adet })
    }

    @Test
    fun `tamamlanmamis odev guncel sayima girmez`() {
        // Durum "tamamlandı" olsa bile tarih yoksa o gün sayılamaz.
        val liste = listOf(odev(durum = Durum.TAMAMLANDI, tamamlanma = null))

        assertEquals(0, liste.sonGunlerinTamamlanmasi(simdi).sumOf { it.adet })
    }
}
