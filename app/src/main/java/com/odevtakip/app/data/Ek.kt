package com.odevtakip.app.data

import java.util.Locale

/**
 * Ek dosyalarının (fotoğraf, PDF, not…) **saf** kuralları.
 *
 * Bu dosyada hiçbir Android sınıfına dokunulmaz: uzantıdan tür çıkarmak,
 * dosya adını güvenli hâle getirmek ve boyutu okunur birime indirmek tamamen
 * Kotlin'dir. Böylece kurallar tek başlarına test edilebilir (`EkTest.kt`);
 * diske dokunan her şey [EkDeposu]'nda kalır.
 */

/** Adı okunamayan ek için dosya adının gövdesi. */
const val EK_VARSAYILAN_ADI = "dosya"

/** Uzantı → MIME eşlemesi. Eşleşmeyen uzantı `null` döner. */
private val UZANTILAR = mapOf(
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "gif" to "image/gif",
    "webp" to "image/webp",
    "bmp" to "image/bmp",
    "heic" to "image/heic",
    "heif" to "image/heif",
    "svg" to "image/svg+xml",
    "pdf" to "application/pdf",
    "txt" to "text/plain",
    "csv" to "text/csv",
    "doc" to "application/msword",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "zip" to "application/zip",
    "rar" to "application/vnd.rar",
    "mp3" to "audio/mpeg",
    "m4a" to "audio/mp4",
)

/** Dosya sisteminde kabul edilebilir en uzun ek adı (karakter). */
private const val EN_UZUN_ADI = 120

/** Uzantıyı adın sonundan ayırır; yoksa ya da yalnızca noktaysa `null`. */
private fun uzanti(ad: String): String? {
    val nokta = ad.lastIndexOf('.')
    if (nokta <= 0 || nokta == ad.length - 1) return null
    return ad.substring(nokta + 1)
}

/**
 * Dosyanın MIME türünü uzantısından çıkarır.
 *
 * Uzantı bilinmiyorsa `null`: arayüz bunu **görsel değil, genel dosya**
 * olarak gösterir. Tersi tehlikeli olurdu — bilinmeyen bir uzantıyı "görsel"
 * sanıp bozuk bir önizleme çizmek, dosya kartı çizmekten kötüdür.
 */
fun ekMimeTipi(ad: String): String? =
    uzanti(ad)?.lowercase(Locale.ROOT)?.let { UZANTILAR[it] }

/** Dosya önizlenebilir bir görselse `true`. */
fun ekGorselMi(ad: String): Boolean =
    ekMimeTipi(ad)?.startsWith("image/") == true

/**
 * Gösterilecek tür adı: `odev.pdf` → `PDF`.
 *
 * Uzantı yoksa `null`; arayüz burada kendi metnini (örn. "Dosya") kullanır.
 * `Locale.ROOT` şart: Türkçe yerleşimde `uppercase` harfleri değiştirir ve
 * "heic" → "HEİC" gibi yanlış etiketler doğardı.
 */
fun ekTuru(ad: String): String? = uzanti(ad)?.uppercase(Locale.ROOT)

/**
 * Dosya adını dosya sistemi için güvenli hâle getirir.
 *
 * Gelen ad seçiciden gelir: yol ayracı, noktalama ya da hiç uzantı
 * içerebilir. Kural üç adımdır:
 *
 *  1. Son bölüm alınır (verilen tam yol olsa bile).
 *  2. Harf/rakamdaki her karakter **korunur** (Türkçe harfler dâhil — dosya
 *     sistemi UTF-8 olduğu için sorun çıkarmaz), gerisi `_` olur.
 *  3. Uzantı korunarak gövde kırpılır: aksi hâlde uzun bir adın sonundaki
 *     `.pdf` kesilir ve tür tespiti bozulurdu.
 *
 * Sonuç boşsa (yalnızca nokta/ayraç gibi) [EK_VARSAYILAN_ADI] yazılır.
 */
fun ekAdiniTemizle(ad: String): String {
    val sonBolum = ad.substringAfterLast('/').substringAfterLast('\\')

    val temiz = buildString(sonBolum.length) {
        sonBolum.forEach { c ->
            append(if (c.isLetterOrDigit() || c == '.' || c == '-' || c == '_') c else '_')
        }
    }

    val nokta = temiz.lastIndexOf('.')
    val govde = if (nokta > 0) temiz.substring(0, nokta) else temiz
    val uzantisi = if (nokta > 0) temiz.substring(nokta) else ""

    val kisaGovde = govde
        .take((EN_UZUN_ADI - uzantisi.length).coerceAtLeast(0))
        .trimEnd('_')

    return (kisaGovde + uzantisi).trim('.', '_', '-').ifBlank { EK_VARSAYILAN_ADI }
}

/**
 * [ad]ı, [mevcutMu] sorgusu hiç `true` demeyene kadar çoğaltır.
 *
 * İki ödevin de "IMG_0001.jpg"i olabilir: ikincisi "IMG_0001 (1).jpg" alır.
 * Çakışma çözümü saf olduğundan dosya sistemi olmadan da test edilir.
 */
fun ekBenzersizAd(ad: String, mevcutMu: (String) -> Boolean): String {
    if (!mevcutMu(ad)) return ad

    val nokta = ad.lastIndexOf('.')
    val govde = if (nokta > 0) ad.substring(0, nokta) else ad
    val uzantisi = if (nokta > 0) ad.substring(nokta) else ""

    var sira = 1
    while (mevcutMu("$govde ($sira)$uzantisi")) sira++
    return "$govde ($sira)$uzantisi"
}

/**
 * Bayt cinsinden boyutu okunur birime indirir: `512 B`, `1,5 KB`, `2,4 MB`.
 *
 * İki ayrıntı önemli:
 *
 *  - Bölme `Double` ile yapılır ama ondalık ayracı **elle** yazılır: sayı
 *    biçimleyici cihaz diline göre nokta ya da virgül seçerdi ve arayüzün
 *    dili sabit Türkçe.
 *  - Yuvarlama `Math.round` ile yapılır; kayan nokta hatası "2,39"u
 *    "2,3"e çekip okunabilirliği bozmasın.
 */
fun boyutMetni(bayt: Long): String {
    val birimler = arrayOf("B", "KB", "MB", "GB", "TB")

    var deger = bayt.coerceAtLeast(0).toDouble()
    var birim = 0
    while (deger >= 1024.0 && birim < birimler.lastIndex) {
        deger /= 1024.0
        birim++
    }

    val yuvarlanmis = Math.round(deger * 10.0)
    val tam = yuvarlanmis / 10
    val kesir = yuvarlanmis % 10
    return if (kesir == 0L) "$tam ${birimler[birim]}" else "$tam,$kesir ${birimler[birim]}"
}
