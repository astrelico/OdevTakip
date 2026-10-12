package com.odevtakip.app

import com.odevtakip.app.ui.deneme.ondalikCoz
import com.odevtakip.app.ui.deneme.tamSayiCoz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Deneme formundaki sayı **çözümleyicileri**.
 *
 * [tamSayiCoz] doğru/yanlış alanlarını, [ondalikCoz] puan alanını
 * okur. Her ikisi de saf işlevdir; ekranların bunları yalnızca
 * kullanıcı girdisiyle çağırması beklenir — bu yüzden testler de
 * yalnızca girdi→çıktı çiftlerini sınar.
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

    // ---- ondalikCoz ----

    @Test
    fun bosPuanGirilmemisDemektir() {
        // Puan isteğe bağlıdır; boş `null` döner ve bu bir hata değildir.
        assertNull(ondalikCoz(""))
        assertNull(ondalikCoz("   "))
    }

    @Test
    fun virgulVeNoktaIkisiDeKabulEdilir() {
        // Bazı Türkçe klavyeler ondalık ayracı olarak nokta da gösterir.
        assertEquals(92.5, ondalikCoz("92,5")!!, 0.0)
        assertEquals(92.5, ondalikCoz("92.5")!!, 0.0)
        assertEquals(84.25, ondalikCoz("84,25")!!, 0.0)
    }

    @Test
    fun tamSayiPuanCozulur() {
        assertEquals(100.0, ondalikCoz("100")!!, 0.0)
        assertEquals(0.0, ondalikCoz("0")!!, 0.0)
    }

    @Test
    fun gecersizPuanHataDoner() {
        assertNull(ondalikCoz("puan"))
        assertNull(ondalikCoz("92,5,5"))
        assertNull(ondalikCoz("-3"))
    }
}
