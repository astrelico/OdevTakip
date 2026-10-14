@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.rutin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.RutinDonemOzeti
import com.odevtakip.app.data.RutinGunOzeti
import com.odevtakip.app.data.RutinGorunumu
import com.odevtakip.app.data.RutinKaydi
import com.odevtakip.app.data.baloncukOrani
import com.odevtakip.app.data.haftaninGunleri
import com.odevtakip.app.data.haftaninSonGunu
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.UstBar
import com.odevtakip.app.util.ayAdiVeYili
import com.odevtakip.app.util.ayinHucreleri
import com.odevtakip.app.util.gunKisaAdi
import com.odevtakip.app.util.tarihMetni
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate
import java.time.YearMonth

/**
 * Ders rutini ekranı.
 *
 * Kullanıcının her gün hangi dersten kaç soru çözdüğünü tuttuğu yer. İki
 * biçim vardır ve ikisi de **aynı veriyi** gösterir — yalnızca bakış
 * açısı değişir:
 *
 *  - **Haftalık** ([RutinGorunumu.HAFTA]): seçili haftanın yedi günü, her
 *    günü bir kart. Kartta o günün ders dökümü ve toplam baloncuğu durur.
 *  - **Aylık** ([RutinGorunumu.AY]): ay ızgarası; çözülen soruyla büyüyen
 *    baloncuklar, altında seçili günün kartı.
 *
 * Seçili gün **iki biçimde ortaktır**: birinde seçilen gün ötekinde de
 * seçili durur ([OdevViewModel.rutinGununuAyarla] bunu güvence altına
 * alır). Kullanıcı biçim değiştirdiğinde aynı günün dökümüne döner.
 *
 * Ekleme ve düzenleme **tek yerdedir**: gün kartındaki ders satırı ya da
 * "+" satırı alttan açılan [RutinGirisiPaneli]'ni çağırır. Ayrı bir form
 * sayfası yoktur — günde bir-iki satır girmek için ekran değiştirmek
 * ağır olurdu.
 *
 * @param onGeri Üst bardaki geri düğmesi; menüye döner.
 */
@Composable
fun RutinEkrani(
    viewModel: OdevViewModel,
    onGeri: () -> Unit,
) {
    val gorunum by viewModel.rutinGorunumu.collectAsStateWithLifecycle()
    val seciliGun by viewModel.rutinGunu.collectAsStateWithLifecycle()
    val seciliAy by viewModel.rutinAyi.collectAsStateWithLifecycle()
    val durum by viewModel.rutinDonemi.collectAsStateWithLifecycle()
    val dersler by viewModel.dersler.collectAsStateWithLifecycle()

    // Panel, bir **amaç** ile açılır: hangi güne, hangi kayda. İkisi de
    // ekranın durumudur — biçim değişiminde ya da geri dönüşte kaybolmaz.
    var panelAcik by remember { mutableStateOf(false) }
    var panelTarihi by remember { mutableStateOf(LocalDate.now()) }
    var panelKaydi by remember { mutableStateOf<RutinKaydi?>(null) }

    fun paneliAc(tarih: LocalDate, kayit: RutinKaydi?) {
        panelTarihi = tarih
        panelKaydi = kayit
        panelAcik = true
    }

    // Kayıtlar güne göre gruplanır; hafta ve ay aynı haritadan okur, böylece
    // iki biçim hiçbir zaman birbirinden farklı toplam göstermez.
    val gunlereGore = remember(durum.kayitlar) {
        durum.kayitlar.groupBy { it.tarih.yerelTarih() }
    }

    // Baloncukların ölçek tabanı: dönemin en yoğun günü. [RutinHesap] içindeki
    // karekök ölçütü bunun üzerinden çalışır.
    val enBuyuk = durum.ozet.gunOzetleri.values.maxOfOrNull { it.soru } ?: 0

    Scaffold(
        topBar = {
            UstBar(
                baslik = { Text(stringResource(R.string.rutin_baslik)) },
                gezinmeIkonu = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
                        )
                    }
                },
                eylemler = {
                    // Biçim anahtarı: etiket her zaman **hedefi** söyler —
                    // Takvim ekranının düğmesiyle aynı kural. Aksi hâlde
                    // kullanıcı "Haftalık"ı okuyup haftalığa basar.
                    TextButton(
                        onClick = { viewModel.rutinGorunumunuAyarla(gorunum.hedefi()) },
                    ) {
                        Text(stringResource(gorunum.dugmeEtiketRes))
                    }
                    TextButton(onClick = { viewModel.rutinBugununuAyarla() }) {
                        Text(stringResource(R.string.bugun))
                    }
                },
            )
        },
    ) { innerPadding ->
        // Kayıtlı ders yoksa hiçbir şey eklenemez; bu, ekranın tek
        // **engelleyici** boş durumudur. Ders listesi uygulama tarafından
        // varsayılanlarla dolu geldiği için bu hâl, kullanıcı derslerin
        // tamamını sildiğinde görülür.
        if (dersler.isEmpty()) {
            BosDurum(
                metin = stringResource(R.string.rutin_ders_yok),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DonemBasligi(
                gorunum = gorunum,
                seciliGun = seciliGun,
                seciliAy = seciliAy,
                ozet = durum.ozet,
                onKaydir = viewModel::rutiniKaydir,
            )

            when (gorunum) {
                RutinGorunumu.HAFTA -> HaftaKartlari(
                    hafta = seciliGun,
                    gunlereGore = gunlereGore,
                    enBuyuk = enBuyuk,
                    onDersEkle = { paneliAc(it, null) },
                    onDersDuzenle = { paneliAc(it.tarih.yerelTarih(), it) },
                )

                RutinGorunumu.AY -> AylikGorunum(
                    ay = seciliAy,
                    seciliGun = seciliGun,
                    gunOzetleri = durum.ozet.gunOzetleri,
                    gunlereGore = gunlereGore,
                    enBuyuk = enBuyuk,
                    onGunSec = viewModel::rutinGununuAyarla,
                    onDersEkle = { paneliAc(it, null) },
                    onDersDuzenle = { paneliAc(it.tarih.yerelTarih(), it) },
                )
            }
        }
    }

    if (panelAcik) {
        RutinGirisiPaneli(
            tarih = panelTarihi,
            mevcut = panelKaydi,
            dersler = dersler.map { it.ad },
            onKaydet = { kayit ->
                viewModel.rutinKaydiniYaz(kayit) { panelAcik = false }
            },
            onSil = { id ->
                viewModel.rutinKaydiniSil(id) { panelAcik = false }
            },
            onKapat = { panelAcik = false },
        )
    }
}

