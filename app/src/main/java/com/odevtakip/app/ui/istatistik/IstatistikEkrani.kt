@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.istatistik

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.ui.DersDagilimi
import com.odevtakip.app.ui.Dilim
import com.odevtakip.app.ui.GunlukSayim
import com.odevtakip.app.ui.Istatistik
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.dersDagilimi
import com.odevtakip.app.ui.durumDilimleri
import com.odevtakip.app.ui.durumMetni
import com.odevtakip.app.ui.durumRengi
import com.odevtakip.app.ui.istatistikHesapla
import com.odevtakip.app.ui.sonGunlerinTamamlanmasi
import com.odevtakip.app.util.gunKisaAdi
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * İstatistik: ödevlerin durum dağılımı, sayısal özeti ve grafikleri.
 *
 * Ekran üç katmandan oluşur, hepsi **tek bir "şimdi"** üzerinden hesaplanır:
 *
 *  1. [Istatistik] — dört sayı ve tamamlama oranı. Bunların toplamı kayıtlara
 *     eşittir, ayrıca "diğer" kategorisi gerekmez.
 *  2. **Halka (pasta) grafiği** — üç durumun dilimleri. Merkezde tamamlama
 *     oranı, sağında renk + ad + adet/% satırları durur.
 *  3. **Sütun ve çubuk grafikleri** — son 7 günün tamamlanması ve derslere
 *     göre dağılım. Ders çubuklarının renkleri pastayla **aynıdır**, böylece
 *     ekranın iki yarısı aynı dili konuşur.
 *
 * Veri [OdevViewModel.tumOdevler] akışından gelir: filtre ve gizleme
 * buraya **dokunmaz** — listede "Geciken" çipi seçili kalmış olsa bile
 * özet tüm kayıtları sayar.
 *
 * Grafiğin tamamı elle çizilir (Canvas); dışarıdan grafik kütüphanesi
 * alınmaz. Giriş animasyonu her sekme açılmasında bir kez oynar.
 *
 * @param onAyarlar Üst bardaki dişli düğmesinin açtığı ayarlar ekranı.
 */
@Composable
fun IstatistikEkrani(
    viewModel: OdevViewModel,
    onAyarlar: () -> Unit,
) {
    val odevler by viewModel.tumOdevler.collectAsStateWithLifecycle()

    // Ekran her açıldığında taze okunur; `remember` anahtarsız olduğu için
    // sekmeler arasında gidip gelindiğinde yeniden hesaplanır, açık kalırken
    // her karede değişmez.
    val simdi = remember { System.currentTimeMillis() }
    val giris = girisAnimasyonu()

    val ozet = remember(odevler, simdi) { odevler.istatistikHesapla(simdi) }
    val dilimler = remember(odevler, simdi) { odevler.durumDilimleri(simdi) }
    val dersDagilim = remember(odevler, simdi) { odevler.dersDagilimi(simdi) }
    val hafta = remember(odevler, simdi) { odevler.sonGunlerinTamamlanmasi(simdi) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.istatistik_baslik)) },
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
        if (odevler.isEmpty()) {
            BosDurum(Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HalkaKarti(ozet, dilimler, giris)
                SayilarIzgarasi(ozet)
                SonYediGunKarti(hafta, giris)
                DersKarti(dersDagilim)
            }
        }
    }
}

/**
 * Sekme açılınca 0'dan 1'e büyüyen çarpan.
 *
 * `animateFloatAsState` hedefi ilk karede zaten hedefte başladığı için burada
 * [Animatable] kullanılır: etki yalnızca **bir kez** koşar ve grafikler
 * soldan büyüyerek belirir. Değer döndüğü için okunması kompozisyonu
 * izler; animasyon bitince sabit kalır.
 */
@Composable
private fun girisAnimasyonu(): Float {
    val hedef = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        hedef.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    return hedef.value
}

/** Sayfayı oluşturan kartın ortak görünümü. */
@Composable
private fun Kart(content: @Composable ColumnScope.() -> Unit) {
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            content = content,
        )
    }
}

// ---- Halka (pasta) grafiği ----

/**
 * Duruma göre dağılım kartı: halka + göstergeler.
 *
 * Sıfır adetli dilim **çizilmez** ama gösterge satırı durur — üç durum da
 * ekranda görünür, kullanıcının "Gecikti 0"yı araması gerekmez.
 */
@Composable
private fun HalkaKarti(
    ozet: Istatistik,
    dilimler: List<Dilim>,
    giris: Float,
) {
    val renkler = MaterialTheme.colorScheme

    Kart {
        Text(
            text = stringResource(R.string.istatistik_dagilim),
            style = MaterialTheme.typography.titleSmall,
            color = renkler.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HalkaGrafik(
                dilimler = dilimler.filter { it.adet > 0 },
                toplam = ozet.toplam,
                oran = ozet.tamamlamaOrani,
                giris = giris,
                modifier = Modifier.size(138.dp),
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                dilimler.forEachIndexed { i, dilim ->
                    if (i > 0) Spacer(Modifier.height(9.dp))
                    DilimSatiri(dilim, ozet.toplam)
                }
            }
        }
    }
}

