package com.odevtakip.app.ui

import androidx.annotation.StringRes
import com.odevtakip.app.R

/**
 * Ana listedeki sıralama seçenekleri.
 *
 * Sıralama **yalnızca ödevler sekmesini** etkiler. Takvim bir güne,
 * istatistik ise tüm kayıtlara baktığı için onların kendi sabit sıraları
 * vardır; kullanıcı bir sekmede yaptığı seçim diğerini yanıltıcı biçimde
 * değiştirmemelidir.
 *
 * Her seçeneğin kısa bir etiketi ve altında ne yaptığı anlatan ikinci bir
 * satırı vardır — panelde tek başına "Ada göre" yazması, sıra bitince
 * neden değiştiğini açık bırakırdı.
 *
 * @property etiketRes Seçenek satırının başlığı.
 * @property aciklamaRes Başlığın altındaki açıklama satırı.
 */
enum class OdevSiralamasi(
    @StringRes val etiketRes: Int,
    @StringRes val aciklamaRes: Int,
) {

    /**
     * Uygulamanın varsayılanı: bekleyenler teslim tarihine göre en yakın
     * olan üstte, tamamlananlar onların altında en son teslim edilen
     * üstte. Liste iş bitince kendiliğinden "temizlenir".
     */
    ONERILEN(R.string.siralama_onerilen, R.string.siralama_onerilen_aciklama),

    /** Aynı dersin ödevleri alt alta toplanır; aralarındaki sıra korunur. */
    DERS_ADI(R.string.siralama_ders_adi, R.string.siralama_ders_adi_aciklama),

    /** Başlıklara göre alfabetik. Bir işi "M" ile ararken. */
    ADA_GORE(R.string.siralama_baslik, R.string.siralama_baslik_aciklama),

    /** En son eklenen ödev en üstte; ne eklediğini unutana. */
    YENI_EKLENEN(R.string.siralama_yeni, R.string.siralama_yeni_aciklama),
}
