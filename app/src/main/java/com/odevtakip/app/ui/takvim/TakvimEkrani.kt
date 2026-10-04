@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.takvim

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.ui.OdevKarti
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.util.gunKisaAdi
import com.odevtakip.app.util.gunUzunAdi
import com.odevtakip.app.util.tarihMetni
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Takvim: yatay gün şeridi + seçili günün ödevleri.
 *
 * Şerit bugünün etrafında ± gün gösterir ve açılışta bugüne kaydırılır;
 * "Bugün" düğmesi hem seçimi hem kaydırmayı geri getirir.
 *
 * Ödevler [OdevKarti] ile aynı kartla gösterilir — liste ile takvim arasında
 * aynı ödev farklı görünürse kullanıcı üzerinde yanlış işlem yapabilir.
 *
 * @param onAyarlar Üst bardaki dişli düğmesinin açtığı ayarlar ekranı.
 */
@Composable
fun TakvimEkrani(
    viewModel: OdevViewModel,
    onOdevSec: (Long) -> Unit,
    onAyarlar: () -> Unit,
) {
    val seciliGun by viewModel.seciliGun.collectAsStateWithLifecycle()
    val odevler by viewModel.gununOdevleri.collectAsStateWithLifecycle()
    val gunSayilari by viewModel.gunSayilari.collectAsStateWithLifecycle()

    // Acilista bugun gorunur; sonraki girislerde kaydirma konumu korunur.
    val seritDurumu = rememberLazyListState(initialFirstVisibleItemIndex = ONCE_GUN)
    val hiz = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.takvim_baslik)) },
                actions = {
                    TextButton(onClick = {
                        viewModel.gunAyarla(LocalDate.now())
                        hiz.launch { seritDurumu.scrollToItem(ONCE_GUN) }
                    }) {
                        Text(stringResource(R.string.bugun))
                    }
                    IconButton(onClick = onAyarlar) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.ayarlar_baslik),
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
        ) {
            GunSeridi(
                secili = seciliGun,
                sayilar = gunSayilari,
                onSecim = viewModel::gunAyarla,
                seritDurumu = seritDurumu,
            )

            Text(
                text = stringResource(
                    R.string.takvim_gun_etiketi,
                    seciliGun.tarihMetni(),
                    seciliGun.gunUzunAdi(),
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            )

            if (odevler.isEmpty()) {
                GunBosDurum(
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

// ---- Gün şeridi ----

private const val ONCE_GUN = 30
private const val SONRA_GUN = 60

@Composable
private fun GunSeridi(
    secili: LocalDate,
    sayilar: Map<LocalDate, Int>,
    onSecim: (LocalDate) -> Unit,
    seritDurumu: LazyListState,
    modifier: Modifier = Modifier,
) {
    val bugun = LocalDate.now()
    val gunler = remember(bugun) {
        generateSequence(bugun.minusDays(ONCE_GUN.toLong())) { it.plusDays(1) }
            .take(ONCE_GUN + SONRA_GUN + 1)
            .toList()
    }

    LazyRow(
        state = seritDurumu,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(gunler, key = { it.toEpochDay() }) { gun ->
            GunHucre(
                gun = gun,
                secili = gun == secili,
                bugun = gun == bugun,
                adet = sayilar[gun] ?: 0,
                onClick = { onSecim(gun) },
            )
        }
    }
}

/**
 * Tek gün hücresi.
 *
 * Seçili gün dolu `primary` zemin; bugün (seçili değilse) yazı rengi `primary`
 * ile vurgulanır. Ödev olan günlerde altına küçük bir nokta konur — nokta
 * rengi zemine göre ters çevrilir, böylece okunaklı kalır.
 */
@Composable
private fun GunHucre(
    gun: LocalDate,
    secili: Boolean,
    bugun: Boolean,
    adet: Int,
    onClick: () -> Unit,
) {
    val renkler = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = Modifier.width(50.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (secili) renkler.primary else renkler.surface,
        contentColor = if (secili) renkler.onPrimary else renkler.onSurface,
        border = if (secili) {
            null
        } else {
            BorderStroke(width = 1.dp, color = renkler.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = gun.gunKisaAdi(),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    secili -> renkler.onPrimary
                    bugun -> renkler.primary
                    else -> renkler.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = gun.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    secili -> renkler.onPrimary
                    bugun -> renkler.primary
                    else -> renkler.onSurface
                },
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(
                        color = when {
                            adet == 0 -> Color.Transparent
                            secili -> renkler.onPrimary
                            else -> renkler.primary
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

// ---- Boş durum ----

@Composable
private fun GunBosDurum(modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.takvim_bos),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
