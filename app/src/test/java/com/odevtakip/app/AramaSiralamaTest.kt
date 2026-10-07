package com.odevtakip.app

import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.ui.OdevFiltresi
import com.odevtakip.app.ui.OdevSiralamasi
import com.odevtakip.app.ui.aramayaGore
import com.odevtakip.app.ui.suzulVeSirala
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Arama ve sıralama fazının saf karar noktaları.
 *
 * İkisi de yalnızca [suzulVeSirala] ve [aramayaGore] üzerinden çalışır;
 * arayüz yalnızca sonucu çizer, dolayısıyla buradaki bir sözleşme ihlali
 * doğrudan yanlış listeye dönüşür.
 *
 * Türkçe arama ayrı bir bölüm: küçük/büyük harf ve `I`/`ı`/`i` eşleşmesi
 * uygulamanın dilinden ayrı düşünülemez.
 */
class AramaSiralamaTest {

    private val gelecek = System.currentTimeMillis() + 5 * 86_400_000L
    private val gecmis = System.currentTimeMillis() - 86_400_000L

    private fun odev(
        baslik: String,
        ders: String = "Matematik",
        aciklama: String = "",
        sonTarih: Long = gelecek,
        durum: Durum = Durum.BEKLIYOR,
        olusturmaTarihi: Long = 1_000L,
    ) = Odev(
        baslik = baslik,
        aciklama = aciklama,
        ders = ders,
        sonTarih = sonTarih,
        durum = durum,
        olusturmaTarihi = olusturmaTarihi,
    )

    /** Ödev adlarını olduğu gibi döndürür — okunuruk için. */
    private fun List<Odev>.basliklar() = map { it.baslik }

    private fun List<Odev>.suz(
        arama: String = "",
        siralama: OdevSiralamasi = OdevSiralamasi.ONERILEN,
        dersFiltresi: String? = null,
        filtre: OdevFiltresi = OdevFiltresi.TUMU,
        tamamlananlariGizle: Boolean = false,
    ) = suzulVeSirala(
        filtre = filtre,
        tamamlananlariGizle = tamamlananlariGizle,
        dersFiltresi = dersFiltresi,
        arama = arama,
        siralama = siralama,
    )

    // ---- Arama ----

    @Test
    fun `bos arama her odevi gecirir`() {
        val liste = listOf(odev("A"), odev("B"))

        assertTrue(liste.suz(arama = "").basliklar().containsAll(listOf("A", "B")))
        assertTrue(liste.suz(arama = "   ").basliklar().containsAll(listOf("A", "B")))
    }

    @Test
    fun `arama baslik uzerinden eslesir`() {
        val liste = listOf(
            odev("Logaritma ödevi"),
            odev("Deneme sınavı"),
            odev("Kompozisyon"),
        )

        val sonuc = liste.suz(arama = "ödev")

        assertEquals(listOf("Logaritma ödevi"), sonuc.basliklar())
    }

    @Test
    fun `arama aciklama uzerinden eslesir`() {
        val liste = listOf(
            odev("Rapor", aciklama = "Fen deneyi föyünü doldur"),
            odev("Rapor2", aciklama = "Başka bir not"),
        )

        val sonuc = liste.suz(arama = "föy")

        assertEquals(listOf("Rapor"), sonuc.basliklar())
    }

    @Test
    fun `arama ders adi uzerinden eslesir`() {
        // Kullanıcı bir ders adını yazdığında o dersteki her ödevin
        // çıkması beklenendir; yalnızca başlık aransaydı "Matematik"
        // yazmak tek bir ödevi getirirdi.
        val liste = listOf(
            odev("Birinci", ders = "Matematik"),
            odev("İkinci", ders = "Matematik"),
            odev("Üçüncü", ders = "Türkçe"),
        )

        val sonuc = liste.suz(arama = "matematik")

        assertEquals(listOf("Birinci", "İkinci"), sonuc.basliklar())
    }

    @Test
    fun `arama buyuk kucuk harfe duyarsizdir`() {
        val liste = listOf(odev("Fizik denemesi"), odev("Kimya"))

        assertTrue(liste.suz(arama = "FIZIK").basliklar().contains("Fizik denemesi"))
        assertTrue(liste.suz(arama = "fizik").basliklar().contains("Fizik denemesi"))
        assertTrue(liste.suz(arama = "KiMyA").basliklar().contains("Kimya"))
    }

    @Test
    fun `arama turkce harflerle de eslesir`() {
        // İngilizce klavyede "ING" yazan kullanıcı İngilizce'yi bulamazdı;
        // "SINIF" yazan kullanıcı da "Sınıf"ı — ikisi de aynı anahtara
        // inmeli.
        assertTrue(liste().suz(arama = "ing").basliklar().contains("İngilizce kelime"))
        assertTrue(liste().suz(arama = "ING").basliklar().contains("İngilizce kelime"))
        assertTrue(liste().suz(arama = "SINIF").basliklar().contains("Sınıf defteri"))
        assertTrue(liste().suz(arama = "sınıf").basliklar().contains("Sınıf defteri"))
    }

