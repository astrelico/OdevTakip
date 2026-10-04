package com.odevtakip.app.bildirim

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.R
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.OdevDatabase
import com.odevtakip.app.work.DurumGuncelleWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

/**
 * Bildirim zincirinin uçtan uca testi:
 *
 *     geçmiş ama "bekliyor" kalmış ödev → worker → repository "yeni geciken"
 *     listesi → bildirim sistemine ulaşır
 *
 * Zincirin ilk halkası olan sistem yayını (`ACTION_DATE_CHANGED`) korumalı
 * olduğu için sahte gönderilemez; o halka ayrı testte doğrulanır. Burada
 * kalan halkalar gerçek olarak birlikte çalıştırılır.
 *
 * Gerçek uygulama veritabanı kullanılır; her testin başında ve sonunda tablolar
 * ile bildirimler temizlenir.
 *
 * Bildirim izni cihazda `pm grant` ile verilir — enstrümantasyon izin diyaloğu
 * gösteremez, bu yüzden `uiAutomation` üzerinden kabuk komutu gönderilir.
 */
@RunWith(AndroidJUnit4::class)
class BildirimAkisiTest {

    private lateinit var uygulama: OdevTakipApplication
    private lateinit var veritabani: OdevDatabase

    @Before
    fun kur() {
        uygulama = ApplicationProvider.getApplicationContext()
        veritabani = OdevDatabase.getInstance(uygulama)

        runBlocking { veritabani.clearAllTables() }
        bildirimYoneticisi().cancelAll()

        BildirimYonetici.kanaliOlustur(uygulama)
        izniVer()
    }

    @After
    fun temizle() {
        runBlocking { veritabani.clearAllTables() }
        bildirimYoneticisi().cancelAll()
    }

    @Test
    fun kanalOlusturulur() {
        val kanal = bildirimYoneticisi().getNotificationChannel(BildirimYonetici.KANAL_ID)

        assertNotNull("Bildirim kanalı oluşturulmuş olmalı", kanal)
    }

    @Test
    fun geciktiBildirAktifBildirimOlusturur() {
        BildirimYonetici.geciktiBildir(
            uygulama,
            listOf(Odev(baslik = "Matematik 3. ünite", sonTarih = System.currentTimeMillis())),
        )

        val bildirim = aktifBildirimBekle()

        assertNotNull("Bildirim sistemde aktif olmalı", bildirim)
        assertEquals(BildirimYonetici.BILDIRIM_ID, bildirim?.id)
    }

    /**
     * Regresyon testi: `BigTextStyle.bigText` dar görünümde `contentText`'in
     * yerini alır. Tek ödevde uzatma eklenirse "son teslim tarihi geçti"
     * uyarısı kaybolup başlık aynen tekrarlanır — bu test tam o hatayı yakalar.
     */
    @Test
    fun geciktiBildirTekOdevdeUyariMetniniGosterir() {
        BildirimYonetici.geciktiBildir(
            uygulama,
            listOf(Odev(baslik = "Matematik 3. ünite", sonTarih = System.currentTimeMillis())),
        )

        val eksikler = aktifBildirimBekle()?.notification?.extras

        assertEquals(
            uygulama.getString(R.string.bildirim_tek_metin),
            eksikler?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
        // Asıl regresyon burada: bigText dar/uzatılmış görünümde
        // contentText'in yerini alıp uyarıyı siler ve başlığı tekrarlardı.
        assertNull(
            "Tek ödevde bigText eklenmemeli",
            eksikler?.getCharSequence(Notification.EXTRA_BIG_TEXT),
        )
    }

    @Test
    fun geciktiBildirCokOdevdeHemBasligiHemListeyiYazar() {
        BildirimYonetici.geciktiBildir(
            uygulama,
            listOf(
                Odev(baslik = "Kimya denemesi", sonTarih = System.currentTimeMillis()),
                Odev(baslik = "Biyoloji sunumu", sonTarih = System.currentTimeMillis()),
            ),
        )

        val eksikler = aktifBildirimBekle()?.notification?.extras

        assertEquals(
            uygulama.getString(R.string.bildirim_cok_baslik, 2),
            eksikler?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        )
        assertEquals(
            "Kimya denemesi, Biyoloji sunumu",
            eksikler?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
        )
    }

    @Test
    fun geciktiBildirBosListeIcinBildirimAtilmaz() {
        BildirimYonetici.geciktiBildir(uygulama, emptyList())

        assertNull("Boş liste bildirim üretmemeli", aktifBildirim())
    }

    @Test
    fun workerGecikinceBildirimGonderir() {
        runBlocking {
            // "Cihaz kapalıyken tarih geçti" senaryosu: son tarih geçmiş ama
            // durum henüz güncellenmemiş. Doğrudan DAO üzerinden yazılır;
            // repository geçmişi `GECEKTI` yazardı ve worker'ın işini
            // test edemezdik.
            val id = veritabani.odevDao().kaydet(
                Odev(
                    baslik = "Tarihçe raporu",
                    sonTarih = System.currentTimeMillis() - GUN,
                    durum = Durum.BEKLIYOR,
                )
            )

            val sonuc = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), sonuc)
            assertEquals(Durum.GECEKTI, veritabani.odevDao().odeviAl(id)?.durum)
        }

        // Bildirim, iş kuralından BAĞIMSIZ doğrulanır: durum zaten
        // `GECEKTI`'ye döndü, şimdi gerçekten görünür mü?
        val bildirim = aktifBildirimBekle()

        assertNotNull("Gecikme sonrası bildirim beklenirdi", bildirim)
    }

    // ---- Yardımcılar ----

    private fun bildirimYoneticisi(): NotificationManager =
        uygulama.getSystemService(NotificationManager::class.java)

    private fun aktifBildirim(): StatusBarNotification? =
        bildirimYoneticisi().activeNotifications
            .firstOrNull { it.id == BildirimYonetici.BILDIRIM_ID }

    /**
     * Sistem bildirimi kaydederken kısa bir gecikme olabilir; bir süre
     * bekleyerek testin zamana duyarlı olmasını engelleriz.
     */
    private fun aktifBildirimBekle(azamiSureMs: Long = 2_000): StatusBarNotification? {
        val bitis = System.currentTimeMillis() + azamiSureMs
        var bulunan = aktifBildirim()
        while (bulunan == null && System.currentTimeMillis() < bitis) {
            Thread.sleep(50)
            bulunan = aktifBildirim()
        }
        return bulunan
    }

    private fun worker(): DurumGuncelleWorker =
        TestListenableWorkerBuilder<DurumGuncelleWorker>(uygulama).build()

    /** Bildirim iznini kabuk üzerinden verir ve sonucu doğrular. */
    private fun izniVer() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val komut = "pm grant ${uygulama.packageName} ${Manifest.permission.POST_NOTIFICATIONS}"
        val boru = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(komut)

        try {
            // Komutun bitmesini bekle: çıkış akışını sonuna kadar okumak yeterli.
            FileInputStream(boru.fileDescriptor).use { it.readBytes() }
        } finally {
            boru.close()
        }

        assertTrue("Bildirim izni verilemedi", BildirimYonetici.izinVerilmis(uygulama))
    }

    private companion object {
        const val GUN = 24L * 60 * 60 * 1000
    }
}
