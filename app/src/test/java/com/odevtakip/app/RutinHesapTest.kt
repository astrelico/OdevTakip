package com.odevtakip.app

import com.odevtakip.app.data.RutinGunOzeti
import com.odevtakip.app.data.RutinKaydi
import com.odevtakip.app.data.baloncukOrani
import com.odevtakip.app.data.donemOzetiniHesapla
import com.odevtakip.app.data.gunOzetiniHesapla
import com.odevtakip.app.data.haftaninGunleri
import com.odevtakip.app.data.haftaninIlkGunu
import com.odevtakip.app.data.haftaninSonGunu
import com.odevtakip.app.util.gunBaslangici
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [gunOzetiniHesapla], [donemOzetiniHesapla] ve baloncuk ölçeği.
 *
 * Yalnızca **saf işlevler** test edilir: girdi ham kayıt listesi, çıktı
 * özet. Ekran ve gezinme testleri yok — bkz. proje notu.
 *
 * Tarihler bilinçli olarak **cihazın saat diliminde** kurulur
 * ([gunBaslangici]): kayıtlar veritabanına o biçimde yazılır, testin
 * hangi dilimde çalıştığı sonucu değiştirmez.
 */
class RutinHesapTest {

    private fun kayit(
        gun: LocalDate,
        ders: String,
        soru: Int,
        dogru: Int = 0,
        yanlis: Int = 0,
        baslik: String = "",
    ) = RutinKaydi(
        tarih = gun.gunBaslangici(),
        ders = ders,
        soru = soru,
        dogru = dogru,
        yanlis = yanlis,
        baslik = baslik,
    )

    // ---- Gün özeti ----

    @Test
    fun kayitYoksaGunOzetiBOSTur() {
        assertEquals(RutinGunOzeti.BOS, gunOzetiniHesapla(emptyList()))
    }

    @Test
    fun ayniGununDersleriToplanir() {
        val gun = LocalDate.of(2026, 10, 13)
        val ozet = gunOzetiniHesapla(
            listOf(
                kayit(gun, "Matematik", soru = 40, dogru = 35, yanlis = 5),
                kayit(gun, "Fizik", soru = 20, dogru = 15, yanlis = 3),
            ),
        )

        assertEquals(60, ozet.soru)
        assertEquals(50, ozet.dogru)
        assertEquals(8, ozet.yanlis)
    }

    @Test
    fun gunOzetindekiDersSayisiAyriOlanDersleriSayar() {
        val gun = LocalDate.of(2026, 10, 13)
        val ozet = gunOzetiniHesapla(
            listOf(
                kayit(gun, "Matematik", soru = 20),
                kayit(gun, "Fizik", soru = 10),
            ),
        )

        assertEquals(2, ozet.dersSayisi)
    }

    /**
     * Aynı günün aynı dersten **iki oturumu** artık serbesttir (sürüm 9).
     *
     * İkisi de gün toplamına girer; `dersSayisi` ise farklı ders
     * **adlarını** saydığı için aynı dersten iki satır tek sayılır —
     * "kaç derse girdim" sorusu oturum sayısını değil, dersi sorar.
     */
    @Test
    fun ayniDersinIkiOturumuAyniGunToplanir() {
        val gun = LocalDate.of(2026, 10, 13)
        val ozet = gunOzetiniHesapla(
            listOf(
                kayit(gun, "Matematik", soru = 40, dogru = 35, yanlis = 5, baslik = "Matematik 1"),
                kayit(gun, "Matematik", soru = 30, dogru = 20, yanlis = 5, baslik = "Matematik 2"),
            ),
        )

        assertEquals(70, ozet.soru)
        assertEquals(55, ozet.dogru)
        assertEquals(10, ozet.yanlis)
        assertEquals("iki oturum tek derstir", 1, ozet.dersSayisi)
    }

    @Test
    fun bosSayisiToplamIleDogruYanlisArasindadir() {
        // 40 soru, 35 doğru, 5 yanlış → boş 0. Kalan 5 soru boş bırakılmış.
        val ozet = gunOzetiniHesapla(
            listOf(kayit(LocalDate.of(2026, 10, 13), "Matematik", soru = 40, dogru = 35, yanlis = 0)),
        )

        assertEquals(5, ozet.bos)
    }

    // ---- Dönem özeti ----

    @Test
    fun donemOzetiAraliginDisindakiGunleriAtlar() {
        val kasim1 = LocalDate.of(2026, 11, 1)
        val kasim15 = LocalDate.of(2026, 11, 15)
        val ekim31 = LocalDate.of(2026, 10, 31)

        val ozet = donemOzetiniHesapla(
            kayitlar = listOf(
                kayit(kasim1, "Matematik", soru = 40),
                kayit(kasim15, "Fizik", soru = 20),
                // Aralık dışında kalan kayıt: dönem toplamına girmemeli.
                kayit(ekim31, "Kimya", soru = 99),
            ),
            ilkGun = kasim1,
            sonGun = kasim15,
        )

        assertEquals(60, ozet.toplamSoru)
        assertEquals(2, ozet.calisilanGun)
    }

