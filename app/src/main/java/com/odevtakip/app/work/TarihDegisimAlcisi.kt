package com.odevtakip.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.odevtakip.app.widget.OdevWidgetCizici
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Cihazın tarihi, saati veya saat dilimi değiştiğinde senkronu tetikler.
 *
 * Neden gerekiyor:
 *  - **Gece yarısında** `sonTarih` sınırı geçer. Uygulama kapalıyken bunu yalnızca
 *    bu alıcı ya da periyodik iş fark edebilir.
 *  - **Saat dilimi değişimi** "yerel gece yarısı"nı başka bir ana kaydırır;
 *    aynı ödev bir cihazda gecikmişken diğerinde bekliyor olabilir.
 *  - **Saat elle değiştirilmesi** — kullanıcının cihaz saatini geri alıp
 *    ileri alması durumunda durumlar doğru kalır.
 *
 * Aynı üç durum **widget'ı** da ilgilendirir: widget'ın "Bugün N" ve
 * "N geciken" sayaçları tarihe bağlıdır ve tarih değiştiğinde veritabanı
 * hiç değişmeyebilir — Room akışı hiç tetiklenmez, bu yüzden ayrıca
 * [OdevWidgetCizici] çağrılır.
 *
 * Manifest'e kayıtlıdır. Sistem yayınları yalnızca manifest'te bildirilen
 * alıcılara gider; `android:exported="true"` bu yüzden zorunlu. Güvenlik açısından
 * sorun yoktur: üç ACTION da Android'in **korumalı yayınlarıdır** (protected
 * broadcast) ve yalnızca sistem gönderebilir — üçüncü taraf uygulamalar
 * bunları taklit edemez.
 *
 * [onReceive] ana iş parçacığında ve kısa sürede dönmelidir: WorkManager'a
 * iş kuyruklanır, widget ise `goAsync()` ile arka planda yenilenir ve
 * beklemeden dönülür.
 */
class TarihDegisimAlcisi : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                DurumZamanlayici.anlikSenkronuTetikle(context)
                widgetiYenile(context)
            }

            // Alıcı manifest'te yalnızca yukarıdaki aksiyonlar için bildirildi;
            // yine de bilinmeyen bir aksiyon gelirse sessizce yoksayılır.
            else -> Unit
        }
    }

    /**
     * Widget'ı yeniden çizer.
     *
     * [OdevWidgetCizici.tumunuCiz] `suspend` olduğu için bir kapsamda
     * çalıştırılır. [goAsync], `onReceive` döndükten sonra sürecin hâlâ
     * yaşamasını sağlar — çağırmadan yapmak, gece yarısı gelen yayının
     * hemen ardından sürecin dondurulmasıyla çizimin yarım kalmasına yol
     * açabilirdi.
     */
    private fun widgetiYenile(context: Context) {
        val sonuc = goAsync()
        OdevWidgetCizici.kapsam.launch {
            try {
                OdevWidgetCizici.tumunuCiz(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Widget yenilenemedi", e)
            } finally {
                sonuc.finish()
            }
        }
    }

    private companion object {
        const val TAG = "OdevTakip"
    }
}
