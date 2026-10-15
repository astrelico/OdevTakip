package com.odevtakip.app.ui

import androidx.annotation.StringRes
import com.odevtakip.app.R
import com.odevtakip.app.data.Deneme

/**
 * Deneme listesindeki sıralama seçenekleri.
 *
 * Sıralama **yalnızca deneme listesini** etkiler; ana liste ve takvim
 * kendi sabit sıralarındadır — kullanıcı bir ekranda yaptığı seçim
 * diğerini yanıltıcı biçimde değiştirmemelidir.
 *
 * Seçim varsayılan olarak [TARIH_YENI]dir: bu, DAO'nun kendi sırasıdır
 * (`tarih DESC, id DESC`) — yani sıralamaya hiç dokunmadan açılan ekran
 * bugüne kadar hep gördüğü sırayı gösterir.
 *
 * Her seçeneğin kısa bir etiketi ve altında ne yaptığı anlatan ikinci bir
 * satırı vardır; panelde tek başına "Nete göre" yazması, sıra bitince
 * neden değiştiğini açık bırakırdı.
 *
 * @property etiketRes Seçenek satırının başlığı.
 * @property aciklamaRes Başlığın altındaki açıklama satırı.
 */
enum class DenemeSiralamasi(
    @StringRes val etiketRes: Int,
    @StringRes val aciklamaRes: Int,
) {

    /** Varsayılan: sınav tarihi azalan, eşitse id azalan — DAO sırası. */
    TARIH_YENI(
        R.string.deneme_siralama_tarih_yeni,
        R.string.deneme_siralama_tarih_yeni_aciklama,
    ),

    /** Tarihsel okuma: ilk yapılan deneme en üstte. */
    TARIH_ESKI(
        R.string.deneme_siralama_tarih_eski,
        R.string.deneme_siralama_tarih_eski_aciklama,
    ),

    /** Kayıt sırası: hangi deneme önce girildi? Sınav tarihinden bağımsız. */
    EKLENEN_YENI(
        R.string.deneme_siralama_eklenen,
        R.string.deneme_siralama_eklenen_aciklama,
    ),

    /** Ada göre alfabetik. Bir denemeyi adıyla ararken. */
    AD_A_Z(
        R.string.deneme_siralama_ad_az,
        R.string.deneme_siralama_ad_az_aciklama,
    ),

    /** Alfabetik ters: "en sona yazdığım" üstte dursun diye. */
    AD_Z_A(
        R.string.deneme_siralama_ad_za,
        R.string.deneme_siralama_ad_za_aciklama,
    ),

    /** En iyi sonuç en üstte — başarı sıralaması. */
    NET_DUSEN(
        R.string.deneme_siralama_net_dusen,
        R.string.deneme_siralama_net_dusen_aciklama,
    ),

    /** En düşük net en üstte — zayıf sonuçları döküm sırası. */
    NET_ARTAN(
        R.string.deneme_siralama_net_artan,
        R.string.deneme_siralama_net_artan_aciklama,
    ),
}

/**
 * Denemeleri gösterim sırasına dizer.
 *
 * Gelen liste DAO'nun kendi sırasındadır (`tarih DESC, id DESC`) ve bu
 * **taban dizilimdir**: Kotlin'in sıralaması kararlıdır, eşit anahtardaki
 * kayıtlar taban sıraya geri döner. Yani "Adına göre" seçildiğinde aynı
 * adı taşıyan iki denemenin iç sırası bozulmaz — tarih azalan düzeni
 * korunur.
 *
 * [TARIH_YENI] ayrıca **elle** kurulur (`tarih` azalan, eşitse `id`
 * azalan): akışın gelen sıraya göz yummasına güvenmek yerine söz
 * kendisinde tutulur ve sonuç, gelen liste nasıl sıralı olursa olsun
 * aynıdır.
 *
 * @param siralama Gösterim sırası; varsayılan [DenemeSiralamasi.TARIH_YENI].
 */
internal fun List<Deneme>.denemeleriSirala(
    siralama: DenemeSiralamasi = DenemeSiralamasi.TARIH_YENI,
): List<Deneme> =
    when (siralama) {
        DenemeSiralamasi.TARIH_YENI ->
            sortedWith(
                compareByDescending<Deneme> { it.tarih }
                    .thenByDescending { it.id },
            )

        DenemeSiralamasi.TARIH_ESKI ->
            sortedWith(
                compareBy<Deneme> { it.tarih }
                    .thenBy { it.id },
            )

        DenemeSiralamasi.EKLENEN_YENI -> sortedByDescending { it.olusturmaTarihi }
        DenemeSiralamasi.AD_A_Z -> sortedBy { it.ad.siralamaAnahtari() }
        DenemeSiralamasi.AD_Z_A -> sortedByDescending { it.ad.siralamaAnahtari() }
        DenemeSiralamasi.NET_DUSEN -> sortedByDescending { it.net }
        DenemeSiralamasi.NET_ARTAN -> sortedBy { it.net }
    }

/**
 * Alfabetik anahtar — küçük harfe indirgenmiş ad.
 *
 * Karar, ödev listesindeki `String.anahtar()` ile **aynıdır** (bkz.
 * [OdevViewModel]): locale'den bağımsız küçük harf, yani `MATEMATİK`
 * `matematik`e iner. İki yerde iki farklı kural olsaydı ödevde "B'den
 * Z'ye" derken denemede tersi çıkabilirdi.
 */
private fun String.siralamaAnahtari(): String = lowercase()
