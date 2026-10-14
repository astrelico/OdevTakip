@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatQuote
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
import com.odevtakip.app.data.DenemeTuru
import com.odevtakip.app.data.kotaAltSonuclari
import com.odevtakip.app.data.kotaSonucu
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
 *  - **Üst kart sonuçtur**: yalnızca net, büyük ve birincil renkte. Ödev
 *    detayındaki "durum + teslim" kartının yerini denemenin tek gerçeği,
 *    net, alır. Puan satırı yoktur — bkz. [Deneme] (sadece sorular ve
 *    netler kaydedilir).
 *  - **Ders dağılımı kartı** durur: her ders için doğru/yanlış/boş ve
 *    en altta "Toplam 92 / 120 soru". Formdaki satırların kayda
 *    dönüşmüş hâlidir — hangi derste ne kaldığını tek yerde okumak,
 *    yeniden formu açmaktan kısadır.
 *  - **Tamamlama düğmesi yoktur**: deneme bitmiştir; geri alınacak bir
 *    işaret yoktur (bkz. [Deneme]).
 *
 * Üst barda üç düğme durur: geri, **Düzenle** (formu açar) ve **Sil**
 * (onay diyaloğuyla). Sıralama bilinçlidir: geri solda, eylemler sağda;
 * sil kırmızıdır çünkü geri alınamazdır — düzenlemek ise günlük iştir.
 * Düzenleme asla **türü değiştirmez**: form, kaydın türüyle açılır.
 *
 * Ekran [OdevViewModel.seciliDeneme] akışını izler; kayıt başka bir yerde
 * silinirse kendiliğinden "bulunamadı" durumuna düşer.
 *
 * @param denemeId Gösterilecek kaydın kimliği.
 * @param onDuzenle Üst bardaki Düzenle; form sayfasını açar. Tür de
 *   birlikte verilir çünkü formun kapağı o türle açılır.
 * @param onGeri Üst bardaki geri düğmesi; listeye döner.
 */
