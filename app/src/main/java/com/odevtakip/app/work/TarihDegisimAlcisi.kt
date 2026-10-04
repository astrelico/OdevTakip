package com.odevtakip.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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
 * Manifest'e kayıtlıdır. Sistem yayınları yalnızca manifest'te bildirilen
 * alıcılara gider; `android:exported="true"` bu yüzden zorunlu. Güvenlik açısından
 * sorun yoktur: üç ACTION da Android'in **korumalı yayınlarıdır** (protected
 * broadcast) ve yalnızca sistem gönderebilir — üçüncü taraf uygulamalar
 * bunları taklit edemez.
 *
 * [onReceive] ana iş parçacığında ve kısa sürede dönmelidir; yalnızca
 * WorkManager'a kuyruk ekler, işin kendisini beklemez.
 */
class TarihDegisimAlcisi : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED ->
                DurumZamanlayici.anlikSenkronuTetikle(context)

            // Alıcı manifest'te yalnızca yukarıdaki aksiyonlar için bildirildi;
            // yine de bilinmeyen bir aksiyon gelirse sessizce yoksayılır.
            else -> Unit
        }
    }
}
