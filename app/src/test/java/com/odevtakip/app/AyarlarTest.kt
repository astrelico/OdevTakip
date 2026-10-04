package com.odevtakip.app

import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.TemaSecenegi
import com.odevtakip.app.ui.OdevFiltresi
import com.odevtakip.app.ui.suzulVeSirala
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ayarlar fazının saf karar noktaları: tema seçimi ve liste gizleme.
 *
 * `Tercihler`'in kendisi SharedPreferences üzerinden çalıştığı için cihaz
 * gerektirir; burada ise onun yorumladığı **değerler** test edilir.
 * Bunlar saf olduğundan testler JVM'de koşar.
 */
class AyarlarTest {

    // ---- TemaSecenegi.koyuTemayaDonusur ----

    @Test
    fun `sistem secenegi sistemin koyu tercihini izler`() {
        assertEquals(true, TemaSecenegi.SISTEM.koyuTemayaDonusur(sistemKoyu = true))
        assertEquals(false, TemaSecenegi.SISTEM.koyuTemayaDonusur(sistemKoyu = false))
    }

    @Test
    fun `acik secenegi sistem koyu olsa bile acik kalir`() {
        assertEquals(false, TemaSecenegi.ACIK.koyuTemayaDonusur(sistemKoyu = true))
        assertEquals(false, TemaSecenegi.ACIK.koyuTemayaDonusur(sistemKoyu = false))
    }

    @Test
    fun `koyu secenegi sistem acik olsa bile koyuya doner`() {
        assertEquals(true, TemaSecenegi.KOYU.koyuTemayaDonusur(sistemKoyu = false))
        assertEquals(true, TemaSecenegi.KOYU.koyuTemayaDonusur(sistemKoyu = true))
    }

    // ---- TemaSecenegi.guvenliDeger ----

    @Test
    fun `bilinen tema metni dogru cozumlenir`() {
        assertEquals(TemaSecenegi.ACIK, TemaSecenegi.guvenliDeger("ACIK"))
        assertEquals(TemaSecenegi.KOYU, TemaSecenegi.guvenliDeger("KOYU"))
        assertEquals(TemaSecenegi.SISTEM, TemaSecenegi.guvenliDeger("SISTEM"))
    }

    @Test
    fun `bilinmeyen veya bos tema metni varsayilana doner`() {
        // Uygulama güncellemesinde anahtar değişse bile uygulama açılamaz
        // hâle gelmemeli; tema sessizce sistem seçeneğine döner.
        assertEquals(TemaSecenegi.SISTEM, TemaSecenegi.guvenliDeger(null))
        assertEquals(TemaSecenegi.SISTEM, TemaSecenegi.guvenliDeger(""))
        assertEquals(TemaSecenegi.SISTEM, TemaSecenegi.guvenliDeger("KARANLIK"))
    }

    @Test
    fun `tema metne cevrilince geri ayni doner`() {
        // SharedPreferences metin yazıp okuduğu için round-trip kayıpsız olmalı.
        TemaSecenegi.entries.forEach { secenek ->
            assertEquals(secenek, TemaSecenegi.guvenliDeger(secenek.name))
        }
    }

    // ---- Liste gizleme ----

    private val gecmis = System.currentTimeMillis() - 86_400_000L
    private val gelecek = System.currentTimeMillis() + 5 * 86_400_000L

    private fun odev(
        baslik: String,
        sonTarih: Long,
        durum: Durum = Durum.BEKLIYOR,
    ) = Odev(baslik = baslik, sonTarih = sonTarih, durum = durum)

    @Test
    fun `gizleme acikken tamamlanan odev listeden cikar`() {
        val liste = listOf(
            odev("Yaklasan", sonTarih = gelecek),
            odev("YaklasanBitmis", sonTarih = gelecek, durum = Durum.TAMAMLANDI),
        )

        val sonuc = liste.suzulVeSirala(OdevFiltresi.YAKLASAN, tamamlananlariGizle = true)

        assertEquals(listOf("Yaklasan"), sonuc.map { it.baslik })
    }

    @Test
    fun `gizleme kapaliyken tamamlanan odev listede kalir`() {
        val liste = listOf(
            odev("Yaklasan", sonTarih = gelecek),
            odev("YaklasanBitmis", sonTarih = gelecek, durum = Durum.TAMAMLANDI),
        )

        val sonuc = liste.suzulVeSirala(OdevFiltresi.YAKLASAN, tamamlananlariGizle = false)

        assertEquals(listOf("Yaklasan", "YaklasanBitmis"), sonuc.map { it.baslik })
    }

    @Test
    fun `tamamlanan odev her kosulda en sonda gosterilir`() {
        // Sıralama gizleme açık/kapalıdan bağımsızdır: gecikmiş ama bitmiş
        // bir ödev, bekleyenlerin arasına girerse "hâlâ işim var" izlenimi verir.
        val liste = listOf(
            odev("Bitmis", sonTarih = gecmis, durum = Durum.TAMAMLANDI),
            odev("Geciken", sonTarih = gecmis),
        )

        val sonuc = liste.suzulVeSirala(OdevFiltresi.TUMU, tamamlananlariGizle = false)

        assertEquals(listOf("Geciken", "Bitmis"), sonuc.map { it.baslik })
    }

    @Test
    fun `gizleme ile birlikte secili filtre de uygulanir`() {
        val simdi = System.currentTimeMillis()
        val liste = listOf(
            odev("BugunAcik", sonTarih = simdi + 60_000L),
            odev("BugunBitmis", sonTarih = simdi, durum = Durum.TAMAMLANDI),
            odev("Gecmis", sonTarih = gecmis),
        )

        // Gizleme yalnızca "hangi kayıtlar görünür" sorusunu yanıtlar;
        // filtre aynı sonuç kümesi üzerinde çalışmaya devam eder.
        val gizli = liste.suzulVeSirala(OdevFiltresi.BUGUN, tamamlananlariGizle = true)
        assertEquals(listOf("BugunAcik"), gizli.map { it.baslik })

        val gorunur = liste.suzulVeSirala(OdevFiltresi.BUGUN, tamamlananlariGizle = false)
        assertEquals(listOf("BugunAcik", "BugunBitmis"), gorunur.map { it.baslik })
    }
}
