package com.odevtakip.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.odevtakip.app.MainActivity
import com.odevtakip.app.OdevTakipApplication
import com.odevtakip.app.R
import com.odevtakip.app.bildirim.HizliTamamlaAlcisi
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.ui.LISTE_SATIR_SAYISI
import com.odevtakip.app.ui.WIDGET_SATIR_SAYISI
import com.odevtakip.app.ui.WIDGET_YUVA_LISTE
import com.odevtakip.app.ui.WIDGET_YUVA_OZET
import com.odevtakip.app.ui.WIDGET_YUVA_SONRAKI
import com.odevtakip.app.ui.WidgetIcerigi
import com.odevtakip.app.ui.widgetIcerigi
import com.odevtakip.app.ui.widgetKalanGun
import com.odevtakip.app.ui.widgetSayaclari
import com.odevtakip.app.ui.widgetSatirMetni
import com.odevtakip.app.ui.widgetTamamlamaAdresi
import com.odevtakip.app.ui.sonrakiTeslim
import com.odevtakip.app.util.formatliTarih
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/**
 * Dört widget'ın içeriğini okuyup [RemoteViews] ile çizen tek nokta.
 *
 * ### Neden Compose değil?
 *
 * Widget'ı ana ekranı açan (launcher) süreç şişirir; Compose ise kendi
 * uygulamasının penceresinde çalışır. Sistemin kabul ettiği tek biçim
 * **RemoteViews** — dar bir view kümesi ve düz metin. Bu yüzden düzenler
 * `res/layout` altında, görünüm bilgisi buradadır.
 *
 * ### Dört widget, tek tur
 *
 * [tumSiniflar] sırayla dolaşılır ve **veritabanı yalnızca bir kez**
 * okunur. Widget'lardan hiçbiri ekli değilse hiç okunmaz: boşuna disk
 * erişimi yapmak, arka planda her ödev yazımında tekrarlanırdı.
 *
 * Dört düzenin içerikleri de aynı kovalardan beslenir (bkz.
 * [com.odevtakip.app.ui.widgetIcerigi]); "4 geciken" diyen küçük widget
 * ile altı satırlık büyük widget asla birbirini yalamaz.
 *
 * ### Ne zaman yenilenir?
 *
 * Üç tetikleyici, hepsi [tumunuCiz] çağırır:
 *
 *  1. **Ödev değişince** — [OdevTakipApplication] içindeki izleyici Room
 *     akışını dinler; ekleme, düzenleme, tamamlama ve silme tek elden
 *     yakalanır.
 *  2. **Gün değişince** — `TarihDegisimAlcisi`. "Bugün X" sayacı tarihe
 *     bağlıdır ve tarih değiştiğinde veritabanı hiç değişmeyebilir; bu
 *     yüzden akış tek başına yeterli değildir.
 *  3. **Widget eklenince** — sistemin gönderdiği `APPWIDGET_UPDATE`
 *     yayını, ilgili [WidgetAlcisi] alt sınıfı üzerinden buraya ulaşır.
 *
 * `updatePeriodMillis` bilerek sıfırdır: sistem en az 30 dakika ister ve
 * bu aralık "geciken" sayacını taze tutmaya yetmez.
 *
 * ### Kutudan tamamlama
 *
 * Satır taşıyan üç widget'ta her satırın solunda bir **tamamla kutusu**
 * vardır. Kutu [HizliTamamlaAlcisi]'ni çağırır — bildirimdeki "Tamamla"
 * düğmesiyle **aynı alıcı, aynı yazım**: iki yerde iki ayrı tamamlama
 * yolu olsaydı biri diğerinin yaptığını unutabilirdi. Yazım bitince Room
 * akışı tetiklenir ve kutular kendiliğinden yeniden çizilir; kutuya
 * basmanın tek sonucu satırın kaybolmasıdır, başka bir şeye gerek yoktur.
 *
 * Sayaç widget'ında satır olmadığı için kutu da yoktur.
 *
 * ### Renkler
 *
 * Metin renkleri kodda okunur, zemin ise düzenden gelir. İkisi de
 * **sistem** koyu moduna bağlıdır: uygulama `setDefaultNightMode`
 * kullanmaz, dolayısıyla uygulama bağlamı ile launcher şişirmesi her
 * zaman aynı `uiMode`'dan çözer. Renklerin ayrılması imkânsızdır.
 */
