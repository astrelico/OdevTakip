package com.odevtakip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ödev Takip'in görsel teması — "Odak Mavisi".
 *
 * ### Renkler
 * Ana renk, ders/odak çağrıştıran **lacivert-mavi**. Yanında arka plana çok
 * az mavi bulaşan, gözün kolay yorulduğu uzun listeler için neredeyse nötr bir
 * yüzey kullanılıyor. Üçüncü renk (amber) yalnızca vurgu amaçlı.
 *
 * Malzeme renk **rolleri** üzerinden kurulduğu için ekranda hiçbir yerde sabit
 * renk yoktur: `com.odevtakip.app.ui.durumRengi` gibi yardımcılar doğrudan
 * `error` / `primary` / `secondary` rollerinden türer, bu yüzden yeni palet
 * geldiğinde durum rozetleri kendiliğinden uyum sağlar.
 *
 * Durum eşlemesi. Rozetler iki biçimde kullanılıyor: liste rozeti zeminini
 * %14 tonlar (metin rengi kalır), detay rozeti ise dolu zemin üzerine `surface`
 * metin koyar. Her iki biçimde de kontrast AA'nın çok üzerindedir
 * (yaklaşık ~8:1 ve ~6:1):
 *
 * | Durum        | Rol         | Açık tema | Koyu tema |
 * |--------------|-------------|-----------|-----------|
 * | Gecikti      | `error`     | `#BA1A1A` | `#FFB4AB` |
 * | Tamamlandı   | `primary`   | `#2E5AAC` | `#A8C7FA` |
 * | Bekliyor     | `secondary` | `#565F71` | `#BFC7D6` |
 *
 * `primary` aynı zamanda "tamamlandı" rengi olduğu için bilinçli olarak
 * **mavi** bırakıldı: kırmızı–yeşil körlüğünde yeşil/gri/kırmızı üçlüsü
 * ayırt edilemezken mavi/gri/kırmızı ayırt edilebilir.
 *
 * Hata kırmızısı ve gri tonları (outline/surfaceVariant/inverse*) Material
 * 3'ün varsayılan "baseline" değerleridir — bunlar zaten dile/temaya göre
 * optimize edilmiş, icat etmeye gerek yoktur.
 */

// ---- Açık tema ----

private val AcikRenkler = lightColorScheme(
    primary = Color(0xFF2E5AAC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE4FF),
    onPrimaryContainer = Color(0xFF001B3F),
    inversePrimary = Color(0xFFA8C7FA),

    secondary = Color(0xFF565F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F1),
    onSecondaryContainer = Color(0xFF131C2B),

    tertiary = Color(0xFF715B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE9A8),
    onTertiaryContainer = Color(0xFF221A00),

    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1A1C22),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474F),

    // Yüzey merdiveni: açık temada yükseltilmiş yüzeyler yüzeyin *karartır*.
    surfaceDim = Color(0xFFD8DAE8),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FD),
    surfaceContainer = Color(0xFFECEDF7),
    surfaceContainerHigh = Color(0xFFE7E8F1),
    surfaceContainerHighest = Color(0xFFE1E2EB),

    inverseSurface = Color(0xFF2F3138),
    inverseOnSurface = Color(0xFFF1F0F7),

    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Temadan bağımsız ("fixed") renkler Material 3'ün varsayılan mor
    // paletinden geliyor; bırakılsaydı mavimizin yanında mor sızardı.
    primaryFixed = Color(0xFFDBE4FF),
    primaryFixedDim = Color(0xFFA8C7FA),
    onPrimaryFixed = Color(0xFF001B3F),
    onPrimaryFixedVariant = Color(0xFF003078),
    secondaryFixed = Color(0xFFDAE2F1),
    secondaryFixedDim = Color(0xFFBFC7D6),
    onSecondaryFixed = Color(0xFF131C2B),
    onSecondaryFixedVariant = Color(0xFF3C4758),
    tertiaryFixed = Color(0xFFFFE9A8),
    tertiaryFixedDim = Color(0xFFE6C255),
    onTertiaryFixed = Color(0xFF221A00),
    onTertiaryFixedVariant = Color(0xFF574500),
)

// ---- Koyu tema ----

