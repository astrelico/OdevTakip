@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R
import com.odevtakip.app.data.DenemeTuru
import com.odevtakip.app.data.DersKotasi
import com.odevtakip.app.data.DersSonucu
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.UstBar
import com.odevtakip.app.ui.kartCizgisi
import com.odevtakip.app.ui.kartElevasyonu
import com.odevtakip.app.util.formatliTarih
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate
import java.time.ZoneId

/** [DatePicker] UTC gecesi: epoch günü, milisaniyeye çevrilmiş hâli. */
private const val GUN_MS = 86_400_000L

/**
 * Deneme ekleme ve düzenleme ekranı — iki mod, tek ekran.
 *
 * `denemeId` `0`'dan büyükse düzenleme (kayıt forma yüklenir), değilse
 * yeni eklemedir. [com.odevtakip.app.ui.form.OdevFormEkrani] ile aynı
 * düzen taşınır: alanlar üstte, Kaydet formun **sonunda tam genişlikte**.
 *
 * Ödev formundan farklar:
 *
 *  - **Saat yoktur.** Deneme "12 Ekim'de yapıldı" diye hatırlanır;
 *    saati bilinçli olarak tutulmaz (bkz. [com.odevtakip.app.data.Deneme]).
 *  - **Ders seçimi yoktur; onun yerine ders bazlı sonuç satırları
 *    vardır.** Her satır kendi **soru sayısıyla kilitlidir**: TYT'de
 *    Türkçe satırı 40 soruyu, AYT'de Fizik satırı 14 soruyu aşamaz
 *    ([DersKotasi.asildiMi]). Kotalar ve toplam **kaynaktan** gelir;
 *    ekranda elle yazılmış ikinci bir kopya yoktur.
 *  - **Toplam ayrıca girilmez.** Doğru, yanlış ve net satırlardan
 *    hesaplanır; kaydın toplamları da [com.odevtakip.app.data.Deneme]
 *    içinde derslerden türetilir.
 *  - **Tür formun başında kilitli karttır**: kullanıcı denemeyi hangi
 *    formatta kaydettiğini açar açmaz görür. Değiştirilemez — 120
 *    soruluk bir formun kaydı 80 soruya taşınamaz.
 *
 * @param denemeId Düzenlenecek kaydın kimliği; `-1` = yeni deneme.
 * @param tur Denemenin türü; liste ekranındaki seçimden gelir.
 * @param onGeri Kaydettikten ya da vazgeçtikten sonra dönülecek yer
 *   (detay ekranındaysanız detay, listedeyseniz liste).
 */
