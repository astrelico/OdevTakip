@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R

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
 * @param gezinmeIkonu Soldaki düğme; yoksa boş bırakılır. Geri oku ya da
 *   hamburger olarak kullanılır.
 * @param eylemler Sağdaki düğmeler.
 * @param menuAc **Sol üstteki hamburger** düğmesi. Verilirse [gezinmeIkonu]
 *   yerine bu çizilir — iki sol düğmenin aynı anda görünmesi anlamsızdır.
 *   Yan menüyü açan ana ekranlar bunu verir; geri dönüş gereken ekranlar
 *   yalnızca [gezinmeIkonu]u verir.
 */
@Composable
fun UstBar(
    baslik: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    gezinmeIkonu: @Composable () -> Unit = {},
    eylemler: @Composable RowScope.() -> Unit = {},
    menuAc: (() -> Unit)? = null,
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
                navigationIcon = {
                    if (menuAc != null) {
                        IconButton(onClick = menuAc) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = stringResource(R.string.menu_ac),
                            )
                        }
                    } else {
                        gezinmeIkonu()
                    }
                },
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

// ---- Uygulama simgesi ----

/**
 * Uygulamanın kendi simgesi: zemin + üstündeki ödev kartı katmanı.
 *
 * `mipmap/ic_launcher` **burada kullanılamaz** — API 26+ yükleyicisi
 * `adaptive-icon` XML'i döndürür ve Compose'un `painterResource`ı yalnızca
 * düz vektörleri ve raster dosyaları kabul eder:
 * `Only VectorDrawables and rasterized asset types are supported`.
 * Uygulama bu yüzden **açılışta** çöküyordu (drawer içeriği kapalıyken de
 * çizildiği için hata ilk karede fırlıyordu).
 *
 * Çözüm, adaptive icon'un **içindeki iki düz vektörü** katmanlamaktır:
 * [com.odevtakip.app.R.drawable.ic_launcher_background] ile
 * [com.odevtakip.app.R.drawable.ic_launcher_foreground]. İkisi de aynı
 * 108 × 108 viewport'u paylaştığı için üst üste bindirmek simgeyi birebir
 * yeniden üretir; launcher'da gördüğünüz görüntüyle hiçbir farkı yoktur.
 *
 * Köşe maskesi verilmez: başlatıcının kendi maskesi (yvarlak/kare/daire)
 * cihaz görevidir, uygulama içi çizimdeyse tema şekilleri belirleyicidir.
 *
 * @param boyut Kenar uzunluğu.
 */
@Composable
fun UygulamaSimgesi(
    boyut: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(boyut),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
        )
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
