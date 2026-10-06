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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.GUNLUK_DERS_SAYISI
import com.odevtakip.app.data.HaftaGunu
import com.odevtakip.app.data.gununDersSayisi
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.dersler.DersSecimPaneli

/**
 * Ders programı: haftanın gününü seç, o günün ders saatlerine elle ders ata.
 *
 * Ekran bir **haftalık** ızgaranın günlük görünümüdür: çiplerden gün seçilir,
 * altta o günün satırları dizilir. Tek günlük görünüm seçiminin nedeni telefon
 * genişliği — 7 sütunluk bir tabloda ders adları okunmaz kalırdı; burada ise
 * her satır tam genişlikte olduğu için uzun adlar da sığar.
 *
 * **Satır sayısı güne özeldir.** Her gün [GUNLUK_DERS_SAYISI] satırla başlar;
 * kartın altındaki "Ders ekle" o güne 9., 10., … satırı açar, "Son dersi
 * kaldır" geri indirir (alt sınıfa inilmez). Okul günleri günden güne
 * değişebildiği için sayı hafta geneline yayılmaz — kullanıcı istediği günü
 * uzatır. Sayaç ayrı bir yerde tutulmaz: [gununDersSayisi] satırları
 * doğrudan tablodan sayar, bu yüzden "Ders ekle" ile açılan — belki henüz
 * boş — satır da varlığını sürdürür.
 *
 * Veri [OdevViewModel.program] akışından gelir. Kaydı olmayan saatin metni
 * "Ders seç" olarak boş görünür.
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

    // Günün satır sayısı; tablonun kendisinden türür, ayrı bir sayaç değil.
    val dersSayisi = remember(program, gunSira) { gununDersSayisi(gunSira, program) }

    // Seçili günün satırları: kayıt yoksa o saat boş sayılır.
    val saatler = remember(program, gunSira, dersSayisi) {
        (1..dersSayisi).map { sira ->
            sira to program.firstOrNull { it.gun == gunSira && it.sira == sira }?.ders.orEmpty()
        }
    }

    var duzenlenecekSaat by remember { mutableStateOf<Int?>(null) }
    var kaldirilacakSaat by remember { mutableStateOf<Int?>(null) }
    var yeniSatirEklendi by remember { mutableStateOf(false) }

    val acilanSaat = duzenlenecekSaat
    val acilanDers = acilanSaat?.let { saat -> saatler.firstOrNull { it.first == saat }?.second }

    // Satır eklenince liste uzar ve yeni satır görüşün altında kalır;
    // kullanıcı "Ders ekle"ye bastığı yere geri döndüğü için listeyi elle
    // kaydırması gerekirdi. Yalnızca **kullanıcı eklediğinde** sona
    // kaydırılır; bayrak yalnızca okunur, etkinin anahtarı değildir —
    // anahtar olsaydı etki, veritabanı yazısı bitmeden çalışıp bayrağı
    // sıfırlar ve gerçek artış geldiğinde kaydırma yapılmazdı. Açılışta
    // akış önce boş liste yayınlar, onu izleyen gerçek veri sayıyı
    // büyütebilir; ekranın açılması tepetaklak kaydırılmamalı.
    val kaydirma = rememberScrollState()
    LaunchedEffect(dersSayisi) {
        if (!yeniSatirEklendi) return@LaunchedEffect
        yeniSatirEklendi = false
        // `maxValue` ölçümde güncellenir; bir çerçeve bekleyip öyle kaydır.
        withFrameNanos { }
        kaydirma.animateScrollTo(kaydirma.maxValue)
    }

    val silinecekSaat = kaldirilacakSaat
    if (silinecekSaat != null) {
        val silinecekDers = saatler.firstOrNull { it.first == silinecekSaat }?.second.orEmpty()
        AlertDialog(
            onDismissRequest = { kaldirilacakSaat = null },
            title = { Text(stringResource(R.string.program_kaldir_onay_baslik)) },
            text = {
                Text(
                    stringResource(
                        R.string.program_kaldir_onay_metin,
                        silinecekSaat,
                        silinecekDers,
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saatKaldir(gunSira, silinecekSaat)
                        kaldirilacakSaat = null
                    }
                ) {
                    Text(stringResource(R.string.kaldir))
                }
            },
            dismissButton = {
                TextButton(onClick = { kaldirilacakSaat = null }) {
                    Text(stringResource(R.string.iptal))
                }
            },
        )
    }

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
                    .verticalScroll(kaydirma)
                    .padding(horizontal = 16.dp)
                    // Alt menünün tabanı içerik alanının bittiği yerdir;
                    // dolgu, son satırın 64 dp yukarıda duran + düğmesinin
                    // altında kalmamasını sağlar.
                    .padding(bottom = 80.dp),
            ) {
                SaatlerKarti(
                    saatler = saatler,
                    dersSayisi = dersSayisi,
                    onSec = { duzenlenecekSaat = it },
                    onSaatEkle = {
                        yeniSatirEklendi = true
                        viewModel.saatAc(gunSira)
                    },
                    onSonSaatKaldir = {
                        // Boş satır doğrudan iner; dolu satır dersi de
                        // götüreceği için önce kullanıcıya sorulur.
                        val sonDers = saatler.lastOrNull()?.second.orEmpty()
                        if (sonDers.isBlank()) {
                            viewModel.saatKaldir(gunSira, dersSayisi)
                        } else {
                            kaldirilacakSaat = dersSayisi
                        }
                    },
                )
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
 * Seçili günün ders saatleri ve listenin altındaki iki eylem satırı.
 *
 * Saat satırının tamamı basılabilir; soldaki numara rozeti `primary` zeminli,
 * ders adı ya da boşsa "Ders seç" yazısı solda, sağda kalem işareti vardır.
 * Eylem satırları aynı gövde geometrisini — 30 dp madde işareti + 14 dp
 * aralık — kullanır, böylece yazı sütunu saatlerle hizalanır; farkı renk ve
 * ikon verir: "Ders ekle" `primary`, kaldırma `error`.
 *
 * Kaldırma satırı yalnızca [GUNLUK_DERS_SAYISI] üzerindeki günlerde çizilir:
 * sekiz satır alt sınırdır, arayüz onu eksiltmeye açık bırakmaz.
 */