@Composable
fun DenemeFormEkrani(
    denemeId: Long,
    tur: DenemeTuru,
    viewModel: OdevViewModel,
    onGeri: () -> Unit,
) {
    val duzenleme = denemeId > 0L
    val kotalar = tur.dersleri
    val kotaSayisi = kotalar.size

    var ad by rememberSaveable { mutableStateOf("") }
    var aciklama by rememberSaveable { mutableStateOf("") }
    var tarihMillis by rememberSaveable { mutableStateOf(baslangicTarihi()) }
    var puanMetin by rememberSaveable { mutableStateOf("") }

    // Ders başına iki metin alanı. Liste boyutu türle birlikte sabittir
    // (her iki türde de ders vardır); "boş metin" o derste soru
    // çözülmemesi demektir (bkz. tamSayiCoz).
    var dogruMetinleri by rememberSaveable { mutableStateOf(List(kotaSayisi) { "" }) }
    var yanlisMetinleri by rememberSaveable { mutableStateOf(List(kotaSayisi) { "" }) }

    // Hatalar da liste hâlinde: her satır kendi hatasını taşır. Kota
    // aşımı ise **durum değildir** — anlık değerlerden hesaplanır, bkz.
    // [kotaAsimlari].
    var adHatasi by rememberSaveable { mutableStateOf(false) }
    var puanHatasi by rememberSaveable { mutableStateOf(false) }
    var dogruHatalari by rememberSaveable { mutableStateOf(List(kotaSayisi) { false }) }
    var yanlisHatalari by rememberSaveable { mutableStateOf(List(kotaSayisi) { false }) }

    var tarihDialogGoster by remember { mutableStateOf(false) }

    // Düzenlemede mevcut kaydı forma yükle. Tür rotadan gelir ama kayıt
    // açıldığında onun türü esastır — rota yalnızca yeni kayıtta anlam
    // taşır.
    LaunchedEffect(denemeId) {
        if (duzenleme) viewModel.denemeyiGet(denemeId)?.let { deneme ->
            ad = deneme.ad
            aciklama = deneme.aciklama
            tarihMillis = deneme.tarih
            puanMetin = deneme.puan?.let(::sayiMetni).orEmpty()
            // 0, boş yazılır: forma "0" basmak değil, boş bırakmak
            // "bu derste soru çözmedim" demenin doğal yoludur.
            dogruMetinleri = kotalar.map { kota ->
                metinOl(deneme.sonuclar.firstOrNull { it.ders == kota.ders }?.dogru)
            }
            yanlisMetinleri = kotalar.map { kota ->
                metinOl(deneme.sonuclar.firstOrNull { it.ders == kota.ders }?.yanlis)
            }
        }
    }

    // Satırlardaki anlık değerler: başlıktaki "12 / 40" göstergesi, canlı
    // toplam ve **kota aşımı** bunlardan beslenir. Aşım ayrıca saklanmaz —
    // yazarken de görünür, kaydetmeye gerek yoktur.
    val dogruSayilari = dogruMetinleri.map { tamSayiCoz(it) }
    val yanlisSayilari = yanlisMetinleri.map { tamSayiCoz(it) }
    val kotaAsimlari = kotalar.indices.map { i ->
        val d = dogruSayilari[i]
        val y = yanlisSayilari[i]
        d != null && y != null && kotalar[i].asildiMi(d, y)
    }
    val toplamDogru = dogruSayilari.sumOf { it ?: 0 }
    val toplamYanlis = yanlisSayilari.sumOf { it ?: 0 }

    fun kaydet() {
        adHatasi = ad.isBlank()

        // Çözümleme tek yerde: sayılmayan giriş (null) satır hatası,
        // kota aşımı ise kaydı durduran koşuldur (bkz. [kotaAsimlari]).
        val dogru = dogruMetinleri.map { tamSayiCoz(it) }
        val yanlis = yanlisMetinleri.map { tamSayiCoz(it) }
        dogruHatalari = dogru.map { it == null }
        yanlisHatalari = yanlis.map { it == null }

        val puan = ondalikCoz(puanMetin)
        puanHatasi = puan == null && puanMetin.isNotBlank()

        val engelVar = adHatasi ||
            puanHatasi ||
            dogruHatalari.any { it } ||
            yanlisHatalari.any { it } ||
            kotaAsimlari.any { it }
        if (engelVar) return

        viewModel.denemeFormunuKaydet(
            denemeId = denemeId,
            tur = tur,
            ad = ad,
            tarih = tarihMillis,
            sonuclar = kotalar.indices.map { i ->
                DersSonucu(
                    ders = kotalar[i].ders,
                    dogru = dogru[i] ?: 0,
                    yanlis = yanlis[i] ?: 0,
                )
            },
            puan = puan,
            aciklama = aciklama,
            onBasarili = onGeri,
        )
    }

    Scaffold(
        topBar = {
            UstBar(
                baslik = {
                    Text(
                        stringResource(
                            if (duzenleme) R.string.deneme_duzenle else R.string.deneme_ekle
                        )
                    )
                },
                gezinmeIkonu = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TurKarti(tur = tur)

            Alan(
                deger = ad,
                onDegisim = {
                    ad = it
                    if (it.isNotBlank()) adHatasi = false
                },
                etiket = stringResource(R.string.deneme_alan_ad),
                ipucu = stringResource(R.string.deneme_alan_ad_ipucu),
                tekSatir = true,
                hata = if (adHatasi) stringResource(R.string.deneme_ad_zorunlu) else null,
                imeAction = ImeAction.Next,
            )

            SeciciKart(
                etiket = stringResource(R.string.deneme_etiket_tarih),
                deger = tarihMillis.formatliTarih(),
                ikon = Icons.Rounded.DateRange,
                onClick = { tarihDialogGoster = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.deneme_sonuc_baslik),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            kotalar.indices.forEach { i ->
                DersSatiri(
                    kota = kotalar[i],
                    dogruMetin = dogruMetinleri[i],
                    yanlisMetin = yanlisMetinleri[i],
                    onDogruDegisim = { yeni ->
                        dogruMetinleri = dogruMetinleri.koy(i, yeni)
                        if (tamSayiCoz(yeni) != null) {
                            dogruHatalari = dogruHatalari.koy(i, false)
                        }
                    },
                    onYanlisDegisim = { yeni ->
                        yanlisMetinleri = yanlisMetinleri.koy(i, yeni)
                        if (tamSayiCoz(yeni) != null) {
                            yanlisHatalari = yanlisHatalari.koy(i, false)
                        }
                    },
                    dogruHatasi = dogruHatalari[i],
                    yanlisHatasi = yanlisHatalari[i],
                    kotaAsildi = kotaAsimlari[i],
                )
            }

            ToplamSatiri(
                dogru = toplamDogru,
                yanlis = toplamYanlis,
                net = toplamDogru - toplamYanlis / 4.0,
            )

            Alan(
                deger = puanMetin,
                onDegisim = {
                    puanMetin = it
                    if (ondalikCoz(it) != null || it.isBlank()) puanHatasi = false
                },
                etiket = stringResource(R.string.deneme_puan_etiket),
                ipucu = stringResource(R.string.deneme_puan_ipucu),
                hata = if (puanHatasi) stringResource(R.string.deneme_puan_hata) else null,
                tekSatir = true,
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            )

            Alan(
                deger = aciklama,
                onDegisim = { aciklama = it },
                etiket = stringResource(R.string.alan_aciklama),
                ipucu = stringResource(R.string.alan_aciklama_ipucu),
                tekSatir = false,
                satirlar = 3,
                imeAction = ImeAction.Default,
            )

            Button(
                onClick = ::kaydet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.kaydet),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }

    if (tarihDialogGoster) {
        TarihSeciciDialog(
            mevcutTarih = tarihMillis.yerelTarih(),
            onKapat = { tarihDialogGoster = false },
            // Yalnızca gün seçilir; saat zaten tutulmuyor. Depo, seçilen
            // değeri günün başına indirger (OdevRepository.denemeKaydet).
            onSecim = { yeniTarih ->
                tarihMillis = yeniTarih.atTime(12, 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            },
        )
    }
}

/** Yeni deneme için varsayılan tarih: bugünün öğleni (gün başına iner). */
private fun baslangicTarihi(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** `0` ve `null` boş yazılır; formda yalnızca dolu sayılar görünür. */
private fun metinOl(sayi: Int?): String =
    if (sayi == null || sayi == 0) "" else sayi.toString()

/** Liste elemanını değiştirir; durum listeleri değiştirilemez olduğu için kopya üretir. */
private fun <T> List<T>.koy(index: Int, deger: T): List<T> =
    toMutableList().also { it[index] = deger }

// ---- Tür kartı ----

/**
 * Formun en üstündeki tür kartı: "Deneme türü — TYT · 120 soru".
 *
 * **Tıklanamaz.** Tür ekleme anında seçilir ve kayıttan sonra
 * değiştirilemez; kartın seçilebilir görünmesi, "bunu değiştirebilirim"
 * yalanını söylerdi. Çizim, altındaki tarih kartıyla aynıdır — yalnızca
 * üzerine basılabilir olmadığı farkıdır.
 */
@Composable
private fun TurKarti(tur: DenemeTuru) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = kartCizgisi(),
        elevation = kartElevasyonu(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.deneme_tur_baslik),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.deneme_tur_ozet,
                        turEtiketi(tur),
                        tur.toplamSoru,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

// ---- Ders satırı ----

/**
 * Tek dersin sonucu: ad + anlık kota, alt test bilgisi, doğru/yanlış.
 *
 * Satırın sağ üstünde **"cevaplanan / kota"** göstergesi durur: kullanıcı
 * yazarken "Türkçe 39 / 40" gibi ilerler ve sınırı gözle takip eder. Kota
 * **aşılır aşılmaz** (kaydetmeye gerek kalmadan) hem gösterge hem de
 * uyarı kırmızıya döner ve kaydetme durur — sayı klavyesi sınırı
 * zorlamayı engellemez, engelleyen şey bu kuraldır.
 *
 * Alt test dağılımı yalnızca tanımlıysa çizilir: TYT'nin Sosyal ve Fen
 * satırları "Tarih 5 · Coğrafya 5 …" gibi bir cümle taşır; Türkçe'de
 * bölünme olmadığı için satır sade kalır.
 */
@Composable
private fun DersSatiri(
    kota: DersKotasi,
    dogruMetin: String,
    yanlisMetin: String,
    onDogruDegisim: (String) -> Unit,
    onYanlisDegisim: (String) -> Unit,
    dogruHatasi: Boolean,
    yanlisHatasi: Boolean,
    kotaAsildi: Boolean,
) {
    val cevaplanan = (tamSayiCoz(dogruMetin) ?: 0) + (tamSayiCoz(yanlisMetin) ?: 0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = kota.ders,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.deneme_kota, cevaplanan, kota.soru),
                style = MaterialTheme.typography.titleSmall,
                color = if (kotaAsildi) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        if (kota.altDagilim != null) {
            Text(
                text = kota.altDagilim,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Doğru/yanlış yan yana: ikisi tek cevap anahtarının iki yüzüdür.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Alan(
                deger = dogruMetin,
                onDegisim = onDogruDegisim,
                etiket = stringResource(R.string.deneme_etiket_dogru),
                ipucu = "0",
                hata = if (dogruHatasi) stringResource(R.string.deneme_sayi_hata) else null,
                tekSatir = true,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                modifier = Modifier.weight(1f),
            )
            Alan(
                deger = yanlisMetin,
                onDegisim = onYanlisDegisim,
                etiket = stringResource(R.string.deneme_etiket_yanlis),
                ipucu = "0",
                hata = if (yanlisHatasi) stringResource(R.string.deneme_sayi_hata) else null,
                tekSatir = true,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                modifier = Modifier.weight(1f),
            )
        }

        if (kotaAsildi) {
            Text(
                text = stringResource(R.string.deneme_kota_hata, kota.ders, kota.soru),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ---- Canlı toplam ----

/**
 * Satırların altında anlık toplam: "92 doğru · 12 yanlış" + "Net 89".
 *
 * Net **birincil renkte ve kalın** yazılır: deneme ekranının tek
 * kahraman sayısıdır ve kullanıcı yazarken onu canlı görmek ister.
 * Karta ayrı bir tıklama ya da gezinme yoktur — salt okunurdur.
 */
@Composable
private fun ToplamSatiri(dogru: Int, yanlis: Int, net: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = kartCizgisi(),
        elevation = kartElevasyonu(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.deneme_sonuc, dogru, yanlis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${stringResource(R.string.deneme_net_etiket)} ${sayiMetni(net)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ---- Metin alanı ----

/**
 * Tek metin alanı; [com.odevtakip.app.ui.form.OdevFormEkrani] ile aynı.
 *
 * Zemin `surface` (açık temada beyaz) yapılır: ekran zemini `background`
 * olduğundan alanlar zeminden ayrılır.
 */
@Composable
private fun Alan(
    deger: String,
    onDegisim: (String) -> Unit,
    etiket: String,
    ipucu: String,
    tekSatir: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    hata: String? = null,
    satirlar: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val renkler = MaterialTheme.colorScheme
    val alanRenkleri = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = renkler.surface,
        unfocusedContainerColor = renkler.surface,
        disabledContainerColor = renkler.surface,
        errorContainerColor = renkler.surface,
    )

    OutlinedTextField(
        value = deger,
        onValueChange = onDegisim,
        label = { Text(etiket) },
        placeholder = { Text(ipucu) },
        singleLine = tekSatir,
        minLines = if (tekSatir) 1 else satirlar,
        maxLines = if (tekSatir) 1 else satirlar + 3,
        isError = hata != null,
        supportingText = if (hata != null) {
            { Text(hata) }
        } else {
            null
        },
        colors = alanRenkleri,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

// ---- Seçici kart ----

/** Tarih seçici kartı; ödev formundaki ile aynı çizim. */
@Composable
private fun SeciciKart(
    etiket: String,
    deger: String,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    degerSoluk: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = kartCizgisi(),
        elevation = kartElevasyonu(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ikon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = etiket,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = deger,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (degerSoluk) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

// ---- Tarih seçici ----

@Composable
private fun TarihSeciciDialog(
    mevcutTarih: LocalDate,
    onKapat: () -> Unit,
    onSecim: (LocalDate) -> Unit,
) {
    val durum = rememberDatePickerState(
        initialSelectedDateMillis = mevcutTarih.toEpochDay() * GUN_MS,
    )

    DatePickerDialog(
        onDismissRequest = onKapat,
        confirmButton = {
            TextButton(
                onClick = {
                    durum.selectedDateMillis?.let { millis ->
                        onSecim(LocalDate.ofEpochDay(Math.floorDiv(millis, GUN_MS)))
                    }
                    onKapat()
                }
            ) {
                Text(stringResource(R.string.tamam))
            }
        },
        dismissButton = {
            TextButton(onClick = onKapat) { Text(stringResource(R.string.iptal)) }
        },
    ) {
        DatePicker(state = durum)
    }
}
