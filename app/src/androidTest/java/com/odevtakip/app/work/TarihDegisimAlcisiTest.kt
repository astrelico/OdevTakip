package com.odevtakip.app.work

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [TarihDegisimAlcisi] yönlendirme testleri.
 *
 * Gerçek `DATE_CHANGED` yayını **korumalı** bir yayındır; yalnızca sistem
 * gönderebilir (emülatörde `am broadcast` bile `SecurityException` alır). Bu
 * yüzden alıcı doğrudan çağrılır — yayını taklit etmek yerine metot imzası
 * üzerinden test edilir. Manifest kaydı ayrı olarak `dumpsys package` ile
 * doğrulanır.
 *
 * WorkManager durumu baz alınır:
 *  - **Olumlu** testte: çağrı sonrası iptal edilmemiş bir iş olmalıdır.
 *  - **Olumsuz** testte: çağrı öncesi ve sonrası durum **değişmemelidir**.
 *
 * Testin hangi durumda başladığı önemsizdir; her iki senaryo da başlangıç
 * durumundan bağımsızdır.
 */
@RunWith(AndroidJUnit4::class)
class TarihDegisimAlcisiTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun kur() {
        context = ApplicationProvider.getApplicationContext()
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun onReceive_tarihDegistigindeIsKuyrugaAlinir() {
        durumlariAl() // WorkManager'ın yanıt verdiğini doğrular

        TarihDegisimAlcisi().onReceive(context, Intent(Intent.ACTION_DATE_CHANGED))

        assertTrue(
            "Tarih değişimi sonrası iptal edilmemiş bir iş beklenirdi",
            durumlariAl().any { it != WorkInfo.State.CANCELLED }
        )
    }

    @Test
    fun onReceive_saatDilimiDegistigindeIsKuyrugaAlinir() {
        TarihDegisimAlcisi().onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))

        assertTrue(
            "Saat dilimi değişimi sonrası iptal edilmemiş bir iş beklenirdi",
            durumlariAl().any { it != WorkInfo.State.CANCELLED }
        )
    }

    @Test
    fun onReceive_bilinmeyenAksiyondaDurumDegismez() {
        val once = durumlariAl()

        TarihDegisimAlcisi().onReceive(context, Intent("com.odevtakip.app.OLMAYAN_AKSIYON"))

        assertEquals(
            "Bilinmeyen bir aksiyon işi tetiklememeli",
            once,
            durumlariAl()
        )
    }

    @Test
    fun onReceive_ayniIsTekrarTetiklenseDeTekKayitKalir() {
        // İki tetikleme art arda gelirse (tarih + saat değişimi) WorkManager'da
        // yine tek bir iş olmalı — `enqueueUniqueWork` bunu garanti eder.
        TarihDegisimAlcisi().onReceive(context, Intent(Intent.ACTION_DATE_CHANGED))
        TarihDegisimAlcisi().onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        assertTrue(
            "Unique work nedeniyle tek kayıt beklenirdi, ${durumlariAl().size} tane var",
            durumlariAl().size <= 1
        )
    }

    /** Benzersiz işin WorkManager durumlarını okur. */
    private fun durumlariAl(): List<WorkInfo.State> =
        workManager
            .getWorkInfosForUniqueWork(DurumZamanlayici.ANLIK_IS_AD)
            .get()
            .map { it.state }
}