object OdevWidgetCizici {

    /**
     * Alıcıların `goAsync()` ile birlikte kullanacağı arka plan kapsamı.
     *
     * Sınıf düzeyinde tutulur: `onReceive` döndükten sonra tek başına bir
     * Job'ın toplanma riski doğardı. [SupervisorJob] sayesinde tek bir
     * çizim düşerse sonrakiler etkilenmez.
     */
    val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Sağlayıcı sınıfları, tek çizim turunda dolaşılacak sıra.
     *
     * Sıra bilinçlidir: en çok kullanılan (özet) başta gelir ki tek bir
     * tur yarım kalırsa en azından o tazelenmiş olsun.
     */
    private val tumSiniflar: List<Class<out WidgetAlcisi>> = listOf(
        OdevWidgetAlcisi::class.java,
        OdevSayacWidgetAlcisi::class.java,
        OdevListeWidgetAlcisi::class.java,
        OdevSonrakiWidgetAlcisi::class.java,
    )

    /** Ekli tüm widget'ları yeniden çizer. Widget yoksa hiçbir şey yapmaz. */
    suspend fun tumunuCiz(context: Context) = ciz(context, tumSiniflar)

    /** Tek bir widget'ı çizer — ilgili alıcının `onUpdate` çağrısıdır. */
    suspend fun ciz(context: Context, sinif: Class<out WidgetAlcisi>) = ciz(context, listOf(sinif))

    private suspend fun ciz(context: Context, siniflar: List<Class<out WidgetAlcisi>>) {
        // Manifest'teki `android:name` bu sınıfı garanti eder; repository'ye
        // doğrudan Context üzerinden erişilemez.
        val uygulama = context.applicationContext as OdevTakipApplication
        val yonetici = uygulama.getSystemService(AppWidgetManager::class.java) ?: return

        // Önce hangilerinin ekli olduğu: widget hiç yokken disk okuması
        // yapmak boşuna, üstelik her ödev yazımında tekrarlanırdı.
        val kimlikler = LinkedHashMap<Class<out WidgetAlcisi>, IntArray>()
        for (sinif in siniflar) {
            val ids = yonetici.getAppWidgetIds(ComponentName(uygulama, sinif)) ?: IntArray(0)
            if (ids.isNotEmpty()) kimlikler[sinif] = ids
        }
        if (kimlikler.isEmpty()) return

        val odevler = uygulama.odevRepository.tumOdevleri().first()
        val veri = CizimVerisi(odevler, System.currentTimeMillis())

        for ((sinif, ids) in kimlikler) {
            val gorunum = gorunumYap(uygulama, sinif, veri) ?: continue
            ids.forEach { yonetici.updateAppWidget(it, gorunum) }
        }
    }

    /** Bir kez okunan veritabanı görüntüsü; dört düzen bunu paylaşıp kendi kovasını çıkarır. */
    private data class CizimVerisi(val odevler: List<Odev>, val simdi: Long)

    private fun gorunumYap(
        context: Context,
        sinif: Class<*>,
        veri: CizimVerisi,
    ): RemoteViews? = when (sinif) {
        OdevWidgetAlcisi::class.java -> ozetGorunumu(context, veri)
        OdevSayacWidgetAlcisi::class.java -> sayacGorunumu(context, veri)
        OdevListeWidgetAlcisi::class.java -> listeGorunumu(context, veri)
        OdevSonrakiWidgetAlcisi::class.java -> sonrakiGorunumu(context, veri)
        else -> null
    }

    // ---- 1) Bugünün özeti (4 × 2) ----

