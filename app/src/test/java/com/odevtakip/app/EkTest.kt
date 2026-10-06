package com.odevtakip.app

import com.odevtakip.app.data.EK_VARSAYILAN_ADI
import com.odevtakip.app.data.boyutMetni
import com.odevtakip.app.data.ekAdiniTemizle
import com.odevtakip.app.data.ekBenzersizAd
import com.odevtakip.app.data.ekGorselMi
import com.odevtakip.app.data.ekMimeTipi
import com.odevtakip.app.data.ekTuru
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dosya / fotoğraf ekinin **saf** kurallarının birim testleri.
 *
 * Hepsi diske dokunmaz: tür tespiti, dosya adı temizliği, çakışma çözümü ve
 * boyut biçimi tamamen Kotlin'dir. Bunlara [com.odevtakip.app.data.EkDeposu]
 * sahip olduğu için bu testler cihaz ya da emülatör gerektirmez.
 */
class EkTest {

    // ---- Tür tespiti ----

    @Test
    fun bilinenUzantiMimeVerir() {
        assertEquals("image/jpeg", ekMimeTipi("odev.jpg"))
        assertEquals("application/pdf", ekMimeTipi("rapor.pdf"))
        assertEquals("image/png", ekMimeTipi("plan.png"))
    }

    @Test
    fun uzantiBuyukHarfleGelseBulunur() {
        assertEquals("image/jpeg", ekMimeTipi("ODEV.JPG"))
        assertEquals("application/pdf", ekMimeTipi("Rapor.PDF"))
    }

    @Test
    fun bilinmeyenUzantiMimeVermez() {
        assertNull(ekMimeTipi("veri.xyz"))
        assertNull(ekMimeTipi("uzantisiz"))
        assertNull(ekMimeTipi("nokta."))
        assertNull(ekMimeTipi("."))
    }

    @Test
    fun yalnizcaGorselUzantilarGorselSayilir() {
        assertTrue(ekGorselMi("foto.jpg"))
        assertTrue(ekGorselMi("ekran.PNG"))
        assertFalse(ekGorselMi("rapor.pdf"))
        assertFalse(ekGorselMi("not.txt"))
        assertFalse(ekGorselMi("uzantisiz"))
    }

    @Test
    fun turEtiketiBuyukHarftirVeTurkceyeCevrilmez() {
        assertEquals("PDF", ekTuru("odev.pdf"))
        // Türkçe yerleşimde `uppercase` "heic"i "HEİC" yapardı.
        assertEquals("HEIC", ekTuru("IMG_0001.heic"))
        assertNull(ekTuru("uzantisiz"))
    }

    // ---- Dosya adı ----

    @Test
    fun yolAyaciSonBolumuAlir() {
        assertEquals("passwd", ekAdiniTemizle("../../etc/passwd"))
        assertEquals("IMG_0001.jpg", ekAdiniTemizle("/storage/emulated/0/IMG_0001.jpg"))
        assertEquals("cizgili", ekAdiniTemizle("C:\\Users\\cizgili"))
    }

    @Test
    fun noktalamaAltCizgiyeDoner() {
        assertEquals("a_b_c", ekAdiniTemizle("a b;c"))
        assertEquals("odev_v1", ekAdiniTemizle("odev v1!"))
    }

    @Test
    fun turkceKarakterlerKorunur() {
        // Dosya sistemi UTF-8 olduğu için "ö" sorun çıkarmaz; onu "_" yapmak
        // yalnızca okunabilirliği düşürürdü.
        assertEquals("ödev_kanıtı.pdf", ekAdiniTemizle("ödev kanıtı.pdf"))
    }

    @Test
    fun bosAdVarsayilanlaDegistirilir() {
        assertEquals(EK_VARSAYILAN_ADI, ekAdiniTemizle(""))
        assertEquals(EK_VARSAYILAN_ADI, ekAdiniTemizle("///"))
        assertEquals(EK_VARSAYILAN_ADI, ekAdiniTemizle(".."))
        assertEquals(EK_VARSAYILAN_ADI, ekAdiniTemizle("   "))
    }

    @Test
    fun uzunAdKirpilirAmaUzantiKorunur() {
        val sonuc = ekAdiniTemizle("a".repeat(500) + ".pdf")

        assertTrue("uzantı korunmalı", sonuc.endsWith(".pdf"))
        assertTrue("en fazla 120 karakter", sonuc.length <= 120)
        assertEquals("a".repeat(116) + ".pdf", sonuc)
    }

    // ---- Çakışma çözümü ----

    @Test
    fun cakismaOlmayanAdAynenKullanilir() {
        assertEquals("not.txt", ekBenzersizAd("not.txt") { false })
    }

    @Test
    fun ayniAddaIkiDosyaBirbiriniEzmez() {
        val varOlanlar = mutableSetOf("IMG_0001.jpg")

        assertEquals(
            "IMG_0001 (1).jpg",
            ekBenzersizAd("IMG_0001.jpg") { it in varOlanlar },
        )
    }

    @Test
    fun cakismaSayaciBoslukluyuDaUretir() {
        val varOlanlar = mutableSetOf("odev.pdf", "odev (1).pdf", "odev (2).pdf")

        assertEquals(
            "odev (3).pdf",
            ekBenzersizAd("odev.pdf") { it in varOlanlar },
        )
    }

    @Test
    fun cakismaSayaciUzantisizAdIcinCalisir() {
        val varOlanlar = mutableSetOf("dosya")

        assertEquals("dosya (1)", ekBenzersizAd("dosya") { it in varOlanlar })
    }

    // ---- Boyut ----

    @Test
    fun boyutOkunurBirimeIndirilir() {
        assertEquals("0 B", boyutMetni(0))
        assertEquals("512 B", boyutMetni(512))
        assertEquals("1023 B", boyutMetni(1023))
        assertEquals("1 KB", boyutMetni(1024))
        assertEquals("1,5 KB", boyutMetni(1536))
        assertEquals("1 MB", boyutMetni(1024L * 1024))
        assertEquals("1 GB", boyutMetni(1024L * 1024 * 1024))
    }

    @Test
    fun boyutTekOdaliklaSinirlanir() {
        // Kayan nokta hatası 2,39'u 2,3'e çekmemeli: yuvarlama `round`.
        assertEquals("2,4 MB", boyutMetni((2.4 * 1024 * 1024).toLong()))
        assertEquals("2,5 MB", boyutMetni((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun negatifBoyutSifiraDusurulur() {
        assertEquals("0 B", boyutMetni(-5))
        assertEquals("0 B", boyutMetni(Long.MIN_VALUE))
    }

    @Test
    fun baytAyraciNoktaDegilVirguldur() {
        // Ayracı elle yazıyoruz: sayı biçimleyici cihaz diline göre
        // nokta seçerdi ve arayüzün dili sabit Türkçe.
        assertFalse(boyutMetni(1536).contains("."))
        assertTrue(boyutMetni(1536).contains(","))
    }
}
