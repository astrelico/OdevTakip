@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.liste

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.ui.OdevFiltresi
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.durumMetni
import com.odevtakip.app.ui.durumRengi
import com.odevtakip.app.ui.teslimMetni

/**
 * Ana ekran: filtre çipleri + ödev listesi + yeni ödev düğmesi.
 *
 * Liste [OdevViewModel.odevler] Flow'undan beslenir; ekleme/silme/tamamlama
 * sonrası ekran kendiliğinden yenilenir.
 */
@Composable
fun OdevListeEkrani(
    viewModel: OdevViewModel,
    onYeniOdev: () -> Unit,
    onOdevSec: (Long) -> Unit,
) {
    val filtre by viewModel.filtre.collectAsStateWithLifecycle()
    val odevler by viewModel.odevler.collectAsStateWithLifecycle()
    val sayilar by viewModel.sayilar.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.liste_baslik)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onYeniOdev,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.yeni_odev_ekle)) },
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 96.dp, // FAB'ın altında kalmasın
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

@Composable
private fun FiltreCipleri(
    secili: OdevFiltresi,
    sayilar: Map<OdevFiltresi, Int>,
    onSecim: (OdevFiltresi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OdevFiltresi.entries.forEach { filtre ->
            val adet = sayilar[filtre] ?: 0
            FilterChip(
                selected = filtre == secili,
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
            imageVector = Icons.Rounded.Assignment,
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

// ---- Tek ödev kartı ----

@Composable
private fun OdevKarti(
    odev: Odev,
    onSec: () -> Unit,
    onDegistir: (Boolean) -> Unit,
) {
    val durum = odev.gercekDurum(System.currentTimeMillis())
    val tamamlandi = durum == Durum.TAMAMLANDI
    val renk = durumRengi(durum)

    Card(
        onClick = onSec,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (tamamlandi) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(width = 1.dp, color = renk.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = tamamlandi,
                onCheckedChange = onDegistir,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 8.dp),
            ) {
                Text(
                    text = odev.baslik,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (tamamlandi) TextDecoration.LineThrough else null,
                    color = if (tamamlandi) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                if (odev.aciklama.isNotBlank()) {
                    Text(
                        text = odev.aciklama,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = renk,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = teslimMetni(odev.sonTarih),
                        style = MaterialTheme.typography.labelMedium,
                        color = renk,
                    )
                }
            }

            DurumRozeti(durum = durum, renk = renk)
        }
    }
}

@Composable
private fun DurumRozeti(durum: Durum, renk: Color) {
    Surface(
        color = renk.copy(alpha = 0.14f),
        contentColor = renk,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = durumMetni(durum),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
