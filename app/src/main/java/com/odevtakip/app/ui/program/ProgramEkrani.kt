@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.program

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.GUNLUK_DERS_SAYISI
import com.odevtakip.app.data.HaftaGunu
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.dersler.DersSecimPaneli

/**
 * Ders programı: haftanın gününü seç, o günün 8 ders saatine elle ders ata.
 *
 * Ekran bir **haftalık** ızgaranın günlük görünümüdür: çiplerden gün seçilir,
 * altta o günün satırları dizilir. Tek günlük görünüm seçiminin nedeni telefon
 * genişliği — 7 sütunluk bir tabloda ders adları okunmaz kalırdı; burada ise
 * her satır tam genişlikte olduğu için uzun adlar da sığar.
 *
 * Veri [OdevViewModel.program] akışından gelir. Tabloda yalnızca **dolu**
 * satırlar tutulduğu için her gün her zaman 8 çizilir: kaydı olmayan saatin
 * metni "Ders seç" olarak boş görünür.
 *
 * Dersler [com.odevtakip.app.ui.dersler.DersSecimPaneli] ile seçildiği için
 * listede yalnızca var olan adlar görünür; ders eklenmediyse panel
 * Ayarlar'a giden ipucunu gösterir.
 *
 * @param onAyarlar Üst bardaki dişli düğmesinin açtığı ayarlar ekranı.
 */
@Composable
fun ProgramEkrani(
    viewModel: OdevViewModel,
    onAyarlar: () -> Unit,
) {
    val gunSira by viewModel.seciliHaftaGunu.collectAsStateWithLifecycle()
    val program by viewModel.program.collectAsStateWithLifecycle()
    val dersler by viewModel.dersler.collectAsStateWithLifecycle()

    val gun = HaftaGunu.den(gunSira)
    val bugun = HaftaGunu.bugun()
    val bugunMetni = stringResource(R.string.bugun)

    // Seçili günün satırları: kayıt yoksa o saat boş sayılır.
    val saatler = remember(program, gunSira) {
        (1..GUNLUK_DERS_SAYISI).map { sira ->
            sira to program.firstOrNull { it.gun == gunSira && it.sira == sira }?.ders.orEmpty()
        }
    }

    var duzenlenecekSaat by remember { mutableStateOf<Int?>(null) }
    val acilanSaat = duzenlenecekSaat
    val acilanDers = acilanSaat?.let { saat -> saatler.firstOrNull { it.first == saat }?.second }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.program_baslik)) },
                actions = {
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
                .padding(innerPadding),
        ) {
            GunCipleri(
                secili = gunSira,
                onSecim = viewModel::haftaGunuAyarla,
            )

            Text(
                text = if (gun == bugun) "${gun.uzunAdi()} · $bugunMetni" else gun.uzunAdi(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            )

            // Ders listesi boşsa ipucu Ayarlar'a yönlendirsin — bu durumda
            // satıra dokunmak paneli boş açar, kullanıcı nereye gideceğini
            // bilmek zorunda.
            Text(
                text = if (dersler.isEmpty()) {
                    stringResource(R.string.ders_yok_ipucu)
                } else {
                    stringResource(R.string.program_ipucu)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (dersler.isEmpty()) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 10.dp,
                ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    // Alt menünün tabanı içerik alanının bittiği yerdir;
                    // dolgu, son satırın 64 dp yukarıda duran + düğmesinin
                    // altında kalmamasını sağlar.
                    .padding(bottom = 80.dp),
            ) {
                SaatlerKarti(saatler = saatler, onSec = { duzenlenecekSaat = it })
            }
        }
    }

    if (acilanSaat != null) {
        DersSecimPaneli(
            baslik = stringResource(R.string.program_slot_baslik, acilanSaat),
            dersler = dersler.map { it.ad },
            secili = acilanDers?.takeIf { it.isNotBlank() },
            // Boş bir saatte "boşalt" satırı çizilmez: seçili radyo yokken
            // "Bu dersi boşalt"ın seçili görünmesi yanıltıcı olurdu.
            ilkSecenekMetni = if (acilanDers.isNullOrBlank()) {
                null
            } else {
                stringResource(R.string.programi_bosalt)
            },
            onSecim = { secilen ->
                viewModel.programaYaz(gunSira, acilanSaat, secilen.orEmpty())
            },
            onKapat = { duzenlenecekSaat = null },
        )
    }
}

// ---- Gün çipleri ----

/**
 * Haftanın günleri: yedi sabit genişlikte çip.
 *
 * Kısa adlar üç harf olduğu için çipler zaten eşit görünür; `46.dp` bunu
 * güvenceye alır ve satırı tam olarak ekran dolgusuna oturtur. Satır
 * `horizontalScroll` ile donatılmıştır: sistem yazı boyutu büyürse çipler
 * taşmak yerine kaydırarak erişilir.
 */
@Composable
private fun GunCipleri(
    secili: Int,
    onSecim: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Büyük yazı boyutunda çipler taşarsın; kaydırarak erişilsin.
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HaftaGunu.entries.forEach { gun ->
            val seciliMi = gun.sira == secili

            FilterChip(
                selected = seciliMi,
                onClick = { onSecim(gun.sira) },
                label = {
                    Text(
                        text = gun.kisaAdi(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                modifier = Modifier.width(46.dp),
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

// ---- Ders saatleri ----

/**
 * Seçili günün 8 ders saati.
 *
 * Satırın tamamı basılabilir; soldaki numara rozeti `primary` zeminli,
 * ders adı ya da boşsa "Ders seç" yazısı solda, sağda kalem işareti vardır.
 * Aynı kart ve ayraç dili dersler ekranıyla paylaşılır — iki liste farklı
 * görünseydi kullanıcı aynı türden kaydı farklı yer sanabilirdi.
 */
@Composable
private fun SaatlerKarti(
    saatler: List<Pair<Int, String>>,
    onSec: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        saatler.forEachIndexed { indeks, (sira, ders) ->
            if (indeks > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            SaatSatiri(
                sira = sira,
                ders = ders,
                onClick = { onSec(sira) },
            )
        }
    }
}

@Composable
private fun SaatSatiri(
    sira: Int,
    ders: String,
    onClick: () -> Unit,
) {
    val renkler = MaterialTheme.colorScheme
    val bos = ders.isBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = renkler.primary.copy(alpha = 0.14f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = sira.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = renkler.primary,
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = if (bos) stringResource(R.string.ders_sec) else ders,
            style = MaterialTheme.typography.bodyLarge,
            color = if (bos) renkler.onSurfaceVariant else renkler.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(12.dp))

        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = null,
            tint = if (bos) renkler.outline else renkler.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