    private fun liste() = listOf(
        odev("İngilizce kelime", ders = "İngilizce"),
        odev("Sınıf defteri", ders = "Türkçe"),
        odev("Fizik denemesi", ders = "Fizik"),
    )

    @Test
    fun `arama kenarlardaki bosluklari yok sayar`() {
        val liste = listOf(odev("Logaritma"))

        assertEquals(1, liste.suz(arama = "   loga   ").size)
    }

    @Test
    fun `eslesmeyen arama bos liste doner`() {
        val liste = listOf(odev("Logaritma"), odev("Deneme"))

        assertTrue(liste.suz(arama = "astronot").isEmpty())
    }

    @Test
    fun `arama diger suzgecle birlikte calisir`() {
        val liste = listOf(
            odev("Mat konu tekrarı", ders = "Matematik"),
            odev("Mat deneme", ders = "Matematik", sonTarih = gecmis),
            odev("Mat bitmis", ders = "Matematik", durum = Durum.TAMAMLANDI),
            odev("Mat konu tekrarı", ders = "Türkçe"),
        )

        // Dört süzgeç üst üste: ders + arama + gizleme + "geciken".
        val sonuc = liste.suz(
            arama = "mat konu",
            dersFiltresi = "Matematik",
            tamamlananlariGizle = true,
            filtre = OdevFiltresi.TUMU,
        )

        assertEquals(listOf("Mat konu tekrarı"), sonuc.basliklar())
    }

    @Test
    fun `arama ozel karakterlerde kucuk harfe iner`() {
        val liste = listOf(odev("Koşu ayakkabısı"))

        assertEquals(1, liste.suz(arama = "KOŞU").size)
    }

    // ---- Sıralama ----

    @Test
    fun `onerilen siralamasi bekleyenleri once tamamlananlari sonra koyar`() {
        val liste = listOf(
            odev("Bitmis", sonTarih = gecmis, durum = Durum.TAMAMLANDI),
            odev("Uzak", sonTarih = gelecek + 10_000L),
            odev("Yakin", sonTarih = gelecek),
            odev("Bitmis2", sonTarih = gelecek, durum = Durum.TAMAMLANDI),
        )

        val sonuc = liste.suz(siralama = OdevSiralamasi.ONERILEN)

        // Bekleyenler en yakın teslimden başlar; bitenler altında en son
        // teslim edilen en üstte.
        assertEquals(
            listOf("Yakin", "Uzak", "Bitmis2", "Bitmis"),
            sonuc.basliklar(),
        )
    }

    @Test
    fun `ders adina gore siralama ayni dersi toplar ve teslim sirasini korur`() {
        val liste = listOf(
            odev("Mat B", ders = "Matematik", sonTarih = gelecek + 10_000L),
            odev("Tur A", ders = "Türkçe", sonTarih = gecmis),
            odev("Mat A", ders = "Matematik", sonTarih = gelecek),
        )

        val sonuc = liste.suz(siralama = OdevSiralamasi.DERS_ADI)

        assertEquals(listOf("Mat A", "Mat B", "Tur A"), sonuc.basliklar())
    }

    @Test
    fun `basliga gore alfabetik siralar`() {
        val liste = listOf(
            odev("Zebra"),
            odev("armut"),
            odev("İngilizce"),
        )

        val sonuc = liste.suz(siralama = OdevSiralamasi.ADA_GORE)

        assertEquals(listOf("armut", "İngilizce", "Zebra"), sonuc.basliklar())
    }

    @Test
    fun `en yeni eklenen once siralar`() {
        val liste = listOf(
            odev("Eski", olusturmaTarihi = 1_000L),
            odev("Yeni", olusturmaTarihi = 3_000L),
            odev("Orta", olusturmaTarihi = 2_000L),
        )

        val sonuc = liste.suz(siralama = OdevSiralamasi.YENI_EKLENEN)

        assertEquals(listOf("Yeni", "Orta", "Eski"), sonuc.basliklar())
    }

    @Test
    fun `siralama hicbir odevi ekleyip cikarmaz`() {
        val liste = listOf(
            odev("A", ders = "Matematik"),
            odev("B", ders = "Türkçe"),
            odev("C", ders = "Fizik"),
            odev("D", ders = "", durum = Durum.TAMAMLANDI),
        )

        OdevSiralamasi.entries.forEach { secenek ->
            val sonuc = liste.suz(siralama = secenek)
            assertEquals(
                "siralama=$secenek",
                liste.size,
                sonuc.size,
            )
        }
    }

    @Test
    fun `siralama arama sonucunu degistirmez`() {
        val liste = listOf(
            odev("Mat konu", ders = "Matematik", sonTarih = gelecek),
            odev("Mat soru", ders = "Matematik", sonTarih = gecmis),
            odev("Türkçe deneme", ders = "Türkçe"),
        )

        val sonuc = liste.suz(arama = "mat", siralama = OdevSiralamasi.ADA_GORE)

        assertEquals(listOf("Mat konu", "Mat soru"), sonuc.basliklar())
    }
}
