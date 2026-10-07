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
 * Bildirimdeki **"Tamamla"** düğmesinin alıcısı.
 *
 * Amaç, hatırlatma döngüsünü kapatmaktır: sistem "teslimine az kaldı"
 * bildirimini gönderir, kullanıcı işi zaten bitirmiştir ve uygulamayı açıp
 * kartı bulmak istemez. Düğmeye basınca ödev doğrudan tamamlanır ve bildirim
 * kendiliğinden kalkar.
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
 * bu alıcıyı uyandırabilir; üçüncü bir uygulama `PendingIntent`'i taklit
 * edip ödevi kendi isteğiyle tamamlayamaz. Yine de manifest'te listelenmek
 * zorundadır: listelenmemiş bir alıcı sistem tarafından hiç oluşturulmaz.
 *
 * ### Süreç kapalıyken?
 *
 * Alıcı uyanınca [OdevTakipApplication.onCreate] da çalışır. Bu iki işe
 * yarar: repository hazır olur ve `hatirlatlariIzle()` kurulur — o akış,
 * yazımın ardından planlanan hatırlatmayı yeniden eşitler ve bu ödev için
 * bekleyen işi iptal eder. Yani `WorkManager` tarafı elle uğraşılmadan
 * temizlenir.
 */
class HizliTamamlaAlcisi : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val odevId = intent.getLongExtra(VERI_ODEV_ID, -1L)
        val bildirimId = intent.getIntExtra(VERI_BILDIRIM_ID, -1)

        // Eksik/bozuk bir niyetle ödev değiştirilmez: sessizce vazgeçilir.
        if (odevId <= 0 || bildirimId < 0) return

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

                BildirimYonetici.bildirimiIptal(uygulama, bildirimId)

                Log.d(
                    TAG,
                    if (tamamlandi) "Bildirimden tamamlandı: $odevId"
                    else "Bildirimden tamamlandı ama kayıt zaten tamammış: $odevId",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Veri yazılamadıysa bildirim **silinmez**: kullanıcı düğmeye
                // yeniden basabilsin, sessiz bir kayıp yaşanmasın.
                Log.e(TAG, "Bildirimden tamamlanamadı: $odevId", e)
            } finally {
                sonuc.finish()
            }
        }
    }

    /**
     * Niyet extra anahtarları ve yazımın yürüttüğü kapsam.
     *
     * `internal`: [BildirimYonetici] eylemi kurarken aynı anahtarları
     * **tek kaynaktan** okumak zorunda — iki dosyada iki ayrı metin
     * yazmak, sessizce eşleşmeyen bir extra demektir.
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
         * Yazımı yürüten kapsam.
         *
         * Sınıf düzeyinde tutulur; `onReceive` içinde kurulsaydı metoddan
         * çıkıldığı anda hiçbir referans kalmaz, tek başına Job'ın toplanma
         * riski doğardı.
         */
        val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
