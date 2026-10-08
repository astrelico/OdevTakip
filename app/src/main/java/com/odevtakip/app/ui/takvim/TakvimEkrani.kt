@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.takvim

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
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
import com.odevtakip.app.data.TakvimGorunumu
import com.odevtakip.app.ui.OdevKarti
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.util.ayAdiVeYili
import com.odevtakip.app.util.ayinHucreleri
import com.odevtakip.app.util.gunKisaAdi
import com.odevtakip.app.util.gunUzunAdi
import com.odevtakip.app.util.tarihMetni
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

/**
 * Takvim: **iki biçim** — gün şeridi / aylık ızgara — + seçili günün ödevleri.
 *
 * Açılışta **gün şeridi** gelir: bugünün çevresinde ±30 kutu, yan yana ve
 * tek dokunuşla gün gezinme. Şerit, uygulamanın ana işine — o günün ödevine
 * bakmaya — en hızlı yoldur; bu yüzden varsayılan odur.
 *
 * Üst bardaki **Aylık** düğmesi aylık ızgaraya geçer: ayın tamamı tek
 * bakışta, günün altındaki nokta o günde ödev olduğunu söyler, ay başlığındaki
 * oklarla ay gezinilir. Düğme **hedefe** ad verir — ızgara açıkken
 * "Günlük" der ve şeride döner. Böylece kullanıcı açık olan görünümün
 * adını okuyup yanlış yola basmaz.
 *
 * Geçiş tercih olarak saklanır ([com.odevtakip.app.data.TakvimGorunumu]):
 * ızgarayı seçip kapatıp açınca yine ızgara gelir. Seçili gün ise iki
 * biçimde **ortaktır** — birinde seçilen gün ötekinde de seçili durur ve
 * alttaki liste hiç sıçramaz.
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
    val seciliAy by viewModel.seciliAy.collectAsStateWithLifecycle()
    val gorunum by viewModel.takvimGorunumu.collectAsStateWithLifecycle()
    val odevler by viewModel.gununOdevleri.collectAsStateWithLifecycle()
    val gunSayilari by viewModel.gunSayilari.collectAsStateWithLifecycle()

    // Şeridin açılışta bugüne bakması için gerekli. Yalnızca şerit biçiminde
    // kullanılır ama durum her iki biçimde de yaşar; geçişte kaybolmaz.
    val seritDurumu = rememberLazyListState(initialFirstVisibleItemIndex = ONCE_GUN)
    val hiz = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.takvim_baslik)) },
                actions = {
                    // Biçim anahtarı: etiket her zaman HEDEFİ söyler.
                    TextButton(
                        onClick = { viewModel.takvimGorunumuAyarla(gorunum.hedefi()) },
                    ) {
                        Text(stringResource(gorunum.dugmeEtiketRes))
                    }
                    TextButton(onClick = {
                        viewModel.gunAyarla(LocalDate.now())
                        // Izgara açıkken şeridi bugüne kaydırmak anlamsızdır.
                        if (gorunum == TakvimGorunumu.GUN) {
                            hiz.launch { seritDurumu.scrollToItem(ONCE_GUN) }
                        }
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
            when (gorunum) {
                TakvimGorunumu.GUN -> GunSeridi(
                    secili = seciliGun,
                    sayilar = gunSayilari,
                    onSecim = viewModel::gunAyarla,
                    seritDurumu = seritDurumu,
                )

                TakvimGorunumu.AY -> AylikTakvim(
                    ay = seciliAy,
                    secili = seciliGun,
                    sayilar = gunSayilari,
                    onGunSec = viewModel::gunAyarla,
                    onAyKaydir = viewModel::ayiKaydir,
                )
            }

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
                        // Alt menünün tabanı içerik alanının bittiği yerdir;
                        // dolgu, listenin son satırının sağ alt köşedeki
                        // "Ödev Ekle" düğmesinin altında kalmamasını sağlar.
                        // Düğme barın 72 dp üstünde biter, 80 dp onu geçer.
                        bottom = 80.dp,
                    ),
                ) {
                    items(odevler, key = { it.id }) { odev ->
                        OdevKarti(
                            odev = odev,
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
}

// ---- Gün şeridi ----

private const val ONCE_GUN = 30
private const val SONRA_GUN = 60

/**
 * Yatay gün şeridi — takvimin **varsayılan** biçimi.
 *
 * Bugün [ONCE_GUN] adım sağda başlar; açılışta oraya kaydırılır, "Bugün"
 * düğmesi hem seçimi hem kaydırmayı geri getirir. Şerit ±30 gün gösterir:
 * gün gün gezinmek için kaydırmak yeterlidir, ayın tamamı gereken tek şey
 * olduğunda ise üst bardaki düğme ızgaraya geçer.
 */
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
 * Şeritteki tek gün kutusu.
 *
 * Seçili gün dolu `primary` zemin; bugün (seçili değilse) yazı rengi
 * `primary` ile vurgulanır. Ödev olan günlerde altına küçük bir nokta konur —
 * nokta rengi zemine göre ters çevrilir, böylece okunaklı kalır.
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

