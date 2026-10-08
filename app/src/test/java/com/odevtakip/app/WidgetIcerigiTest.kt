package com.odevtakip.app

import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.ui.WIDGET_SATIR_SAYISI
import com.odevtakip.app.ui.widgetIcerigi
import com.odevtakip.app.ui.widgetSatirMetni
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Ana ekran widget'ının saf kararı — [widgetIcerigi].
 *
 * Widget doğrudan veritabanından beslenmediği için "neyi gösterip neyi
 * göstermeyeceği" tek burada belirlenir: yanlış kova, kullanıcıya yanlış
 * öncelik; yanlış sıra, widget'ın boş görünen ilk satırı demektir.
 *
 * Zaman **sabit tutulur**: `simdi` 8 Ekim 2026 12:00. `System.currentTimeMillis()`
 * kullansaydı test, gece yarısına yakın çalıştığında kova sınırlarından
 * kayabilirdi.
 */
class WidgetIcerigiTest {

    private val gun: LocalDate = LocalDate.of(2026, 10, 8)

    private val simdi: Long = gun.atTime(12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    private fun milis(tarih: LocalDate, saat: LocalTime = LocalTime.NOON): Long =
        tarih.atTime(saat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun odev(
        baslik: String,
        ders: String = "Matematik",
        sonTarih: Long = milis(gun.plusDays(3)),
        durum: Durum = Durum.BEKLIYOR,
    ) = Odev(
        baslik = baslik,
        aciklama = "",
        ders = ders,
        sonTarih = sonTarih,
        durum = durum,
        olusturmaTarihi = 1_000L,
    )

    private fun List<Odev>.basliklar() = map { it.baslik }

    // ---- Bosluk ve eleme ----

    @Test
    fun `hic odev yokken icerik bos olur`() {
        val icerik = widgetIcerigi(emptyList(), simdi)

        assertTrue(icerik.bos)
        assertEquals(0, icerik.bekleyenToplam)
        assertEquals(0, icerik.geciken)
        assertEquals(0, icerik.bugun)
        assertTrue(icerik.satirlar.isEmpty())
    }

    @Test
    fun `tamamlananlar eleenir ve toplami dusurur`() {
        val liste = listOf(
            odev("bitmis", sonTarih = milis(gun.minusDays(2)), durum = Durum.TAMAMLANDI),
            odev("kalan", sonTarih = milis(gun.plusDays(1))),
        )

        val icerik = widgetIcerigi(liste, simdi)

        assertEquals(1, icerik.bekleyenToplam)
        assertEquals(0, icerik.geciken)
        assertEquals(listOf("kalan"), icerik.satirlar.basliklar())
    }

    // ---- Kova ayrimi ----

    @Test
    fun `geciken once sirada gelir`() {
        val liste = listOf(
            odev("ileri", sonTarih = milis(gun.plusDays(4))),
            odev("bugun", sonTarih = milis(gun, LocalTime.of(23, 59))),
            odev("eski", sonTarih = milis(gun.minusDays(3))),
        )

        val icerik = widgetIcerigi(liste, simdi)

        assertEquals(listOf("eski", "bugun", "ileri"), icerik.satirlar.basliklar())
    }

    @Test
    fun `bugunun sonuna kadar olan is bugun sayilir`() {
        val icerik = widgetIcerigi(
            listOf(odev("aksam", sonTarih = milis(gun, LocalTime.of(23, 59)))),
            simdi,
        )

        assertEquals(0, icerik.geciken)
        assertEquals(1, icerik.bugun)
        assertEquals(1, icerik.bekleyenToplam)
    }

    @Test
    fun `bugunun saatini gecen is geciken sayilir`() {
        // Öğlen 12:00'de 09:00 teslimli iş artık "bugün" değil "gecikti"dir;
        // iki kovada birden sayılsaydı özet şişerdi.
        val icerik = widgetIcerigi(
            listOf(odev("sabah", sonTarih = milis(gun, LocalTime.of(9, 0)))),
            simdi,
        )

        assertEquals(1, icerik.geciken)
        assertEquals(0, icerik.bugun)
        assertEquals(1, icerik.bekleyenToplam)
    }

    // ---- Sira ----

    @Test
    fun `her kova icinde eski teslim once_gelir`() {
        val liste = listOf(
            odev("g3", sonTarih = milis(gun.minusDays(1))),
            odev("g1", sonTarih = milis(gun.minusDays(7))),
            odev("g2", sonTarih = milis(gun.minusDays(4))),
        )

        val icerik = widgetIcerigi(liste, simdi)

        // En çok geciken en başta: yapılması gereken ilk iş odur.
        assertEquals(listOf("g1", "g2", "g3"), icerik.satirlar.basliklar())
    }

    @Test
    fun `ileri tarihli odevler de sirayla gelir`() {
        val liste = listOf(
            odev("u2", sonTarih = milis(gun.plusDays(9))),
            odev("u1", sonTarih = milis(gun.plusDays(2))),
            odev("u3", sonTarih = milis(gun.plusDays(30))),
        )

        val icerik = widgetIcerigi(liste, simdi)

        assertEquals(listOf("u1", "u2", "u3"), icerik.satirlar.basliklar())
    }

    // ---- Sınır ----

    @Test
    fun `satir sayisi asilmaz`() {
        val liste = (1..6).map { odev("o$it", sonTarih = milis(gun.minusDays(it.toLong()))) }

        val icerik = widgetIcerigi(liste, simdi)

        assertEquals(WIDGET_SATIR_SAYISI, icerik.satirlar.size)
        assertEquals(6, icerik.bekleyenToplam)
    }

    @Test
    fun `gosterilmeyen kalan toplamdan hesaplanir`() {
        val liste = (1..5).map { odev("o$it") }

        val icerik = widgetIcerigi(liste, simdi)

        // Widget "+N daha" der; N, toplam bekleyen ile çizilen satırın farkıdır.
        assertEquals(5, icerik.bekleyenToplam)
        assertEquals(WIDGET_SATIR_SAYISI, icerik.satirlar.size)
        assertEquals(5 - WIDGET_SATIR_SAYISI, icerik.bekleyenToplam - icerik.satirlar.size)
        assertFalse(icerik.bos)
    }

    // ---- Satir metni ----

    @Test
    fun `satir_metni_ders_ve_basliktan_olusur`() {
        assertEquals(
            "Matematik — logaritma",
            widgetSatirMetni(odev("logaritma", ders = "Matematik")),
        )
    }

    @Test
    fun `dersi olmayan odevde tire konmaz`() {
        assertEquals("isim verilmemis", widgetSatirMetni(odev("isim verilmemis", ders = " ")))
    }
}
