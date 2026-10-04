package com.odevtakip.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Durum senkronu işlerini planlar.
 *
 * İki ayrı plan var ve ikisi de aynı [DurumGuncelleWorker]'ı çalıştırır:
 *
 *  1. **Periyodik** (12 saatte bir) — emniyet ağı. Cihaz kapalı kaldıysa,
 *     tarih alıcısı hiç tetiklenmediyse ya da zaman aşımına uğradıysa yine de
 *     eşitleme yapılır. [ExistingPeriodicWorkPolicy.KEEP] kullanılır; böylece
 *     uygulama her açıldığında yeniden planlansa bile mevcut iş **sıfırlanmaz**
 *     ve 12 saatlik sayaç korunur.
 *
 *  2. **Anlık** — tarih/saat/saat dilimi değiştiğinde veya uygulama
 *     açıldığında tetiklenir. [ExistingWorkPolicy.REPLACE] kullanılır; iş
 *     idempotenttir (aynı sorguyu tekrar çalıştırmak zararsız), bu yüzden
 *     çalışan bir kopyayı iptal edip taze bir tane başlatmak güvenlidir.
 *
 * WorkManager'ın kendisi varsayılan olarak `androidx.startup` ile başlar;
 * elle `initialize` çağırmak gerekmez.
 */
object DurumZamanlayici {

    /** Periyodik işin adı. */
    private const val PERIODIK_AD = "odev-durum-periyodik"

    /**
     * Anlık işin adı.
     *
     * Dışa açıktır; testler WorkManager kuyruğunu bu adla sorgular.
     */
    const val ANLIK_IS_AD = "odev-durum-anlik"

    /** Tek seferlik işlerin tekrar arası (periyodik minimum süresi 15 dk). */
    private const val PERIYOT_SAAT = 12L

    /**
     * Periyodik senkronu planlar.
     *
     * Uygulama her açılışında çağrılır; [ExistingPeriodicWorkPolicy.KEEP]
     * sayesinde tekrar çağrılması zararsızdır.
     */
    fun periyodikSenkronuPlanla(context: Context) {
        val istek = PeriodicWorkRequestBuilder<DurumGuncelleWorker>(
            repeatInterval = PERIYOT_SAAT,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIK_AD,
            ExistingPeriodicWorkPolicy.KEEP,
            istek,
        )
    }

    /**
     * Tek seferlik senkronu hemen kuyruğa alır.
     *
     * Uygulama açılışında ve [TarihDegisimAlcisi] tarafından çağrılır.
     * İş yalnızca WorkManager'a kuyruk ekler; beklemez.
     */
    fun anlikSenkronuTetikle(context: Context) {
        val istek = OneTimeWorkRequestBuilder<DurumGuncelleWorker>().build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ANLIK_IS_AD,
            ExistingWorkPolicy.REPLACE,
            istek,
        )
    }
}
