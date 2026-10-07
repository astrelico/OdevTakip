@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.liste

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.Odev
import com.odevtakip.app.ui.OdevFiltresi
import com.odevtakip.app.ui.OdevKarti
import com.odevtakip.app.ui.OdevSiralamasi
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.dersler.DersSecimPaneli

/**
 * Ana ekran: büyüteç, sıralama, filtre çipleri + ödev listesi.
 *
 * Liste [OdevViewModel.odevler] Flow'undan beslenir; ekleme/silme/tamamlama
 * sonrası ekran kendiliğinden yenilenir. Arama ve sıralama da aynı akışın
 * girdisidir, dolayısıyla sonuç **anında** listelenir; ayrı bir "Ara"
 * düğmesi ya da yükleme durumu yoktur.
 *
 * Üst barda üç düğme durur: büyüteç (aramayı açar), sıralama ve dişli.
 * Büyüteçe basılınca başlık yerine metin alana geçer ve odak kendiliğinden
 * oraya düşer. Kullanıcı geriye basarsa arama **kapanır ve temizlenir** —
 * sonraki açılışta sürpriz bir filtreyle karşılaşmamalıdır.
 *
 * Yeni ödev düğmesi bu ekranda değil, alt menünün üzerindeki ortak
 * `+` düğmesinde durur (bkz. `MainActivity`).
 *
 * @param onAyarlar Üst bardaki dişli düğmesinin açtığı ayarlar ekranı.
 */