@Composable
private fun SaatlerKarti(
    saatler: List<Pair<Int, String>>,
    dersSayisi: Int,
    onSec: (Int) -> Unit,
    onSaatEkle: () -> Unit,
    onSonSaatKaldir: () -> Unit,
) {
    val renkler = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = renkler.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        saatler.forEachIndexed { indeks, (sira, ders) ->
            if (indeks > 0) {
                HorizontalDivider(color = renkler.outlineVariant)
            }
            SaatSatiri(
                sira = sira,
                ders = ders,
                onClick = { onSec(sira) },
            )
        }

        HorizontalDivider(color = renkler.outlineVariant)
        EylemSatiri(
            ikon = Icons.Rounded.Add,
            etiket = stringResource(R.string.programa_ekle),
            renk = renkler.primary,
            onClick = onSaatEkle,
        )

        if (dersSayisi > GUNLUK_DERS_SAYISI) {
            HorizontalDivider(color = renkler.outlineVariant)
            EylemSatiri(
                ikon = Icons.Rounded.Remove,
                etiket = stringResource(R.string.program_saat_kaldir),
                renk = renkler.error,
                onClick = onSonSaatKaldir,
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

/**
 * Kartın altındaki eylem satırı: madde işareti yerine ikon, yazı [renk]de.
 *
 * `Role.Button` ile ekran okuyucuya listede bir kayıt değil bir **eylem**
 * söylendiği bildirilir.
 */
@Composable
private fun EylemSatiri(
    ikon: ImageVector,
    etiket: String,
    renk: Color,
    onClick: () -> Unit,
) {
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
                .background(color = renk.copy(alpha = 0.14f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ikon,
                contentDescription = null,
                tint = renk,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyLarge,
            color = renk,
            maxLines = 1,
        )
    }
}
