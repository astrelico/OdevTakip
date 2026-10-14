package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R
import com.odevtakip.app.data.Deneme
import com.odevtakip.app.ui.kartCizgisi
import com.odevtakip.app.ui.kartElevasyonu
import com.odevtakip.app.util.formatliTarih

/**
 * Deneme listesindeki tek kart: kim, ne zaman, kaç net.
 *
 * Kart [com.odevtakip.app.ui.OdevKarti] ile aynı iskeleti paylaşır —
 * `surface` zemin, hairline çerçeve, 2 dp gölge — çünkü kullanıcı iki
 * listeyi aynı uygulamada, üst üste gezerken görür; farklı görürse
 * "bu kart başka şey mi?" sorusu doğar. Fark yalnızca **içeriktedir**:
 *
 *  - **Sağ sütun neti taşır.** Denemede tek bakışta bakılan değer netdir;
 *    ödev kartındaki durum rozetinin yerini o alır. Yanında tek başına
 *    durur — listenin tek "sayısal" sonucudur.
 *  - **Sol üstte tür rozeti durur** ("TYT" / "AYT"): denemenin hangi
 *    formatta yapıldığı, kartın kategorisidir. Her kayıt bir tür taşır
 *    (bkz. [Deneme.tur]) — bu yüzden rozet hiç boş kalmaz, eski
 *    "ders seçilmedi" gizlemesinin yerini alır. Metin değil **rozet**
 *    çizilir (bkz. [TurRozeti]): zemini tonlu olduğundan kartın üst
 *    satırı sayılarla yarışmaz, kategori olarak okunur.
 *  - **Alt satır sonuçtur** (`45 doğru · 5 yanlış`), tarihin altındadır;
 *    hemen altında **oran şeridi** (bkz. [OranSeridi]) aynı sayının
 *    orantısını çizer. Metin sayıyı, şerit hissi verir — ikisi birbirini
 *    tekrar etmez.
 *  - **Durum/kaydırma yoktur**: deneme bitmiştir, "tamamlanamaz";
 *    bkz. [Deneme] sınıfı.
 *
 * Karta dokunmak [onSec] çağırır; ekran ayrı sayfadaki detaya gider.
 *
 * @param deneme Gösterilecek kayıt; sıralama DAO'da tarih azalandır.
 */
@Composable
fun DenemeKarti(
    deneme: Deneme,
    onSec: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onSec,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = kartCizgisi(),
        elevation = kartElevasyonu(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ---- Sol: kimlik ve zaman ----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                // Tür, ödev kartındaki kategori etiketi gibi başlığın
                // üstünde durur: kartın hangi formatta olduğu tek
                // bakışta okunur. Rozet zemini, kaydın kategorik
                // olduğunu sayıdan önce söyler — okuma sırası
                // "hangi tür → hangi deneme → ne sonuç" olur.
                TurRozeti(
                    tur = deneme.tur,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                Text(
                    text = deneme.ad,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = deneme.tarih.formatliTarih(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(
                        R.string.deneme_sonuc,
                        deneme.dogru,
                        deneme.yanlis,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Oran şeridi: sayının **orantısını** taşıyan çizgi.
                // "5 yanlış" tek başına iyi ya da kötü değildir; şeridin
                // kırmızı tarafının ne kadar yer kapladığını görmek,
                // judgment'ı sayıyı okumadan verir. Kartın genişliği
                // sabit olmadığı için şerit `weight(1f)` ile sol sütuna
                // yayılır — sağdaki net sütununa asla girmez.
                OranSeridi(
                    dogru = deneme.dogru,
                    yanlis = deneme.yanlis,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(),
                )
            }

            // ---- Sağ: net ----
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.deneme_net_etiket),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = sayiMetni(deneme.net),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
