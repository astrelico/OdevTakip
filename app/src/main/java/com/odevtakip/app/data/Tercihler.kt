package com.odevtakip.app.data

import android.content.Context
import androidx.annotation.StringRes
import com.odevtakip.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tema seçeneği.
 *
 * Üç seçenek de bilinçlidir: yalnızca "açık/koyu" olsaydı kullanıcı sistem
 * temasını değiştirdiğinde uygulama onu takip edemezdi. [SISTEM] bu yüzden
 * varsayılandır.
 */
enum class TemaSecenegi(
    @StringRes val etiketRes: Int,
    @StringRes val aciklamaRes: Int,
) {
    /** Cihazın sistem koyu tema tercihini izler. */
    SISTEM(R.string.tema_sistem, R.string.tema_sistem_aciklama),

    /** Her koşulda açık tema. */
    ACIK(R.string.tema_acik, R.string.tema_acik_aciklama),

    /** Her koşulda koyu tema. */
    KOYU(R.string.tema_koyu, R.string.tema_koyu_aciklama);

    /**
     * Bu seçenek altında uygulama koyu temada mı görünmeli?
     *
     * Tek saf karar noktası: [com.odevtakip.app.ui.theme.OdevTakipTheme] hem
     * ayarlar ekranı hem açılış için aynı fonksiyonu çağırır, böylece seçilen
     * seçenek ile ekrana gelen tema hiçbir yerde ayrı ayrı yorumlanmaz.
     *
     * @param sistemKoyu Cihazın sistem koyu tema tercihi.
     */
    fun koyuTemayaDonusur(sistemKoyu: Boolean): Boolean = when (this) {
        SISTEM -> sistemKoyu
        ACIK -> false
        KOYU -> true
    }

    companion object {
        /**
         * Depodaki metni çözer.
         *
         * Bilinmeyen ya da eksik değer (ör. uygulama güncellenince anahtarın
         * değişmesi) sessizce varsayılana döner — uygulama açılamaz hâle gelmez.
         */
        fun guvenliDeger(metin: String?): TemaSecenegi =
            entries.firstOrNull { it.name == metin } ?: SISTEM
    }
}

/**
 * Teslim öncesi hatırlatma aralığı.
 *
 * Her seçenek teslim anına göre farklı bir "önceden" tanımladığı için sabit bir
 * milisaniye listesi yerine semantik adlar kullanılır; hangi ana hangi an
 * karşılık geldiğini tek noktada [com.odevtakip.app.work.HatirlatmaZamanlayici]
 * söyler.
 *
 * Varsayılan [UC_SAAT]: ödevlerin çoğu akşam teslim edildiği için günün içinde
 * anlamlı bir an — günün son saatlerinde ya da okul öncesinde değil.
 */
enum class HatirlatmaAraligi(
    @StringRes val etiketRes: Int,
    @StringRes val aciklamaRes: Int,
) {
    /** Sadece mevcut davranış: süre geçince bildirim. */
    KAPALI(R.string.hatirlatma_kapali, R.string.hatirlatma_kapali_aciklama),

    /** Teslimden 3 saat önce. */
    UC_SAAT(R.string.hatirlatma_uc_saat, R.string.hatirlatma_uc_saat_aciklama),

    /** Teslim gününün sabahı, okuldan önce. */
    SABAH(R.string.hatirlatma_sabah, R.string.hatirlatma_sabah_aciklama),

    /** Teslimden bir gün önce, aynı saatte. */
    BIR_GUN(R.string.hatirlatma_bir_gun, R.string.hatirlatma_bir_gun_aciklama);

    companion object {
        /**
         * Depodaki metni çözer; bilinmeyen değer sessizce varsayılana döner.
         *
         * Aynı caydırma [TemaSecenegi.guvenliDeger] içindedir: güncellemede
         * anahtar değişse bile uygulama hatırlatmasız kalmaz, sessizce varsayılana geçer.
         */
        fun guvenliDeger(metin: String?): HatirlatmaAraligi =
            entries.firstOrNull { it.name == metin } ?: UC_SAAT
    }
}

/**
 * Kalıcı kullanıcı tercihleri (SharedPreferences).
 *
 * Neden DataStore değil? Tek bir seçenek + tek bir bayrak var; `apply()`
 * disk yazısını arka plana atar, okuma ise açılışta bir kez yapılır. Bu
 * ölçekte akış kütüphanesi taşımaya değmez.
 *
 * Değerler **StateFlow** olarak tutulur: ekranlar yazmayı beklemeden anında
 * yeni değeri görür, tema değişimi composition'ı kendiliğinden yeniler.
 *
 * Örnek: [com.odevtakip.app.ui.ayarlar.AyarlarViewModel],
 * [com.odevtakip.app.ui.OdevViewModel].
 */
class Tercihler(context: Context) {

    private val depo = context.getSharedPreferences(DOSYA_ADI, Context.MODE_PRIVATE)

    private val _tema = MutableStateFlow(TemaSecenegi.guvenliDeger(depo.getString(ANAHTAR_TEMA, null)))
    val tema: StateFlow<TemaSecenegi> = _tema.asStateFlow()

    private val _tamamlananlariGizle = MutableStateFlow(depo.getBoolean(ANAHTAR_GIZLE, false))
    val tamamlananlariGizle: StateFlow<Boolean> = _tamamlananlariGizle.asStateFlow()

    /**
     * Teslim öncesi hatırlatma aralığı.
     *
     * Akış olması burada kritik: değer değiştiğinde
     * [com.odevtakip.app.OdevTakipApplication] bunu izleyip tüm hatırlatmaları
     * yeniden planlar — ekranın ayrıca bir şey yapması gerekmez.
     */
    private val _hatirlatma = MutableStateFlow(
        HatirlatmaAraligi.guvenliDeger(depo.getString(ANAHTAR_HATIRLATMA, null))
    )
    val hatirlatma: StateFlow<HatirlatmaAraligi> = _hatirlatma.asStateFlow()

    fun temaAyarla(secenek: TemaSecenegi) {
        _tema.value = secenek
        depo.edit().putString(ANAHTAR_TEMA, secenek.name).apply()
    }

    fun tamamlananlariGizleAyarla(deger: Boolean) {
        _tamamlananlariGizle.value = deger
        depo.edit().putBoolean(ANAHTAR_GIZLE, deger).apply()
    }

    fun hatirlatmaAyarla(secenek: HatirlatmaAraligi) {
        _hatirlatma.value = secenek
        depo.edit().putString(ANAHTAR_HATIRLATMA, secenek.name).apply()
    }

    private companion object {
        const val DOSYA_ADI = "tercihler"
        const val ANAHTAR_TEMA = "tema"
        const val ANAHTAR_GIZLE = "tamamlananlari_gizle"
        const val ANAHTAR_HATIRLATMA = "hatirlatma"
    }
}
