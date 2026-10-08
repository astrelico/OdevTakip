package com.odevtakip.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.odevtakip.app.MainActivity
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.R
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.ui.WidgetIcerigi
import com.odevtakip.app.ui.widgetIcerigi
import com.odevtakip.app.ui.widgetSatirMetni
import com.odevtakip.app.util.formatliTarih
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/**
 * Widget'ın içeriğini okuyup [RemoteViews] ile çizen tek nokta.
 *
 * ### Neden Compose değil?
 *
 * Widget'ı ana ekranı açan (launcher) süreç şişirir; Compose ise kendi
 * uygulamasının penceresinde çalışır. Sistemin kabul ettiği tek biçim
 * **RemoteViews** — dar bir view kümesi ve düz metin. Bu yüzden düzen
 * `res/layout/odev_widget.xml` içinde, görünüm bilgisi ise burada durur.
 *
 * ### Ne zaman yenilenir?
 *
 * Üç tetikleyici, hepsi bu fonksiyonu çağırır:
 *
 *  1. **Ödev değişince** — [com.odevtakip.app.OdevTakipApplication]
 *     içindeki izleyici Room akışını dinler; ekleme, düzenleme, tamamlama
 *     ve silme tek elden yakalanır.
 *  2. **Gün değişince** — `TarihDegisimAlcisi`. "Bugün X" sayacı tarihe
 *     bağlıdır ve tarih değiştiğinde veritabanı hiç değişmeyebilir; bu
 *     yüzden akış tek başına yeterli değildir.
 *  3. **Widget eklenince** — sistemin gönderdiği `APPWIDGET_UPDATE`
 *     yayını, [OdevWidgetAlcisi] üzerinden buraya ulaşır.
 *
 * `updatePeriodMillis` bilerek sıfırdır: sistem en az 30 dakika ister ve
 * bu aralık "geciken" sayacını taze tutmaya yetmez.
 */
object OdevWidgetCizici {

    /**
     * Alıcıların `goAsync()` ile birlikte kullanacağı arka plan kapsamı.
     *
     * Sınıf düzeyinde tutulur: `onReceive` döndükten sonra tek başına bir
     * Job'ın toplanma riski doğardı. [kotlinx.coroutines.SupervisorJob]
     * sayesinde tek bir çizim düşerse sonrakiler etkilenmez.
     */
    val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ekli tüm widget'ları yeniden çizer. Widget yoksa hiçbir şey yapmaz. */
    suspend fun tumunuCiz(context: Context) {
        // Manifest'teki `android:name` bu sınıfı garanti eder; repository'ye
        // doğrudan Context üzerinden erişilemez.
        val uygulama = context.applicationContext as OdevTakipApplication

        val yonetici = uygulama.getSystemService(AppWidgetManager::class.java) ?: return
        val kimlikler = yonetici.getAppWidgetIds(
            ComponentName(uygulama, OdevWidgetAlcisi::class.java),
        )
        // Widget yokken veritabanını okumak boşuna disk erişimidir.
        if (kimlikler == null || kimlikler.isEmpty()) return

        val icerik = icerikHesapla(uygulama)
        val gorunum = uzakGorunum(uygulama, icerik)
        kimlikler.forEach { yonetici.updateAppWidget(it, gorunum) }
    }

    /**
     * Veritabanından bugünün tablosunu okur.
     *
     * `first()` Room akışının ilk (güncel) değerini alır; sorgu Room'un kendi
     * iş parçacığında koştuğu için çağıran tarafın kilitlenmesi söz konusu
     * değildir. [kapsam] zaten IO üzerindedir.
     */
    private suspend fun icerikHesapla(uygulama: OdevTakipApplication): WidgetIcerigi {
        val odevler = uygulama.odevRepository.tumOdevleri().first()
        return widgetIcerigi(odevler, System.currentTimeMillis())
    }

