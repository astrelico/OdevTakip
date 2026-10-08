package com.odevtakip.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Widget'ın sistemle konuşan sağlayıcısı.
 *
 * [AppWidgetProvider] aslında bir `BroadcastReceiver`'dır; sistem ona üç
 * durumda haber verir: widget **eklendiğinde**, **boyutu değiştiğinde** ve
 * **kaldırıldığında**. Buradaki tek iş, bu haberleri ortak çizim
 * fonksiyonuna [OdevWidgetCizici.tumunuCiz] bağlamaktır.
 *
 * `appWidgetIds` parametresi bilerek kullanılmaz: sistem yalnızca bu
 * dalgadaki kimlikleri gönderir, oysa bir widget başka bir kaynaktan (gün
 * değişimi, ödev yazımı) bayat kalmış olabilir. Tüm kimliklerin tek yerden
 * tazelenmesi, tek çizim yolu bırakır ve çizim idempotent olduğu için
 * fazladan iş yapmış olmak zarar vermez.
 *
 * ### Neden `goAsync()`?
 *
 * `onReceive` ana iş parçacığında çalışır ve döndüğü anda iş bitti sayılır;
 * o sırada başlatılan arka plan işi korunmaz. `goAsync()` bunu tersine
 * çevirir — süreç iş bitene kadar yaşar, karşılığında `finish()` çağırmak
 * zorundayız. Veritabanı okuması kısa sürse de beklemeden dönülür; alıcının
 * beklemesi ANR riski taşırdı.
 *
 * ### Manifest kaydı
 *
 * `android:exported="false"` güvenliktir: `APPWIDGET_UPDATE` sistem
 * tarafından gönderildiği için alıcı çalışmaya devam eder, ancak üçüncü bir
 * uygulamanın bu alıcıyı kendi niyetiyle çağırması engellenir. Kayıt
 * kendisi zorunludur — manifest'te listelenmemiş bir alıcı sistem tarafından
 * hiç oluşturulmaz; `meta-data` ise widget tanımını taşır.
 */
class OdevWidgetAlcisi : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val sonuc = goAsync()
        OdevWidgetCizici.kapsam.launch {
            try {
                OdevWidgetCizici.tumunuCiz(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Widget çizilemedi", e)
            } finally {
                sonuc.finish()
            }
        }
    }

    private companion object {
        const val TAG = "OdevTakip"
    }
}
