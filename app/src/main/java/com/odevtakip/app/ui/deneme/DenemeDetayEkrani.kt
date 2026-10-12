@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.Deneme
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.UstBar
import com.odevtakip.app.ui.kartCizgisi
import com.odevtakip.app.ui.kartElevasyonu
import com.odevtakip.app.util.formatliTarih

/**
 * Tek bir deneme sınavının detayı.
 *
 * [OdevDetayEkrani] ile aynı iskeleti taşır — üst barda geri + sil, altta
 * kaydırılabilir içerik kartları — çünkü kullanıcı iki detay ekranını da
 * aynı uygulamada kullanır. Farklar:
 *
 *  - **Üst kart sonuçtur**: net ve puan yan yana. Ödev detayındaki
 *    "durum + teslim" kartının yerini denemenin tek gerçeği, net, alır.
 *  - **Tamamlama düğmesi yoktur**: deneme bitmiştir; geri alınacak bir
 *    işaret yoktur (bkz. [Deneme]).
 *
 * Üst barda üç düğme durur: geri, **Düzenle** (formu açar) ve **Sil**
 * (onay diyaloğuyla). Sıralama bilinçlidir: geri solda, eylemler sağda;
 * sil kırmızıdır çünkü geri alınamazdır — düzenlemek ise günlük iştir.
 *
 * Ekran [OdevViewModel.seciliDeneme] akışını izler; kayıt başka bir yerde
 * silinirse kendiliğinden "bulunamadı" durumuna düşer.
 *
 * @param denemeId Gösterilecek kaydın kimliği.
 * @param onDuzenle Üst bardaki Düzenle; form sayfasını açar.
 * @param onGeri Üst bardaki geri düğmesi; listeye döner.
 */
@Composable
fun DenemeDetayEkrani(
    denemeId: Long,
    viewModel: OdevViewModel,
    onDuzenle: (Long) -> Unit,
    onGeri: () -> Unit,
) {
    val deneme by viewModel.seciliDeneme.collectAsStateWithLifecycle()
    val yuklendi by viewModel.seciliDenemeYuklendi.collectAsStateWithLifecycle()
    var silOnayiGoster by remember { mutableStateOf(false) }

    LaunchedEffect(denemeId) { viewModel.seciliDenemeyiAyarla(denemeId) }
    DisposableEffect(denemeId) {
        onDispose { viewModel.seciliDenemeyiAyarla(null) }
    }

    if (silOnayiGoster && deneme != null) {
        SilOnayDialog(
            baslik = deneme!!.ad,
            onIptal = { silOnayiGoster = false },
            onSil = {
                silOnayiGoster = false
                viewModel.denemeSil(denemeId, onBasarili = onGeri)
            },
        )
    }

    Scaffold(
        topBar = {
            UstBar(
                baslik = { Text(stringResource(R.string.deneme_detay)) },
                gezinmeIkonu = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
                        )
                    }
                },
                eylemler = {
                    if (deneme != null) {
                        IconButton(onClick = { onDuzenle(denemeId) }) {
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
            )
        },
    ) { innerPadding ->
        when {
            !yuklendi -> Yukleniyor(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            deneme == null -> Bulunamadi(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            else -> DenemeIcerik(
                deneme = deneme!!,
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
            text = stringResource(R.string.deneme_bulunamadi),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- İçerik ----

/**
 * Detayın gövdesi.
 *
 * Sıra bilinçlidir: kullanıcı önce **sonuca** bakar (net/puan kartı),
 * sonra **kime ait** olduğuna (ad, ders), en sonda künyeye (tarih,
 * eklenme) iner. Açıklama arada durur — not, sayının hemen altında
 * okunmak ister.
 */
@Composable
private fun DenemeIcerik(
    deneme: Deneme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SonucKarti(deneme = deneme)

        // Başlık
        Text(
            text = deneme.ad,
            style = MaterialTheme.typography.headlineSmall,
        )

        // Ders; genel denemede etiket yoktur.
        if (deneme.ders.isNotBlank()) {
            Text(
                text = deneme.ders,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        HorizontalDivider()

        // Açıklama
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (deneme.aciklama.isBlank()) {
                Text(
                    text = stringResource(R.string.aciklama_yok),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = deneme.aciklama,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        HorizontalDivider()

        // Künye. Doğru/yanlış **burada tekrar edilmez**: üstteki sonuç
        // kartı zaten ikonlu olarak taşıyor; aynı sayının iki kez okunması
        // ekrana güveni düşürür ("bunlardan hangisi doğru?").
        BilgiSatiri(
            etiket = stringResource(R.string.deneme_etiket_tarih),
            deger = deneme.tarih.formatliTarih(),
        )
        BilgiSatiri(
            etiket = stringResource(R.string.etiket_olusturma),
            deger = deneme.olusturmaTarihi.formatliTarih(),
        )

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Sonuç kartı: solda net, sağda puan; altında doğru/yanlış satırı.
 *
 * Net **birincil renkte ve büyük** yazılır — deneme ekranının tek
 * "kahraman" sayısıdır. Puan, okul puanı olduğu için ayrı durur ve
 * girilmemişse `—` ile boş bırakılır: "Puan girilmedi" yazısı, kullanıcının
 * zaten bildiği bir yokluğu tekrar anlatır.
 *
 * Doğru/yanlış ikonlu satır, sayının **nereden** geldiğini gösterir;
 * net formülünü bilmeyen kullanıcı da sonucu doğrular.
 */
@Composable
private fun SonucKarti(deneme: Deneme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = kartCizgisi(),
        elevation = kartElevasyonu(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.deneme_net_etiket),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = sayiMetni(deneme.net),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = stringResource(R.string.deneme_puan_etiket),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (deneme.puan != null) {
                            sayiMetni(deneme.puan)
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (deneme.puan != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SayacSatiri(
                    deger = deneme.dogru.toString(),
                    etiket = stringResource(R.string.deneme_etiket_dogru),
                    ikon = {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                SayacSatiri(
                    deger = deneme.yanlis.toString(),
                    etiket = stringResource(R.string.deneme_etiket_yanlis),
                    ikon = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Doğru/yanlış sayacı: ikon + sayı + etiket.
 *
 * Sayı büyük, etiket küçük — kartın alt satırı iki sütuna bölünür ve
 * her sütun kendi değerini okutur.
 */
@Composable
private fun SayacSatiri(
    deger: String,
    etiket: String,
    ikon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ikon()
        Spacer(Modifier.width(6.dp))
        Text(
            text = deger,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

// ---- Silme onayı ----

@Composable
private fun SilOnayDialog(
    baslik: String,
    onIptal: () -> Unit,
    onSil: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onIptal,
        title = { Text(stringResource(R.string.deneme_sil_onay_baslik)) },
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
