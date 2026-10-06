@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.form

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.bildirim.BildirimYonetici
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.dersler.DersSecimPaneli
import com.odevtakip.app.util.formatliSaat
import com.odevtakip.app.util.formatliTarih
import com.odevtakip.app.util.tarihSaatiniDonustur
import com.odevtakip.app.util.yerelSaat
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

private const val GUN_MS = 86_400_000L

/**
 * Ödev ekleme ve düzenleme ekranı.
 *
 * [odevId] `0` veya negatifse yeni ödev eklenir.
 *
 * Tarih/saat tek bir `Long` (epoch millis) olarak saklanır; dönüşümler
 * [tarihSaatiniDonustur] üzerinden yapılır, böylece saat dilimi kayması olmaz.
 *
 * Kaydetme üst barda değil, formun **sonunda tam genişlikte** duruyor:
 * referans tasarımdaki gibi ve başparmak erişiminde. Üst barda yalnızca geri
 * düğmesi kalır.
 */
@Composable
fun OdevFormEkrani(
    odevId: Long,
    viewModel: OdevViewModel,
    onGeri: () -> Unit,
) {
    val yeniMi = odevId <= 0L

    var baslik by rememberSaveable { mutableStateOf("") }
    var aciklama by rememberSaveable { mutableStateOf("") }
    var ders by rememberSaveable { mutableStateOf("") }
    var sonTarihMillis by rememberSaveable {
        mutableStateOf(baslangicTarihi())
    }
    var baslikHatasi by rememberSaveable { mutableStateOf(false) }
    var dersHatasi by rememberSaveable { mutableStateOf(false) }

    var tarihDialogGoster by remember { mutableStateOf(false) }
    var saatDialogGoster by remember { mutableStateOf(false) }
    var dersPanelGoster by remember { mutableStateOf(false) }

    // ---- Dosya / fotoğraf eki ----
    //
    // Seçim anında **yalnızca bir Uri** tutulur; dosyanın diske kopyalanması
    // Kaydet'e basılır (OdevViewModel.formuKaydet). Böylece formdan
    // vazgeçildiğinde geriye hiçbir artık dosya kalmaz — kopyası yapılmamış
    // bir şeyin temizlenmesi de gerekmez.
    //
    // `ek`: veritabanındaki mevcut değer (düzenleme modunda başlangıçta o).
    // `secilenEkUri != null` ise kullanıcı yeni bir dosya seçmiştir ve o,
    // `ek`'in yerine geçer.
    var ek by remember { mutableStateOf<String?>(null) }
    var secilenEkUri by remember { mutableStateOf<Uri?>(null) }
    var secilenEkAdi by remember { mutableStateOf<String?>(null) }
    var ekHatasi by remember { mutableStateOf(false) }

    val dersler by viewModel.dersler.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hiz = rememberCoroutineScope()

    /** Seçici bizi geri çağırdığında: Uri'yi tut, adını oku, uyarıyı temizle. */
    fun ekSecildi(uri: Uri?) {
        // `null` = kullanıcı seçmekten vazgeçti; mevcut durum değişmez.
        if (uri == null) return
        secilenEkUri = uri
        secilenEkAdi = null
        ekHatasi = false
        hiz.launch {
            val okunan = viewModel.ekAdi(uri)
            // Arada ikinci bir seçim yapıldıysa eski ad yazılmaz.
            if (secilenEkUri == uri) secilenEkAdi = okunan
        }
    }

    // Fotoğraf sistem Photo Picker'ından gelir (izin gerekmez), dosya da
    // SAF belge seçicisinden. İkisi de aynı geri çağraya düşer.
    val fotografSecici = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> ekSecildi(uri) }

    val dosyaSecici = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> ekSecildi(uri) }

    // Düzenleme modunda mevcut kaydı forma yükle.
    LaunchedEffect(odevId) {
        if (!yeniMi) {
            viewModel.odeviGet(odevId)?.let { odev ->
                baslik = odev.baslik
                aciklama = odev.aciklama
                ders = odev.ders
                sonTarihMillis = odev.sonTarih
                ek = odev.ek
            }
        }
    }

    // Kartta görünecek ek adı: yeni seçim varsa o, yoksa kayıtlı olan.
    // Ad henüz okunmadıysa (milisaniyelik bir aralık) jenerik metin durur.
    val ekGosterilen = if (secilenEkUri != null) {
        secilenEkAdi ?: stringResource(R.string.ek_secildi)
    } else {
        ek
    }

    fun kaydet() {
        if (baslik.isBlank()) {
            baslikHatasi = true
            return
        }
        if (ders.isBlank()) {
            dersHatasi = true
            return
        }
        viewModel.formuKaydet(
            odevId = odevId,
            baslik = baslik,
            aciklama = aciklama,
            ders = ders,
            sonTarih = sonTarihMillis,
            ekUri = secilenEkUri,
            ek = ek,
            onEkHatasi = {
                // Dosya diske yazılamadı. Ödev yine de kaydedilmez: eksik
                // kayıt, hatadan kötüdür. Seçim başa döner, kullanıcı
                // tekrar deneyebilir.
                secilenEkUri = null
                secilenEkAdi = null
                ekHatasi = true
            },
            onBasarili = {
                // Bağlamda iste: kullanıcı ilk ödevini kaydetti, yani
                // hatırlatmanın artık bir anlamı var. İzin en fazla bir kez sorulur.
                (context as? Activity)?.let(BildirimYonetici::izinBirKezIste)
                onGeri()
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (yeniMi) R.string.odev_ekle else R.string.odev_duzenle
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Alan(
                deger = baslik,
                onDegisim = {
                    baslik = it
                    if (it.isNotBlank()) baslikHatasi = false
                },
                etiket = stringResource(R.string.alan_baslik),
                ipucu = stringResource(R.string.alan_baslik_ipucu),
                tekSatir = true,
                hata = if (baslikHatasi) stringResource(R.string.baslik_zorunlu) else null,
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

            TarihSaatSecici(
                etiket = stringResource(R.string.ders),
                deger = ders.ifBlank { stringResource(R.string.ders_secilmedi) },
                ikon = Icons.Rounded.School,
                onClick = { dersPanelGoster = true },
                modifier = Modifier.fillMaxWidth(),
                hata = if (dersHatasi) stringResource(R.string.ders_zorunlu) else null,
                degerSoluk = ders.isBlank(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarihSaatSecici(
                    etiket = stringResource(R.string.teslim_tarihi),
                    deger = sonTarihMillis.formatliTarih(),
                    ikon = Icons.Rounded.DateRange,
                    onClick = { tarihDialogGoster = true },
                    modifier = Modifier.weight(1f),
                )
                TarihSaatSecici(
                    etiket = stringResource(R.string.teslim_saati),
                    deger = sonTarihMillis.formatliSaat(),
                    ikon = Icons.Rounded.Schedule,
                    onClick = { saatDialogGoster = true },
                    modifier = Modifier.weight(1f),
                )
            }

            EkAlani(
                ad = ekGosterilen,
                hata = if (ekHatasi) stringResource(R.string.ek_kaydedilemedi) else null,
                onFotografSec = {
                    fotografSecici.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        )
                    )
                },
                onDosyaSec = { dosyaSecici.launch(arrayOf("*/*")) },
                onKaldir = {
                    secilenEkUri = null
                    secilenEkAdi = null
                    ek = null
                    ekHatasi = false
                },
                modifier = Modifier.fillMaxWidth(),
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
            mevcutTarih = sonTarihMillis.yerelTarih(),
            onKapat = { tarihDialogGoster = false },
            onSecim = { yeniTarih ->
                // Yeni tarih + eski saat → tek epoch değeri
                sonTarihMillis = tarihSaatiniDonustur(yeniTarih, sonTarihMillis.yerelSaat())
            },
        )
    }

    if (saatDialogGoster) {
        SaatSeciciDialog(
            mevcutSaat = sonTarihMillis.yerelSaat(),
            onKapat = { saatDialogGoster = false },
            onSecim = { yeniSaat ->
                sonTarihMillis = tarihSaatiniDonustur(sonTarihMillis.yerelTarih(), yeniSaat)
            },
        )
    }

    if (dersPanelGoster) {
        DersSecimPaneli(
            baslik = stringResource(R.string.ders_sec),
            dersler = dersler.map { it.ad },
            secili = ders.takeIf { it.isNotBlank() },
            onSecim = { secilen ->
                ders = secilen.orEmpty()
                dersHatasi = false
            },
            onKapat = { dersPanelGoster = false },
        )
    }
}

/** Yeni ödev için varsayılan: bugün 23:59. */
private fun baslangicTarihi(): Long =
    tarihSaatiniDonustur(LocalDate.now(), LocalTime.of(23, 59))

// ---- Metin alanları ----

/**
 * Tek metin alanı.
 *
 * Zemini bilinçli olarak `surface` (açık temada beyaz) yapılıyor: ekran zemini
 * `background` olduğundan alanlar zeminden ayrılır, referanstaki gibi okunur.
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
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = imeAction,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

// ---- Tarih/saat seçici düğmeleri ----

/**
 * Tarih / saat / ders seçici kartı.
 *
 * [hata] verilirse kartın altında hata cümlesi belirir — zorunlu alanlar
 * (ders) seçilmemişken Kaydet'e basıldığında buradan uyarılır.
 */
@Composable
private fun TarihSaatSecici(
    etiket: String,
    deger: String,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hata: String? = null,
    degerSoluk: Boolean = false,
) {
    Column(modifier = modifier) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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

        if (hata != null) {
            Text(
                text = hata,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}

// ---- Dosya / fotoğraf eki ----

/**
 * Formun "Ek" bölümü.
 *
 * Dosya seçilmeden önce tek bir **"Ek ekle"** düğmesi durur; açılan menü
 * ikisini de ayrı ayrı sunar (galeri fotoğrafı sistem Photo Picker'ı ile,
 * belge dosya seçicisiyle). Ek varken kart **kendi kendine tıklanabilir
 * değildir**: ne yapılacağı iki ayrı düğmeyle (Ek değiştir / Kaldır)
 * açıkça yazılıdır — tek bir dokunuşun "yanlışlıkla siler miyim?" korkusu
 * yaratması istenmez.
 *
 * Burada yalnızca bir Uri tutulur; dosya diske **Kaydet**'te yazılır.
 * Önizleme bu yüzden dosyanın kendisini değil, seçiciden okunan **adını**
 * gösterir.
 */
@Composable
private fun EkAlani(
    ad: String?,
    hata: String?,
    onFotografSec: () -> Unit,
    onDosyaSec: () -> Unit,
    onKaldir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuAcik by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box {
            if (ad == null) {
                OutlinedButton(
                    onClick = { menuAcik = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ek_ekle),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ek_baslik),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = ad,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { menuAcik = true }) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = stringResource(R.string.ek_degistir),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onKaldir) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.kaldir),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Menü kutunun **alt soluna** bağlanır: hem butonun hem kartın
            // altında açılır, seçicinin getirdiği liste ekranın üstüne
            // taşarsa sistem kendisi ekrana kırpır.
            DropdownMenu(
                expanded = menuAcik,
                onDismissRequest = { menuAcik = false },
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ek_fotograf_sec)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Photo,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuAcik = false
                        onFotografSec()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ek_dosya_sec)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuAcik = false
                        onDosyaSec()
                    },
                )
            }
        }

        if (hata != null) {
            Text(
                text = hata,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
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
        // DatePicker UTC gecesini döndürür; epoch günü ile birebir eşleşir.
        initialSelectedDateMillis = mevcutTarih.toEpochDay() * GUN_MS,
    )

    DatePickerDialog(
        onDismissRequest = onKapat,
        confirmButton = {
            TextButton(
                onClick = {
                    durum.selectedDateMillis?.let { millis ->
                        // Negatif epoch günlerinde bölme yuvarlamasını önlemek için floorDiv.
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

// ---- Saat seçici ----

@Composable
private fun SaatSeciciDialog(
    mevcutSaat: LocalTime,
    onKapat: () -> Unit,
    onSecim: (LocalTime) -> Unit,
) {
    val durum = rememberTimePickerState(
        initialHour = mevcutSaat.hour,
        initialMinute = mevcutSaat.minute,
        is24Hour = true,
    )

    Dialog(onDismissRequest = onKapat) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = durum)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onKapat) { Text(stringResource(R.string.iptal)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onSecim(LocalTime.of(durum.hour, durum.minute))
                            onKapat()
                        }
                    ) {
                        Text(stringResource(R.string.tamam))
                    }
                }
            }
        }
    }
}
