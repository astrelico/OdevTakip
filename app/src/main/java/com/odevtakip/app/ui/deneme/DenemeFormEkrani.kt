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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.UstBar
import com.odevtakip.app.ui.dersler.DersSecimPaneli
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
 *    Tarih seçici tek başına durur.
 *  - **Ders opsiyoneldir.** Panelin en üstüne "Ders seçilmedi" satırı
 *    eklenir; genel deneme bu yolla kaydedilir.
 *  - **Doğru/yanlış sayı alanlarıdır**, boş bırakılırsa `0` sayılır
 *    ([tamSayiCoz]); puan ise isteğe bağlıdır ve boş bırakılabilir.
 *
 * @param denemeId Düzenlenecek kaydın kimliği; `-1` = yeni deneme.
 * @param onGeri Kaydettikten ya da vazgeçtikten sonra dönülecek yer
 *   (detay ekranındaysanız detay, listedeyseniz liste).
 */
@Composable
fun DenemeFormEkrani(
    denemeId: Long,
    viewModel: OdevViewModel,
    onGeri: () -> Unit,
) {
    val duzenleme = denemeId > 0L

    var ad by rememberSaveable { mutableStateOf("") }
    var aciklama by rememberSaveable { mutableStateOf("") }
    var ders by rememberSaveable { mutableStateOf("") }
    var tarihMillis by rememberSaveable { mutableStateOf(baslangicTarihi()) }
    var dogruMetin by rememberSaveable { mutableStateOf("") }
    var yanlisMetin by rememberSaveable { mutableStateOf("") }
    var puanMetin by rememberSaveable { mutableStateOf("") }

    var adHatasi by rememberSaveable { mutableStateOf(false) }
    var dogruHatasi by rememberSaveable { mutableStateOf(false) }
    var yanlisHatasi by rememberSaveable { mutableStateOf(false) }
    var puanHatasi by rememberSaveable { mutableStateOf(false) }

    var tarihDialogGoster by remember { mutableStateOf(false) }
    var dersPanelGoster by remember { mutableStateOf(false) }

    val dersler by viewModel.dersler.collectAsStateWithLifecycle()

    // Düzenlemede mevcut kaydı forma yükle.
    LaunchedEffect(denemeId) {
        if (duzenleme) viewModel.denemeyiGet(denemeId)?.let { deneme ->
            ad = deneme.ad
            aciklama = deneme.aciklama
            ders = deneme.ders
            tarihMillis = deneme.tarih
            // Sayılar metne çevrilir: kullanıcı düzeltip kaydetmek isterse
            // boş alanla karşılaşmamalı. Puan `sayiMetni` ile yazılır ki
            // "84,25" virgülüyle görünsün ve geri çözülebilsin.
            dogruMetin = deneme.dogru.toString()
            yanlisMetin = deneme.yanlis.toString()
            puanMetin = deneme.puan?.let(::sayiMetni).orEmpty()
        }
    }

    fun kaydet() {
        adHatasi = ad.isBlank()
        val dogru = tamSayiCoz(dogruMetin)
        val yanlis = tamSayiCoz(yanlisMetin)
        val puan = ondalikCoz(puanMetin)
        dogruHatasi = dogru == null
        yanlisHatasi = yanlis == null
        puanHatasi = puan == null && puanMetin.isNotBlank()

        if (adHatasi || dogruHatasi || yanlisHatasi || puanHatasi) return

        viewModel.denemeFormunuKaydet(
            denemeId = denemeId,
            ad = ad,
            ders = ders,
            tarih = tarihMillis,
            dogru = dogru ?: 0,
            yanlis = yanlis ?: 0,
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

            SeciciKart(
                etiket = stringResource(R.string.ders),
                deger = ders.ifBlank { stringResource(R.string.ders_secilmedi) },
                ikon = Icons.Rounded.School,
                onClick = { dersPanelGoster = true },
                modifier = Modifier.fillMaxWidth(),
                degerSoluk = ders.isBlank(),
            )

            // Doğru/yanlış yan yana: ikisi tek cevap anahtarının iki
            // yüzüdür, ayrı satırlarda olsaydı göz iki kez yukarı
            // aşağı inmek zorunda kalırdı.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Alan(
                    deger = dogruMetin,
                    onDegisim = {
                        dogruMetin = it
                        if (tamSayiCoz(it) != null) dogruHatasi = false
                    },
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
                    onDegisim = {
                        yanlisMetin = it
                        if (tamSayiCoz(it) != null) yanlisHatasi = false
                    },
                    etiket = stringResource(R.string.deneme_etiket_yanlis),
                    ipucu = "0",
                    hata = if (yanlisHatasi) stringResource(R.string.deneme_sayi_hata) else null,
                    tekSatir = true,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f),
                )
            }

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

    if (dersPanelGoster) {
        DersSecimPaneli(
            baslik = stringResource(R.string.ders_sec),
            dersler = dersler.map { it.ad },
            secili = ders.takeIf { it.isNotBlank() },
            // Denemede ders opsiyoneldir: "Ders seçilmedi" satırı panelin
            // en üstünde durur ve seçimi geri alır.
            ilkSecenekMetni = stringResource(R.string.ders_secilmedi),
            onSecim = { secilen -> ders = secilen.orEmpty() },
            onKapat = { dersPanelGoster = false },
        )
    }
}

/** Yeni deneme için varsayılan tarih: bugünün öğleni (gün başına iner). */
private fun baslangicTarihi(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

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

/** Tarih / ders seçici kartı; ödev formundaki ile aynı çizim. */
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
