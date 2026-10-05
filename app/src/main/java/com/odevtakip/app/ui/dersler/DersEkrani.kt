@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.dersler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.Ders
import com.odevtakip.app.ui.OdevViewModel

/**
 * Ders ekle-değiştir ekranı (Ayarlar → "Ders ekle-değiştir").
 *
 * Üstte yeni ders ekleme alanı, altta mevcut liste ve her satırda silme
 * düğmesi vardır. Silme onay ister: listeden kalkmak ile ödevlerin o dersi
 * kaybetmesi farklı şeylerdir — ödevler kaybolmaz, yalnızca listedeki ad
 * gider.
 *
 * Yinelenen ad engeli [OdevViewModel.dersEkle] içinde kurulur; burada yalnızca
 * sonucun metni gösterilir.
 */
@Composable
fun DersEkrani(
    viewModel: OdevViewModel,
    onGeri: () -> Unit,
) {
    val dersler by viewModel.dersler.collectAsStateWithLifecycle()

    var yeniDers by rememberSaveable { mutableStateOf("") }
    var hata by remember { mutableStateOf<String?>(null) }
    var silinecek by remember { mutableStateOf<Ders?>(null) }

    // stringResource composable bağlamı dışında çağrılamadığı için metinler
    // burada bir kez çözülür ve eylem lambdalarında kullanılır.
    val bosAdMetni = stringResource(R.string.ders_adi_bos)
    val mevcutAdMetni = stringResource(R.string.ders_zaten_var)

    fun ekle() {
        if (yeniDers.isBlank()) {
            hata = bosAdMetni
            return
        }
        viewModel.dersEkle(yeniDers) { eklendi ->
            if (eklendi) {
                yeniDers = ""
                hata = null
            } else {
                hata = mevcutAdMetni
            }
        }
    }

    val silinecekDers = silinecek
    if (silinecekDers != null) {
        AlertDialog(
            onDismissRequest = { silinecek = null },
            title = { Text(stringResource(R.string.ders_sil_baslik)) },
            text = {
                Text(stringResource(R.string.ders_sil_metin, silinecekDers.ad))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dersSil(silinecekDers.id)
                        silinecek = null
                    }
                ) {
                    Text(stringResource(R.string.sil))
                }
            },
            dismissButton = {
                TextButton(onClick = { silinecek = null }) {
                    Text(stringResource(R.string.iptal))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dersler_baslik)) },
                navigationIcon = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
                        )
                    }
                },
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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EkleKarti(
                deger = yeniDers,
                onDegisim = {
                    yeniDers = it
                    if (hata != null) hata = null
                },
                hata = hata,
                onEkle = ::ekle,
            )

            DersListesi(
                dersler = dersler,
                onSil = { silinecek = it },
            )
        }
    }
}

// ---- Ekleme ----

@Composable
private fun EkleKarti(
    deger: String,
    onDegisim: (String) -> Unit,
    hata: String?,
    onEkle: () -> Unit,
) {
    val renkler = MaterialTheme.colorScheme
    val alanRenkleri = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = renkler.surface,
        unfocusedContainerColor = renkler.surface,
        disabledContainerColor = renkler.surface,
        errorContainerColor = renkler.surface,
    )

    Kart {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = deger,
                onValueChange = onDegisim,
                label = { Text(stringResource(R.string.ders_adi)) },
                placeholder = { Text(stringResource(R.string.ders_adi_ipucu)) },
                singleLine = true,
                isError = hata != null,
                colors = alanRenkleri,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onEkle,
                modifier = Modifier.height(56.dp),
            ) {
                Text(stringResource(R.string.ders_ekle))
            }
        }

        if (hata != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = hata,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ---- Liste ----

@Composable
private fun DersListesi(
    dersler: List<Ders>,
    onSil: (Ders) -> Unit,
) {
    Kart {
        if (dersler.isEmpty()) {
            Text(
                text = stringResource(R.string.ders_ekran_bos),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            dersler.forEachIndexed { indeks, ders ->
                if (indeks > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ders.ad,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                    )
                    IconButton(onClick = { onSil(ders) }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.ders_sil_ikon),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---- Ortak kart ----

/** Ayarlar kartlarıyla aynı dil: beyaz `surface`, 2 dp gölge, çerçevesiz. */
@Composable
private fun Kart(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
    }
}
