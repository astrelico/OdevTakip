package com.odevtakip.app

import com.odevtakip.app.ui.deneme.tamSayiCoz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Deneme formundaki sayı **çözümleyicisi**.
 *
 * [tamSayiCoz] doğru/yanlış alanlarını okur. Saf bir işlevdir; ekranın
 * bunu yalnızca kullanıcı girdisiyle çağırması beklenir — bu yüzden
 * testler de yalnızca girdi→çıktı çiftlerini sınar.
 */
class DenemeFormCozmeTest {

    // ---- tamSayiCoz ----

    @Test
    fun bosAlanSifirdir() {
        // Boş metin hata DEĞİLDİR: cevap yazılmadıysa o derste soru
        // yoktur. Kaydı geri çevirmek kullanıcıyı yorardı.
        assertEquals(0, tamSayiCoz(""))
        assertEquals(0, tamSayiCoz("   "))
    }

    @Test
    fun gecerliSayilarCozulur() {
        assertEquals(40, tamSayiCoz("40"))
        assertEquals(0, tamSayiCoz("0"))
        assertEquals(120, tamSayiCoz("120"))
    }

    @Test
    fun sayisalOlmayanMetinHataDoner() {
        assertNull(tamSayiCoz("abc"))
        assertNull(tamSayiCoz("4a"))
        assertNull(tamSayiCoz("3.5"))
    }

    @Test
    fun negatifSayiKabulEdilmez() {
        // Pano yapıştırmasıyla gelebilir; sayı klavyesi `-` yazmaz.
        assertNull(tamSayiCoz("-5"))
    }

    @Test
    fun bosluklarKirpilir() {
        assertEquals(40, tamSayiCoz("  40  "))
    }
}