private val KoyuRenkler = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF003078),
    primaryContainer = Color(0xFF1B4B9E),
    onPrimaryContainer = Color(0xFFDBE4FF),
    inversePrimary = Color(0xFF2E5AAC),

    secondary = Color(0xFFBFC7D6),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3C4758),
    onSecondaryContainer = Color(0xFFDAE2F1),

    tertiary = Color(0xFFE6C255),
    onTertiary = Color(0xFF3B2E00),
    tertiaryContainer = Color(0xFF574500),
    onTertiaryContainer = Color(0xFFFFE9A8),

    background = Color(0xFF11141A),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF11141A),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),

    // Koyu temada merdiven tersine döner: yükseltilmiş yüzeyler yüzeyin *açılır*.
    surfaceDim = Color(0xFF0C0F14),
    surfaceBright = Color(0xFF373941),
    surfaceContainerLowest = Color(0xFF0B0E14),
    surfaceContainerLow = Color(0xFF191C22),
    surfaceContainer = Color(0xFF1D2026),
    surfaceContainerHigh = Color(0xFF282B31),
    surfaceContainerHighest = Color(0xFF33363C),

    inverseSurface = Color(0xFFE2E2E9),
    inverseOnSurface = Color(0xFF2F3138),

    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Fixed renkler tema değiştirsen de sabit kalır; aynı değerler kullanılır.
    primaryFixed = Color(0xFFDBE4FF),
    primaryFixedDim = Color(0xFFA8C7FA),
    onPrimaryFixed = Color(0xFF001B3F),
    onPrimaryFixedVariant = Color(0xFF003078),
    secondaryFixed = Color(0xFFDAE2F1),
    secondaryFixedDim = Color(0xFFBFC7D6),
    onSecondaryFixed = Color(0xFF131C2B),
    onSecondaryFixedVariant = Color(0xFF3C4758),
    tertiaryFixed = Color(0xFFFFE9A8),
    tertiaryFixedDim = Color(0xFFE6C255),
    onTertiaryFixed = Color(0xFF221A00),
    onTertiaryFixedVariant = Color(0xFF574500),
)

// ---- Tipografi ----

private val varsayilanTipografi = Typography()

/**
 * Malzeme 3'ün ölçeğini (punto, satır yüksekliği, harf aralığı) olduğu gibi
 * korur; yalnızca **hiyerarşi** için kalınlık ayarları yapar.
 *
 * Ölçeği baştan yazmak, kütüphanenin uzun uzun dengelenmiş satır yüksekliği
 * değerlerini bozmak demek — gerek yok. Buradaki tek fikir: ekranda ilk
 * okunan şey (ekran başlığı, ödev adı) diğerinden net olarak ayrılmalı.
 */
private val OdevTipografisi = Typography(
    // Ekran başlıkları: "Ödev Ekle", detay başlığı.
    headlineSmall = varsayilanTipografi.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    // Diyalog ve üst bar başlıkları.
    titleLarge = varsayilanTipografi.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    // Ödev kartının adı — listenin en önemli metni.
    titleMedium = varsayilanTipografi.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleSmall = varsayilanTipografi.titleSmall.copy(letterSpacing = 0.sp),
    // Rozet ve çip etiketleri Türkçe'de düzgün oturması için sıkıştırılır.
    labelLarge = varsayilanTipografi.labelLarge.copy(letterSpacing = 0.sp),
    labelMedium = varsayilanTipografi.labelMedium.copy(letterSpacing = 0.sp),
    labelSmall = varsayilanTipografi.labelSmall.copy(letterSpacing = 0.sp),
)

// ---- Şekiller ----

/**
 * Köşe yarıçapı ölçeği. Kartlar (`large`) ve tam ekran yüzeyler (`extraLarge`)
 * bu jetonları kullanır; böylece tüm kartların köşesini tek yerden değiştirmek
 * mümkün olur.
 */
private val OdevSekilleri = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Uygulamanın teması. Koyu tema sistem ayarını izler.
 *
 * Dinamik renk (Material You) **kasten kapalıdır**: kullanıcı sistem rengini
 * seçtiğinde de uygulamanın okunabilir, tasarlanmış kimliği korunur.
 */
@Composable
fun OdevTakipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) KoyuRenkler else AcikRenkler,
        typography = OdevTipografisi,
        shapes = OdevSekilleri,
        content = content
    )
}
