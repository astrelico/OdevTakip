package com.odevtakip.app.util

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.odevtakip.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * [turkceyeSabitle] davranışının testi.
 *
 * Kritik nokta: test **cihazın dilinden bağımsız** çalışmalıdır. Yöntem, zaten
 * Türkçe olan bir cihazda kendiliğinden geçerdi; bu yüzden bağlamı elle
 * **İngilizce** kurup sonucun Türkçe olmasını bekliyoruz. Aksi hâlde bu test
 * Türkçe olmayan tek ortamda, yani tam da hatanın yaşadığı yerde çözülemezdi.
 */
@RunWith(AndroidJUnit4::class)
class YerellestirmeTest {

    private val kaynak: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun ingilizceBaglamTurkceyeCevrilir() {
        val sonuc = baglamiKur(Locale.UK).turkceyeSabitle()

        assertEquals(
            "İngilizce bağlam Türkçe'ye çevrilmeli",
            "tr",
            sonuc.resources.configuration.locales[0].language,
        )
    }

    @Test
    fun turkceBaglamTurkceKalir() {
        val sonuc = baglamiKur(Locale("tr", "TR")).turkceyeSabitle()

        assertEquals("tr", sonuc.resources.configuration.locales[0].language)
    }

    /**
     * Yukarıdaki testler yardımcı fonksiyonu sınar; [com.odevtakip.app.MainActivity]'
     * onu çağırmayı unutursa hepsi sessizce geçmeye devam eder. Bu test zincirin
     * son halkasıdır: ekrana gerçekten gelen bağlamın dili Türkçe olmalı.
     *
     * Not: Türkçe sistemli bir cihazda bu test düzeltmesiz de geçerdi; asıl
     * kanıt [ingilizceBaglamTurkceyeCevrilir] ile
     * [kutuphaneKaynaklariDaTurkceCozulur]'dadır. Buradaki amaç bağlantı
     * (wiring) regresyonunu yakalamak.
     */
    @Test
    fun anaEkranBaglamiTurkceyeSabitlenir() {
        ActivityScenario.launch(MainActivity::class.java).use { senaryo ->
            senaryo.onActivity { aktivite ->
                assertEquals(
                    "MainActivity'nin bağlamı Türkçe olmalı",
                    "tr",
                    aktivite.resources.configuration.locales[0].language,
                )
            }
        }
    }

    /**
     * Asıl regresyon: uygulamanın **kendi** metinleri zaten Türkçe olduğu için
     * sessizce geçebilir. Burada kontrol edilen şey kütüphane kaynağı —
     * Material3 tarih seçicisinin başlığı — çünkü uyumsuzluk tam olarak
     * oradan geliyordu.
     */
    @Test
    fun kutuphaneKaynaklariDaTurkceCozulur() {
        val kimlik = kaynak.resources.getIdentifier(
            "m3c_date_picker_title",
            "string",
            kaynak.packageName,
        )
        assertEquals(
            "Material3 tarih seçici başlığı bulunamadı; " +
                "kaynak adı değişmiş olabilir",
            true,
            kimlik != 0,
        )

        // Bilinmeyen bir İngilizce bağlam ile bilinen bir Türkçe bağlamın
        // aynı sonucu vermesi beklenir; metni sabit yazmak kütüphane
        // metnini değiştirdiğinde gereksiz kırılma yaratırdı.
        val turkceKarsilik = baglamiKur(Locale("tr")).resources.getString(kimlik)
        val turkceyeSabitlenmis = baglamiKur(Locale.UK).turkceyeSabitle()
            .resources.getString(kimlik)

        assertEquals(
            "Tarih seçici başlığı Türkçe çözülmeli",
            turkceKarsilik,
            turkceyeSabitlenmis,
        )
    }

    // ---- Yardımcılar ----

    /** Verilen dile sahip, aksi hâlde uygulamanınkinden farklı bir bağlam kurar. */
    private fun baglamiKur(dil: Locale): Context {
        val yapilandirma = Configuration(kaynak.resources.configuration).apply {
            setLocale(dil)
            setLayoutDirection(dil)
        }
        return kaynak.createConfigurationContext(yapilandirma)
    }
}
