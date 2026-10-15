@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.deneme

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.DenemeTuru
import com.odevtakip.app.ui.DenemeSiralamasi
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.UstBar
import com.odevtakip.app.ui.theme.vurguGradyani
import com.odevtakip.app.ui.theme.vurguMetni

/**
 * Yan menüden girilen deneme sınavı listesi.
 *
 * Ekran **tam sayfadır**, panel değil: Hakkında gibi yığına itilir, üst
 * barda geri oku taşır ve alt menü görünmez — burası dört ana sekmeden
 * biri değildir, dönülecek bir **ara sayfadır**.
 *
 * **Sıralama üst sağdadır**: [DenemeSiralamaDugmesi] tuşu
 * [DenemeSiralamaPaneli]ni açar — tarihe göre yeni/eski, son eklenen,
 * ada göre ve nete göre seçenekleri taşır. Varsayılan
 * [DenemeSiralamasi.TARIH_YENI]dir: DAO'nun kendi sırası (tarih azalan),
 * yani liste ilk açılışta bugüne kadar hep gördüğü sırayı gösterir.
 * Varsayılandan çıkıldığında tuşun rengi `primary`e döner — "liste neden
 * bu sırada?" sorusunun cevabı ekranda kalır.
 *
 * Filtre ve arama **yoktur** — denemelerin tek mantığı zaman olduğu için
 * ayrı bir süzgeç, kullanıcıya yalnızca "neden boş" sorusu sorardı.
 * Sıralama ise süzgeç değildir: kayıtların **tamamını** gösterir,
 * hiçbirini saklamaz; yalnızca sırayı değiştirir.
 *
 * **Sağ altta "Deneme ekle" düğmesi** durur; boş durum metni de onu
 * işaret eder. Düğme, ana ekranların [com.odevtakip.app.MainActivity]'deki
 * ödev FAB'ıyla **aynı görünümü** paylaşır — uzatılmış form, mavi degrade,
 * hap kesim: kullanıcı iki listede de aynı eylemi aynı yerde arar.
 *
 * Düğmeye basınca önce [DenemeTurPaneli] açılır: deneme ekleme akışının
 * ilk adımı sınavın **türünü** (TYT/AYT) seçmektir. Seçim burada
 * alınırsa form, kilitli bir başlıkla açılır ve kullanıcı 120 soruluk bir
 * kaydı 80 soruya taşıyamaz.
 *
 * @param viewModel Denemeleri [OdevViewModel.denemeler] akışından alır.
 * @param onDenemeSec Karta dokununca açılacak detay sayfası.
 * @param onEkle Sağ alttaki düğme; tür panelini açar, seçimle birlikte
 *   form sayfasını çağırır.
 * @param onGeri Üst bardaki geri düğmesi; menüye döner.
 */
@Composable
fun DenemeListeEkrani(
    viewModel: OdevViewModel,
    onDenemeSec: (Long) -> Unit,
    onEkle: (DenemeTuru) -> Unit,
    onGeri: () -> Unit,
) {
    val denemeler by viewModel.denemeler.collectAsStateWithLifecycle()
    val siralama by viewModel.denemeSiralama.collectAsStateWithLifecycle()

    // Tür seçimi, ekleme akışının ilk adımıdır: düğmeye basınca panel
    // açılır, seçimle birlikte [onEkle] çağrılır ve panel kapanır.
    var turSecimiGoster by remember { mutableStateOf(false) }

    // Sıralama paneli de tek dokunuşla açılıp kapanır; seçim anında biter.
    var siralamaPanelGoster by remember { mutableStateOf(false) }

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
                eylemler = {
                    DenemeSiralamaDugmesi(
                        secili = siralama,
                        onClick = { siralamaPanelGoster = true },
                    )
                },
            )
        },
        floatingActionButton = {
            DenemeEkleDugmesi(onClick = { turSecimiGoster = true })
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        // FAB yüzünden: son kart, düğmenin altında kalmasın.
                        bottom = 80.dp,
                    ),
                ) {
                    items(denemeler, key = { it.id }) { deneme ->
                        DenemeKarti(
                            deneme = deneme,
                            onSec = { onDenemeSec(deneme.id) },
                            // Kayıt silinince kart yeni sırasına kayarak
                            // animasyonla çıkar; liste "ani" kesilmez.
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    if (turSecimiGoster) {
        DenemeTurPaneli(
            onSecim = { tur ->
                turSecimiGoster = false
                onEkle(tur)
            },
            onKapat = { turSecimiGoster = false },
        )
    }

    if (siralamaPanelGoster) {
        DenemeSiralamaPaneli(
            secili = siralama,
            onSecim = viewModel::denemeSiralamaAyarla,
            onKapat = { siralamaPanelGoster = false },
        )
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

// ---- Ekleme düğmesi ----

/**
 * Sağ alttaki "Deneme ekle" düğmesi.
 *
 * Çizim, [com.odevtakip.app.MainActivity]'deki ödev FAB'ının aynısıdır:
 * gölge ve degrade **dışta** çizilir, M3 düğme yüzeyi şeffaf kalır; böylece
 * arkasındaki mavi görünür ve gölge tek katman olur. Yalnızca simge ve
 * etiket farklıdır. Ekranın en güçlü eylemi, ekranın en güçlü nesnesi
 * olarak durur.
 *
 * Simge `contentDescription` içermez: etiket zaten [Text] çocuğundan
 * gelir, ikisi aynı sözü iki kez söylemesin.
 */
@Composable
private fun DenemeEkleDugmesi(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(elevation = 10.dp, shape = CircleShape)
            .background(brush = vurguGradyani(), shape = CircleShape),
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = Color.Transparent,
            contentColor = vurguMetni(),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
            ),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                )
            },
            text = { Text(stringResource(R.string.deneme_ekle)) },
        )
    }
}

// ---- Sıralama tuşu ----

/**
 * Üst bardaki sıralama düğmesi — deneme listesine özel.
 *
 * Kural [com.odevtakip.app.ui.liste.OdevListeEkrani]deki tuşla **aynıdır**:
 * varsayılan sıralamadayken nötr (`onSurfaceVariant`), başka bir seçenek
 * seçilince `primary`. Böylece ekranın neden o sırada olduğu, düğmeye
 * bakılınca anlaşılır; seçim değişmedikçe de dikkat çekmez.
 */
@Composable
private fun DenemeSiralamaDugmesi(
    secili: DenemeSiralamasi,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.SwapVert,
            contentDescription = stringResource(R.string.siralama),
            tint = if (secili == DenemeSiralamasi.TARIH_YENI) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}
