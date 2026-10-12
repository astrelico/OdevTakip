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
