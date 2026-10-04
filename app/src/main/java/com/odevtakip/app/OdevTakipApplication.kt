package com.odevtakip.app

import android.app.Application
import android.util.Log
import com.odevtakip.app.bildirim.BildirimYonetici
import com.odevtakip.app.data.OdevDatabase
import com.odevtakip.app.data.OdevRepository
import com.odevtakip.app.data.Tercihler
import com.odevtakip.app.work.DurumZamanlayici
import com.odevtakip.app.work.HatirlatmaZamanlayici
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
     * Kullanıcı tercihleri (tema, liste görünürlüğü, hatırlatma).
     *
     * Tek örnek olarak tutulur; böylece ayarlar ekranı bir değeri değiştirince
     * [OdevViewModel] aynı [Tercihler] üzerindeki akışı izlediği için liste ve
     * tema anında güncellenir.
     */
    val tercihler: Tercihler by lazy { Tercihler(this) }

    /**
     * Hatırlatmaların planlandığı arka plan kapsamı.
     *
     * Uygulama açık kaldığı sürece yaşar; işlem öldürülünce zaten yok olur.
     * Bunun hatırlatmaları etkilememesinin nedeni, işin kendisinin
     * WorkManager'da kalıcı olması — kapsam yalnızca **planlamayı** yapar.
     */
    private val hatirlatmaKapsami =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Kanal, ilk bildirimden önce hazır olmalı; aksi halde bildirim
        // Android 8.0+ üzerinde sessizce kaybolur.
        BildirimYonetici.kanaliOlustur(this)
        planla()
        hatirlatlariIzle()
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

    /**
     * Ödev listesi ya da hatırlatma tercihi değiştiğinde planlamayı yeniler.
     *
     * **Neden ViewModel'de değil?** Hatırlatma, verinin kendisinin değil, onun
     * arka plan görünümünün işidir ve altı ayrı kaynağın ortak sonucudur:
     * ekleme, düzenleme, tamamlama, tamamlamayı geri alma, silme ve
     * ayarlardan aralık değişimi. Eylemlerin her birine bir "hatırlatmayı da
     * güncelle" satırı eklemek yerine tek bir izleyici kurmak, unutulacak
     * adım bırakmaz; yeni bir eylem eklenirken ayrıca hatırlatma düşünülmez.
     *
     * İlk emission aynı zamanda açılış eşitlemesidir: uygulama her
     * açıldığında plan gözden geçirilir, eksik ya da bozuk kalmış işler
     * düzeltilir. Değerler değişmedikçe ([distinctUntilChanged]) gereksiz
     * iş kurulmaz.
     *
     * Hata yutulur ama kapsam **ölümez**: akış bir kez düşerse planlama
     * süresiz dururdu.
     */
    private fun hatirlatlariIzle() {
        combine(
            odevRepository.tumOdevleri(),
            tercihler.hatirlatma,
        ) { odevler, aralik -> odevler to aralik }
            .distinctUntilChanged()
            .onEach { (odevler, aralik) ->
                try {
                    HatirlatmaZamanlayici.eslestir(
                        context = this@OdevTakipApplication,
                        odevler = odevler,
                        aralik = aralik,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Hatırlatmalar planlanamadı", e)
                }
            }
            .launchIn(hatirlatmaKapsami)
    }

    private companion object {
        const val TAG = "OdevTakip"
    }
}
