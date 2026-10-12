@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.UstBar

/**
 * Yan menüden girilen deneme sınavı listesi.
 *
 * Ekran **tam sayfadır**, panel değil: Hakkında gibi yığına itilir, üst
 * barda geri oku taşır ve alt menü görünmez — burası dört ana sekmeden
 * biri değildir, dönülecek bir **ara sayfadır**.
 *
 * Sıralama DAO'dan gelir (tarih azalan): en son yapılan deneme en üstte.
 * Liste ekranındaki gibi filtre, arama ya da sıralama **yoktur** —
 * denemelerin tek mantığı zaman olduğu için ayrı bir süzgeç, kullanıcıya
 * yalnızca "neden boş" sorusu sorardı.
 *
 * Yeni deneme ekleme düğmesi bu adımda **yoktur**; form sonraki adımda
 * gelince Scaffold'un kendi FAB yuvasına bağlanacaktır. Boş durum
 * metninin andığı "sağ alttaki düğme" de o adımda gerçekten çizilmiş
 * olur.
 *
 * @param viewModel Denemeleri [OdevViewModel.denemeler] akışından alır.
 * @param onGeri Üst bardaki geri düğmesi; menüye döner.
 */
@Composable
fun DenemeListeEkrani(
    viewModel: OdevViewModel,
    onGeri: () -> Unit,
) {
    val denemeler by viewModel.denemeler.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            UstBar(
                baslik = { Text(stringResource(R.string.deneme_baslik)) },
                gezinmeIkonu = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (denemeler.isEmpty()) {
                BosDurum(
                    metin = stringResource(R.string.deneme_bos),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                )
            } else {
                // Kayıt sayısı listenin üstünde küçük bir başlık satırıdır:
                // kullanıcı kaç denemesi olduğunu listeyi saymadan görür.
                Text(
                    text = stringResource(R.string.deneme_sayisi, denemeler.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 16.dp,
                    ),
                ) {
                    items(denemeler, key = { it.id }) { deneme ->
                        DenemeKarti(
                            deneme = deneme,
                            // Kayıt silinince kart yeni sırasına kayarak
                            // animasyonla çıkar; liste "ani" kesilmez.
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Süzgeçten hiçbir şey geçmeyince görülen ekran.
 *
 * [com.odevtakip.app.ui.liste.OdevListeEkrani] ile aynı çizim: ikon
 * dairenin içinde durur — boş ekran "kırık" değil, "hazır" görünür.
 */
@Composable
private fun BosDurum(metin: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Assignment,
                contentDescription = null,
                modifier = Modifier
                    .padding(28.dp)
                    .size(56.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
