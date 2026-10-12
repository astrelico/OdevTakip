package com.odevtakip.app.ui.deneme

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Deneme ekranlarında sayı gösterimi için tek kaynak.
 *
 * Net ve puan **ondalık değerlerdir** ve arayüz dili Türkçe olduğu için
 * biçim de Türkçe olmalıdır: `42.5` yerine `42,5` yazılır. Java'nın
 * varsayılan `Double.toString`ı nokta üretir — metinler doğrudan
 * stringResource'tan gelmediği için oradan da düzeltilmez, tek burada
 * düzeltilir.
 *
 * Kalıb `0.##`dır: en fazla iki ondalık basamak yeterlidir çünkü net
 * çeyrek adımlarla üretilir (`.25`, `.5`, `.75`) ve puan kullanıcı
 * eliyle en fazla iki basamakla yazılır. Gereksiz sıfır da yazmaz —
 * `42` "42,00" olmaz.
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

/**
 * Formdaki puan metnini sayuya çevirir.
 *
 *  - **Boş metin `null`** döner ve "puan girilmedi" demektir — bu, hata
 *    değildir; puan isteğe bağlıdır.
 *  - Virgül ve nokta **ikisi de kabul edilir**: bazı Türkçe klavyeler
 *    ondalık ayracı olarak nokta da gösterir. Puan 100'ü geçmediği için
 *    binlik ayracının karışması mümkün değildir (`1.234` ≈ `1.234`).
 *  - Negatif puan kabul edilmez.
 */
fun ondalikCoz(metin: String): Double? {
    val temiz = kirpilmis(metin) ?: return null
    val sayi = temiz.replace(',', '.').toDoubleOrNull() ?: return null
    return if (sayi < 0) null else sayi
}

/** Metni kırpıp boşsa `null` verir; çözümleyicilerin ortak ön adımı. */
private fun kirpilmis(metin: String): String? =
    metin.trim().takeIf { it.isNotEmpty() }
