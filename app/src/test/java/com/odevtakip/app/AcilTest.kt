package com.odevtakip.app

import com.odevtakip.app.data.AcilDurumu
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.acilDurumu
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.util.tarihSaatiniDonustur
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Aciliyet kuralının birim testleri (Faz 13).
 *
 * Kural saf bir fonksiyondur ve her test **sabit bir "şimdi"** ile çalışır:
 * cihaz saati gece yarısını geçse de sonuç değişmez. Gün sınırı tarihe göre
 * çizildiğinden teslim saatinin geçip geçmemesi sonucu etkilemez.
 */
class AcilTest {

    private val bugun = LocalDate.now()

    /** "Şimdi": bugün 12:00 — gündüzün ortası, gün sınırı sorunu yok. */
    private val simdi = tarihSaatiniDonustur(bugun, LocalTime.NOON)

    private fun odev(
        teslim: LocalDate,
        durum: Durum = Durum.BEKLIYOR,
        saat: LocalTime = LocalTime.of(23, 59),
    ) = Odev(
        baslik = "Test ödevi",
        sonTarih = tarihSaatiniDonustur(teslim, saat),
        durum = durum,
    )

    // ---- Kapsam: hangi günler acil? ----

    @Test
    fun `teslim gunu bugun ve tamamlanmadiysa acildir`() {
        assertEquals(AcilDurumu.BUGUN, odev(bugun).acilDurumu(simdi))
    }

    @Test
    fun `yarın teslim edilecek odev uyaridir`() {
        assertEquals(AcilDurumu.YARIN, odev(bugun.plusDays(1)).acilDurumu(simdi))
    }

    @Test
    fun `ikiden fazla gun kaldiysa acil degildir`() {
        assertEquals(AcilDurumu.YOK, odev(bugun.plusDays(2)).acilDurumu(simdi))
        assertEquals(AcilDurumu.YOK, odev(bugun.plusDays(30)).acilDurumu(simdi))
    }

    @Test
    fun `teslim tarihi gecmis odev acil sayilmaz`() {
        // Gecikenlerin ayrı "Gecikti" rozeti var; iki uyarı üst üste binmesin.
        assertEquals(AcilDurumu.YOK, odev(bugun.minusDays(1)).acilDurumu(simdi))
        assertEquals(AcilDurumu.YOK, odev(bugun.minusDays(5)).acilDurumu(simdi))
    }

    // ---- Tamamlananlar ----

    @Test
    fun `tamamlanmis odev teslim gununde bile acil degildir`() {
        assertEquals(
            AcilDurumu.YOK,
            odev(bugun, durum = Durum.TAMAMLANDI).acilDurumu(simdi),
        )
        assertEquals(
            AcilDurumu.YOK,
            odev(bugun.plusDays(1), durum = Durum.TAMAMLANDI).acilDurumu(simdi),
        )
    }

    // ---- Gün sınırı: saat mi, gün mü? ----

    @Test
    fun `teslim saati gecmis olsa bile gun hala bugunse acil kalir`() {
        // 08:00'de teslim, şimdi 12:00 → durum "gecikti" ama takvim günü hâlâ
        // bugün; teslim günü bitmeden iş bitmez.
        val gecikenAmaBugun = odev(bugun, saat = LocalTime.of(8, 0))

        assertEquals(Durum.GECEKTI, gecikenAmaBugun.gercekDurum(simdi))
        assertEquals(AcilDurumu.BUGUN, gecikenAmaBugun.acilDurumu(simdi))
    }

    @Test
    fun `teslim saati 23 59 iken gunun tamami acil sayilir`() {
        assertEquals(AcilDurumu.BUGUN, odev(bugun).acilDurumu(simdi))
        assertEquals(AcilDurumu.BUGUN, odev(bugun, saat = LocalTime.of(0, 1)).acilDurumu(simdi))
    }

    @Test
    fun `gece yarisi gectiginde aciliyet bir gun kayar`() {
        // Akşam 23:59'da yarın teslim olan ödev, gece yarısından sonra
        // "bugün teslim" olur — hesap kayıttan değil andan gelir.
        val aksam = tarihSaatiniDonustur(bugun, LocalTime.of(23, 59))
        val geceYarisi = tarihSaatiniDonustur(bugun.plusDays(1), LocalTime.MIDNIGHT)
        val yarinTeslim = odev(bugun.plusDays(1))

        assertEquals(AcilDurumu.YARIN, yarinTeslim.acilDurumu(aksam))
        assertEquals(AcilDurumu.BUGUN, yarinTeslim.acilDurumu(geceYarisi))
    }
}
