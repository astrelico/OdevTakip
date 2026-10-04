@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.form

import android.app.Activity
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.odevtakip.app.R
import com.odevtakip.app.bildirim.BildirimYonetici
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.util.formatliSaat
import com.odevtakip.app.util.formatliTarih
import com.odevtakip.app.util.tarihSaatiniDonustur
import com.odevtakip.app.util.yerelSaat
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate
import java.time.LocalTime

private const val GUN_MS = 86_400_000L

/**
 * Ödev ekleme ve düzenleme ekranı.
 *
 * [odevId] `0` veya negatifse yeni ödev eklenir.
 *
 * Tarih/saat tek bir `Long` (epoch millis) olarak saklanır; dönüşümler
 * [tarihSaatiniDonustur] üzerinden yapılır, böylece saat dilimi kayması olmaz.
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
    var sonTarihMillis by rememberSaveable {
        mutableStateOf(baslangicTarihi())
    }
    var baslikHatasi by rememberSaveable { mutableStateOf(false) }

    var tarihDialogGoster by remember { mutableStateOf(false) }
    var saatDialogGoster by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Düzenleme modunda mevcut kaydı forma yükle.
    LaunchedEffect(odevId) {
        if (!yeniMi) {
            viewModel.odeviGet(odevId)?.let { odev ->
                baslik = odev.baslik
                aciklama = odev.aciklama
                sonTarihMillis = odev.sonTarih
            }
        }
    }

    fun kaydet() {
        if (baslik.isBlank()) {
            baslikHatasi = true
            return
        }
        viewModel.formuKaydet(
            odevId = odevId,
            baslik = baslik,
            aciklama = aciklama,
            sonTarih = sonTarihMillis,
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
                actions = {
                    TextButton(onClick = ::kaydet) {
                        Text(stringResource(R.string.kaydet))
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
            OutlinedTextField(
                value = baslik,
                onValueChange = {
                    baslik = it
                    if (it.isNotBlank()) baslikHatasi = false
                },
                label = { Text(stringResource(R.string.alan_baslik)) },
                placeholder = { Text(stringResource(R.string.alan_baslik_ipucu)) },
                singleLine = true,
                isError = baslikHatasi,
                supportingText = if (baslikHatasi) {
                    { Text(stringResource(R.string.baslik_zorunlu)) }
                } else {
                    null
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = aciklama,
                onValueChange = { aciklama = it },
                label = { Text(stringResource(R.string.alan_aciklama)) },
                placeholder = { Text(stringResource(R.string.alan_aciklama_ipucu)) },
                minLines = 3,
                maxLines = 6,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Default,
                ),
                modifier = Modifier.fillMaxWidth(),
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
}

/** Yeni ödev için varsayılan: bugün 23:59. */
private fun baslangicTarihi(): Long =
    tarihSaatiniDonustur(LocalDate.now(), LocalTime.of(23, 59))

// ---- Tarih/saat seçici düğmeleri ----

@Composable
private fun TarihSaatSecici(
    etiket: String,
    deger: String,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
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
                Text(text = deger, style = MaterialTheme.typography.titleSmall)
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
