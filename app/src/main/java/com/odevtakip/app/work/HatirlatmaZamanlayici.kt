package com.odevtakip.app.work

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.odevtakip.app.bildirim.BildirimYonetici
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.HatirlatmaAraligi
import com.odevtakip.app.data.Odev
import com.odevtakip.app.util.tarihSaatiniDonustur
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Teslim öncesi hatırlatma işlerini planlar.
 *
 * ### Neden tek tek değil de baştan sona?
 *
 * [eslestir] verilen **tüm** listeyi silip yeniden kurar. Tek tek
 * ekleme/çıkarma yapmak yerine bu yolu seçmek iki nedenden:
 *
 *  1. **Yetim iş kalmaz.** Bir ödev silindiğinde ya da teslim tarihi
 *     değiştiğinde eski işi elle bulup iptal etmek gerekir; unutulan bir iş,
 *     olmayan bir ödev için bildirim atar. Etiketle toptan iptal etmek bu
 *     sınıfı hataya kapalı kılar.
 *  2. **Tek giriş noktası.** Ödev ekleme, düzenleme, tamamlama, silme ve
 *     ayarlardan aralık değişimi hepsi aynı yolu çağırır. Aksi hâlde her
 *     yeni eyleme bir "hatırlatmayı da güncelle" adımı eklemek gerekirdi
 *     ve biri unutulurdu.
 *
 * Çağrılmadan önce hangi değişikliklerin tetikleyeceğine bak:
 * [com.odevtakip.app.OdevTakipApplication] ödev listesi ile hatırlatma tercihini
 * izleyip her değişiklikte burayı çağırır; ViewModel'e ek kod konmaz.
 *
 * Planlama yalnızca WorkManager'a yazım yapar, beklemez.
 */
object HatirlatmaZamanlayici {

    /**
     * Bütün hatırlatma işlerinin etiketi.
     *
     * Dışa açıktır; testler kuyruğu bu adla sorgular.
     */
    const val ETIKET = "odev-hatirlatma"

    /** İş kimliğinin öneki — tanıdık olsun diye etiketten ayrı tutulur. */
    private const val IS_AD_ON_EK = "odev-hatirlatma-"

    /** [HatirlatmaWorker]'a aktarılan ödev kimliği. */
    const val VERI_ODEV_ID = "odevId"

    /** Teşhis için: planlama sessiz kalırsa "hatırlatma neden çalışmıyor?" sorusu yanıtsız kalır. */
    private const val TAG = "OdevTakip"

    /**
     * Verilen ödev listesi için hatırlatmaları yeniden eşitler.
     *
     * Her ödev için dört olasılık vardır:
     *
     *  - **Uygun ve en az bir zamanı gelmemiş** → gelecekte kalan her aralık
     *    için gecikmeli tek seferlik iş kuyruğa alınır. Aynı ödev için kurulan
     *    işlerin hepsi **aynı bildirim kimliğine** yazar; bu yüzden sırayla
     *    gelen hatırlatmalar üst üste yığılmaz, yenisi eskisinin yerini alır.
     *  - **Uygun ama bütün plan anları geçmiş** → hiçbir şey yapılmaz. İş
     *    zaten kurulamazdı; daha önce fırlamış bildirime de dokunulmaz çünkü
     *    o an hâlâ doğru tarihi söyler.
     *  - **Uygun değil** (tamamlandı ya da süresi geçti) → hem bekleyen işi
     *    yok edilir hem de gölgede duran bildirimi kaldırılır.
     *  - **Hiç aralık seçilmedi** → üçüncü durumla aynıdır: plan da
     *    gölgedeki bildirim de temizlenir.
     *
     * @param odevler Eşitlenecek tam liste (silinenler zaten yoktur).
     * @param secimler Ayarlardan seçilen aralıklar; **boş küme** hatırlatmanın
     *   kapalı olduğu anlamına gelir.
     */
    suspend fun eslestir(
        context: Context,
        odevler: List<Odev>,
        secimler: Set<HatirlatmaAraligi>,
    ) {
        val simdi = System.currentTimeMillis()
        val yonetici = WorkManager.getInstance(context)

        // Tek etiketle toptan iptal: listede artık olmayan bir ödevin işi de
        // böylece temizlenir.
        yonetici.cancelAllWorkByTag(ETIKET)

        var planlanan = 0

        odevler.forEach { odev ->
            val zamanlar = hatirlatmaZamanlari(odev.sonTarih, secimler)
            val uygun = odev.durum != Durum.TAMAMLANDI && odev.sonTarih > simdi

            if (zamanlar.isEmpty() || !uygun) {
                BildirimYonetici.yaklasaniIptal(context, odev.id)
            }

            if (uygun) {
                zamanlar.filter { it > simdi }.forEach { zaman ->
                    yonetici.enqueueUniqueWork(
                        IS_AD_ON_EK + odev.id + "-" + zaman,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<HatirlatmaWorker>()
                            .setInitialDelay(zaman - simdi, TimeUnit.MILLISECONDS)
                            .setInputData(workDataOf(VERI_ODEV_ID to odev.id))
                            .addTag(ETIKET)
                            .build(),
                    )
                    planlanan++
                }
            }

            // Zamanı beklemiyorsa (tüm plan anları geçti) mevcut bildirime
            // dokunulmaz: o hâlâ doğru tarihi söyler.
        }

        Log.d(
            TAG,
            "Hatırlatma eşitlendi: $planlanan iş / ${odevler.size} ödev, seçili=$secimler",
        )
    }
}

