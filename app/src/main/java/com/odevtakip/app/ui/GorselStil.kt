@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Uygulama genelinde **bir kez** verilmiş görsel kararlar.
 *
 * Uygulama boyunca tekrar eden üç şey var: üst bar, kart ve boş durum.
 * Bunların aynı görünmesi gerekiyor — aynı ödev iki ekranda farklı çerçeveyle
 * görünüse kullanıcı "bunlar aynı şey mi?" diye düşünür. Bu yüzden kararlar
 * burada toplanır, ekranlar yalnızca çağırır.
 *
 * Görsel dil üç katman üzerine kuruludur:
 *
 *  - **Zemin ayrımı:** üst bar ve alt menü `surface`, ekranın kendisi
 *    `background`dur. İçerik, çerçevesi olmayan bir yüzeyin üstünde durur.
 *  - **Kart:** `surface` zemin + 2 dp gölge + %60-opacity `outlineVariant`
 *    hairline. Gölge açık temada, hairline koyu temada işe yarar; ikisi de
 *    her temada çizildiği için geçişte kart "kaybolmaz".
 *  - **Vurgu:** tek renk yerine degrade — bkz. [vurguGradyani].
 */

// ---- Üst bar ----

/**
 * Ekranların ortak üst barı: `surface` zemin + alt hairline.
 *
 * Malzeme 3'ün [TopAppBar] bileşeni sarmalanır; barın **kendisi** şeffaf
 * bırakılır, zemini dıştaki [Surface] verir. Böylece durum çubuğunun arkası
 * dâhil barın tamamı tek renk olur ve altındaki ince çizgi içerikle barı
 * ayırır — "site gibi" düz zeminlerin en çok eksik bıraktığı şey tam olarak
 * budur.
 *
 * @param baslik Başlık içeriği; ekranlar kendi [androidx.compose.material3.Text]ini verir.
 * @param gezinmeIkonu Soldaki geri düğmesi; yoksa boş bırakılır.
 * @param eylemler Sağdaki düğmeler.
 */
@Composable
fun UstBar(
    baslik: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    gezinmeIkonu: @Composable () -> Unit = {},
    eylemler: @Composable RowScope.() -> Unit = {},
) {
    val renkler = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        color = renkler.surface,
        contentColor = renkler.onSurface,
    ) {
        Column {
            TopAppBar(
                title = baslik,
                navigationIcon = gezinmeIkonu,
                actions = eylemler,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = renkler.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

// ---- Kart ----

/**
 * Kartların ortak hairline çerçevesi.
 *
 * `outlineVariant`'ın %60'ı: tam opaklık kartı kutu gibi, sıfır ise ekranda
 * kayıp gibi gösterir. Gölge açık temada zaten kenarı ayırır; çizgi koyu
 * temada — gölgenin görünmediği yerde — aynı işi yapar.
 */
@Composable
fun kartCizgisi(): BorderStroke = BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
)

/**
 * Kartların ortak gölge davranışı.
 *
 * Açık temada duruş gölgesi 2 dp'de tutulur (kart "yapışık" durur, liste
 * uzun görünmez) ve **basıldığında** 5 dp'ye çıkar — dokunuşun karşılığı
 * hemen görünür.
 *
 * **Koyu temada gölge tamamen kalkar.** Zemin zaten neredeyse siyah olduğu
 * için gölge görünmez; karşılığında her kart için RenderNode'ın blur katmanı
 * çizilir ve bu, kaydırırken GPU'nun en çok zaman yediği işlerden biridir
 * (ölçüm: kare çizim komutlarının ~30'u gölgeden). Ayırt ediciliği
 * [kartCizgisi] hairline'ı zaten taşıdığı için kayıp yoktur.
 */
@Composable
fun kartElevasyonu() = CardDefaults.cardElevation(
    defaultElevation = if (koyuTemada()) 0.dp else 2.dp,
    pressedElevation = if (koyuTemada()) 0.dp else 5.dp,
    focusedElevation = if (koyuTemada()) 0.dp else 3.dp,
    hoveredElevation = if (koyuTemada()) 0.dp else 3.dp,
)

/**
 * Temanın koyu olup olmadığı — sistem ayarına değil, uygulamanın kendi
 * seçtiği temaya bakar.
 *
 * `isSystemInDarkTheme()` burada yanlıştır: kullanıcı "Koyu tema" seçip
 * telefonu açık modda kullanabilir. Renk şemasının kendisi her zaman
 * doğruyu söylediği için kontrol onun üzerinden yapılır.
 */
@Composable
internal fun koyuTemada(): Boolean =
    MaterialTheme.colorScheme.surface.luminance() < 0.5f
