package com.odevtakip.app

import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.ui.LISTE_SATIR_SAYISI
import com.odevtakip.app.ui.WIDGET_SATIR_SAYISI
import com.odevtakip.app.ui.sonrakiTeslim
import com.odevtakip.app.ui.widgetIcerigi
import com.odevtakip.app.ui.widgetKalanGun
import com.odevtakip.app.ui.widgetSayaclari
import com.odevtakip.app.ui.widgetSatirMetni
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // ---- Buyuk widget: satir siniri ----

    @Test
    fun `liste_widgeti_alti_satira_kadar_cizer`() {
        val liste = (1..9).map { odev("o$it", sonTarih = milis(gun.minusDays(it.toLong()))) }

        val icerik = widgetIcerigi(liste, simdi, satirSayisi = LISTE_SATIR_SAYISI)

        assertEquals(LISTE_SATIR_SAYISI, icerik.satirlar.size)
        assertEquals(9, icerik.bekleyenToplam)
        assertEquals(9 - LISTE_SATIR_SAYISI, icerik.bekleyenToplam - icerik.satirlar.size)
    }

    @Test
    fun `kucuk_ve_buyuk_widget_ayni_kovalari_kullanir`() {
        // İkisi aynı kaynaktan beslenir; küçük "4 geciken" derken büyük
        // widget da aynı dört işi göstermek zorundadır. Fark yalnızca
        // kaç satırın sığıdığıdır, hangi işlerin girdiği değil.
        val liste = listOf(
            odev("g1", sonTarih = milis(gun.minusDays(3))),
            odev("g2", sonTarih = milis(gun.minusDays(1))),
            odev("b1", sonTarih = milis(gun, LocalTime.of(23, 59))),
            odev("i1", sonTarih = milis(gun.plusDays(5))),
        )

        val kucuk = widgetIcerigi(liste, simdi, satirSayisi = WIDGET_SATIR_SAYISI)
        val buyuk = widgetIcerigi(liste, simdi, satirSayisi = LISTE_SATIR_SAYISI)

        assertEquals(kucuk.bekleyenToplam, buyuk.bekleyenToplam)
        assertEquals(kucuk.geciken, buyuk.geciken)
        assertEquals(kucuk.bugun, buyuk.bugun)
        assertEquals(kucuk.satirlar, buyuk.satirlar.take(kucuk.satirlar.size))
    }

    // ---- Sayac widget'i ----

    @Test
    fun `sayaclar_geciken_ve_bugunu_ayirt_eder`() {
        val liste = listOf(
            odev("eski", sonTarih = milis(gun.minusDays(2))),
            odev("sabah", sonTarih = milis(gun, LocalTime.of(9, 0))),
            odev("bugun", sonTarih = milis(gun, LocalTime.of(23, 59))),
            odev("ileri", sonTarih = milis(gun.plusDays(4))),
        )

        val s = widgetSayaclari(liste, simdi)

        // 09:00 teslimi 12:00'de gecikmiştir; geciken 2, bugün 1.
        assertEquals(2, s.geciken)
        assertEquals(1, s.bugun)
    }

    @Test
    fun `toplam_sayisi_tum_odevleri_kapsar`() {
        // "Toplam" bilerek bekleyenleri değil her şeyi sayar: küçük
        // widget'ın işi "8 ödevin 4'ü gecikmiş" cümlesini kurmaktır.
        // Tamamlananlar geciken/bugün sayaçlarına girmez, yalnızca
        // toplamı büyütür.
        val liste = listOf(
            odev("bitmis1", sonTarih = milis(gun.minusDays(5)), durum = Durum.TAMAMLANDI),
            odev("bitmis2", sonTarih = milis(gun.plusDays(1)), durum = Durum.TAMAMLANDI),
            odev("gecikmis", sonTarih = milis(gun.minusDays(1))),
            odev("kalan", sonTarih = milis(gun.plusDays(2))),
        )

        val s = widgetSayaclari(liste, simdi)

        assertEquals(4, s.toplam)
        assertEquals(1, s.geciken)
        assertEquals(0, s.bugun)
    }

    @Test
    fun `bostayken_uc_sayi_sifirdir`() {
        val s = widgetSayaclari(emptyList(), simdi)

        assertEquals(0, s.geciken)
        assertEquals(0, s.bugun)
        assertEquals(0, s.toplam)
    }

    // ---- Sonraki teslim ----

    @Test
    fun `en_eski_bekleyen_is_secilir`() {
        val liste = listOf(
            odev("yaklasan", sonTarih = milis(gun.plusDays(1))),
            odev("uzak", sonTarih = milis(gun.plusDays(9))),
            odev("dun", sonTarih = milis(gun.minusDays(1))),
        )

        assertEquals("dun", sonrakiTeslim(liste, simdi)?.baslik)
    }

    @Test
    fun `tamamlanan_is_siradaki_olmaz`() {
        // Gecikmiş ama yapılmış bir iş "sıradaki" sayılsaydı widget,
        // biten bir işi tekrar tekrar hatırlatırdı.
        val liste = listOf(
            odev("bitmis", sonTarih = milis(gun.minusDays(4)), durum = Durum.TAMAMLANDI),
            odev("kalan", sonTarih = milis(gun.plusDays(6))),
        )

        assertEquals("kalan", sonrakiTeslim(liste, simdi)?.baslik)
    }

    @Test
    fun `bekleyen_yokken_sonraki_null_doner`() {
        assertNull(sonrakiTeslim(emptyList(), simdi))
        assertNull(
            sonrakiTeslim(
                listOf(odev("bitmis", durum = Durum.TAMAMLANDI)),
                simdi,
            ),
        )
    }

    // ---- Kalan sure ----

    @Test
    fun `kalan_gun_takvim_gunu_farkidir`() {
        assertEquals(0L, widgetKalanGun(odev("bugun", sonTarih = milis(gun, LocalTime.of(23, 59))), simdi))
        assertEquals(1L, widgetKalanGun(odev("yarin", sonTarih = milis(gun.plusDays(1))), simdi))
        assertEquals(7L, widgetKalanGun(odev("hafta", sonTarih = milis(gun.plusDays(7))), simdi))
    }

    @Test
    fun `gecmis_ictin_gun_farki_negatiftir`() {
        // Saat farkı değil gün farkı verilir; "dün" -1'dir. Arayüz önce
        // gecikenliği sorar, bu negatif değeri tek başına yorumlamaz:
        // bugünün saati geçmiş bir iş için fark 0 kalır ve "Bugün"
        // yazmak onu zaman varmış gibi gösterirdi.
        assertEquals(-3L, widgetKalanGun(odev("eski", sonTarih = milis(gun.minusDays(3))), simdi))
        assertEquals(
            0L,
            widgetKalanGun(odev("sabah", sonTarih = milis(gun, LocalTime.of(9, 0))), simdi),
        )
    }
}