// ---- Zaman hesabı ----

/** Hatırlatma bu saatten önce gitmez; gece yarısı bildirimi kullanıcıyı uyandırır. */
private const val HATIRLATMA_SAATI = 8

private const val SAAT_MS = 60L * 60L * 1000L

/**
 * Hatırlatmanın gönderileceği anı hesaplar.
 *
 * Kural iki aşamalıdır:
 *
 *  1. Seçilen aralık teslim anına uygulanır (`sonTarih − gecikme`).
 *  2. Sonuç, [HATIRLATMA_SAATI]'nin altına düşüyorsa yukarı çekilir. Aksi
 *     hâlde "1 gün önce" gibi bir tercih, gece yarısını biraz aşan teslimlerde
 *     23.59 civarında; sabah teslimlerinde ise 04.00 gibi bir anda bildirim
 *     gönderirdi.
 *
 * Bulunan an teslim anına eşit ya da ondan sonra ise `null` döner: teslimden
 * sonraki hatırlatmanın anlamı yoktur, o boşluğu zaten "gecikti" bildirimi
 * üstlenir.
 *
 * Geçmişte kalan bir an burada **ele alınmaz** — "bu an çoktan geçti mi"
 * sorusunun yanıtı planlamanın anına bağlıdır ve [HatirlatmaZamanlayici.eslestir]
 * içindedir. Böylece bu fonksiyon hem geçmiş hem gelecek teslimlerde
 * deterministik sonuç verir ve cihaz olmadan test edilebilir.
 *
 * @param sonTarih Teslim anı (epoch millis, yerel saat diliminde).
 * @param aralik   Ayarlardan seçilen hatırlatma aralığı.
 * @return Hatırlatma anı; gönderilmeyecekse `null`.
 */
internal fun hatirlatmaZamani(sonTarih: Long, aralik: HatirlatmaAraligi): Long? =
    when (aralik) {
        HatirlatmaAraligi.UC_SAAT -> gecikmeliZaman(sonTarih, 3 * SAAT_MS)
        HatirlatmaAraligi.BIR_GUN -> gecikmeliZaman(sonTarih, 24 * SAAT_MS)
        HatirlatmaAraligi.SABAH -> gecerliZaman(sonTarih.yerelTarih().sabahZamani(), sonTarih)
    }

/**
 * Seçilen **tüm** aralıkların hatırlatma anlarını, sıraya dizilmiş hâlde verir.
 *
 * Sonuç iki yönden hazırlanır:
 *
 *  - **Sıralı** → en erken hatırlatma en küçük gecikmeyle kurulur; planda
 *    okunabilir bir sıra oluşur.
 *  - **Tekrarsız** → iki aralık aynı ana düşebilir (teslim 10.00 ise hem
 *    "3 saat önce" hem "teslim günü sabahı" 08.00'dedir). O hâlde tek iş
 *    kurulur, aynı anda iki kez tetiklenen gereksiz bir iş bırakılmaz.
 *
 * Boş küme boş liste verir; hatırlatmanın "kapalı" olması tam olarak budur.
 *
 * @param sonTarih Teslim anı (epoch millis, yerel saat diliminde).
 * @param secimler Kullanıcının işaretlediği aralıklar.
 */
internal fun hatirlatmaZamanlari(
    sonTarih: Long,
    secimler: Set<HatirlatmaAraligi>,
): List<Long> =
    secimler
        .mapNotNull { aralik -> hatirlatmaZamani(sonTarih, aralik) }
        .distinct()
        .sorted()

/** Teslimden `gecikme` kadar önceki anı, sabah saatinin altına düşmeyecek şekilde verir. */
private fun gecikmeliZaman(sonTarih: Long, gecikme: Long): Long? {
    val aday = sonTarih - gecikme
    val sabah = aday.yerelTarih().sabahZamani()
    return gecerliZaman(maxOf(aday, sabah), sonTarih)
}

/** Verilen günün [HATIRLATMA_SAATI]:00 anını üretir. */
private fun LocalDate.sabahZamani(): Long =
    tarihSaatiniDonustur(this, LocalTime.of(HATIRLATMA_SAATI, 0))

/** Aday teslimden önce değilse gönderilecek an yoktur. */
private fun gecerliZaman(aday: Long, sonTarih: Long): Long? =
    if (aday >= sonTarih) null else aday