    private fun ozetGorunumu(context: Context, veri: CizimVerisi): RemoteViews {
        val icerik = widgetIcerigi(veri.odevler, veri.simdi)
        val gorunum = RemoteViews(context.packageName, R.layout.odev_widget)

        gorunum.setTextViewText(R.id.widget_baslik, context.getString(R.string.widget_baslik))
        gorunum.setTextViewText(R.id.widget_tarih, veri.simdi.formatliTarih())
        gorunum.setTextViewText(R.id.widget_ozet, ozetMetni(context, icerik))

        val normalRenk = context.getColor(R.color.widget_metin)
        val hataRengi = context.getColor(R.color.widget_hata)

        ozetSatirKimlikleri.forEachIndexed { i, kutu ->
            val odev = icerik.satirlar.getOrNull(i)
            if (odev == null) {
                gorunum.setViewVisibility(kutu, View.GONE)
            } else {
                gorunum.setViewVisibility(kutu, View.VISIBLE)
                gorunum.setTextViewText(ozetMetinKimlikleri[i], widgetSatirMetni(odev))
                // Geciken satır, uygulamadaki rozet gibi `hata` rengine döner:
                // widget bir bakışta "burada bir sorun var" demelidir.
                gorunum.setTextColor(
                    ozetMetinKimlikleri[i],
                    renkVeGecikti(context, odev, veri.simdi, normalRenk, hataRengi),
                )
                tamamlaKutusu(gorunum, ozetIsaretKimlikleri[i], context, WIDGET_YUVA_OZET + i, odev)
            }
        }

        dahaSatiriniYaz(context, gorunum, R.id.widget_daha, icerik)
        gorunum.setOnClickPendingIntent(R.id.widget_kok, anaEkranNiyeti(context, TIKLAMA_OZET))
        return gorunum
    }

    // ---- 2) Sayaçlar (2 × 2) ----

    /**
     * Üç rakam, hiçbir ödev satırı yok.
     *
     * Renkler **anlam** taşır, süs değil: geciken kırmızı, bugün vurgu
     * mavisi, toplam nötr. Sayının rengine bakmak, etiketi okumaktan
     * hızlıdır ve küçük widget'ın asıl avantajıdır.
     */
    private fun sayacGorunumu(context: Context, veri: CizimVerisi): RemoteViews {
        val s = widgetSayaclari(veri.odevler, veri.simdi)
        val gorunum = RemoteViews(context.packageName, R.layout.sayac_widget)

        gorunum.setTextViewText(R.id.sayac_sayi1, s.geciken.toString())
        gorunum.setTextViewText(R.id.sayac_sayi2, s.bugun.toString())
        gorunum.setTextViewText(R.id.sayac_sayi3, s.toplam.toString())

        gorunum.setTextColor(R.id.sayac_sayi1, context.getColor(R.color.widget_hata))
        gorunum.setTextColor(R.id.sayac_sayi2, context.getColor(R.color.widget_vurgu))
        gorunum.setTextColor(R.id.sayac_sayi3, context.getColor(R.color.widget_metin))

        gorunum.setOnClickPendingIntent(R.id.sayac_kok, anaEkranNiyeti(context, TIKLAMA_SAYAC))
        return gorunum
    }

    // ---- 3) Günün listesi (4 × 4) ----

    /**
     * Altı satır ve her satırın sağında kalan süre.
     *
     * Özet widget'ından farkı yalnızca satır sayısı değildir: burada
     * **ne kadar zaman kaldığı** da yazar. Aynı "3 gün" hem sıralamayı
     * doğrular hem de günün baskısını gösterir.
     */
    private fun listeGorunumu(context: Context, veri: CizimVerisi): RemoteViews {
        val icerik = widgetIcerigi(veri.odevler, veri.simdi, satirSayisi = LISTE_SATIR_SAYISI)
        val gorunum = RemoteViews(context.packageName, R.layout.liste_widget)

        gorunum.setTextViewText(R.id.liste_baslik, context.getString(R.string.widget_liste_etiket))
        gorunum.setTextViewText(R.id.liste_tarih, veri.simdi.formatliTarih())
        gorunum.setTextViewText(R.id.liste_ozet, ozetMetni(context, icerik))

        val normalRenk = context.getColor(R.color.widget_metin)
        val hataRengi = context.getColor(R.color.widget_hata)
        val ikincilRenk = context.getColor(R.color.widget_ikincil)

        for (i in 0 until LISTE_SATIR_SAYISI) {
            val satir = listeSatirKimlikleri[i]
            val baslik = listeBaslikKimlikleri[i]
            val kalan = listeKalanKimlikleri[i]
            val odev = icerik.satirlar.getOrNull(i)

            if (odev == null) {
                gorunum.setViewVisibility(satir, View.GONE)
                continue
            }

            val gecikti = odev.gercekDurum(veri.simdi) == Durum.GECEKTI
            gorunum.setViewVisibility(satir, View.VISIBLE)
            gorunum.setTextViewText(baslik, widgetSatirMetni(odev))
            gorunum.setTextColor(baslik, if (gecikti) hataRengi else normalRenk)
            gorunum.setTextViewText(kalan, kalanMetni(context, odev, veri.simdi))
            gorunum.setTextColor(kalan, if (gecikti) hataRengi else ikincilRenk)
            tamamlaKutusu(gorunum, listeIsaretKimlikleri[i], context, WIDGET_YUVA_LISTE + i, odev)
        }

        dahaSatiriniYaz(context, gorunum, R.id.liste_daha, icerik)
        gorunum.setOnClickPendingIntent(R.id.liste_kok, anaEkranNiyeti(context, TIKLAMA_LISTE))
        return gorunum
    }

