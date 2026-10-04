@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.detay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.durumMetni
import com.odevtakip.app.ui.durumRengi
import com.odevtakip.app.ui.teslimMetni
import com.odevtakip.app.util.formatliSaat
import com.odevtakip.app.util.formatliTarih

/**
 * Tek bir ödevin detayı.
 *
 * [OdevViewModel.seciliOdev] Flow'unu izler; böylece listeden geri dönüp
 * tamamlama işareti değişmişse ya da başka bir yerde silinmişse ekran
 * kendiliğinden güncellenir.
 */
@Composable
fun OdevDetayEkrani(
    odevId: Long,
    viewModel: OdevViewModel,
    onGeri: () -> Unit,
    onDuzenle: (Long) -> Unit,
) {
    val odev by viewModel.seciliOdev.collectAsStateWithLifecycle()
    val yuklendi by viewModel.seciliOdevYuklendi.collectAsStateWithLifecycle()
    var silOnayiGoster by remember { mutableStateOf(false) }

    LaunchedEffect(odevId) { viewModel.seciliOdeviAyarla(odevId) }
    DisposableEffect(odevId) {
        onDispose { viewModel.seciliOdeviAyarla(null) }
    }

    if (silOnayiGoster && odev != null) {
        SilOnayDialog(
            baslik = odev!!.baslik,
            onIptal = { silOnayiGoster = false },
            onSil = {
                silOnayiGoster = false
                viewModel.sil(odevId, onBasarili = onGeri)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.odev_detay)) },
                navigationIcon = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
                        )
                    }
                },
                actions = {
                    if (odev != null) {
                        IconButton(onClick = { onDuzenle(odevId) }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.duzenle),
                            )
                        }
                        IconButton(onClick = { silOnayiGoster = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.sil),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            val anlikOdev = odev
            if (anlikOdev != null) {
                TamamlaDugmesi(
                    odev = anlikOdev,
                    onTamamla = { viewModel.tamamla(anlikOdev.id) },
                    onGeriAl = { viewModel.tamamlamayiGeriAl(anlikOdev.id) },
                )
            }
        },
    ) { innerPadding ->
        when {
            !yuklendi -> Yukleniyor(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            odev == null -> Bulunamadi(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            else -> OdevIcerik(
                odev = odev!!,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

// ---- Durumlar ----

@Composable
private fun Yukleniyor(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Bulunamadi(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.odev_bulunamadi),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- İçerik ----

@Composable
private fun OdevIcerik(odev: Odev, modifier: Modifier = Modifier) {
    val durum = odev.gercekDurum(System.currentTimeMillis())
    val renk = durumRengi(durum)
    val tamamlandi = durum == Durum.TAMAMLANDI

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Durum + teslim bilgisi
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = renk,
                    contentColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = durumMetni(durum),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.etiket_teslim),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = teslimMetni(odev.sonTarih),
                        style = MaterialTheme.typography.titleMedium,
                        color = renk,
                    )
                }
            }
        }

        // Başlık
        Text(
            text = odev.baslik,
            style = MaterialTheme.typography.headlineSmall,
            textDecoration = if (tamamlandi) TextDecoration.LineThrough else null,
        )

        HorizontalDivider()

        // Açıklama
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (odev.aciklama.isBlank()) {
                Text(
                    text = stringResource(R.string.aciklama_yok),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = odev.aciklama,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        HorizontalDivider()

        // Tarih bilgileri
        BilgiSatiri(
            etiket = stringResource(R.string.etiket_teslim),
            deger = "${odev.sonTarih.formatliTarih()} ${odev.sonTarih.formatliSaat()}",
        )
        BilgiSatiri(
            etiket = stringResource(R.string.etiket_olusturma),
            deger = "${odev.olusturmaTarihi.formatliTarih()} ${odev.olusturmaTarihi.formatliSaat()}",
        )
        odev.tamamlanmaTarihi?.let { zaman ->
            BilgiSatiri(
                etiket = stringResource(R.string.etiket_tamamlanma),
                deger = "${zaman.formatliTarih()} ${zaman.formatliSaat()}",
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BilgiSatiri(etiket: String, deger: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = deger, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---- Alt eylem ----

@Composable
private fun TamamlaDugmesi(
    odev: Odev,
    onTamamla: () -> Unit,
    onGeriAl: () -> Unit,
) {
    // Dış Scaffold'ta contentWindowInsets sıfırlandığı için alt çubuk
    // sistem navigasyon çubuğunun altına düşer; burada kendi payını alır.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        val tamamlandi = odev.durum == Durum.TAMAMLANDI
        Button(
            onClick = if (tamamlandi) onGeriAl else onTamamla,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            colors = if (tamamlandi) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Text(
                text = stringResource(
                    if (tamamlandi) R.string.tamamlamayi_geri_al else R.string.tamamla
                )
            )
        }
    }
}

// ---- Silme onayı ----

@Composable
private fun SilOnayDialog(
    baslik: String,
    onIptal: () -> Unit,
    onSil: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onIptal,
        title = { Text(stringResource(R.string.sil_onay_baslik)) },
        text = { Text(stringResource(R.string.sil_onay_metin, baslik)) },
        confirmButton = {
            TextButton(onClick = onSil) {
                Text(stringResource(R.string.sil))
            }
        },
        dismissButton = {
            TextButton(onClick = onIptal) {
                Text(stringResource(R.string.iptal))
            }
        },
    )
}
