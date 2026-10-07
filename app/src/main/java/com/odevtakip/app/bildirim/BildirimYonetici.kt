package com.odevtakip.app.bildirim

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.odevtakip.app.MainActivity
import com.odevtakip.app.R
import com.odevtakip.app.data.Odev
import com.odevtakip.app.util.formatliSaat
import com.odevtakip.app.util.formatliTarih

/**
 * Ödev bildirimlerini yönetir: kanalları oluşturur, izin ister, bildirim atar.
 *
 * Kapsam iki bildirim türü ve **iki kanal** ile sınırlı:
 *
 *  - **Geciken ödevler** ([KANAL_ID]) — yalnızca geçiş anında çalışır:
 *    [com.odevtakip.app.work.DurumGuncelleWorker] yeni bir ödev geciktiğinde
 *    çağırır. Zaten gecikmiş ödevler her periyotta **tekrar hatırlatılmaz**;
 *    bu, uygulamayı spam'e çevirirdi.
 *
 *  - **Yaklaşan teslimler** ([YAKLASAN_KANAL_ID]) — tesliminden önce tek sefer
 *    ([com.odevtakip.app.work.HatirlatmaZamanlayici] planlar,
 *    [com.odevtakip.app.work.HatirlatmaWorker] gönderir). Ayrı kanal olması
 *    bilinçlidir: kullanıcı yaklaşanı susturup gecikenleri açık bırakabilsin.
 *
 * Kanalların ayrı olması, derin bağlantının da ayrı olması demek: gecikme
 * bildirimi bir **grup** bildirimidir (tek bir hedefe inemez) ve ana ekrana
 * açılır; yaklaşan teslim bildirimi tek bir ödevi anlatır ve doğrudan o
 * ödevin **detayına** iner.
 *
 * Tek hedefi olan her iki bildirime de **"Tamamla"** eylemi iliştirilir;
 * bkz. [hizliTamamlaEylemi]. Dokunmak detaya götürür, eylem ise işi
 * bitirir — ikisi farklı iştir ve karıştırılmaz.
 */
object BildirimYonetici {

    /**
     * Bildirim kanalı kimliği. Dışa açıktır; testler kanalın varlığını
     * bununla doğrular. (Kanal adı kullanıcı ayarlarında görünür — bkz.
     * `R.string.bildirim_kanal_adi`.)
     */
    const val KANAL_ID = "geciken_odevler"

    /**
     * Yaklaşan teslim bildirimlerinin kanal kimliği.
     *
     * Ayrı kanal, Android'in kullanıcıya tanıdığı ayrı susturma anahtarı
     * demektir — ikisi tek kanalda toplansaydı "ödev gecikince haber ver ama
     * önceden uyarma" seçilemezdi.
     */
    const val YAKLASAN_KANAL_ID = "yaklasan_teslim"

    /** Bildirim kimliği. Dışa açıktır; testler aktif bildirimi bununla bulur. */
    const val BILDIRIM_ID = 1001

    /**
     * Yaklaşan teslim bildirimlerinin kimlik tabanı.
     *
     * Her ödev kendi kimliğini alır (`taban + ödev kimliği`); böylece iki
     * ödev aynı anda yaklaşsa birbirinin bildirimini ezmezler ve tek tek
     * iptal edilebilirler. `BILDIRIM_ID` ile çakışmaz.
     */
    const val YAKLASAN_ID_BASLANGIC = 2000

    private const val IZIN_ISTEK_KODU = 4401

    /**
     * Ana ekran (extra'sız) açılış niyetinin istek kodu.
     *
     * `PendingIntent`, aynı istek kodu + aynı bileşen verildiğinde tek kayıtta
     * birleşir ve en son yazılan **extra**'lar hepsine uygulanır. Bu yüzden
     * gecikme bildirimi (ana ekrana açılır) ile yaklaşan teslim bildirimi
     * (bir ödevin detayına açılır) **farklı** kodlar kullanmak zorundadır.
     */
    private const val ANA_ISTEK_KODU = 0