    // ---- 4) Sonraki teslim (2 × 1) ----

    /**
     * Tek ödev — ve tek tıklamada **o ödevin detayı**.
     *
     * Dört widget içinde niyeti farklı olan tek budur: en sık bakılan
     * bilgi tek bir iş olduğu için, ayrıntısı da o işin ayrıntısıdır.
     * Açılmayacak bir şey yoksa (bekleyen ödev kalmadıysa) tıklama
     * normalde olduğu gibi listeye düşer — ölü bir tıklama bırakılmaz.
     */
    private fun sonrakiGorunumu(context: Context, veri: CizimVerisi): RemoteViews {
        val odev = sonrakiTeslim(veri.odevler, veri.simdi)
        val gorunum = RemoteViews(context.packageName, R.layout.sonraki_widget)

        gorunum.setTextViewText(R.id.sonraki_etiket, context.getString(R.string.widget_sonraki_etiket))

        if (odev == null) {
            gorunum.setTextViewText(R.id.sonraki_baslik, context.getString(R.string.widget_sonraki_bos))
            gorunum.setTextViewText(R.id.sonraki_alt, "")
            // Tamamlanacak bir şey yokken kutu da görünmez: dolu halka,
            // "yapılacak bir şey var" der ama gösterilen bir şey olmazdı.
            gorunum.setViewVisibility(R.id.sonraki_isaret, View.GONE)
            gorunum.setOnClickPendingIntent(
                R.id.sonraki_kok,
                anaEkranNiyeti(context, TIKLAMA_SONRAKI),
            )
        } else {
            gorunum.setTextViewText(R.id.sonraki_baslik, odev.baslik)
            gorunum.setTextViewText(R.id.sonraki_alt, sonrakiAltMetni(context, odev, veri.simdi))
            gorunum.setTextColor(
                R.id.sonraki_alt,
                if (odev.gercekDurum(veri.simdi) == Durum.GECEKTI) {
                    context.getColor(R.color.widget_hata)
                } else {
                    context.getColor(R.color.widget_ikincil)
                },
            )
            gorunum.setViewVisibility(R.id.sonraki_isaret, View.VISIBLE)
            tamamlaKutusu(gorunum, R.id.sonraki_isaret, context, WIDGET_YUVA_SONRAKI, odev)
            gorunum.setOnClickPendingIntent(
                R.id.sonraki_kok,
                detayNiyeti(context, odev.id),
            )
        }

        return gorunum
    }

    // ---- Ortak parçalar ----

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
     * Satırın sağındaki kalan süre: "Gecikti" / "Bugün" / "Yarın" / "3 gün".
     *
     * Gecikenlik önce kontrol edilir: bugünün saati geçmiş bir iş için gün
     * farkı 0'dır ve "Bugün" yazmak onu hâlâ zaman varmış gibi gösterirdi.
     */
    private fun kalanMetni(context: Context, odev: Odev, simdi: Long): String {
        val gun = widgetKalanGun(odev, simdi)
        if (odev.gercekDurum(simdi) == Durum.GECEKTI || gun < 0) {
            return context.getString(R.string.widget_kalan_gecikti)
        }
        return when (gun) {
            0L -> context.getString(R.string.bugun)
            1L -> context.getString(R.string.yarin)
            else -> context.getString(R.string.widget_kalan_gun, gun)
        }
    }

