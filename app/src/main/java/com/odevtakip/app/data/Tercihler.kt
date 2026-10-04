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
 * **Seçim tektir değil kümedir**: kullanıcı "1 gün önce" ile "teslim günü
 * sabahı"nı aynı anda seçebilir, her biri ayrı bir bildirim gönderir. Bu yüzden
 * "Kapalı" adlı bir seçenek yoktur — hiçbir aralık işaretlenmemesi kapalı
 * anlamına gelir ve arayüz bunu ayrıca söyler. [HatirlatmaAraligi] böylece
 * *mümkün aralıkların* listesi olur, tek bir tercihin değil.
 *
 * Varsayılan [UC_SAAT]: ödevlerin çoğu akşam teslim edildiği için günün içinde
 * anlamlı bir an — günün son saatlerinde ya da okul öncesinde değil.
 */
enum class HatirlatmaAraligi(
    @StringRes val etiketRes: Int,
    @StringRes val aciklamaRes: Int,
) {
    /** Teslimden 3 saat önce. */
    UC_SAAT(R.string.hatirlatma_uc_saat, R.string.hatirlatma_uc_saat_aciklama),

    /** Teslim gününün sabahı, okuldan önce. */
    SABAH(R.string.hatirlatma_sabah, R.string.hatirlatma_sabah_aciklama),

    /** Teslimden bir gün önce, aynı saatte. */
    BIR_GUN(R.string.hatirlatma_bir_gun, R.string.hatirlatma_bir_gun_aciklama);

    companion object {

        /** Dizi adlarının birleştiği ayraç — depodaki tek biçim. */
        private const val AYRAC = ","

        /**
         * Yeni kurulumda işaretli gelen aralık.
         *
         * Kullanıcının hiçbir şey yapmadığı anda hatırlatma çalışsın diye
         * seçilidir; "hiçbiri seçilmedi" hâline yalnızca bilinçli olarak
         * getirilir.
         */
        fun varsayilanSecimler(): Set<HatirlatmaAraligi> = setOf(UC_SAAT)

        /**
         * Depodaki metni **seçim kümesine** çözer.
         *
         * Biçim virgülle ayrılmış addır (`"UC_SAAT,BIR_GUN"`); tek seçenekli
         * eski sürümün yazdığı değer (`"UC_SAAT"` ya da `"KAPALI"`) bu biçime
         * kendiliğinden uyar. Bilinmeyen ad sessizce atlanır:
         *
         *  - `null` (anahtar hiç yok) → [varsayilanSecimler], yani yeni kurulum
         *    hatırlatmayla başlar.
         *  - `""` (kullanıcı her şeyin işaretini kaldırdı) → boş küme, yani
         *    hatırlatma kapalı.
         *  - eski `"KAPALI"` → adı artık listede olmadığı için boş küme; o
         *    kullanıcının zaten "kapalı" seçmiş olmasıyla aynı sonucu verir.
         *
         * @param metin Depodaki ham metin; anahtar yoksa `null`.
         */
        fun secimleriCozumle(metin: String?): Set<HatirlatmaAraligi> =
            if (metin == null) {
                varsayilanSecimler()
            } else {
                metin.split(AYRAC)
                    .mapNotNull { ad -> entries.firstOrNull { it.name == ad.trim() } }
                    .toSet()
            }

        /**
         * Seçim kümesini depoya yazılacak tek metne çevirir.
         *
         * Sıra dizi sırasıdır, kullanıcının tıklama sırası değil: aynı küme
         * her zaman aynı metni üretir, gereksiz yazma tetiklenmez.
         */
        fun secimleriYaz(secimler: Set<HatirlatmaAraligi>): String =
            entries.filter { it in secimler }.joinToString(AYRAC) { it.name }
    }
}

/**
 * Kalıcı kullanıcı tercihleri (SharedPreferences).
 *
 * Neden DataStore değil? Üç küçük tercih var (tema, bayrak, aralık kümesi);
 * `apply()` disk yazısını arka plana atar, okuma ise açılışta bir kez yapılır.
 * Bu ölçekte akış kütüphanesi taşımaya değmez.
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
     * Seçili teslim öncesi hatırlatma aralıkları (**küme** — birden fazla
     * olabilir; boş küme hatırlatmanın kapalı olduğu anlamına gelir).
     *
     * Akış olması burada kritik: değer değiştiğinde
     * [com.odevtakip.app.OdevTakipApplication] bunu izleyip tüm hatırlatmaları
     * yeniden planlar — ekranın ayrıca bir şey yapması gerekmez.
     */
    private val _hatirlatma = MutableStateFlow(
        HatirlatmaAraligi.secimleriCozumle(depo.getString(ANAHTAR_HATIRLATMA, null))
    )
    val hatirlatma: StateFlow<Set<HatirlatmaAraligi>> = _hatirlatma.asStateFlow()

    fun temaAyarla(secenek: TemaSecenegi) {
        _tema.value = secenek
        depo.edit().putString(ANAHTAR_TEMA, secenek.name).apply()
    }

    fun tamamlananlariGizleAyarla(deger: Boolean) {
        _tamamlananlariGizle.value = deger
        depo.edit().putBoolean(ANAHTAR_GIZLE, deger).apply()
    }

    fun hatirlatmaAyarla(secimler: Set<HatirlatmaAraligi>) {
        _hatirlatma.value = secimler
        depo.edit().putString(ANAHTAR_HATIRLATMA, HatirlatmaAraligi.secimleriYaz(secimler)).apply()
    }

    private companion object {
        const val DOSYA_ADI = "tercihler"
        const val ANAHTAR_TEMA = "tema"
        const val ANAHTAR_GIZLE = "tamamlananlari_gizle"
        const val ANAHTAR_HATIRLATMA = "hatirlatma"
    }
}
