@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R
import com.odevtakip.app.data.DenemeTuru

/**
 * Deneme ekleme akışının **ilk adımı**: sınavın hangi formatta olduğu.
 *
 * Panel, [com.odevtakip.app.ui.deneme.DenemeListeEkrani]'ndeki
 * "Deneme ekle" düğmesine basınca açılır. Seçim bilinçli olarak formdan
 * **önce** alınır: kullanıcı "ekle" dediği anda denemenin kaderi (kaç
 * soru, hangi dersler) belli olur ve form, o kararla **kilitli** bir
 * başlıkla açılır.
 *
 * Her satır türün derslerini ve soru sayılarını sayar; TYT'nin Sosyal
 * ve Fen testlerinin alt dağılımı da (Tarih 5, Coğrafya 5 …) ayrıca
 * gösterilir. Bu bilgi ekranda elle yazılmış değildir — hepsi
 * [DenemeTuru]'ndan üretilir, yani sayılar tek kaynaktan beslenir.
 *
 * Seçim anında [onSecim] çağrılıp panel kendisi kapanır; ayrı bir
 * "Tamam" düğmesi yoktur — listede tek dokunuşla iş bitirilsin diye.
 *
 * @param onSecim Seçilen tür; akış buradan forma yürür.
 * @param onKapat Panel yalnızca dışarı dokunarak kapanırsa.
 */
@Composable
fun DenemeTurPaneli(
    onSecim: (DenemeTuru) -> Unit,
    onKapat: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onKapat,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.deneme_tur_baslik),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )

            DenemeTuru.entries.forEach { tur ->
                TurSatiri(
                    tur = tur,
                    onClick = { onSecim(tur) },
                )
            }
        }
    }
}

/**
 * Tek tür satırı: başlık (özetle birlikte) ve varsa alt dağılım.
 *
 * Başlık TYT'de yalın `"TYT"`, AYT'de `"AYT (Sayısal)"`dır: uygulama
 * yalnızca sayısal adayın AYT testini desteklediği için bunu yazının
 * kendisi söyler — kullanıcı "AYT"dan eşit ağırlık testlerini beklemesin.
 * Kısa etiket (kartlarda, detayda görünen) ise iki yerde de kısadır
 * ([turEtiketi]).
 */
@Composable
private fun TurSatiri(
    tur: DenemeTuru,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = when (tur) {
                DenemeTuru.TYT -> stringResource(R.string.deneme_tur_tyt)
                DenemeTuru.AYT -> stringResource(R.string.deneme_tur_ayt_secim)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${stringResource(R.string.deneme_soru, tur.toplamSoru)} · ${tur.dersOzeti}",
            style = MaterialTheme.typography.bodyMedium,
        )
        // Alt dağılım: TYT'nin Sosyal ve Fen satırları bölünmüştür; AYT
        // dersleri bölünmediği için bu liste orada boştur. Satırın hangi
        // derse ait olduğu **burada** yazılır — formda satır zaten o
        // dersin başlığının altında durduğu için orada tekrarlanmaz.
        tur.dersleri.filter { it.altDagilim != null }.forEach { kota ->
            Text(
                text = "${kota.ders}: ${kota.altDagilim}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
