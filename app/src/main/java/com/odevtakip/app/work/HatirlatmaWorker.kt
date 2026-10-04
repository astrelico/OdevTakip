package com.odevtakip.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.bildirim.BildirimYonetici
import com.odevtakip.app.data.Durum

/**
 * Zamanı gelen tek bir ödevin teslim öncesi hatırlatmasını gönderir.
 *
 * İş, [HatirlatmaZamanlayici] tarafından gecikmeli olarak kuyruğa alınır.
 * Aradan geçen sürede ödevin elden geçmesi ihtimaline karşı Worker **kendi
 * kararını yeniden verir**: planlamanın yaptığı tek şey "şu ana kadar bekle"
 * demektir, gönderme sırasındaki doğruluk burada kalır.
 *
 * Dört durumda vazgeçilir; hepsi normaldir — hatırlatma bir ikramiyedir,
 * veri işlemi değil:
 *
 *  1. Ödev silinmişse ya da kimlik geçersizse.
 *  2. Ödev bu sırada tamamlanmışsa — kullanıcı işi bitirmiştir, "teslim et"
 *     demek rahatsız edici olurdu.
 *  3. Teslim tarihi bu sırada geçmişse — o uyarıyı [DurumGuncelleWorker]ın
 *     "gecikti" bildirimi üstlenir; iki bildirim üst üste binerdi.
 *  4. Bildirim izni kapalıysa — [BildirimYonetici.yaklasanBildir] `false`
 *     döner. İş **iptal edilmez**; izin sonradan açılırsa sıradaki
 *     eşitlemede plan yenilenir.
 *
 * Üçüncü kontrol ayrıca bir yarışın da çözümüdür: ödev tam sıradan çıktıktan
 * hemen sonra uygulama kapanırsa eşitleme atlanabilir, Worker yine de doğru
 * kararı verir.
 */
class HatirlatmaWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val id = inputData.getLong(HatirlatmaZamanlayici.VERI_ODEV_ID, -1L)
        val odev = if (id > 0) repository.odeviAl(id) else null

        val gonder = when {
            odev == null -> {
                Log.d(TAG, "Hatırlatma atlandı: ödev artık yok ($id)")
                false
            }

            odev.durum == Durum.TAMAMLANDI -> {
                Log.d(TAG, "Hatırlatma atlandı: tamamlanmış ($id)")
                false
            }

            odev.sonTarih <= System.currentTimeMillis() -> {
                Log.d(TAG, "Hatırlatma atlandı: süre geçmiş ($id)")
                false
            }

            else -> true
        }

        if (gonder && odev != null) {
            val atildi = BildirimYonetici.yaklasanBildir(applicationContext, odev)
            // Log, gerçekten olanı söylemeli: izin kapalıyken "bildirildi"
            // yazmak, kuyruktaki işi başarılı sanıp sorunu görünmez kılmak
            // demektir.
            Log.d(
                TAG,
                if (atildi) "Yaklaşan teslim bildirildi: ${odev.baslik}"
                else "Yaklaşan teslim ertelendi (bildirim izni kapalı): ${odev.baslik}",
            )
        }

        Result.success()
    } catch (e: Exception) {
        // Genelde geçici bir sorun (disk dolu, veritabanı kilitli). WorkManager
        // üstel geri çekilme ile yeniden dener.
        Log.e(TAG, "Yaklaşan teslim gönderilemedi", e)
        Result.retry()
    }

    /**
     * Repository'yi uygulama nesnesinden alır.
     *
     * Enstrümantasyon testlerinde de aynı [OdevTakipApplication] çalıştığı için
     * dönüştürme güvenlidir — [DurumGuncelleWorker] da aynı yolu kullanır.
     */
    private val repository
        get() = (applicationContext as OdevTakipApplication).odevRepository

    private companion object {
        const val TAG = "OdevTakip"
    }
}
