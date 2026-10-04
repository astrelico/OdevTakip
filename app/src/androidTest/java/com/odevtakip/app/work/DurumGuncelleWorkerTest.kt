package com.odevtakip.app.work

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.OdevDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [DurumGuncelleWorker] uçtan uca testleri.
 *
 * Worker, repository'yi [OdevTakipApplication] üzerinden alır; enstrümantasyon
 * testleri de aynı Application sınıfıyla çalıştığı için gerçek veritabanı
 * kullanılır. Testler başlangıçta ve bitişte tabloları temizler.
 *
 * Senaryo, "saat ileri alındı"yı taklit eder: son tarihi geçmiş ama hâlâ
 * `BEKLIYOR` durumunda kalan bir kayıt (gerçek senaryoda cihaz kapalıyken
 * tarih geçtiğinde de oluşur) worker çalışınca `GECEKTI` olmalıdır.
 */
@RunWith(AndroidJUnit4::class)
class DurumGuncelleWorkerTest {

    private lateinit var uygulama: OdevTakipApplication
    private lateinit var veritabani: OdevDatabase

    @Before
    fun kur() {
        uygulama = ApplicationProvider.getApplicationContext()
        veritabani = OdevDatabase.getInstance(uygulama)
        runBlocking { veritabani.clearAllTables() }
    }

    @After
    fun temizle() {
        runBlocking { veritabani.clearAllTables() }
    }

    @Test
    fun doWork_gectiBekleyenOdeviGeciktIsareti() {
        runBlocking {
            val id = ekle(
                baslik = "Geciken odev",
                sonTarih = System.currentTimeMillis() - GUN,
                durum = Durum.BEKLIYOR,
            )

            val sonuc = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), sonuc)
            assertEquals(Durum.GECEKTI, veritabani.odevDao().odeviAl(id)?.durum)
        }
    }

    @Test
    fun doWork_tamamlanmisOdeveDokunmaz() {
        runBlocking {
            val id = ekle(
                baslik = "Bitmis odev",
                sonTarih = System.currentTimeMillis() - GUN,
                durum = Durum.TAMAMLANDI,
                tamamlanmaTarihi = System.currentTimeMillis(),
            )

            val sonuc = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), sonuc)
            assertEquals(Durum.TAMAMLANDI, veritabani.odevDao().odeviAl(id)?.durum)
        }
    }

    @Test
    fun doWork_gelecektekiOdevBeklerKaliyor() {
        runBlocking {
            val id = ekle(
                baslik = "Gelecekteki odev",
                sonTarih = System.currentTimeMillis() + GUN,
                durum = Durum.BEKLIYOR,
            )

            worker().doWork()

            assertEquals(Durum.BEKLIYOR, veritabani.odevDao().odeviAl(id)?.durum)
        }
    }

    @Test
    fun doWork_bosVeritabanindaBasariDondurur() {
        runBlocking {
            val sonuc = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), sonuc)
            assertEquals(0, veritabani.odevDao().toplamSayi())
        }
    }

    // ---- Yardımcılar ----

    /**
     * Doğrudan DAO üzerinden yazar.
     *
     * [com.odevtakip.app.data.OdevRepository.kaydet] kullanılmıyor; o, geçmiş
     * tarihi `GECEKTI` yazardı ve worker'ın işini test edemezdik.
     */
    private suspend fun ekle(
        baslik: String,
        sonTarih: Long,
        durum: Durum,
        tamamlanmaTarihi: Long? = null,
    ): Long = veritabani.odevDao().kaydet(
        Odev(
            baslik = baslik,
            sonTarih = sonTarih,
            durum = durum,
            tamamlanmaTarihi = tamamlanmaTarihi,
        )
    )

    private fun worker(): DurumGuncelleWorker =
        TestListenableWorkerBuilder<DurumGuncelleWorker>(uygulama).build()

    private companion object {
        const val GUN = 24L * 60 * 60 * 1000
    }
}