// ---- Dönem başlığı ----

/**
 * Dönemin üst satırı: oklar + dönem adı, altında toplam özeti.
 *
 * Oklar her iki biçimde de aynı işi yapar — haftada ±1 hafta, ayda ±1 ay
 * kaydırır ([OdevViewModel.rutiniKaydir]). Özeti ayrı bir karta
 * koymadım: dönem toplamı **bağlamdır**, gün kartları ise içerik. Aynı
 * satırda durması, kullanıcının "bu hafta toplam ne çözdüm" sorusunu
 * karta bakmadan yanıtlamasını sağlar.
 */
@Composable
private fun DonemBasligi(
    gorunum: RutinGorunumu,
    seciliGun: LocalDate,
    seciliAy: YearMonth,
    ozet: RutinDonemOzeti,
    onKaydir: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onKaydir(-1) }) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.rutin_onceki_donem),
                )
            }
            Text(
                text = when (gorunum) {
                    RutinGorunumu.HAFTA -> haftaMetni(seciliGun)
                    RutinGorunumu.AY -> seciliAy.atDay(1).ayAdiVeYili()
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onKaydir(1) }) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.rutin_sonraki_donem),
                )
            }
        }

        Text(
            text = if (ozet.toplamSoru == 0) {
                stringResource(R.string.rutin_donem_ozet_bos)
            } else {
                stringResource(R.string.rutin_donem_ozet, ozet.calisilanGun, ozet.toplamSoru)
            },
            style = MaterialTheme.typography.labelLarge,
            color = renkler.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 2.dp),
        )

        if (ozet.toplamSoru > 0) {
            Text(
                text = stringResource(
                    R.string.rutin_dogru_yanlis,
                    ozet.toplamDogru,
                    ozet.toplamYanlis,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = renkler.outline,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

/**
 * Haftanın adı.
 *
 * Ay içindeyse yalnız günler yazılır ("13 – 19 Ekim 2026"); ay
 * sınırını aşıyorsa iki tarih de tam yazılır ("28 Ekim – 3 Kasım
 * 2026"). Kısaltılmış biçim, haftanın **tek** ayda olduğu %75'lik
 * durumda satırın satırda kalmasını sağlar.
 */
private fun haftaMetni(hafta: LocalDate): String {
    val ilk = haftaninSonGunu(hafta).minusDays(6)
    val son = haftaninSonGunu(hafta)
    return if (ilk.month == son.month && ilk.year == son.year) {
        "${ilk.dayOfMonth} – ${son.dayOfMonth} ${son.ayAdiVeYili()}"
    } else {
        "${ilk.tarihMetni()} – ${son.tarihMetni()}"
    }
}

// ---- Haftalık biçim ----

/**
 * Haftalık biçim: yedi gün kartı, yukarıdan aşağı.
 *
 * Kartlar **LazyColumn** ile çizilir. Boş günler de karttır ve "Bu gün için
 * kayıt yok" der — listenin yapısı sabit kalır, kullanıcı haftanın hangi
 * gününde olduğunu kaydırırken kaybetmez. Her kart kendi "+" satırını
 * taşır: ekleme eylemi tek bir merkezî düğmeye bağlı değildir.
 */
@Composable
private fun HaftaKartlari(
    hafta: LocalDate,
    gunlereGore: Map<LocalDate, List<RutinKaydi>>,
    enBuyuk: Int,
    onDersEkle: (LocalDate) -> Unit,
    onDersDuzenle: (RutinKaydi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gunler = haftaninGunleri(hafta)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
    ) {
        items(gunler, key = { it.toEpochDay() }) { gun ->
            RutinKarti(
                gun = gun,
                kayitlar = gunlereGore[gun].orEmpty(),
                enBuyukGunSoru = enBuyuk,
                onDersEkle = { onDersEkle(gun) },
                onDersDuzenle = onDersDuzenle,
            )
        }
    }
}

// ---- Aylık biçim ----

/**
 * Aylık biçim: ay ızgarası, altında seçili günün kartı.
 *
 * Izgara **baloncuk** haritasıdır: çözülen soru arttıkça baloncuk büyür ve
 * koyulaşır. Kullanıcı böylece "hangi gün çok çalıştım" sorusunu tek
 * bakışta yanıtlar, sonra alttaki kartta o günün dökümünü okur.
 *
 * Izgara kaydırma alanının **dışında** tutulur ve kart LazyColumn'da
 * değil, sütunun içinde durur: içerik her zaman izgara + tek kart
 * yüksekliğindedir, ekran kaydırması gereksiz yere uzamaz.
 */
@Composable
private fun AylikGorunum(
    ay: YearMonth,
    seciliGun: LocalDate,
    gunOzetleri: Map<LocalDate, RutinGunOzeti>,
    gunlereGore: Map<LocalDate, List<RutinKaydi>>,
    enBuyuk: Int,
    onGunSec: (LocalDate) -> Unit,
    onDersEkle: (LocalDate) -> Unit,
    onDersDuzenle: (RutinKaydi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AylikIzgara(
            ay = ay,
            secili = seciliGun,
            gunOzetleri = gunOzetleri,
            enBuyuk = enBuyuk,
            onGunSec = onGunSec,
        )

        RutinKarti(
            gun = seciliGun,
            kayitlar = gunlereGore[seciliGun].orEmpty(),
            enBuyukGunSoru = enBuyuk,
            onDersEkle = { onDersEkle(seciliGun) },
            onDersDuzenle = onDersDuzenle,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

/**
 * Aylık ızgara: hafta günleri başlıkları + 5–6 satır gün hücresi.
 *
 * Hücre hesabı saf [ayinHucreleri] içinde ve testlidir; bu fonksiyonun işi
 * yalnızca çizimdir. Baloncuk **hücrenin altında** durur ve [enBuyuk]
 * günün oranıyla ölçeklenir — ızgaradaki baloncuklar karttakinden küçük
 * kalır, aksi hâlde bir hücre komşusunu örterdi.
 */
@Composable
private fun AylikIzgara(
    ay: YearMonth,
    secili: LocalDate,
    gunOzetleri: Map<LocalDate, RutinGunOzeti>,
    enBuyuk: Int,
    onGunSec: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme
    val hucreler = remember(ay) { ayinHucreleri(ay) }
    val bugun = LocalDate.now()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            haftaBasliklari.forEach { ad ->
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
                        // Ayın dışında kalan kare: hafta gününü korumak
                        // için boş bırakılır, çizilmez.
                        Spacer(Modifier.weight(1f).height(IZGARA_HUCRE))
                    } else {
                        IzgaraHucre(
                            gun = gun,
                            secili = gun == secili,
                            bugun = gun == bugun,
                            ozet = gunOzetleri[gun],
                            enBuyuk = enBuyuk,
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
 * Izgaradaki tek gün hücresi: gün numarası + altında o günün baloncuğu.
 *
 * Seçili gün dolu `primary` zeminle kaplanır; baloncuk o durumda zemine
 * göre ters çevrilir ki seçili günün toplamı da okunaklı kalsın.
 * Kaydı olmayan günde baloncuk **çizilmez** — soluk bir daire, "burada
 * sıfır var" der; hiçbir şey çizmemek ise "buraya bakmadım" demektir.
 *
 * **Bugün**, seçili olmasa bile ince bir halka ile belirginleşir: ızgarada
 * onlarca sayı durur ve yalnızca kalın yazı, kalabalıkta kaybolur. Halka
 * `primary`'in ince bir çizgisidir; seçili günün dolu zeminiyle
 * çakışmaz, çünkü seçiliyken gereksizleşir.
 *
 * Baloncuk rengi, haftalık karttakiyle **aynı skaladan** geçer (bkz.
 * [com.odevtakip.app.ui.rutin.RutinKarti]): iki görünüm aynı veriyi
 * gösterdiği için aynı koyuluk diliyle konuşur.
 */
@Composable
private fun IzgaraHucre(
    gun: LocalDate,
    secili: Boolean,
    bugun: Boolean,
    ozet: RutinGunOzeti?,
    enBuyuk: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme
    val soru = ozet?.soru ?: 0
    val oran = if (soru > 0) {
        baloncukOrani(soru, enBuyuk)
    } else {
        0f
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(IZGARA_HUCRE),
        shape = RoundedCornerShape(12.dp),
        color = if (secili) renkler.primary else Color.Transparent,
        // Bugünün halkası: yalnız seçili değilken çizilir. Seçili hücre
        // zaten dolu mavi olduğu için halka orada okunmazdı.
        border = if (bugun && !secili) {
            BorderStroke(1.dp, renkler.primary.copy(alpha = 0.55f))
        } else {
            null
        },
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (bugun) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    secili -> renkler.onPrimary
                    bugun -> renkler.primary
                    else -> renkler.onSurface
                },
            )

            if (soru > 0) {
                Spacer(Modifier.size(2.dp))
                Box(
                    modifier = Modifier
                        .size(IZGARA_BALONCUK_EN_KUCUK +
                            (IZGARA_BALONCUK_EN_BUYUK - IZGARA_BALONCUK_EN_KUCUK) * oran)
                        .clip(CircleShape)
                        .background(
                            // Aynı skala, daha geniş alfa: ızgaradaki
                            // baloncuk karttakinden küçük olduğu için
                            // koyuluk farkı daha da belirleyicidir.
                            if (secili) {
                                renkler.onPrimary.copy(alpha = 0.35f + 0.35f * oran)
                            } else {
                                renkler.primary.copy(alpha = 0.18f + 0.77f * oran)
                            }
                        ),
                )
            }
        }
    }
}

/**
 * Izgaranın hafta günleri: Pzt … Paz.
 *
 * Takvim ekranıyla **aynı** üretim yolu kullanılır: sabit bir
 * pazartesiden üretilir, cihazın "haftanın ilk günü" ayarına bağlı
 * değildir. Başlıklar ızgaranın kendisiyle aynı sırada durduğu için her
 * ay sütun düzeni korunur.
 */
private val haftaBasliklari: List<String> = run {
    val pazartesi = LocalDate.of(2024, 1, 1)
    List(7) { pazartesi.plusDays(it.toLong()).gunKisaAdi() }
}

// Izgara hücre yüksekliği ve baloncuk uçları: hücre satırı 44 dp'de kalır
// ki altı satırlı ızgara ekranın yarısını geçmesin; baloncuk 8–20 dp
// aralığında büyür, komşu hücreyi örtemez.
private val IZGARA_HUCRE = 44.dp
private val IZGARA_BALONCUK_EN_KUCUK = 8.dp
private val IZGARA_BALONCUK_EN_BUYUK = 20.dp

// ---- Boş durum ----

/**
 * Hiç kayıtlı ders kalmayınca görülen ekran.
 *
 * [com.odevtakip.app.ui.deneme.DenemeListeEkrani] ile aynı çizim: ikon
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
                imageVector = Icons.Rounded.Add,
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