@Composable
fun DenemeDetayEkrani(
    denemeId: Long,
    viewModel: OdevViewModel,
    onDuzenle: (Long, DenemeTuru) -> Unit,
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
                        IconButton(onClick = { onDuzenle(denemeId, deneme!!.tur) }) {
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
 * Sıra bilinçlidir: kullanıcı önce **sonuca** bakar (net kartı),
 * sonra **kime ait** olduğuna (ad, tür, ders dağılımı), en sonda künyeye
 * (tarih, eklenme) iner. Açıklama arada durur — not, sayının hemen
 * altında okunmak ister.
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

        // Tür: "TYT · 120 soru" — denemenin kaderi, formun kapağı.
        Text(
            text = stringResource(
                R.string.deneme_tur_ozet,
                turEtiketi(deneme.tur),
                deneme.tur.toplamSoru,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        // Formdaki satırların kayda dönüşmüş hâli.
        DagilimKarti(deneme = deneme)

        // Açıklama, **kart** olarak çizilir: tırnak ikonu, notun sayı
        // bloklarından farklı bir şey olduğunu — kullanıcının kendi
        // cümlesi olduğunu — söyler. Düz metin olarak kalsaydı künye
        // satırlarıyla aynı ağırlıkta dururdu ve göz kaydırırken
        // "burası benim yazdığım" diye durmazdı.
        AciklamaKarti(aciklama = deneme.aciklama)

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
 * Açıklama kartı: kullanıcının kendi cümlesi.
 *
 * Not, sayılardan **farklı bir nesnedir** ve öyle görünmelidir: sayılar
 * tablodur, not ise günlüktür. Kart bu yüzden iki ayrı dille çizilir —
 *
 *  - Solunda tırnak ikonu durur (`FormatQuote`): metnin alıntı değil,
 *    **kullanıcının kendi ifadesi** olduğunu söyler. İkon `primary`
 *    rengindedir çünkü not, denemenin sayısız sonucu arasından
 *    ayıklanmış tek insani işarettir.
 *  - Gövde, tablo satırlarının aksine **serbest metin**tir: satır
 *    uzunluğu değişir, çok satırlı olabilir. `bodyLarge` kullanılır —
 *    sayılar `bodyMedium`/`labelMedium` okunduğu için not, bir kademe
 *    büyük kalır ve gözün "burası farklı" demesi sağlanır.
 *
 * Not yoksa kart **çizilmez**, yalnız bir satır metin görünür. Boş bir
 * kart, "buraya bir şey yazmamışsın" der; bu, kullanıcının hatası değil,
 * kaydın durumudur — suçlayıcı değil bilgilendirici olmalıdır.
 *
 * @param aciklama Kaydın notu; boş olabilir.
 */
@Composable
private fun AciklamaKarti(aciklama: String, modifier: Modifier = Modifier) {
    if (aciklama.isBlank()) {
        Text(
            text = stringResource(R.string.aciklama_yok),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = kartCizgisi(),
        elevation = kartElevasyonu(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Rounded.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = aciklama,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Sonuç kartı: üstte net, altında doğru/yanlış satırı.
 *
 * Net **birincil renkte ve büyük** yazılır — deneme ekranının tek
 * "kahraman" sayısıdır. Puan alanı bilinçli olarak **yoktur**: kaydın
 * özü sorular ve netlerdir, kullanıcıdan gizlice eklenen bir türev
 * değer okunacak bir şey katmaz.
 *
 * Doğru/yanlış ikonlu satır, sayının **nereden** geldiğini gösterir;
 * net formülünü bilmeyen kullanıcı da sonucu doğrular. Onların üstünde
 * duran [OranSeridi] ise aynı üç sayıyı **orantısal** olarak çizer —
 * metin "kaç tane", şerit "ne kadar temiz" der.
 *
 * Şerit burada **boş soruları da** taşır: detay ekranının işi ayrıntıdır
 * ve "kaç soruya hiç dokunmadım" sorusunun yanıtı yalnız burada verilir.
 * (Liste kartında şerit yalnız doğru/yanlış üzerinden ölçeklenir —
 * bkz. [DenemeKarti].)
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
            }

            // Oran şeridi: netin altında, sayının **orantısı**. Kartın
            // kenarına dayanmaz — 16 dp içeride durur ki çerçevenin alt
            // çizgisi gibi okunmasın, ayrı bir nesne olduğu belli olsun.
            //
            // Boş sayısı **kayda yazılmaz**, kota ile türetilir: soru
            // sayısı sabittir (bkz. [DenemeTuru.toplamSoru]), geriye
            // kalan boş olur. Elle düzenlenmiş bir kayıt kotaları
            // aşabildiği için sayı `0`da da düşmez.
            OranSeridi(
                dogru = deneme.dogru,
                yanlis = deneme.yanlis,
                bos = (deneme.tur.toplamSoru - deneme.dogru - deneme.yanlis)
                    .coerceAtLeast(0),
                enKucukAgirlik = 0.015f,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

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

// ---- Ders dağılımı ----

/**
 * Ders bazlı sonuç kartı: her satır bir ders, en altta toplam.
 *
 * Satırlar [Deneme.tur]'un **kendi sırasıyla** çizilir; kaydın o derste
 * sonucu yoksa `0` görünür. "Bu derste soru çözülmemiş" ile "sonuç
 * kaybolmuş" aynı şey değildir — birincisi sayıyla yazılır, ikincisi
 * satır hiç çizilmezdi.
 *
 * **Okuma hiyerarşisi** bilinçlidir, kullanıcı istediği budur: en belirgin
 * satır **ana dallar**dır (Sosyal, Fen — toplamlarıyla), onların altında
 * **alt testler** (Tarih, Fizik …) girintili, soluk ve küçük satırlarda
 * durur. Kullanıcı önce "Sosyal 12/3"ü okur, ayrıntı isterse altındakilere
 * iner; tersi olsaydı dört alt satır ana dalı gölgede bırakırdı.
 *
 * Ana dalın sonucu [kotaSonucu] ile bulunur: kayıt yaprak biçimindeyse alt
 * testler toplanır, eski biçimdeyse Sosyal'ın kendisi okunur. Alt satırlar
 * yalnızca yaprak kaydı **varken** çizilir ([kotaAltSonuclari]) — eski
 * kayıtta alt testlerin sıfırla görünmesi, kaydın eski bütünlüğünü ezerdi.
 *
 * "Boş", kota ile (doğru + yanlış) arasındaki farktır: kullanıcıya
 * sınavdan ne kadar geride kaldığını söyler ve formdaki kilit ile aynı
 * aritmetikten gelir. Elle düzenlenmiş bir kayıt kotaları aşabildiği
 * için sayı `0`da da düşmez.
 */
@Composable
private fun DagilimKarti(deneme: Deneme) {
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
            Text(
                text = stringResource(R.string.deneme_dagilim_baslik),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
            )

            deneme.tur.dersleri.forEach { kota ->
                val sonuc = kotaSonucu(kota, deneme.sonuclar)
                DagilimSatiri(
                    etiket = kota.ders,
                    deger = stringResource(
                        R.string.deneme_sonuc_bos,
                        sonuc.dogru,
                        sonuc.yanlis,
                        (kota.soru - sonuc.dogru - sonuc.yanlis).coerceAtLeast(0),
                    ),
                    // Ana dala kendi oran şeridi çizilir: her dersin
                    // "ne kadar temiz" olduğu metin satırının hemen
                    // altında görsel olarak okunur. Toplam kartın
                    // üstünde zaten global şerit taşıdığı için burada
                    // yalnızca ana dallara çizilir — alt testler ayrı
                    // şerit çizseydi kart görsel gürültüye boğulurdu.
                    seritDogru = sonuc.dogru,
                    seritYanlis = sonuc.yanlis,
                    seritBos = (kota.soru - sonuc.dogru - sonuc.yanlis).coerceAtLeast(0),
                )

                // Alt testler: ana dalın altında soluk satırlar. Sıra
                // kota sırasıdır — formda girilen sırayla aynı okunur.
                kotaAltSonuclari(kota, deneme.sonuclar)?.forEach { alt ->
                    DagilimSatiri(
                        etiket = alt.ders,
                        deger = stringResource(
                            R.string.deneme_sonuc_bos,
                            alt.dogru,
                            alt.yanlis,
                            (kota.altlar.first { it.ders == alt.ders }.soru -
                                alt.dogru - alt.yanlis).coerceAtLeast(0),
                        ),
                        altDal = true,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )

            DagilimSatiri(
                etiket = stringResource(R.string.deneme_toplam),
                deger = stringResource(
                    R.string.deneme_soru_dogrulugu,
                    deneme.dogru + deneme.yanlis,
                    deneme.tur.toplamSoru,
                ),
                vurgu = true,
            )
        }
    }
}

/**
 * Dağılım satırı: solda ders adı, sağda sayılar.
 *
 * Üç kademe — okuma sırası da bu: **toplam** (SemiBold, tam renk),
 * **ana dal** (normal ağırlık, tam renk), **alt dal** (küçük, soluk,
 * girintili). Alt dalın değeri de soluktur: ayrıntı, ana daldan daha
 * sessiz olmalıdır.
 *
 * Ana dallara **oran şeridi** çizilir ([seritDogru] verildiğinde):
 * satırın altına inen 4 dp'lik çizgi, "Sosyal 14/6" metninin
 * orantısını okumadan verir. Alt dallara şerit çizilmez — dört alt
 * testin üst üste şeridi, kartı görsel gürültüye boğar; oran orada
 * sayı olarak yeterlidir.
 *
 * Alt dalın soluna ayrıca **ince bir dikey çizgi** ([androidx.compose.foundation.layout.Box])
 * çekilir: girintinin "hangi ana dala bağlı olduğu" tek bakışta
 * anlaşılır. Çizgi yalnızca alt dalda bulunur; ana dal ona ihtiyaç
 * duymaz, çünkü zaten kartın sol kenarına hizalıdır.
 */
@Composable
private fun DagilimSatiri(
    etiket: String,
    deger: String,
    vurgu: Boolean = false,
    altDal: Boolean = false,
    seritDogru: Int = 0,
    seritYanlis: Int = 0,
    seritBos: Int = 0,
) {
    val cizgiVar = seritDogru + seritYanlis + seritBos > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (altDal) 28.dp else 16.dp,
                end = 16.dp,
                top = if (altDal) 3.dp else 10.dp,
                bottom = if (altDal) 3.dp else if (cizgiVar) 8.dp else 10.dp,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (altDal) {
                // Ana dala bağlayan dikey çizgi: girintinin görsel
                // karşılığı. Çizgi ile metin arasında 10 dp bırakılır
                // ki etiket çizgiye yapışmasın.
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            RoundedCornerShape(1.dp),
                        ),
                )
                Spacer(Modifier.width(10.dp))
            }

            Text(
                text = etiket,
                style = if (altDal) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = when {
                    vurgu || !altDal -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (vurgu) FontWeight.SemiBold else null,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = deger,
                style = if (altDal) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = if (altDal) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (vurgu) FontWeight.SemiBold else null,
            )
        }

        if (cizgiVar) {
            // Şerit, satırın **tam genişliğinde değil**, etiketin
            // başladığı hizada durur: sağdaki sayı sütununu kesmez,
            // iki sütun birbirine karışmaz.
            OranSeridi(
                dogru = seritDogru,
                yanlis = seritYanlis,
                bos = seritBos,
                enKucukAgirlik = 0.015f,
                modifier = Modifier
                    .padding(top = 5.dp)
                    .fillMaxWidth(),
            )
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
