package com.odevtakip.app.bildirim

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.odevtakip.app.OdevTakipApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Bildirimdeki **"Tamamla"** düğmesinin ve widget'lardaki **tamamla
 * kutusunun** alıcısı.
 *
 * Amaç, hatırlatma döngüsünü kapatmaktır: sistem "teslimine az kaldı"
 * bildirimini gönderir, kullanıcı işi zaten bitirmiştir ve uygulamayı açıp
 * kartı bulmak istemez. Düğmeye basınca ödev doğrudan tamamlanır ve bildirim
 * kendiliğinden kalkar. Ana ekran widget'ındaki kutu da **aynı alıcıya**
 * gider — iki yerde iki ayrı tamamlama yolu olsaydı biri diğerinin yaptığını
 * unutabilirdi. Tek fark niyetin taşıdığı ekstradır: bildirim eylemi
 * kaldırılacak [VERI_BILDIRIM_ID]'yi yazar, widget yazmaz, çünkü elinde
 * kaldırılacak bir bildirim yoktur.
 *
 * ### Neden alıcı, neden activity değil?
 *
 * Bildirim eylemi ya bir activity'yi ya da bir alıcıyı çağırır. Activity
 * açmak (arka planda) Android 10'dan beri kısıtlı ve ekranı gereksiz
 * karıştırırdı; alıcı ise arka planda sessizce işi bitirir. Ayrıca eylemin
 * tek yaptığı **yazmak** — gösterecek bir şey yok.
 *
 * ### Manifest kaydı neden gerekli?
 *
 * `android:exported="false"` ile **yalnızca bu uygulamanın** bildirimleri
 * ve niyetleri bu alıcıyı uyandırabilir; üçüncü bir uygulama `PendingIntent`'i
 * taklit edip ödevi kendi isteğiyle tamamlayamaz. Yine de manifest'te
 * listelenmek zorundadır: listelenmemiş bir alıcı sistem tarafından hiç
 * oluşturulmaz.
 *
 * ### Süreç kapalıyken?
 *
 * Alıcı uyanınca [OdevTakipApplication.onCreate] da çalışır. Bu iki işe
 * yarar: repository hazır olur ve `hatirlatlariIzle()` kurulur — o akış,
 * yazımın ardından planlanan hatırlatmayı yeniden eşitler ve bu ödev için
 * bekleyen işi iptal eder. Yani `WorkManager` tarafı elle uğraşılmadan
 * temizlenir. `widgetiIzle()` de kurulduğu için yazımın ardından ana ekran
 * widget'ları kendiliğinden yeniden çizilir ve satır kaybolur.
 */
class HizliTamamlaAlcisi : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val odevId = intent.getLongExtra(VERI_ODEV_ID, -1L)
        val bildirimId = intent.getIntExtra(VERI_BILDIRIM_ID, BILDIRIM_YOK)

        // Eksik/bozuk bir niyetle ödev değiştirilmez: sessizce vazgeçilir.
        // Tek şart ödev kimliğidir. [VERI_BILDIRIM_ID] **gelmeyebilir** —
        // widget kutusunu ilgilendiren bir bildirim yoktur — ve bu bir hata
        // değildir; yalnızca "kaldırılacak bildirim yok" demektir.
        if (odevId <= 0) return

        val uygulama = context.applicationContext
        // `goAsync()`, `onReceive` döndükten sonra sürecin hâlâ yaşamasını
        // sağlar; karşılığında iş bitince `finish()` çağırmak zorundayız.
        val sonuc = goAsync()

        kapsam.launch {
            try {
                // `false` dönmüş olması kaydın zaten tamamlanmış olduğu
                // anlamına gelir — ikinci kez basılmıştır. Yine de bildirim
                // kaldırılır: bayat uyarının tek sebebi o düğmedir.
                val tamamlandi =
                    (uygulama as OdevTakipApplication).odevRepository.tamamla(odevId)

                if (bildirimId >= 0) {
                    BildirimYonetici.bildirimiIptal(uygulama, bildirimId)
                }

                val kaynak = if (bildirimId >= 0) "Bildirimden" else "Widget'tan"
                val not = if (tamamlandi) "" else " (kayıt zaten tamammış)"
                Log.d(TAG, "$kaynak tamamlandı: $odevId$not")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Veri yazılamadıysa bildirim **silinmez**: kullanıcı düğmeye
                // yeniden basabilsin, sessiz bir kayıp yaşanmasın.
                Log.e(TAG, "Tamamlanamadı: $odevId", e)
            } finally {
                // `?.`: bkz. [TarihDegisimAlcisi.widgetiYenile]. Burada
                // `finish()` NPE'i bildirimin asla kaldırılmamasına yol
                // açabilirdi (ödül işi yarım kalır, kullanıcı düğmeye yeniden
                // basamazdı).
                sonuc?.finish()
            }
        }
    }

    /**
     * Niyet extra anahtarları, yok bildirimi ve yazımın yürüttüğü kapsam.
     *
     * `internal`: [BildirimYonetici] ile widget çizerken aynı anahtarları
     * **tek kaynaktan** okumak zorunda — iki yerde iki ayrı metin yazmak,
     * sessizce eşleşmeyen bir extra demektir.
     */
    internal companion object {
        const val TAG = "OdevTakip"

        /** Bildirim eyleminin niyete yazdığı ödev kimliği. */
        const val VERI_ODEV_ID = "hizliOdevId"

        /**
         * Kaldırılacak bildirimin kimliği.
         *
         * Eylem hangi bildirimdeyse **sadece o** kaldırılır: yaklaşan teslim
         * bildirimi tamamlanınca gecikme bildirimi (ya da tersi) hâlâ doğru
         * söylüyor olabilir ve o da ekranda durabilir.
         */
        const val VERI_BILDIRIM_ID = "hizliBildirimId"

        /**
         * "Kaldırılacak bildirim yok" anlamı taşıyan değer.
         *
         * Bildirim kimlikleri 0'dan başlar; bu yüzden eksik extra bu
         * indirgeçle karşılaşır ve alıcı bildirimi **silmeden** yalnızca
         * ödevi tamamlar. Widget kutusu bu değeri yazar.
         */
        const val BILDIRIM_YOK = -1

        /**
         * Yazımı yürüten kapsam.
         *
         * Sınıf düzeyinde tutulur; `onReceive` içinde kurulsaydı metoddan
         * çıkıldığı anda hiçbir referans kalmaz, tek başına Job'ın toplanma
         * riski doğardı.
         */
        val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