    /** Verilmiş içeriğe karşılık gelen widget görünümünü kurar. */
    private fun uzakGorunum(context: Context, icerik: WidgetIcerigi): RemoteViews {
        val simdi = System.currentTimeMillis()
        val gorunum = RemoteViews(context.packageName, R.layout.odev_widget)

        gorunum.setTextViewText(R.id.widget_baslik, context.getString(R.string.widget_baslik))
        gorunum.setTextViewText(R.id.widget_tarih, simdi.formatliTarih())
        gorunum.setTextViewText(R.id.widget_ozet, ozetMetni(context, icerik))

        val normalRenk = context.getColor(R.color.widget_metin)
        val hataRengi = context.getColor(R.color.widget_hata)

        satirKimlikleri.forEachIndexed { i, kimlik ->
            val odev = icerik.satirlar.getOrNull(i)
            if (odev == null) {
                gorunum.setViewVisibility(kimlik, View.GONE)
            } else {
                gorunum.setViewVisibility(kimlik, View.VISIBLE)
                gorunum.setTextViewText(kimlik, widgetSatirMetni(odev))
                // Geciken satır, uygulamadaki rozet gibi `hata` rengine döner:
                // widget bir bakışta "burada bir sorun var" demelidir.
                gorunum.setTextColor(
                    kimlik,
                    if (odev.gercekDurum(simdi) == Durum.GECEKTI) hataRengi else normalRenk,
                )
            }
        }

        val kalan = icerik.bekleyenToplam - icerik.satirlar.size
        if (kalan > 0) {
            gorunum.setViewVisibility(R.id.widget_daha, View.VISIBLE)
            gorunum.setTextViewText(R.id.widget_daha, context.getString(R.string.widget_daha, kalan))
        } else {
            gorunum.setViewVisibility(R.id.widget_daha, View.GONE)
        }

        gorunum.setOnClickPendingIntent(R.id.widget_kok, anaEkranNiyeti(context))
        return gorunum
    }

    /**
     * Özet satırı.
     *
     * Üç durum var ve sırasıyla denenir: hiçbir şey yoksa "ödev yok";
     * bugün ve geciken yok ama ileride varsa yalnızca toplam bekleyen;
     * aksi halde geciken ve bugün ayrı sayılar olarak birleştirilir.
     * İki sayaçtan yalnızca sıfır olmayanlar yazılır — "0 geciken · 2 bugün"
     * gibi gereksiz parçalar üretilmez.
     */
    private fun ozetMetni(context: Context, icerik: WidgetIcerigi): String = when {
        icerik.bos -> context.getString(R.string.widget_bos)

        icerik.geciken == 0 && icerik.bugun == 0 ->
            context.getString(R.string.widget_bekliyor, icerik.bekleyenToplam)

        else -> listOfNotNull(
            icerik.geciken.takeIf { it > 0 }?.let { context.getString(R.string.widget_geciken, it) },
            icerik.bugun.takeIf { it > 0 }?.let { context.getString(R.string.widget_bugun, it) },
        ).joinToString(" · ")
    }

    /**
     * Widget'a tıklandığında açılacak niyet.
     *
     * `FLAG_ACTIVITY_CLEAR_TOP`, uygulama zaten arka plandaysa yeni bir
     * yığın değil mevcut ödev listesini getirir — kullanıcı ana ekrana
     * döndüğünde listede kalır.
     */
    private fun anaEkranNiyeti(context: Context): PendingIntent {
        val niyet = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            TIKLAMA_KODU,
            niyet,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Düzendeki üç satır kimliği, soldan sağa. */
    private val satirKimlikleri =
        intArrayOf(R.id.widget_satir1, R.id.widget_satir2, R.id.widget_satir3)

    /**
     * Tıklama niyetinin istek kodu.
     *
     * `getActivity` ile açılan niyetler ayrı bir kimlik uzayında yaşar; yine
     * de sabit bir kod, sonradan eklenecek ikinci bir widget eyleminin bu
     * niyeti sessizce ezmesini engeller.
     */
    private const val TIKLAMA_KODU = 0
}
