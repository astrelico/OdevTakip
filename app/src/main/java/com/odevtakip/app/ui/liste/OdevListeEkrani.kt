@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.liste

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.Odev
import com.odevtakip.app.ui.OdevFiltresi
import com.odevtakip.app.ui.OdevKarti
import com.odevtakip.app.ui.OdevViewModel

/**
 * Ana ekran: filtre çipleri + ödev listesi.
 *
 * Liste [OdevViewModel.odevler] Flow'undan beslenir; ekleme/silme/tamamlama
 * sonrası ekran kendiliğinden yenilenir.
 *
 * Yeni ödev düğmesi bu ekranda değil, alt menünün üzerindeki ortak
 * `+` düğmesinde durur (bkz. `MainActivity`).
 */
@Composable
fun OdevListeEkrani(
    viewModel: OdevViewModel,
    onOdevSec: (Long) -> Unit,
) {
    val filtre by viewModel.filtre.collectAsStateWithLifecycle()
    val odevler by viewModel.odevler.collectAsStateWithLifecycle()
    val sayilar by viewModel.sayilar.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.liste_baslik)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FiltreCipleri(
                secili = filtre,
                sayilar = sayilar,
                onSecim = viewModel::filtreAyarla,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (odevler.isEmpty()) {
                BosDurum(
                    tumListeBos = sayilar.getValue(OdevFiltresi.TUMU) == 0,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        // Alt menü + ortak + düğmesi altında kalmasın.
                        bottom = 104.dp,
                    ),
                ) {
                    items(odevler, key = { it.id }) { odev ->
                        OdevKarti(
                            odev = odev,
                            onSec = { onOdevSec(odev.id) },
                            onDegistir = { tamamlandi ->
                                if (tamamlandi) viewModel.tamamla(odev.id)
                                else viewModel.tamamlamayiGeriAl(odev.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---- Filtre çipleri ----

/**
 * Filtre çipleri.
 *
 * Seçili çip dolu `primary` zemine geçer (referanstaki "aktif" çip gibi);
 * seçilmemiş çipler kart renginde, ince `outlineVariant` çerçeveli kalır.
 * Böylece hangi filtrede olunduğu tek bakışta belli olur.
 */
@Composable
private fun FiltreCipleri(
    secili: OdevFiltresi,
    sayilar: Map<OdevFiltresi, Int>,
    onSecim: (OdevFiltresi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OdevFiltresi.entries.forEach { filtre ->
            val adet = sayilar[filtre] ?: 0
            val seciliMi = filtre == secili

            FilterChip(
                selected = seciliMi,
                onClick = { onSecim(filtre) },
                label = {
                    Text(
                        text = if (adet > 0) {
                            "${stringResource(filtre.etiketRes)} ($adet)"
                        } else {
                            stringResource(filtre.etiketRes)
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = renkler.surface,
                    labelColor = renkler.onSurfaceVariant,
                    selectedContainerColor = renkler.primary,
                    selectedLabelColor = renkler.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = seciliMi,
                    borderColor = renkler.outlineVariant,
                ),
            )
        }
    }
}

// ---- Boş durum ----

@Composable
private fun BosDurum(tumListeBos: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Assignment,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(
                if (tumListeBos) R.string.liste_bos else R.string.filtre_bos
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