    // İzin en fazla bir kez istenir; kullanıcı reddettikten sonra tekrar
    // sorulmaz (sistem de sormaz, ama uygulama da ısrar etmez).
    private const val TERCIH_ADI = "ayarlar"
    private const val ANAHTAR_IZIN_ISTENDI = "bildirim_izni_istendi"

    // ---- Kanal ----

    /**
     * Bildirim kanallarını oluşturur. Android 8.0 (API 26) öncesi kanal gerekmez.
     *
     * [com.odevtakip.app.OdevTakipApplication.onCreate] içinde, ilk bildirimden
     * **önce** çağrılır — kanal yoksa bildirim sessizce kaybolur. Aynı fonksiyon
     * iki kanalı da açar; ayrı ayrı çağırmak unutulan kanal demektir.
     */
    fun kanaliOlustur(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        kanalEkle(context, KANAL_ID, R.string.bildirim_kanal_adi, R.string.bildirim_kanal_tanimi)
        kanalEkle(
            context,
            YAKLASAN_KANAL_ID,
            R.string.bildirim_yaklasan_kanal_adi,
            R.string.bildirim_yaklasan_kanal_tanimi,
        )
    }

    private fun kanalEkle(
        context: Context,
        kimlik: String,
        @StringRes adRes: Int,
        @StringRes tanimRes: Int,
    ) {
        val kanal = NotificationChannel(
            kimlik,
            context.getString(adRes),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(tanimRes)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(kanal)
    }

    // ---- İzin ----

    /** Android 13 ve üzerinde bildirim izni gerekir; alt sürümlerde otomatik verilir. */
    fun izinVerilmis(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Daha önce istenmediyse bildirim iznini bir kez ister.
     *
     * Bağlam bilinçli olarak seçildi: kullanıcı ilk ödevini kaydettiğinde, yani
     * hatırlatmanın anlamlı olduğu an. Uygulama açılışında sorulmuş olsaydı
     * kullanıcı henüz neyin hatırlatılacağını bilmeden karar vermek zorunda
     * kalırdı.
     *
     * Kullanıcı reddederse bir daha sorulmaz — ayarlardan elle açabilir.
     */
    fun izinBirKezIste(activity: Activity) {
        if (izinVerilmis(activity)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val tercihler = activity.getSharedPreferences(TERCIH_ADI, Context.MODE_PRIVATE)
        if (tercihler.getBoolean(ANAHTAR_IZIN_ISTENDI, false)) return

        tercihler.edit().putBoolean(ANAHTAR_IZIN_ISTENDI, true).apply()
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            IZIN_ISTEK_KODU,
        )
    }

    // ---- Bildirim ----

    /**
     * Yeni geciken ödevler için bildirim gönderir.
     *
     * Herhangi bir nedenle (izin yok, kullanıcı ayarlardan kapatmış) bildirim
     * gönderilemiyorsa **sessizce vazgeçer** — iş kuralı [odevler] işaretlemekti
     * ve o zaten başarılı oldu; bildirim yalnızca ikramiye.
     *
     * @param odevler Bu senkron döngüsünde **yeni olarak** geciken ödevler.
     *   Boşsa hiçbir şey yapılmaz.
     */
    fun geciktiBildir(context: Context, odevler: List<Odev>) {
        if (odevler.isEmpty()) return
        if (!izinVerilmis(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val (baslik, satir) = if (odevler.size == 1) {
            odevler[0].baslik to context.getString(R.string.bildirim_tek_metin)
        } else {
            context.getString(R.string.bildirim_cok_baslik, odevler.size) to
                odevler.take(2).joinToString(", ") { it.baslik }
        }

        val kurucu = NotificationCompat.Builder(context, KANAL_ID)
            .setSmallIcon(R.drawable.ic_bildirim_odev)
            .setContentTitle(baslik)
            .setContentText(satir)
            .setContentIntent(acilisNiyeti(context, odevId = null, istekKodu = ANA_ISTEK_KODU))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Eylem yalnızca **tek** ödev söz konusuyken anlamlı: grup
        // bildiriminde "Tamamla" hangi ödevi kastettiğin söylemez.
        if (odevler.size == 1 && odevler[0].id > 0) {
            kurucu.addAction(hizliTamamlaEylemi(context, odevler[0].id, BILDIRIM_ID))
        }

        // BigTextStyle'ın `bigText` değeri dar görünümde `contentText`'in yerini
        // alır. Tek ödevde bu, "son teslim tarihi geçti" uyarısını silip başlığı
        // aynen tekrarlardı — bu yüzden uzatma yalnızca başlıkların gerçekten
        // sığmadığı çoklu durumda eklenir.
        if (odevler.size > 1) {
            kurucu.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(odevler.joinToString(", ") { it.baslik })
            )
        }

        val bildirim = kurucu.build()

        // Bekleyen bildirim yoksa varsayılan davranışı kullan; tek iş kuyruğu
        // olduğu için kimlik sabittir — aynı ödev grubu güncellenir, yığılmaz.
        NotificationManagerCompat.from(context).notify(BILDIRIM_ID, bildirim)
    }

    // ---- Yaklaşan teslim ----

    /**
     * Tek bir ödev için teslim öncesi hatırlatma bildirimi gönderir.
     *
     * Gecikme bildiriminin aksine burada tek bir hedef vardır, bu yüzden
     * derin bağlantı anlamlıdır: dokunulduğunda doğrudan o ödevin detayı açılır.
     *
     * Metin **mutlak** tarih verir ("Son teslim: 5 Eki 2026 23:59"), göreli
     * değil: bildirim gölgede saatlerce yatabilir ve "Bugün" o an bayatlamış
     * olur.
     *
     * İzin yoksa **geri dönüp `false` döner** — hatırlatma bir ikramiyedir,
     * veri yazmak değil. Dönen değer önemli: çağıran Worker aksi hâlde
     * "bildirildi" diye loglayıp kuyruktaki işi başarılı sanırdı ve neden
     * bildirim gitmediği anlaşılamazdı.
     *
     * @return Bildirim gerçekten sistemden geçtiyse `true`.
     */
    fun yaklasanBildir(context: Context, odev: Odev): Boolean {
        if (!izinVerilmis(context)) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        val teslim = "${odev.sonTarih.formatliTarih()} ${odev.sonTarih.formatliSaat()}"

        val bildirim = NotificationCompat.Builder(context, YAKLASAN_KANAL_ID)
            .setSmallIcon(R.drawable.ic_bildirim_odev)
            .setContentTitle(odev.baslik)
            .setContentText(context.getString(R.string.bildirim_yaklasan_metin, teslim))
            .setContentIntent(acilisNiyeti(context, odev.id, yaklasanId(odev.id)))
            .addAction(hizliTamamlaEylemi(context, odev.id, yaklasanId(odev.id)))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(yaklasanId(odev.id), bildirim)

        return true
    }

    /**
     * Bir ödevin bekleyen teslim öncesi bildirimini kaldırır.
     *
     * [com.odevtakip.app.work.HatirlatmaZamanlayici] bunu, ödev artık
     * hatırlatılmaya değer olmadığında (tamamlandı, silindi ya da süresi
     * geçti) çağırır. Bildirim gölgede duruyorsa burada temizlenir; yoksa
     * eski bir tarih göstermeye devam ederdi.
     */
    fun yaklasaniIptal(context: Context, odevId: Long) {
        NotificationManagerCompat.from(context).cancel(yaklasanId(odevId))
    }

    /**
     * Verilen kimlikteki bildirimi sistemden kaldırır.
     *
     * [HizliTamamlaAlcisi] iş tamamlanınca çağırır. `setAutoCancel` yalnızca
     * bildirimin **kendisine** dokunulduğunda çalışır; eylem düğmesine
     * basıldığında çalışmaz. Burada yapılmazsa ödev bittikten sonra bayat
     * uyarı ekranda kalmaya devam ederdi.
     *
     * Kimlik yoksa sessizce biter — bildirim zaten kaldırılmış olabilir,
     * bu bir hata değildir.
     */
    fun bildirimiIptal(context: Context, bildirimId: Int) {
        NotificationManagerCompat.from(context).cancel(bildirimId)
    }

    /**
     * Bildirime iliştirilen **"Tamamla"** eylemi.
     *
     * Düğme [HizliTamamlaAlcisi]'ni çağırır; alıcı ödevi tamamlayıp taşıdığı
     * bildirimi kaldırır. Uygulama süreç kapalıyken bile uyanır, çünkü niyet
     * manifest'e kayıtlı açık bir bileşene gider.
     *
     * **İstek kodu olarak bildirim kimliği** kullanılır, ödev kimliği değil:
     * `PendingIntent`, aynı kod + aynı bileşen verildiğinde tek kayıtta
     * birleşir ve en son yazılan extra'lar hepsine uygulanır. Aynı ödev hem
     * yaklaşan teslim bildiriminde (2000 + id) hem de gecikme bildiriminde
     * (1001) eylem taşıyabilir; iki kod da farklı olduğu için birbirinin
     * extra'larını ezmezler. `FLAG_UPDATE_CURRENT` ise aynı bildirim
     * yenilendiğinde eski extra'ların kalmasını engeller.
     *
     * @param odevId Tamamlanacak ödev.
     * @param bildirimId Eylemin üzerinde durduğu bildirim; alıcı bu kimliği
     *   kaldırır ve istek kodu buradan türetilir.
     */
    private fun hizliTamamlaEylemi(
        context: Context,
        odevId: Long,
        bildirimId: Int,
    ): NotificationCompat.Action {
        val niyet = Intent(context, HizliTamamlaAlcisi::class.java).apply {
            putExtra(HizliTamamlaAlcisi.VERI_ODEV_ID, odevId)
            putExtra(HizliTamamlaAlcisi.VERI_BILDIRIM_ID, bildirimId)
        }

        val gonderi = PendingIntent.getBroadcast(
            context,
            bildirimId,
            niyet,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Action(
            R.drawable.ic_isaret,
            context.getString(R.string.bildirim_tamamla),
            gonderi,
        )
    }

    /**
     * Kullanıcıyı sistemin bu uygulamaya ait bildirim ayarlarına götürür.
     *
     * Uygulamanın izni kendisi isteyip değiştiremez. Android 13+ izin bir kez
     * reddedildikten sonra aynı diyaloğu bir daha göstermez — tek meşru yol
     * kullanıcıyı ayarlara yönlendirmektir. Ayrıca iki kanalın (geciken /
     * yaklaşan) ayrı ayrı susturulabilmesi de yalnızca orada mümkündür.
     *
     * 8.0 öncesi kanal ayarı olmadığından genel uygulama bilgi sayfasına
     * düşülür.
     */
    fun bildirimAyalariniAc(context: Context) {
        val niyet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }

        // Activity dışı bir bağlamdan çağrılıyorsa ayrı görevde açılması gerekir.
        if (context !is Activity) niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(niyet)
    }

    /**
     * Ödevin teslim öncesi bildirimi için kullanılan tekil sayı.
     *
     * Hem bildirim kimliği hem de açılış niyetinin istek kodu olabilir: ikisi
     * farklı alanlardır ve bu ödev için **aynı** sayıyı taşımak sakıncasızdır —
     * asıl şart, birbirinden farklı ödevlerin birbirini ezmemesidir.
     */
    private fun yaklasanId(odevId: Long): Int = YAKLASAN_ID_BASLANGIC + odevId.toInt()

    /** Bildirime dokunulunca açılacak niyet; `odevId` verilirse detaya iner. */
    private fun acilisNiyeti(
        context: Context,
        odevId: Long?,
        istekKodu: Int,
    ): PendingIntent {
        val niyet = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (odevId != null && odevId > 0) {
                putExtra(MainActivity.EXTRA_ODEV_ID, odevId)
            }
        }
        return PendingIntent.getActivity(
            context,
            istekKodu,
            niyet,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