// ---- Aylık ızgara ----

/** Gün hücresi yüksekliği — satır payı bu kadar. */
private val HUCRE_YUKSEKLIGI = 50.dp

/**
 * Izgaranın hafta günleri: Pzt … Paz.
 *
 * Sabit bir pazartesiden (`2024-01-01`) üretilir; cihazın "haftanın ilk
 * günü" ayarına bağlı değildir. Başlıklar ızgaranın kendisi gibi pazartesi
 * ile başladığı için her ay aynı sütun düzeni korunur.
 */
private val haftaGunleri: List<String> = run {
    val pazartesi = LocalDate.of(2024, 1, 1)
    List(7) { pazartesi.plusDays(it.toLong()).gunKisaAdi() }
}

/**
 * [ay]ın aylık takvim ızgarası.
 *
 * Üstte ay başlığı (oklarla aya geçiş), altında hafta günleri, sonra 5–6
 * satırlık gün ızgarası gelir. Hücrelerin hesabı saf [ayinHucreleri] içinde
 * ve testlidir; bu fonksiyonun işi yalnızca çizimdir.
 *
 * Nokta, o günde **kayıt** olduğunu söyler (tamamlanmış ödevler dâhil);
 * durumu alttaki liste ve kart rozeti verir.
 */
@Composable
private fun AylikTakvim(
    ay: YearMonth,
    secili: LocalDate,
    sayilar: Map<LocalDate, Int>,
    onGunSec: (LocalDate) -> Unit,
    onAyKaydir: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bugun = LocalDate.now()
    val hucreler = remember(ay) { ayinHucreleri(ay) }
    val renkler = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAyKaydir(-1) }) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.takvim_onceki_ay),
                )
            }
            Text(
                text = ay.atDay(1).ayAdiVeYili(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onAyKaydir(1) }) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.takvim_sonraki_ay),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            haftaGunleri.forEach { ad ->
                Text(
                    text = ad,
                    style = MaterialTheme.typography.labelSmall,
                    color = renkler.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        hucreler.chunked(7).forEach { satir ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                satir.forEach { gun ->
                    if (gun == null) {
                        // Ayın dışında kalan kare: aynı hafta gününü
                        // korumak için boş bırakılır, çizilmez.
                        Spacer(Modifier.weight(1f).height(HUCRE_YUKSEKLIGI))
                    } else {
                        AyHucre(
                            gun = gun,
                            secili = gun == secili,
                            bugun = gun == bugun,
                            adet = sayilar[gun] ?: 0,
                            onClick = { onGunSec(gun) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Izgaradaki tek gün hücresi.
 *
 * Seçili gün dolu `primary` daireyle kaplanır; bugün seçili değilse günü
 * yalnızca **yazı** `primary` olur — iki vurgunun üst üste binmesi okumayı
 * zorlaştırırdı. Ödev olan günlerde altına nokta konur ve nokta zemine göre
 * ters çevrilir ki seçili günün üzerinde de okunaklı kalsın.
 *
 * Hücre satırdaki tüm payını kaplar (~57 × 50 dp): hedef, ince bir daire
 * değil günün olduğu yatay banttır — küçük dokunma alanı kaçan tıklamaların
 * en yaygın nedenidir.
 */
@Composable
private fun AyHucre(
    gun: LocalDate,
    secili: Boolean,
    bugun: Boolean,
    adet: Int,
    onClick: () -> Unit,
    // weight yalnızca RowScope içinde çalıştığı için taban genişlik çağıran
    // taraftan gelir.
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = modifier.height(HUCRE_YUKSEKLIGI),
        shape = RoundedCornerShape(12.dp),
        color = if (secili) renkler.primary else Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = gun.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    secili -> renkler.onPrimary
                    bugun -> renkler.primary
                    else -> renkler.onSurface
                },
            )
            Spacer(Modifier.height(3.dp))
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