    /** Sonraki teslim widget'ının ikinci satırı: `Ders · kalan süre`. */
    private fun sonrakiAltMetni(context: Context, odev: Odev, simdi: Long): String {
        val kalan = kalanMetni(context, odev, simdi)
        return if (odev.ders.isBlank()) kalan else "${odev.ders} · $kalan"
    }

    /** Geciktiyse [hata], değilse [normal]. */
    private fun renkVeGecikti(
        context: Context,
        odev: Odev,
        simdi: Long,
        normal: Int,
        hata: Int,
    ): Int = if (odev.gercekDurum(simdi) == Durum.GECEKTI) hata else normal

    /** Satır sınırını aşan kalanı "+N daha" diye söyler; aşmıyorsa satırı gizler. */
    private fun dahaSatiriniYaz(
        context: Context,
        gorunum: RemoteViews,
        kimlik: Int,
        icerik: WidgetIcerigi,
    ) {
        val kalan = icerik.bekleyenToplam - icerik.satirlar.size
        if (kalan > 0) {
            gorunum.setViewVisibility(kimlik, View.VISIBLE)
            gorunum.setTextViewText(kimlik, context.getString(R.string.widget_daha, kalan))
        } else {
            gorunum.setViewVisibility(kimlik, View.GONE)
        }
    }

    /**
     * Satırdaki tamamla kutusuna tıklamayı ve erişilebilirlik etiketini yazar.
     *
     * Etiket ödevin adını taşır: ekranda altısı da aynı halka olsa sesli
     * okuyucu "Matematik — Üslü ifadeler tamamla" diyebilsin. Sabit
     * [R.string.widget_tamamla] yalnızca düzenin ilk hâlinde geçerlidir.
     *
     * @param yuva Kutunun kendi numarası — ödevin kimliği değil. Adresin
     *   neden ödevden değil yuvadan geldiğini bkz. [widgetTamamlamaAdresi].
     */
    private fun tamamlaKutusu(
        gorunum: RemoteViews,
        kimlik: Int,
        context: Context,
        yuva: Int,
        odev: Odev,
    ) {
        gorunum.setOnClickPendingIntent(kimlik, hizliTamamlaNiyeti(context, yuva, odev.id))
        gorunum.setCharSequence(
            kimlik,
            "setContentDescription",
            context.getString(R.string.widget_tamamla_ada, widgetSatirMetni(odev)),
        )
    }

    // ---- Niyetler ----

