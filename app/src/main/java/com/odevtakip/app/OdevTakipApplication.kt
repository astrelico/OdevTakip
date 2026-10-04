package com.odevtakip.app

import android.app.Application
import com.odevtakip.app.bildirim.BildirimYonetici
import com.odevtakip.app.data.OdevDatabase
import com.odevtakip.app.data.OdevRepository
import com.odevtakip.app.data.Tercihler
import com.odevtakip.app.work.DurumZamanlayici

/**
 * Uygulamanın yaşam döngüsü kökü.
 *
 * Repository'yi burada tek seferlik oluştururuz; ViewModel'ler ve
 * [com.odevtakip.app.work.DurumGuncelleWorker] bu örneği alır.
 * (Ayrı bir DI kütüphanesi gerekmiyor — proje küçük ölçekli.)
 */
class OdevTakipApplication : Application() {

    val odevRepository: OdevRepository by lazy {
        OdevRepository(OdevDatabase.getInstance(this).odevDao())
    }

    /**
     * Kullanıcı tercihleri (tema, liste görünürlüğü).
     *
     * Tek örnek olarak tutulur; böylece ayarlar ekranı bir değeri değiştirince
     * [OdevViewModel] aynı [Tercihler] üzerindeki akışı izlediği için liste ve
     * tema anında güncellenir.
     */
    val tercihler: Tercihler by lazy { Tercihler(this) }

    override fun onCreate() {
        super.onCreate()
        // Kanal, ilk bildirimden önce hazır olmalı; aksi halde bildirim
        // Android 8.0+ üzerinde sessizce kaybolur.
        BildirimYonetici.kanaliOlustur(this)
        planla()
    }

    /**
     * Arka plan eşitlemesini kurar.
     *
     * İki iş de aynı [com.odevtakip.app.work.DurumGuncelleWorker]'ı çalıştırır:
     *
     *  - **Anlık**: uygulama en son ne zaman açıldıysa aradaki farkı kapatır.
     *    "Cihaz üç gün kapalıydı" senaryosunda kayıt bu sayede güncellenir.
     *  - **Periyodik**: 12 saatte bir tekrarlayan emniyet ağı. [DurumZamanlayici.]
     *    içindeki `KEEP` politikası sayesinde her açılışta yeniden planlansa da
     *    sayaç sıfırlanmaz.
     *
     * Ayrı bir `gecikmisleriIsaretle()` kopyası tutmuyoruz — iş kuralı tek yerde
     * (repository) kalsın, arayüz/arka plan aynı hesabı paylaşsın.
     */
    private fun planla() {
        DurumZamanlayici.anlikSenkronuTetikle(this)
        DurumZamanlayici.periyodikSenkronuPlanla(this)
    }
}
