package com.odevtakip.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.bildirim.BildirimYonetici

/**
 * Son tarihi geçmiş ama tamamlanmamış ödevleri [com.odevtakip.app.data.Durum.GECEKTI]
 * olarak işaretleyen arka plan işi.
 *
 * Ne zaman çalıştığına bakmak için [DurumZamanlayici]'ye bak. Üç tetikleyici var:
 *  1. Uygulama açılışı
 *  2. Gece yarısı / saat / saat dilimi değişimi ([TarihDegisimAlcisi])
 *  3. 12 saatte bir periyodik emniyet ağı
 *
 * İş kuralı [com.odevtakip.app.data.OdevRepository.gecikmisleriIsaretle]'de tek
 * yerde tanımlıdır — Worker yalnızca tetikler. Böylece arayüz, açılış ve arka
 * plan aynı kuralı paylaşır.
 *
 * Not: Arayüz bu işi beklemez; liste `gercekDurum(simdi)` hesaplayarak zaten
 * doğruyu gösterir. Bu işin amacı **veritabanındaki `durum` alanını** arka planda
 * tutmak — bildirimler (Faz 4) ve sorgular bunun üzerine kurulacak.
 */
class DurumGuncelleWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val yeniGecikenler = repository.gecikmisleriIsaretle()
        // Her çalıştırmayı yaz — "durum neden bayat?" sorusunu log'dan
        // yanıtlamak, kod içine gereksiz teşhis eklemekten ucuzdur.
        Log.d(TAG, "Durum senkronu çalıştı (${yeniGecikenler.size} ödev güncellendi)")

        // Yalnızca bu geçişte gecikenler bildirilir. Boşsa bildirim atılmaz.
        if (yeniGecikenler.isNotEmpty()) {
            BildirimYonetici.geciktiBildir(applicationContext, yeniGecikenler)
        }

        Result.success()
    } catch (e: Exception) {
        // Genelde geçici bir sorun (disk dolu, veritabanı kilitli).
        // WorkManager üstel geri çekilme ile yeniden dener; periyodik iş
        // ayrıca bir sonraki dönemde zaten tekrar çalışır.
        Log.e(TAG, "Durum senkronu başarısız", e)
        Result.retry()
    }

    /**
     * Repository'yi uygulama nesnesinden alır.
     *
     * Enstrümantasyon testlerinde de aynı [OdevTakipApplication] çalıştığı için
     * dönüştürme güvenlidir.
     */
    private val repository
        get() = (applicationContext as OdevTakipApplication).odevRepository

    private companion object {
        const val TAG = "OdevTakip"
    }
}
