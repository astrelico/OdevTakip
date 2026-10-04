package com.odevtakip.app.bildirim

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.odevtakip.app.MainActivity
import com.odevtakip.app.R
import com.odevtakip.app.data.Odev

/**
 * Geciken ödev bildirimlerini yönetir: kanal oluşturur, izin ister, bildirim atar.
 *
 * Kapsam bilinçli olarak dar tutuldu:
 *  - **Tek bir kanal** — kullanıcı ayarlardan toptan açıp kapatabilsin.
 *  - **Yalnızca geçiş anında** bildirim: `DurumGuncelleWorker` yeni bir ödev
 *    geciktiğinde çağırır. Zaten gecikmiş ödevler her periyotta **tekrar
 *    hatırlatılmaz**; bu, uygulamayı spam'e çevirirdi.
 *  - Bildirim merkezine dokununca uygulama açılır (derin bağlantı bilinçli
 *    olarak yalnızca ana ekrana — kapsamı küçük tutmak için).
 */
object BildirimYonetici {

    /**
     * Bildirim kanalı kimliği. Dışa açıktır; testler kanalın varlığını
     * bununla doğrular. (Kanal adı kullanıcı ayarlarında görünür — bkz.
     * `R.string.bildirim_kanal_adi`.)
     */
    const val KANAL_ID = "geciken_odevler"

    /** Bildirim kimliği. Dışa açıktır; testler aktif bildirimi bununla bulur. */
    const val BILDIRIM_ID = 1001

    private const val IZIN_ISTEK_KODU = 4401

    // İzin en fazla bir kez istenir; kullanıcı reddettikten sonra tekrar
    // sorulmaz (sistem de sormaz, ama uygulama da ısrar etmez).
    private const val TERCIH_ADI = "ayarlar"
    private const val ANAHTAR_IZIN_ISTENDI = "bildirim_izni_istendi"

    // ---- Kanal ----

    /**
     * Bildirim kanalını oluşturur. Android 8.0 (API 26) öncesi kanal gerektirmez.
     *
     * [com.odevtakip.app.OdevTakipApplication.onCreate] içinde, ilk bildirimden
     * **önce** çağrılır — kanal yoksa bildirim sessizce kaybolur.
     */
    fun kanaliOlustur(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val kanal = NotificationChannel(
            KANAL_ID,
            context.getString(R.string.bildirim_kanal_adi),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.bildirim_kanal_tanimi)
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
            .setContentIntent(acilisNiyeti(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

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

    /** Bildirime dokununca ana ekranı açar. */
    private fun acilisNiyeti(context: Context): PendingIntent {
        val niyet = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            niyet,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
