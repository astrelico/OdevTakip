package com.odevtakip.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Dört widget'ın ortak sağlayıcısı.
 *
 * [AppWidgetProvider] aslında bir `BroadcastReceiver`'dır; sistem ona üç
 * durumda haber verir: widget **eklendiğinde**, **boyutu değiştiğinde** ve
 * **kaldırıldığında**. Buradaki tek iş, bu haberleri ortak çizim
 * fonksiyonuna [OdevWidgetCizici.tumunuCiz] bağlamaktır.
 *
 * ### Neden dört ayrı sınıf?
 *
 * Android bir widget'ı, `meta-data` ile `android.appwidget.provider`
 * tanımını gösteren **alıcı bileşeniyle** tanımlar. Aynı sınıf iki kez
 * bildirilemez ve iki farklı tanımı aynı anda taşıyamaz; farklı boylarda
 * ve farklı amaçlar için ayrı widget çıkarmak istiyorsak ayrı alıcılar
 * şarttır. Bu sınıflar bilerek **boştur**: içerik, düzen ve tıklama
 * hepsi tek yerde ([OdevWidgetCizici]) toplanır, dört dosyada aynı kodun
 * kopyalanması engellenir.
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
 * Her alt sınıfın `exported="false"` ile ayrı bir `intent-filter` ve
 * `meta-data` kaydı vardır. `exported="false"` üçüncü bir uygulamanın bu
 * alıcıyı kendi niyetiyle çağırmasını engeller, sistem yayını ise yine de
 * ulaşır — `APPWIDGET_UPDATE` Android'in korumalı yayınlarındandır ve
 * yalnızca sistem gönderir. Kayıt kendisi zorunludur: manifest'te
 * listelenmemiş bir alıcı sistem tarafından hiç oluşturulmaz.
 *
 * `OdevWidgetCizici.tumSiniflar` dördünü de sıralar; cizici bu listeyi
 * dolaşarak **tek seferde** tüm ekli widget'ları tazeler (veritabanı bir
 * kez okunur). Liste bilerek cizicidedir: alt sınıflar henüz
 * yüklenmemişken onların `Class` nesnelerine erişmek sınıf yüklemesini
 * birbirine dolandırabilirdi.
 */
abstract class WidgetAlcisi : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // `appWidgetIds` bilerek kullanılmaz: sistem yalnızca bu dalgadaki
        // kimlikleri gönderir, oysa başka bir widget başka bir kaynaktan
        // (gün değişimi, ödev yazımı) bayat kalmış olabilir. Tüm kimlikler
        // tek yerden tazelenir ve çizim idempotenttir.
        val sonuc = goAsync()
        // `javaClass` lambda içinde yakalanamaz: `launch` bloğunun `this`'i
        // CoroutineScope olduğu için `this.javaClass` oraya bağlanır ve
        // yanlış sınıf gider. Değer bloktan önce alınır.
        val sinif = javaClass
        OdevWidgetCizici.kapsam.launch {
            try {
                OdevWidgetCizici.ciz(context, sinif)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Widget çizilemedi: ${sinif.simpleName}", e)
            } finally {
                sonuc.finish()
            }
        }
    }

    private companion object {
        const val TAG = "OdevTakip"
    }
}

/**
 * **Bugünün özeti** — 4 × 2. Üstte başlık ve tarih, altta özet satırı,
 * sonra en fazla üç ödev. Geciken satır kırmızıya döner. Genel amaçlı
 * "baksam yeter" widget'ı.
 */
class OdevWidgetAlcisi : WidgetAlcisi()

/**
 * **Sayaçlar** — 2 × 2. Hiç ödev satırı yok, yalnızca üç rakam:
 * geciken, bugün, toplam. Küçük alanda sayı okumak için tasarlandı;
 * ayrıntıya inmek isteyen zaten listeye dokunur.
 */
class OdevSayacWidgetAlcisi : WidgetAlcisi()

/**
 * **Günün listesi** — 4 × 4. En fazla altı satır ve her satırın sağında
 * kalan süre. Günün tamamını tek bakışta görmek isteyen büyük alan.
 */
class OdevListeWidgetAlcisi : WidgetAlcisi()

/**
 * **Sonraki teslim** — 2 × 1. Tek ödev: adı, dersi ve kalan gün.
 *
 * Tek tıklamalı widget'ın hepsinden farkı, **o ödevin detay** ekranını
 * açmasıdır — zaten en çok bakılan bilgi tek bir iş olduğu için, ayrıntı
 * da o işin ayrıntısıdır.
 */
class OdevSonrakiWidgetAlcisi : WidgetAlcisi()