    /**
     * Ana listeye giden tıklama.
     *
     * `FLAG_ACTIVITY_CLEAR_TOP`, uygulama zaten arka plandaysa yeni bir
     * yığın değil mevcut listeyi getirir. `SINGLE_TOP` da buna eklenir:
     * yoksa CLEAR_TOP aktiviteyi bitirip yeniden kurardı ve kullanıcı
     * bulunduğu ekrandan kopardı.
     */
    private fun anaEkranNiyeti(context: Context, kod: Int): PendingIntent {
        val niyet = Intent(context, MainActivity::class.java).apply {
            flags = bayraklar
        }
        return PendingIntent.getActivity(
            context, kod, niyet,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Belirli bir ödevin detayına giden tıklama.
     *
     * Extra, Intent eşitliğine **dahil değildir**; aynı istek koduyla
     * yazılınca sistem eskisini tutar. `FLAG_UPDATE_CURRENT` bu yüzden
     * zorunludur — sıradaki ödev değiştiğinde eski kimlik sessizce ezilmez.
     */
    private fun detayNiyeti(context: Context, odevId: Long): PendingIntent {
        val niyet = Intent(context, MainActivity::class.java).apply {
            flags = bayraklar
            putExtra(MainActivity.EXTRA_ODEV_ID, odevId)
        }
        return PendingIntent.getActivity(
            context, TIKLAMA_SONRAKI, niyet,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Tamamla kutusunun yayını — [HizliTamamlaAlcisi]'ni çağırır.
     *
     * Adres **yuvadan**, extra **ödevden** gelir (bkz. [widgetTamamlamaAdresi]).
     * `FLAG_UPDATE_CURRENT` bu ikilinin yerinde tazelenmesini sağlar: kutu
     * yeni bir ödev gösterdiğinde eski extra sessizce ezilir ve sistemde
     * kullanılmayan bir niyet birikmez.
     *
     * `FLAG_IMMUTABLE` zorunlu; doldurulacak bir extra yok, hepsi baştan
     * yazılır — gönderenin (launcher) ekleyebileceği bir şey bırakılmaz.
     */
    private fun hizliTamamlaNiyeti(context: Context, yuva: Int, odevId: Long): PendingIntent {
        val niyet = Intent(context, HizliTamamlaAlcisi::class.java).apply {
            data = Uri.parse(widgetTamamlamaAdresi(yuva))
            putExtra(HizliTamamlaAlcisi.VERI_ODEV_ID, odevId)
        }
        return PendingIntent.getBroadcast(
            context, TAMAMLAMA, niyet,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val bayraklar: Int
        get() = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP

    // ---- Kimlik listeleri ----

    /** Özet widget'ının üç satır kasası, soldan sağa. */
    private val ozetSatirKimlikleri =
        intArrayOf(R.id.widget_satir1, R.id.widget_satir2, R.id.widget_satir3)

    /** Kasanın içindeki metin — kutuyla karıştırılmamalı, ayrı kimliktir. */
    private val ozetMetinKimlikleri =
        intArrayOf(R.id.widget_metin1, R.id.widget_metin2, R.id.widget_metin3)

    /** Özet widget'ının tamamla kutuları, satırlarla aynı sırada. */
    private val ozetIsaretKimlikleri =
        intArrayOf(R.id.widget_isaret1, R.id.widget_isaret2, R.id.widget_isaret3)

    /** Liste widget'ının altı satır kasası, her kasada başlık ve süre vardır. */
    private val listeSatirKimlikleri = intArrayOf(
        R.id.liste_satir1, R.id.liste_satir2, R.id.liste_satir3,
        R.id.liste_satir4, R.id.liste_satir5, R.id.liste_satir6,
    )

    private val listeBaslikKimlikleri = intArrayOf(
        R.id.liste_baslik1, R.id.liste_baslik2, R.id.liste_baslik3,
        R.id.liste_baslik4, R.id.liste_baslik5, R.id.liste_baslik6,
    )

    private val listeKalanKimlikleri = intArrayOf(
        R.id.liste_kalan1, R.id.liste_kalan2, R.id.liste_kalan3,
        R.id.liste_kalan4, R.id.liste_kalan5, R.id.liste_kalan6,
    )

    /** Liste widget'ının tamamla kutuları, satırlarla aynı sırada. */
    private val listeIsaretKimlikleri = intArrayOf(
        R.id.liste_isaret1, R.id.liste_isaret2, R.id.liste_isaret3,
        R.id.liste_isaret4, R.id.liste_isaret5, R.id.liste_isaret6,
    )

    /**
     * Tıklama istek kodları.
     *
     * `getActivity` ile açılan niyetler kendi kimlik uzayında yaşar, ama
     * **kod + Intent** birlikte eşittir. Her widget'ın ayrı kodu,
     * sonradan eklenecek bir eylemin başka bir widget'ın niyetini sessizce
     * ezmesini engeller. Sıralı ve seyrek tutulmuştur ki araya bir tane
     * daha konabilsin.
     */
    private const val TIKLAMA_OZET = 0
    private const val TIKLAMA_SAYAC = 10
    private const val TIKLAMA_LISTE = 20
    private const val TIKLAMA_SONRAKI = 30

    /**
     * Tamamla kutularının istek kodu — **tek bir değer**, kutu başına değil.
     *
     * Ayırt edici yük istek kodunda değil, [hizliTamamlaNiyeti] adresinin
     * üstündedir; bu yüzden kod paylaşılsa bile kutular birbirinin niyetini
     * ezmez. Bildirim eylemi ise adresi olmayan başka bir niyettir ve kendi
     * kodunu (bildirim kimliği) kullanır — iki eylem hiçbir koşulda
     * birleşemez.
     */
    private const val TAMAMLAMA = 40
}
