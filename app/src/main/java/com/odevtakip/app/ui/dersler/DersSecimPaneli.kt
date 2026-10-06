@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.dersler

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R

/**
 * Alttan kayan ders listesi — **tek seçimli**.
 *
 * Üç yerde kullanılır, aradaki tek fark [ilkSecenekMetni]dir:
 *
 *  - **Ödev formu**: yalnızca dersler. Ders zorunlu olduğu için "boş bırak"
 *    seçeneği yoktur (`ilkSecenekMetni = null`); kullanıcı birine dokunmadan
 *    paneli kapatabilir.
 *  - **Ödevler listesindeki "Filtreler" paneli**: en üstte "Tüm dersler"
 *    satırı vardır; onunla filtre geri alınır.
 *  - **Ders programı**: en üstte "Bu dersi boşalt" satırı durur; boş bir
 *    saatte bu satır gösterilmez, böylece radyo "seçili" bir seçenekle
 *    kullanıcıyı yanıltmaz.
 *
 * Seçim anında [onSecim] çağrılıp panel kendisi kapanır; ayrı bir "Tamam"
 * düğmesi yoktur — listede tek dokunuşla iş bitirilsin diye.
 *
 * @param dersler Görüntülenecek ders adları (boşsa [dersYokMetni] görünür).
 * @param secili Seçili dersin adı; `null` hiçbir şeyin seçili olmadığı ya da
 *   filtrede "tümü"nün seçili olduğu anlamına gelir.
 * @param ilkSecenekMetni Listenin başına eklenecek, `onSecim(null)` çağıran
 *   ek seçenek. `null` ise böyle bir satır çizilmez.
 * @param dersYokMetni Liste boşken söylenecek cümle. İkinci satır her üç
 *   kullanımda da Ayarlar'a yönlendiren ipucudur.
 */
@Composable
fun DersSecimPaneli(
    baslik: String,
    dersler: List<String>,
    secili: String?,
    ilkSecenekMetni: String? = null,
    onSecim: (String?) -> Unit,
    onKapat: () -> Unit,
    dersYokMetni: String = stringResource(R.string.ders_yok),
) {
    ModalBottomSheet(
        onDismissRequest = onKapat,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Listeyi büyütüp taşmasın; kısa listelerde panel kendiliğinden
                // içeriğe sarılır (verticalScroll yalnızca taşma anında devreye girer).
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = baslik,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )

            if (ilkSecenekMetni != null) {
                DersSatiri(
                    etiket = ilkSecenekMetni,
                    secili = secili == null,
                    onClick = { onSecim(null); onKapat() },
                )
            }

            if (dersler.isEmpty()) {
                BosListe(metin = dersYokMetni)
            } else {
                dersler.forEach { ad ->
                    DersSatiri(
                        etiket = ad,
                        secili = ad == secili,
                        onClick = { onSecim(ad); onKapat() },
                    )
                }
            }
        }
    }
}

/** Tek bir ders satırı. Satırın tamamı seçilebilir; radyo kendisi tıklanamaz. */
@Composable
private fun DersSatiri(
    etiket: String,
    secili: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = secili,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = secili, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ders listesi boşken panelin altındaki bilgilendirme. */
@Composable
private fun BosListe(metin: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.ders_yok_ipucu),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