/** Gösterge satırı: renk noktası, durum adı ve `adet · %pay`. */
@Composable
private fun DilimSatiri(dilim: Dilim, toplam: Int) {
    val renkler = MaterialTheme.colorScheme
    val yuzde = if (toplam == 0) 0 else (dilim.adet * 100f / toplam).roundToInt()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(durumRengi(dilim.durum), CircleShape),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = durumMetni(dilim.durum),
            style = MaterialTheme.typography.bodyMedium,
            color = renkler.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = stringResource(R.string.istatistik_dilim, dilim.adet, yuzde),
            style = MaterialTheme.typography.bodyMedium,
            color = renkler.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Üç dilimli halka; merkezde tamamlama oranı.
 *
 * Dilimler 12 saat yönünden başlar ve aralarına sabit açılık bırakılır —
 * dilim çok küçükse (`< 2 × açılık`) boşluk düşülmez, yoksa çubuk negative
 * sweep ile çizilir ve grafikte ters bir dilim belirirdi. [giris] her dilimin
 * yayını 0'dan uzatır; başlangıç açıları sabit kaldığı için parçalar
 * yerinden oynamaz, yalnızca uzar.
 */
@Composable
private fun HalkaGrafik(
    dilimler: List<Dilim>,
    toplam: Int,
    oran: Float,
    giris: Float,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme
    // `durumRengi` composable; drawScope içinde çağrılamadığı için önceden alınır.
    val dilimRenkleri = dilimler.associate { it.durum to durumRengi(it.durum) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val kalinlik = 17.dp.toPx()
            val yariCap = (size.minDimension - kalinlik) / 2f
            val alan = Size(yariCap * 2f, yariCap * 2f)
            val bas = Offset(
                x = (size.width - alan.width) / 2f,
                y = (size.height - alan.height) / 2f,
            )

            // Dilimlerin altındaki sessiz halka: kayıt olsun ya da olmasın
            // çember tamamlanır, eksik dilim "veri kaybı" izlenimi vermez.
            drawArc(
                color = renkler.outlineVariant,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = bas,
                size = alan,
                style = Stroke(kalinlik, cap = StrokeCap.Butt),
            )

            val bosluk = 3f
            var aci = -90f
            dilimler.forEach { dilim ->
                val tam = 360f * dilim.adet / toplam
                val eksilt = if (tam > bosluk * 2f) bosluk else 0f
                drawArc(
                    color = dilimRenkleri.getValue(dilim.durum),
                    startAngle = aci + eksilt / 2f,
                    sweepAngle = max(0f, (tam - eksilt) * giris),
                    useCenter = false,
                    topLeft = bas,
                    size = alan,
                    style = Stroke(kalinlik, cap = StrokeCap.Butt),
                )
                aci += tam
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(
                    R.string.istatistik_yuzde,
                    (oran * 100f).roundToInt(),
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.istatistik_merkez_etiket),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Sayı taşları ----

/** Toplam / tamamlanan / geciken / bekleyen — dördü toplamı verir. */
@Composable
private fun SayilarIzgarasi(ozet: Istatistik) {
    val renkler = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SayiTasi(
                deger = ozet.toplam,
                etiket = stringResource(R.string.istatistik_toplam),
                ikon = Icons.Rounded.Folder,
                renk = renkler.primary,
                modifier = Modifier.weight(1f),
            )
            SayiTasi(
                deger = ozet.tamamlanan,
                etiket = stringResource(R.string.durum_tamamlandi),
                ikon = Icons.Rounded.CheckCircle,
                renk = renkler.primary,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SayiTasi(
                deger = ozet.geciken,
                etiket = stringResource(R.string.durum_gecikti),
                ikon = Icons.Rounded.ErrorOutline,
                renk = renkler.error,
                modifier = Modifier.weight(1f),
            )
            SayiTasi(
                deger = ozet.bekleyen,
                etiket = stringResource(R.string.durum_bekliyor),
                ikon = Icons.Rounded.Schedule,
                renk = renkler.secondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Tek bir sayı taşı: yuvarlak ikon + sayı + etiket. */
@Composable
private fun SayiTasi(
    deger: Int,
    etiket: String,
    ikon: ImageVector,
    renk: Color,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = renkler.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(renk.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ikon,
                    contentDescription = null,
                    tint = renk,
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = deger.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = renkler.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = etiket,
                    style = MaterialTheme.typography.bodySmall,
                    color = renkler.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---- Son 7 gün ----

/**
 * Son yedi günün tamamlanan ödevleri, günlük sütunlar hâlinde.
 *
 * Sütun yüksekliği en büyük güne oranlanır; ay etiketi ve sayı ayrı sabit
 * yükseklikte yuvalarda durur, böylece **günler dikeyde hizalanır** — yoksa
 * "0" yazmayan sütunun etiketi yukarı kayar ve eksen kırılırdı.
 * Hiç tamamlama yoksa grafik yerine tek satırlık açıklama çizilir.
 */
@Composable
private fun SonYediGunKarti(hafta: List<GunlukSayim>, giris: Float) {
    val renkler = MaterialTheme.colorScheme
    val enBuyuk = hafta.maxOfOrNull { it.adet } ?: 0

    Kart {
        Text(
            text = stringResource(R.string.istatistik_hafta),
            style = MaterialTheme.typography.titleSmall,
            color = renkler.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        if (enBuyuk == 0) {
            Text(
                text = stringResource(R.string.istatistik_hafta_bos),
                style = MaterialTheme.typography.bodyMedium,
                color = renkler.outline,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                hafta.forEach { gunSayim ->
                    val oran = gunSayim.adet.toFloat() / enBuyuk
                    val yukseklik = 72.dp * oran * giris

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Sabit yuva: "0" yazan günde de satır kaymasın.
                        Box(
                            modifier = Modifier.height(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (gunSayim.adet > 0) {
                                Text(
                                    text = gunSayim.adet.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = renkler.primary,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(72.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(yukseklik)
                                    .background(
                                        color = renkler.primary,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                        ),
                                    ),
                            )
                        }

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = gunSayim.gun.gunKisaAdi(),
                            style = MaterialTheme.typography.labelSmall,
                            color = renkler.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ---- Derslere göre ----

/**
 * Ders başına yığılı çubuklar: tamamlanan + geciken + bekleyen.
 *
 * Çubuğun **uzunluğu** o dersteki toplam ödevi, **bölüm renkleri** ise
 * pastadaki üç durumu gösterir; bu yüzden renk sözlüğü ayrı bir yerde
 * tutulmaz, aynı [durumRengi] kullanılır. Çubuk genişliği en kalabalık
 * derse oranlanır — dersler arasında hacim karşılaştırılabilir.
 */
@Composable
private fun DersKarti(liste: List<DersDagilimi>) {
    val renkler = MaterialTheme.colorScheme
    val enBuyuk = liste.maxOfOrNull { it.toplam } ?: 1

    Kart {
        Text(
            text = stringResource(R.string.istatistik_dersler),
            style = MaterialTheme.typography.titleSmall,
            color = renkler.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        MiniGosterge()
        Spacer(Modifier.height(14.dp))

        liste.forEachIndexed { i, ders ->
            if (i > 0) Spacer(Modifier.height(13.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (ders.ders.isBlank()) {
                        stringResource(R.string.ders_secilmedi)
                    } else {
                        ders.ders
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = renkler.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(
                        R.string.istatistik_ders_sayi,
                        ders.tamamlanan,
                        ders.toplam,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = renkler.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.height(6.dp))

            YigiliCubuk(ders, enBuyuk, renkler)
        }
    }
}

/**
 * Çubuk kartının üstündeki küçük renk sözlüğü.
 *
 * Pasta ile çubukların aynı renkleri kullandığı ayrı bir yerde yazılı
 * durmazsa, kullanıcı iki grafiği birbirine bağlamak zorunda kalırdı.
 */
@Composable
private fun MiniGosterge() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GostergeSatiri(R.string.durum_tamamlandi, MaterialTheme.colorScheme.primary)
        GostergeSatiri(R.string.durum_gecikti, MaterialTheme.colorScheme.error)
        GostergeSatiri(R.string.durum_bekliyor, MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun GostergeSatiri(@StringRes etiket: Int, renk: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(renk, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(etiket),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Tek bir dersin çubuğu.
 *
 * Arka plandaki soluk şerit **en kalabalık dersi** temsil eder; renkli
 * kısım o dersin yerini gösterir. Yuvarlak köşe yalnızca dış kaplamada
 * olduğundan dilimler kenarda kesilir, ortada dik durur.
 */
@Composable
private fun YigiliCubuk(
    ders: DersDagilimi,
    enBuyuk: Int,
    renkler: ColorScheme,
) {
    val dilimRenkleri = listOf(
        ders.tamamlanan to renkler.primary,
        ders.geciken to renkler.error,
        ders.bekleyen to renkler.secondary,
    ).filter { it.first > 0 }

    // `clip` şart: yalnızca `background` zemini yuvarlar, içteki dilimler
    // köşelerin üstüne kare kare taşardı.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(renkler.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(if (enBuyuk == 0) 0f else ders.toplam.toFloat() / enBuyuk)
                .height(14.dp),
        ) {
            dilimRenkleri.forEach { (adet, renk) ->
                Box(
                    modifier = Modifier
                        .weight(adet.toFloat())
                        .height(14.dp)
                        .background(renk),
                )
            }
        }
    }
}

// ---- Boş durum ----

@Composable
private fun BosDurum(modifier: Modifier = Modifier) {
    val renkler = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.PieChart,
            contentDescription = null,
            tint = renkler.outline,
            modifier = Modifier.size(56.dp),
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.istatistik_bos),
            style = MaterialTheme.typography.bodyMedium,
            color = renkler.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
