package com.odevtakip.app.ui

import androidx.annotation.StringRes
import com.odevtakip.app.R
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate

/**
 * Ana liste ekranındaki filtre çipleri.
 *
 * Her filtre, bir ödevi koşula göre seçip seçmeyeceğini [eslesir] ile söyler.
 * Filtreleme ViewModel'de yapılır; ekran yalnızca son listeyi çizer.
 */
enum class OdevFiltresi(@StringRes val etiketRes: Int) {

    /** Tüm ödevler, tamamlanmışlar en sonda. */
    TUMU(R.string.filtre_tumu),

    /** Tarihi geçmiş ve tamamlanmamış ödevler. */
    GECIKEN(R.string.filtre_geciken),

    /** Teslim tarihi bugüne denk gelen ödevler. */
    BUGUN(R.string.filtre_bugun),

    /** Önümüzdeki 7 gün içinde teslimi olan ödevler. */
    YAKLASAN(R.string.filtre_yaklasan);

    /**
     * [odev] bu filtreye giriyor mu?
     *
     * @param simdi Karşılaştırılacak an. Her çağrıda güncellenir; böylece
     *   uygulama açık kalırken filtreler bayatlamaz.
     */
    fun eslesir(odev: Odev, simdi: Long): Boolean {
        val durum = odev.gercekDurum(simdi)
        val tarih = odev.sonTarih.yerelTarih()
        val bugun = LocalDate.now()

        return when (this) {
            // Tamamlanmış ödev "gecikmiş" sayılmaz; tanıma aykırıdır.
            GECIKEN -> durum == Durum.GECEKTI
            BUGUN -> tarih == bugun
            YAKLASAN -> tarih.isAfter(bugun) && !tarih.isAfter(bugun.plusDays(GUN_SAYISI))
            TUMU -> true
        }
    }

    private companion object {
        /** "Yaklaşan" penceresi: bugünden itibaren kaç gün. */
        const val GUN_SAYISI = 7L
    }
}
