@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
 *  - **Üst kart sonuçtur**: net ve puan yan yana. Ödev detayındaki
 *    "durum + teslim" kartının yerini denemenin tek gerçeği, net, alır.
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
 * Sıra bilinçlidir: kullanıcı önce **sonuca** bakar (net/puan kartı),
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

        HorizontalDivider()

        // Açıklama
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (deneme.aciklama.isBlank()) {
                Text(
                    text = stringResource(R.string.aciklama_yok),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = deneme.aciklama,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

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
 * Sonuç kartı: solda net, sağda puan; altında doğru/yanlış satırı.
 *
 * Net **birincil renkte ve büyük** yazılır — deneme ekranının tek
 * "kahraman" sayısıdır. Puan, okul puanı olduğu için ayrı durur ve
 * girilmemişse `—` ile boş bırakılır: "Puan girilmedi" yazısı, kullanıcının
 * zaten bildiği bir yokluğu tekrar anlatır.
 *
 * Doğru/yanlış ikonlu satır, sayının **nereden** geldiğini gösterir;
 * net formülünü bilmeyen kullanıcı da sonucu doğrular.
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

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = stringResource(R.string.deneme_puan_etiket),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (deneme.puan != null) {
                            sayiMetni(deneme.puan)
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (deneme.puan != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

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
 */
@Composable
private fun DagilimSatiri(
    etiket: String,
    deger: String,
    vurgu: Boolean = false,
    altDal: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (altDal) 32.dp else 16.dp,
                end = 16.dp,
                top = if (altDal) 4.dp else 10.dp,
                bottom = if (altDal) 4.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