    @Test
    fun donemOzetiSonGunuDahildir() {
        val kasim1 = LocalDate.of(2026, 11, 1)
        val kasim30 = LocalDate.of(2026, 11, 30)

        val ozet = donemOzetiniHesapla(
            kayitlar = listOf(kayit(kasim30, "Biyoloji", soru = 25)),
            ilkGun = kasim1,
            sonGun = kasim30,
        )

        // Bitiş günü dâhil edilmemiş olsaydı ayın sonu kaybolurdu.
        assertEquals(25, ozet.toplamSoru)
        assertEquals(1, ozet.calisilanGun)
    }

    @Test
    fun donemOzetiGunOzetleriniGunBazindaGruplar() {
        val kasim1 = LocalDate.of(2026, 11, 1)
        val kasim2 = LocalDate.of(2026, 11, 2)

        val ozet = donemOzetiniHesapla(
            kayitlar = listOf(
                kayit(kasim1, "Matematik", soru = 40),
                kayit(kasim1, "Fizik", soru = 20),
                kayit(kasim2, "Türkçe", soru = 15),
            ),
            ilkGun = kasim1,
            sonGun = kasim2,
        )

        assertEquals(2, ozet.gunOzetleri.size)
        assertEquals(60, ozet.gunOzetleri[kasim1]?.soru)
        assertEquals(15, ozet.gunOzetleri[kasim2]?.soru)
    }

    @Test
    fun donemOzetindeToplamlarHamKayitlardanGelir() {
        val kasim1 = LocalDate.of(2026, 11, 1)

        val ozet = donemOzetiniHesapla(
            kayitlar = listOf(
                kayit(kasim1, "Matematik", soru = 40, dogru = 35, yanlis = 5),
                kayit(kasim1, "Fizik", soru = 20, dogru = 15, yanlis = 3),
            ),
            ilkGun = kasim1,
            sonGun = kasim1,
        )

        assertEquals(60, ozet.toplamSoru)
        assertEquals(50, ozet.toplamDogru)
        assertEquals(8, ozet.toplamYanlis)
    }

    @Test
    fun bombozDoneminOzetiBOSTur() {
        val ozet = donemOzetiniHesapla(
            kayitlar = emptyList(),
            ilkGun = LocalDate.of(2026, 11, 1),
            sonGun = LocalDate.of(2026, 11, 30),
        )

        assertTrue(ozet.gunOzetleri.isEmpty())
        assertEquals(0, ozet.toplamSoru)
        assertEquals(0, ozet.calisilanGun)
    }

    // ---- Haftanın günleri ----

    @Test
    fun haftaPazartesiIleBaslar() {
        // 2026-10-14 bir **Çarşamba**; haftanın ilk günü 12'si Pazartesi olmalı.
        val carshamba = LocalDate.of(2026, 10, 14)

        assertEquals(LocalDate.of(2026, 10, 12), haftaninIlkGunu(carshamba))
    }

    @Test
    fun haftaPazarIleBiter() {
        val carshamba = LocalDate.of(2026, 10, 14)

        assertEquals(LocalDate.of(2026, 10, 18), haftaninSonGunu(carshamba))
    }

    @Test
    fun haftaninGunleriYediGundurVeSiraliDir() {
        val gunler = haftaninGunleri(LocalDate.of(2026, 10, 14))

        assertEquals(7, gunler.size)
        assertEquals(LocalDate.of(2026, 10, 12), gunler.first())
        assertEquals(LocalDate.of(2026, 10, 18), gunler.last())
        assertTrue(gunler.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
    }

    @Test
    fun haftaninIlkGunuPazartesiIseDegismez() {
        val pazartesi = LocalDate.of(2026, 10, 12)

        assertEquals(pazartesi, haftaninIlkGunu(pazartesi))
    }

    // ---- Baloncuk ölçeği ----

    @Test
    fun bosSorununBaloncuguSifirdir() {
        assertEquals(0f, baloncukOrani(soru = 0, enBuyukGunSoru = 100), 0.0001f)
        assertEquals(0f, baloncukOrani(soru = -5, enBuyukGunSoru = 100), 0.0001f)
    }

    @Test
    fun bombozDonemdeBolmeYapilmaz() {
        // Bölümden önce bakılır; 0'a bölme hatası değil, **sıfır** döner.
        assertEquals(0f, baloncukOrani(soru = 40, enBuyukGunSoru = 0), 0.0001f)
    }

    @Test
    fun enBuyukGunTamOranlidir() {
        assertEquals(1f, baloncukOrani(soru = 120, enBuyukGunSoru = 120), 0.0001f)
    }

    @Test
    fun oranKarekokIleOlcunur() {
        // 25, 100'ün çeyreğidir; karekök 0,5 verir — **oranla** değil.
        // Doğrusal ölçek 0,25 olurdu ve küçük günler ekranda kayıp olurdu.
        assertEquals(0.5f, baloncukOrani(soru = 25, enBuyukGunSoru = 100), 0.0001f)
    }

    @Test
    fun oranBiriniGecmez() {
        // Savunma: veri bozulursa baloncuk kartın dışına taşmaz.
        assertEquals(1f, baloncukOrani(soru = 500, enBuyukGunSoru = 100), 0.0001f)
    }
}
