@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R
import com.odevtakip.app.ui.DenemeSiralamasi

/**
 * Deneme listesinin sıralama paneli — alttan kayar, tek seçimli.
 *
 * [com.odevtakip.app.ui.liste.SiralamaPaneli] ile **aynı iskeleti** paylaşır
 * (tek satırlık başlık + etiket/açıklama ikilisi taşıyan radyo satırları);
 * iki panelin ayrı dosyalarda durması, iki ekranın kendi kararlarını kendi
 * yanlarında taşıması içindir — ödevlerin "Önerilen" diye bir seçeneği
 * denemede anlamsızdır, denemenin "Nete göre"si ödevde.
 *
 * Seçim anında [onSecim] çağrılıp panel kendisi kapanır; ayrı bir "Tamam"
 * düğmesi yoktur.
 *
 * @param secili Seçili sıralama; ilk açılışta [DenemeSiralamasi.TARIH_YENI].
 */
@Composable
fun DenemeSiralamaPaneli(
    secili: DenemeSiralamasi,
    onSecim: (DenemeSiralamasi) -> Unit,
    onKapat: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onKapat,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.siralama),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )

            DenemeSiralamasi.entries.forEach { secenek ->
                DenemeSiralamaSatiri(
                    etiket = stringResource(secenek.etiketRes),
                    aciklama = stringResource(secenek.aciklamaRes),
                    secili = secenek == secili,
                    onClick = { onSecim(secenek); onKapat() },
                )
            }
        }
    }
}

/**
 * Tek sıralama satırı.
 *
 * Başlık ile açıklama ayrı satırlardır; satırın tamamı seçilebilir, radyo
 * kendisi tıklanamaz (böylece açıklama metnine basmak da seçimi değiştirir).
 */
@Composable
private fun DenemeSiralamaSatiri(
    etiket: String,
    aciklama: String,
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = secili, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = etiket,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = aciklama,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
