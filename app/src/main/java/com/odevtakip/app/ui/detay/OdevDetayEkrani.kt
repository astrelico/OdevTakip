@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.detay

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odevtakip.app.R
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.boyutMetni
import com.odevtakip.app.data.ekGorselMi
import com.odevtakip.app.data.ekTuru
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.ui.OdevViewModel
import com.odevtakip.app.ui.durumMetni
import com.odevtakip.app.ui.durumRengi
import com.odevtakip.app.ui.teslimMetni
import com.odevtakip.app.util.formatliSaat
import com.odevtakip.app.util.formatliTarih
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [ekGorseli] için çözüm sınırı.
 *
 * En uzun kenar bunu geçmez: 4032×3024'lük bir fotoğraf ~3 MB'a iner ve
 * bellek tepe değeri hiçbir koşulda **10 MB'ı aşmaz**. Sınır "en az" değil
 * "en çok" kuralıdır — `inSampleSize` yalnızca 2'nin kuvveti olabildiği
 * için sonuç her zaman sınırın altında kalır, üstüne çıkmaz.
 *
 * 1440+ piksel, 1080p bir ekranda tam ekran görüntüleme için fazlasıyla
 * yeterlidir.
 */
private const val EN_BUYUK_KENAR = 1600

/** Önizleme kutusunun yüksekliği; görsel gelse de yüklenmese de aynı kalır. */
private val ONIZLEME_YUKSEKLIGI = 200.dp

/**
 * Tek bir ödevin detayı.
 *
 * [OdevViewModel.seciliOdev] Flow'unu izler; böylece listeden geri dönüp
 * tamamlama işareti değişmişse ya da başka bir yerde silinmişse ekran
 * kendiliğinden güncellenir.
 */