@Composable
fun OdevListeEkrani(
    viewModel: OdevViewModel,
    onOdevSec: (Long) -> Unit,
    onAyarlar: () -> Unit,
) {
    val filtre by viewModel.filtre.collectAsStateWithLifecycle()
    val odevler by viewModel.odevler.collectAsStateWithLifecycle()
    val sayilar by viewModel.sayilar.collectAsStateWithLifecycle()
    val toplamSayi by viewModel.toplamSayi.collectAsStateWithLifecycle()
    val gizlemeAcik by viewModel.tamamlananlariGizle.collectAsStateWithLifecycle()
    val dersler by viewModel.dersler.collectAsStateWithLifecycle()
    val dersFiltresi by viewModel.dersFiltresi.collectAsStateWithLifecycle()
    val arama by viewModel.arama.collectAsStateWithLifecycle()
    val siralama by viewModel.siralama.collectAsStateWithLifecycle()

    var filtrePanelGoster by remember { mutableStateOf(false) }
    var siralamaPanelGoster by remember { mutableStateOf(false) }

    // `rememberSaveable`: döndürmede ekranda ne arandığı unutulmasın.
    // (Arama **metninin** kendisi ViewModel'dedir; burada yalnızca açık
    // olup olmadığı duruyor.)
    var aramaAcik by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (aramaAcik) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                aramaAcik = false
                                viewModel.aramaAyarla("")
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.arama_kapat),
                            )
                        }
                    },
                    title = {
                        AramaAlani(
                            deger = arama,
                            onDegisiklik = viewModel::aramaAyarla,
                            onTemizle = { viewModel.aramaAyarla("") },
                        )
                    },
                    actions = {
                        SiralamaDugmesi(
                            secili = siralama,
                            onClick = { siralamaPanelGoster = true },
                        )
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
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.liste_baslik)) },
                    actions = {
                        IconButton(onClick = { aramaAcik = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.ara),
                            )
                        }
                        SiralamaDugmesi(
                            secili = siralama,
                            onClick = { siralamaPanelGoster = true },
                        )
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
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Çipler kaydırılabilir; "Filtreler" onların **dışında**, her an
            // görünür kalır — kaydırma çubuğuna gizlenen bir filtre, kullanılamaz
            // bir filtredir.
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FiltreCipleri(
                    secili = filtre,
                    sayilar = sayilar,
                    onSecim = viewModel::filtreAyarla,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FiltrelerDugmesi(
                    seciliDers = dersFiltresi,
                    onClick = { filtrePanelGoster = true },
                )
            }

            if (odevler.isEmpty()) {
                // Hangi süzgecin boşalttığını **en spesifikten** söyle:
                // arama açıkken "bu filtrede ödev yok" demek, aramayı
                // unutan kullanıcıyı yanlış yola sürer.
                BosDurum(
                    metin = when {
                        toplamSayi == 0 -> stringResource(R.string.liste_bos)
                        arama.isNotBlank() ->
                            stringResource(R.string.arama_bos, arama.trim())
                        gizlemeAcik && filtre == OdevFiltresi.TUMU ->
                            stringResource(R.string.liste_bos_gizli)
                        else -> stringResource(R.string.filtre_bos)
                    },
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
                        // Alt menünün tabanı içerik alanının bittiği yerdir;
                        // dolgu, listenin son satırının sağ alt köşedeki
                        // "Ödev Ekle" düğmesinin altında kalmamasını sağlar.
                        // Düğme barın 72 dp üstünde biter (56 dp yükseklik +
                        // 16 dp boşluk), 80 dp bunu 8 dp geçer.
                        bottom = 80.dp,
                    ),
                ) {
                    items(odevler, key = { it.id }) { odev ->
                        OdevKarti(
                            odev = odev,
                            // Kaydırarak tamamlanan kart listeyi terk ederken
                            // olduğu yerden uçmasın, yeni sırasına kaydıkça gitsin.
                            modifier = Modifier.animateItem(),
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

    if (filtrePanelGoster) {
        DersSecimPaneli(
            baslik = stringResource(R.string.ders_filtre_baslik),
            dersler = dersler.map { it.ad },
            secili = dersFiltresi,
            ilkSecenekMetni = stringResource(R.string.tum_dersler),
            onSecim = viewModel::dersFiltresiAyarla,
            onKapat = { filtrePanelGoster = false },
        )
    }

    if (siralamaPanelGoster) {
        SiralamaPaneli(
            secili = siralama,
            onSecim = viewModel::siralamaAyarla,
            onKapat = { siralamaPanelGoster = false },
        )
    }
}

// ---- Arama alanı ----

/**
 * Üst bardaki arama alanı.
 *
 * Büyüteç tuşuna basıldığında başlığın **yerine** geçer; alan ilk kez
 * çizildiğinde odak kendiliğinden oraya düşer, kullanıcı ayrıca bir
 * dokunuş daha yapmak zorunda kalmaz. Metin girildikçe liste anında
 * süzülür.
 *
 * Yuvarlak profil (`28.dp`) ve yalnızca odaktayken beliren çerçeve ile
 * referanstaki arama çubuğuna benzer; `singleLine` kısayol satırının
 * taşıp üst barda iki satır olmasını engeller.
 */
@Composable
private fun AramaAlani(
    deger: String,
    onDegisiklik: (String) -> Unit,
    onTemizle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val odakIsteyen = remember { FocusRequester() }

    LaunchedEffect(Unit) { odakIsteyen.requestFocus() }

    OutlinedTextField(
        value = deger,
        onValueChange = onDegisiklik,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(odakIsteyen),
        placeholder = {
            Text(
                text = stringResource(R.string.arama_yeri),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (deger.isNotEmpty()) {
                IconButton(onClick = onTemizle) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.arama_temizle),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

// ---- Sıralama tuşu ----

/**
 * Üst bardaki sıralama düğmesi.
 *
 * Varsayılan sıralamadayken nötr, başka bir seçenek seçilince `primary`
 * rengine döner — "liste neden bu sırada?" sorusunun cevabı ekranda durur.
 * [FiltrelerDugmesi] aynı kuralı uygular.
 */
@Composable
private fun SiralamaDugmesi(
    secili: OdevSiralamasi,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.SwapVert,
            contentDescription = stringResource(R.string.siralama),
            tint = if (secili == OdevSiralamasi.ONERILEN) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

// ---- Filtreler tuşu ----

/**
 * Çiplerin sağındaki "Filtreler" tuşu.
 *
 * Ders filtresi açıkken yazısı seçili ders adına döner ve zemini vurgulanır;
 * böylece kullanıcı "liste neden kısaldı?" sorusunun cevabını görür.
 */
@Composable
private fun FiltrelerDugmesi(
    seciliDers: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme
    val aktifMi = seciliDers != null

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = if (aktifMi) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = renkler.primary.copy(alpha = 0.14f),
                contentColor = renkler.primary,
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
    ) {
        Icon(
            imageVector = Icons.Rounded.FilterList,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = seciliDers ?: stringResource(R.string.filtreler),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Uzun ders adı çipleri sıkıştırmasın.
            modifier = Modifier.widthIn(max = 120.dp),
        )
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

/**
 * Süzgeçten hiçbir şey geçmeyince görülen ekran.
 *
 * @param metin Gösterilecek cümle. Neden boş olduğunu **söyleyen** metin
 *   çağrılır: arama, filtre ve gizleme farklı öneriler gerektirir.
 */
@Composable
private fun BosDurum(metin: String, modifier: Modifier = Modifier) {
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
            text = metin,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
