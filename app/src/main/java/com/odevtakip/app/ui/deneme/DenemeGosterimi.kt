package com.odevtakip.app.ui.deneme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odevtakip.app.R
import com.odevtakip.app.data.DenemeTuru
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Deneme ekranlarında sayı gösterimi için tek kaynak.
 *
 * Net **ondalık bir değerdir** ve arayüz dili Türkçe olduğu için biçim de
 * Türkçe olmalıdır: `42.5` yerine `42,5` yazılır. Java'nın varsayılan
 * `Double.toString`ı nokta üretir — metinler doğrudan stringResource'tan
 * gelmediği için oradan da düzeltilmez, tek burada düzeltilir.
 *
 * Kalıb `0.##`dır: en fazla iki ondalık basamak yeterlidir çünkü net
 * çeyrek adımlarla üretilir (`.25`, `.5`, `.75`). Gereksiz sıfır da
 * yazmaz — `42` "42,00" olmaz.
 */
private val sayiBicimi = DecimalFormat(
    "0.##",
    DecimalFormatSymbols(Locale.forLanguageTag("tr")),
)

/** `42.0` → `"42"`, `42.5` → `"42,5"`, `42.25` → `"42,25"`. */
fun sayiMetni(deger: Double): String = sayiBicimi.format(deger)

// ---- Form girdisi çözümleme ----

/**
 * Formdaki doğru/yanlış metnini sayuya çevirir.
 *
 *  - Boş metin **`0`**dır: kullanıcı cevap sayısını yazmadıysa o derste
 *    soru yoktur; ayrı bir "zorunlu alan" hatası, kaydı gereksiz yere
 *    geri çevirirdi.
 *  - Sayısal olmayan metin `null` döner — çağıran ekran hata gösterir.
 *  - **Negatif değer kabul edilmez** (`null`): sayı klavyesi `-` yazmaz
 *    ama panodan yapıştırma yoluyla gelebilir.
 */
fun tamSayiCoz(metin: String): Int? {
    val temiz = kirpilmis(metin) ?: return 0
    val sayi = temiz.toIntOrNull() ?: return null
    return if (sayi < 0) null else sayi
}

/** Metni kırpıp boşsa `null` verir; çözümleyicinin ön adımı. */
private fun kirpilmis(metin: String): String? =
    metin.trim().takeIf { it.isNotEmpty() }

// ---- Tür etiketi ----

/**
 * Denemenin türünü kısa etiketine çevirir: `"TYT"` / `"AYT"`.
 *
 * Metinler kaynaktan değil kaynak dosyadan gelir; [DenemeTuru]'nun adı
 * ise yalnızca veritabanındaki anahtardır. Kart, detay ve form aynı
 * işlevi paylaştığı için iki ekranda aynı söz asla yazılmaz.
 */
@Composable
fun turEtiketi(tur: DenemeTuru): String = when (tur) {
    DenemeTuru.TYT -> stringResource(R.string.deneme_tur_tyt)
    DenemeTuru.AYT -> stringResource(R.string.deneme_tur_ayt)
}
