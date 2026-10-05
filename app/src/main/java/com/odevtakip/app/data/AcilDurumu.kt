package com.odevtakip.app.data

import com.odevtakip.app.util.yerelTarih

/**
 * Ödevin teslimine ne kaldığını betimleyen **urgans** (aciliyet) rozeti.
 *
 * Bu, [Durum]'un ayrı bir boyutudur:
 *
 *  - [Durum] "ne oldu?" sorusunu yanıtlar (bekliyor / gecikti / tamamlandı)
 *    ve veritabanında saklanır.
 *  - [AcilDurumu] "ne kadar vakti kaldı?" sorusunu yanıtlar; **saklanmaz**,
 *    çizim anında hesaplanır — yarın olduğunda kart kendiliğinden "Acil"
 *    olur, hiçbir kayıt güncellenmez.
 *
 * Kural yalnızca iki günü kapsar, kullanıcı seçimiyledir:
 *
 *  - Teslim tarihi **bugün** ve hâlâ tamamlanmamış → [BUGUN]
 *  - Teslim tarihi **yarın** ve hâlâ tamamlanmamış → [YARIN]
 *  - Gecikenler ayrıca [YOK] sayılır: onların zaten "Gecikti" rozeti ve
 *    kırmızı tarih metni var, iki uyarı üst üste binmesin.
 */
enum class AcilDurumu {

    /** Acil değil (teslimine iki günden fazla var, geçti ya da bitmiş). */
    YOK,

    /** Teslim tarihi yarın ve henüz tamamlanmadı. */
    YARIN,

    /** Teslim günü ve henüz tamamlanmadı — en kritik durum. */
    BUGUN;

    /** Kart kırmızı çerçeve + rozet alsın mı? */
    val acilMi: Boolean get() = this != YOK

    /** Bugün teslim: çerçeve ve rozet daha da vurgulu olur. */
    val vurgulu: Boolean get() = this == BUGUN
}

/**
 * Ödevin aciliyetini hesaplar.
 *
 * Gün sınırı takvim gününe göre çizilir, saate göre değil: teslim saati
 * geçmiş olsa bile **hâlâ bugünün içindeyse** ödev Acil'dir — teslim günü
 * bitmeden iş bitmedi demektir.
 *
 * @param simdi Karşılaştırılacak an. [Odev.gercekDurum] ile aynı parametre,
 *   böylece ekran tek bir "şimdi" değeri üzerinden hem durumu hem aciliyeti
 *   aynı anda çizer.
 */
internal fun Odev.acilDurumu(simdi: Long): AcilDurumu {
    // Tamamlanmış iş tesliminden bağımsız olarak acil değildir.
    if (gercekDurum(simdi) == Durum.TAMAMLANDI) return AcilDurumu.YOK

    val teslim = sonTarih.yerelTarih()
    val bugun = simdi.yerelTarih()
    return when (teslim) {
        bugun -> AcilDurumu.BUGUN
        bugun.plusDays(1) -> AcilDurumu.YARIN
        else -> AcilDurumu.YOK
    }
}