@Composable
fun OdevDetayEkrani(
    odevId: Long,
    viewModel: OdevViewModel,
    onGeri: () -> Unit,
    onDuzenle: (Long) -> Unit,
) {
    val odev by viewModel.seciliOdev.collectAsStateWithLifecycle()
    val yuklendi by viewModel.seciliOdevYuklendi.collectAsStateWithLifecycle()
    var silOnayiGoster by remember { mutableStateOf(false) }
    var tamEkranEkAcik by remember { mutableStateOf(false) }

    // Ek, ekranda **bir kez** çözülür: önizleme ile tam ekran aynı bitmap'i
    // paylaşır — ikisi ayrı ayrı çözülseydi bellek iki katına çıkardı.
    // Ödevin eki yoksa `[bekliyor]` kalır ve hiçbir çözüm yapılmaz.
    val ekDosya = odev?.ek?.let(viewModel::ekDosyasi)
    val ekCozum = ekGorseli(ekDosya).value

    LaunchedEffect(odevId) { viewModel.seciliOdeviAyarla(odevId) }
    DisposableEffect(odevId) {
        onDispose { viewModel.seciliOdeviAyarla(null) }
    }

    // Pencere yalnızca tıklanabilir bir önizlemeden açılabilir; yani bu
    // noktada bitmap zaten elde vardır, boş ekran gösterilmez.
    if (tamEkranEkAcik) {
        TamEkranEk(gorsel = ekCozum.gorsel, onKapat = { tamEkranEkAcik = false })
    }

    if (silOnayiGoster && odev != null) {
        SilOnayDialog(
            baslik = odev!!.baslik,
            onIptal = { silOnayiGoster = false },
            onSil = {
                silOnayiGoster = false
                viewModel.sil(odevId, onBasarili = onGeri)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.odev_detay)) },
                navigationIcon = {
                    IconButton(onClick = onGeri) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.geri),
                        )
                    }
                },
                actions = {
                    if (odev != null) {
                        IconButton(onClick = { onDuzenle(odevId) }) {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            val anlikOdev = odev
            if (anlikOdev != null) {
                TamamlaDugmesi(
                    odev = anlikOdev,
                    onTamamla = { viewModel.tamamla(anlikOdev.id) },
                    onGeriAl = { viewModel.tamamlamayiGeriAl(anlikOdev.id) },
                )
            }
        },
    ) { innerPadding ->
        when {
            !yuklendi -> Yukleniyor(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            odev == null -> Bulunamadi(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            else -> OdevIcerik(
                odev = odev!!,
                ekDosya = ekDosya,
                ekGorsel = ekCozum,
                onEkAc = { tamEkranEkAcik = true },
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
            text = stringResource(R.string.odev_bulunamadi),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- İçerik ----

@Composable
private fun OdevIcerik(
    odev: Odev,
    ekDosya: File?,
    ekGorsel: GorselCozum,
    onEkAc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val durum = odev.gercekDurum(System.currentTimeMillis())
    val renk = durumRengi(durum)
    val tamamlandi = durum == Durum.TAMAMLANDI

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Durum + teslim bilgisi
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = renk,
                    contentColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = durumMetni(durum),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.etiket_teslim),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = teslimMetni(odev.sonTarih),
                        style = MaterialTheme.typography.titleMedium,
                        color = renk,
                    )
                }
            }
        }

        // Başlık
        Text(
            text = odev.baslik,
            style = MaterialTheme.typography.headlineSmall,
            textDecoration = if (tamamlandi) TextDecoration.LineThrough else null,
        )

        // Ders
        if (odev.ders.isNotBlank()) {
            Text(
                text = odev.ders,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        HorizontalDivider()

        // Açıklama
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (odev.aciklama.isBlank()) {
                Text(
                    text = stringResource(R.string.aciklama_yok),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = odev.aciklama,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        HorizontalDivider()

        // Tarih bilgileri
        BilgiSatiri(
            etiket = stringResource(R.string.etiket_teslim),
            deger = "${odev.sonTarih.formatliTarih()} ${odev.sonTarih.formatliSaat()}",
        )
        BilgiSatiri(
            etiket = stringResource(R.string.etiket_olusturma),
            deger = "${odev.olusturmaTarihi.formatliTarih()} ${odev.olusturmaTarihi.formatliSaat()}",
        )
        odev.tamamlanmaTarihi?.let { zaman ->
            BilgiSatiri(
                etiket = stringResource(R.string.etiket_tamamlanma),
                deger = "${zaman.formatliTarih()} ${zaman.formatliSaat()}",
            )
        }

        // Ek, "altta" — kullanıcının eklediği şeyi aramak için yukarı
        // taramasın; bilgi satırlarının hemen ardında dursun.
        if (ekDosya != null) {
            HorizontalDivider()
            EkBolumu(
                dosya = ekDosya,
                cozum = ekGorsel,
                onGorselAc = onEkAc,
            )
        }

        Spacer(Modifier.height(8.dp))
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

// ---- Dosya / fotoğraf eki ----

/**
 * Görsel çözümünün durumu.
 *
 * `bitti == false` → çözüm sürüyor. `bitti == true` **ve** `gorsel == null` →
 * dosya okunamadı; uzantısı görsel olsa da kendisi olmayabilir (ör. eski
 * Android'de HEIC çözülmez). Bu ayrım olmasa ikinci durum **sonsuz dönen** bir
 * ilerleme göstergesinde kalırdı; arayüz onu dosya kartına düşürür.
 */
private data class GorselCozum(val gorsel: Bitmap?, val bitti: Boolean)

/**
 * Dosyadan ekrana sığacak kadar küçültülmüş bir görsel çözer.
 *
 * Ölçekleme `inSampleSize` ile **çözüm öncesi** yapılır: 4032×3024'lük bir
 * fotoğraf ham çözüldüğünde tek karede ~36 MB bellek gerekirdi. En uzun kenar
 * [EN_BUYUK_KENAR]a inince tepe kullanım ~3 MB'a düşer; hem alttaki
 * önizleme hem tam ekran üstünde aynı nesne durduğu için ikinci bir kopya
 * tutulmaz.
 *
 * Çözüm `Dispatchers.IO`'da çalışır; kompozisyon kilitlenmez.
 *
 * @param dosya `null` ise hiçbir şey yapılmaz ve sonuç [bekliyor] kalır.
 */
@Composable
private fun ekGorseli(dosya: File?): State<GorselCozum> =
    produceState(
        initialValue = GorselCozum(gorsel = null, bitti = false),
        key1 = dosya,
    ) {
        val hedef = dosya ?: return@produceState

        value = withContext(Dispatchers.IO) {
            try {
                val sinirlar = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(hedef.absolutePath, sinirlar)

                if (sinirlar.outWidth <= 0 || sinirlar.outHeight <= 0) {
                    GorselCozum(gorsel = null, bitti = true)
                } else {
                    // En uzun kenarı sınırın altına indiren ilk kuvvet-i-iki.
                    var olcek = 1
                    val enBuyuk = maxOf(sinirlar.outWidth, sinirlar.outHeight)
                    while (enBuyuk / olcek > EN_BUYUK_KENAR) olcek *= 2

                    GorselCozum(
                        gorsel = BitmapFactory.decodeFile(
                            hedef.absolutePath,
                            BitmapFactory.Options().apply { inSampleSize = olcek },
                        ),
                        bitti = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                GorselCozum(gorsel = null, bitti = true)
            } catch (e: Exception) {
                GorselCozum(gorsel = null, bitti = true)
            }
        }
    }

/**
 * Detayın altındaki ek bölümü.
 *
 * Görsel olan **önizleme** olarak çizilir; kalanı ad + tür + boyut kartı.
 * Bölüm başlığı, üstteki "Teslim / Eklenme" satırlarıyla aynı etiket
 * tipindedir — ek de onlar gibi ödevin bir bilgisi olarak okunur.
 */
@Composable
private fun EkBolumu(
    dosya: File,
    cozum: GorselCozum,
    onGorselAc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ek_baslik),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when {
            // Dosya silinmiş ya da taşınmış: adı göstermek kullanıcıyı
            // "dosya burada" diye yanıltırdı.
            !dosya.exists() -> Text(
                text = stringResource(R.string.ek_bulunamadi),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Çözüm sürüyor ya da başarılı: kutu boyutu sabit, sayfa zıplamaz.
            ekGorselMi(dosya.name) && (cozum.gorsel != null || !cozum.bitti) ->
                GorselOnizleme(cozum = cozum, onAc = onGorselAc)

            // Uzantısı görsel ama çözülemedi → dosya kartı. Dürüst olan bu:
            // kullanıcı ne yüklediğini görür, sahte bir dönerle bekletilmez.
            else -> DosyaKarti(dosya = dosya)
        }
    }
}

/**
 * Görselin önizlemesi.
 *
 * Kutu yüksekliği [ONIZLEME_YUKSEKLIGI]ye **sabitlenir**: büyük bir fotoğraf
 * gelince de boşluk da aynı olduğundan içerik altında zıplamaz.
 * [ContentScale.Fit] tüm görseli gösterir; yanal boşluklar kartın
 * `surfaceVariant` zeminine düşer.
 */
@Composable
private fun GorselOnizleme(cozum: GorselCozum, onAc: () -> Unit) {
    val gorsel = cozum.gorsel

    Card(
        onClick = onAc,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        if (gorsel != null) {
            Image(
                bitmap = gorsel.asImageBitmap(),
                contentDescription = stringResource(R.string.ek_gorsel),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ONIZLEME_YUKSEKLIGI)
                    .clip(MaterialTheme.shapes.medium),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ONIZLEME_YUKSEKLIGI),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

/**
 * Görsel olmayan ek için kart: ad + tür + boyut.
 *
 * Dosya burada **açılmaz**: uygulamanın bir dosya görüntüleyicisi yok ve ek,
 * uygulamanın özel deposunda olduğu için başka bir uygulamaya da
 * gösterilemez. Kart yalnızca "ne yükledim?" sorusunu yanıtlar.
 */
@Composable
private fun DosyaKarti(dosya: File) {
    val turMetni = ekTuru(dosya.name) ?: stringResource(R.string.ek_dosya)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AttachFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = dosya.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    // "PDF · 2,4 MB" — tür sabit Türkçe, boyut `boyutMetni`
                    // ile elle biçimlenir (bkz. Ek.kt).
                    text = "$turMetni · ${boyutMetni(dosya.length())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Görselin tam ekranı.
 *
 * Zemin bilinçli olarak saf siyah: koyu temanın gri zemini bile fotoğrafın
 * kenarında bir çerçeve gibi okunurdu. Pencere çerçevesi kapatılır, görsel
 * sistem çubuklarının altına kadar uzanır; kapatma düğmesi üstte durur.
 */
@Composable
private fun TamEkranEk(gorsel: Bitmap?, onKapat: () -> Unit) {
    Dialog(
        onDismissRequest = onKapat,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (gorsel != null) {
                Image(
                    bitmap = gorsel.asImageBitmap(),
                    contentDescription = stringResource(R.string.ek_gorsel),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            }

            IconButton(
                onClick = onKapat,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.kapat),
                    tint = Color.White,
                )
            }
        }
    }
}

// ---- Alt eylem ----

@Composable
private fun TamamlaDugmesi(
    odev: Odev,
    onTamamla: () -> Unit,
    onGeriAl: () -> Unit,
) {
    // Dış Scaffold'ta contentWindowInsets sıfırlandığı için alt çubuk
    // sistem navigasyon çubuğunun altına düşer; burada kendi payını alır.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        val tamamlandi = odev.durum == Durum.TAMAMLANDI
        Button(
            onClick = if (tamamlandi) onGeriAl else onTamamla,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            colors = if (tamamlandi) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Text(
                text = stringResource(
                    if (tamamlandi) R.string.tamamlamayi_geri_al else R.string.tamamla
                )
            )
        }
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
        title = { Text(stringResource(R.string.sil_onay_baslik)) },
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
