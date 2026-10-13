package com.odevtakip.app.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R
import com.odevtakip.app.ui.UygulamaSimgesi

/**
 * Yan menüdeki bir düğme: etiket, ikon ve tıklama.
 *
 * [ikon] yalnızca çizimdir; içerik açıklaması menüde anlamsızdır —
 * yanında zaten okunabilir bir metin durur.
 */
private class MenuOgesi(
    val etiket: String,
    val ikon: @Composable () -> Unit,
    val tik: () -> Unit,
)

/**
 * Sol kenardan açılan yan menü.
 *
 * Menü **yalnızca ikinci kademe ekranlara** yol gösterir: alt bardaki dört
 * sekme burada tekrar edilmez — aynı yere iki yerden gidilebiliyorsa
 * kullanıcı hangisinin "asıl" olduğunu bilemez. Bu yüzden menüde şu an
 * yalnızca ana ekranların dışına çıkan sayfalar durur.
 *
 * Öğeler [ModalDrawerSheet] içinde `NavigationDrawerItem` ile çizilir;
 * zemin `surfaceContainerLow`dur, böylece menü alt menüyle aynı
 * "üzerinde durulan kat" hissini verir.
 *
 * Seçim göstergesi **hiçbir öğede yanmaz**: menü tek bir gruptan oluşuyor
 * ve öğeler bir "sekme" değil, birer **bağlantı**. Birini seçili göstermek
 * her açılışta yanlış bir durum söylemiş olurdu.
 *
 * @param denemelerHedefi Deneme sınavları listesini açar; menü kapanır.
 * @param rutinHedefi Ders rutini ekranını açar; menü kapanır.
 * @param hakkindaHedefi Hakkında sayfasını açar; menü kapanır.
 */
@Composable
fun YanMenu(
    denemelerHedefi: () -> Unit,
    rutinHedefi: () -> Unit,
    hakkindaHedefi: () -> Unit,
) {
    val renkler = MaterialTheme.colorScheme

    ModalDrawerSheet(
        drawerContainerColor = renkler.surfaceContainerLow,
        // Kimlik satırı elle çizildiği için sheet'in kendi başlık boşluğu
        // istenmiyor; tonal elevation da kapatılır — zemin zaten ayrı.
        drawerTonalElevation = 0.dp,
    ) {
        Column {
            // ---- Kimlik satırı ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gerçek uygulama simgesi: menünün "burası Ödev Takip"
                // demesini sağlar. Köşe maskesi adaptive icon'un kendi
                // çiziminden gelir; buraya ek köşe verilmez.
                UygulamaSimgesi(boyut = 32.dp)

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.menu_baslik),
                        style = MaterialTheme.typography.bodySmall,
                        color = renkler.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = renkler.outlineVariant.copy(alpha = 0.6f),
            )

            Spacer(Modifier.size(8.dp))

            // ---- Öğeler ----
            val ogeler = listOf(
                MenuOgesi(
                    etiket = stringResource(R.string.menu_denemeler),
                    ikon = {
                        Icon(
                            imageVector = Icons.Outlined.Assessment,
                            contentDescription = null,
                        )
                    },
                    tik = denemelerHedefi,
                ),
                MenuOgesi(
                    etiket = stringResource(R.string.menu_rutin),
                    ikon = {
                        Icon(
                            // Yineleyen bir takvim: "rutin" sözcüğünün
                            // görsel karşılığı tekrardır, tek seferlik bir
                            // kayıt değil.
                            imageVector = Icons.Outlined.EventRepeat,
                            contentDescription = null,
                        )
                    },
                    tik = rutinHedefi,
                ),
                MenuOgesi(
                    etiket = stringResource(R.string.menu_hakkinda),
                    ikon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                        )
                    },
                    tik = hakkindaHedefi,
                ),
            )

            ogeler.forEach { oge ->
                NavigationDrawerItem(
                    label = { Text(oge.etiket) },
                    icon = oge.ikon,
                    selected = false,
                    onClick = oge.tik,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    }
}
