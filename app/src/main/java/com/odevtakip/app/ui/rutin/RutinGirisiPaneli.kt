@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.odevtakip.app.ui.rutin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R
import com.odevtakip.app.data.RutinKaydi
import com.odevtakip.app.ui.deneme.tamSayiCoz
import com.odevtakip.app.ui.dersler.DersSecimPaneli
import com.odevtakip.app.ui.kartCizgisi
import com.odevtakip.app.ui.kartElevasyonu
import com.odevtakip.app.util.gunBaslangici
import com.odevtakip.app.util.gunUzunAdi
import com.odevtakip.app.util.tarihMetni
import java.time.LocalDate

/**
 * Bir güne ders kaydı **ekleyen** ya da **düzenleyen** alt panel.
 *
 * Panel, gün kartındaki "+" satırı ya da bir ders satırı tarafından açılır.
 * Ayrı bir form sayfası değildir: günde bir-iki satır girmek için ekran
 * değiştirmek ağır olurdu ve kullanıcı günün bağlamını (hangi gün
 * olduğunu) kaybederdi.
 *
 * **Ders seçimi ödevdeki gibi listeden yapılır**: alan dokunulunca alttan
 * [com.odevtakip.app.ui.dersler.DersSecimPaneli] açılır, tek dokunuşla
 * seçim tamamlanır. Çip satırının yerini bu almıştır: dersler artık
 * elle yazılmış metin değil, kullanıcının kendi listesidir ve uzun
 * listenin tek satıra sığması mümkün değildir.
 *
 * **Ders başlığı isteğe bağlıdır**: aynı günün aynı dersinden ikinci,
 * üçüncü kayıt birbirinden ancak isimle ayrılır ("Matematik 2"). Boş
 * bırakılırsa kartta yalnız ders adı görünür — bu yüzden eski kayıtlar
 * da uygulama güncellendiğinde aynı gibi okunur.
 *
 * **Düzenleme yalnızca o satırı etkiler**: kayıt `id`siyle yazılır
 * ([com.odevtakip.app.data.RutinDao.kaydet]) ve 9. sürümde `(gün,
 * ders)` unique indeksi kalktığı için dersi ya da başlığı değiştirmek
 * başka bir kaydı bozmaz; ikinci kaydı silecek bir çakışma yoktur.
 *
 * @param tarih Kaydın günü — panelin başlığında yazılır ve kaydedilen
 *   değerin `tarih`i olur.
 * @param mevcut Düzenlenecek kayıt; `null` ise yeni ekleme.
 * @param dersler Seçilebilecek ders adları (kullanıcının kendi listesi).
 * @param onKaydet Kaydet düğmesi; panel kapanması çağıran taraftadır.
 * @param onSil Düzenlemede görünen silme düğmesi.
 * @param onKapat Vazgeç: panel kapanır, hiçbir şey yazılmaz.
 */
@Composable
fun RutinGirisiPaneli(
    tarih: LocalDate,
    mevcut: RutinKaydi?,
    dersler: List<String>,
    onKaydet: (RutinKaydi) -> Unit,
    onSil: (Long) -> Unit,
    onKapat: () -> Unit,
) {
    val renkler = MaterialTheme.colorScheme
    val sayfa = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Durum, **amaçla** birlikte sıfırlanır: panel kapanıp başka bir güne
    // yeniden açıldığında eski sayılar taşınmaz.
    var seciliDers by remember(tarih, mevcut) { mutableStateOf(mevcut?.ders.orEmpty()) }
    var baslikMetni by remember(tarih, mevcut) { mutableStateOf(mevcut?.baslik.orEmpty()) }
    var soruMetni by remember(tarih, mevcut) { mutableStateOf(metin(mevcut?.soru)) }
    var dogruMetni by remember(tarih, mevcut) { mutableStateOf(metin(mevcut?.dogru)) }
    var yanlisMetni by remember(tarih, mevcut) { mutableStateOf(metin(mevcut?.yanlis)) }

    // Ders listesi, panelin **üstünde** açılan ikinci bir paneldir. Form
    // kapanmaz ki seçim sırasında doldurulan sayılar kaybolmasın.
    var dersPanelGoster by remember { mutableStateOf(false) }

    val soru = tamSayiCoz(soruMetni)
    val dogru = tamSayiCoz(dogruMetni)
    val yanlis = tamSayiCoz(yanlisMetni)

    // Doğrulama: hepsi yalnızca girdiye bakar, birbirine değil.
    val sayiHatasi = soru == null || dogru == null || yanlis == null
    val toplamHatasi = !sayiHatasi && dogru!! + yanlis!! > soru!!
    val dersSecildi = seciliDers.isNotEmpty()
    val kaydedilebilir = !sayiHatasi && !toplamHatasi && dersSecildi

    ModalBottomSheet(
        onDismissRequest = onKapat,
        sheetState = sayfa,
        containerColor = renkler.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text(
                    text = stringResource(
                        if (mevcut == null) R.string.rutin_panel_baslik
                        else R.string.rutin_panel_duzenle,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    // Hangi güne yazıldığı panelin en üstünde durur: kullanıcı
                    // üç alan doldururken hangi günde olduğunu unutmasın.
                    text = "${tarih.tarihMetni()} · ${tarih.gunUzunAdi()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = renkler.onSurfaceVariant,
                )
            }

            // ---- Ders ----

            // Kart etiketini **kendi içinde** taşır (ödev formuyla aynı
            // dil); üstte ayrı bir başlık satırı olsaydı "Ders" iki kez
            // yazılmış olurdu.
            DersSecimKart(
                ders = seciliDers,
                onClick = { dersPanelGoster = true },
                modifier = Modifier.fillMaxWidth(),
            )

            if (!dersSecildi) {
                Text(
                    text = stringResource(R.string.rutin_ders_zorunlu),
                    style = MaterialTheme.typography.bodySmall,
                    color = renkler.error,
                )
            }

            // ---- Ders başlığı (isteğe bağlı) ----

            // Aynı günün aynı dersinden ikinci kaydı ayıran isim. Boş
            // bırakmak serbesttir: kartta yalnız ders adı görünür ve
            // tek kayıt giren kullanıcı hiçbir şey yazmak zorunda değildir.
            OutlinedTextField(
                value = baslikMetni,
                onValueChange = { baslikMetni = it },
                label = { Text(stringResource(R.string.rutin_etiket_baslik)) },
                placeholder = { Text(stringResource(R.string.rutin_baslik_ipucu)) },
                supportingText = { Text(stringResource(R.string.rutin_baslik_yardim)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = renkler.surface,
                    unfocusedContainerColor = renkler.surface,
                    disabledContainerColor = renkler.surface,
                    errorContainerColor = renkler.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // ---- Sayılar ----

            SayiAlani(
                deger = soruMetni,
                onDegisim = { soruMetni = it },
                etiket = stringResource(R.string.rutin_etiket_soru),
                hata = when {
                    soru == null -> stringResource(R.string.rutin_sayi_hata)
                    toplamHatasi -> stringResource(R.string.rutin_toplam_hata)
                    else -> null
                },
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SayiAlani(
                    deger = dogruMetni,
                    onDegisim = { dogruMetni = it },
                    etiket = stringResource(R.string.rutin_etiket_dogru),
                    hata = if (dogru == null) stringResource(R.string.rutin_sayi_hata) else null,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f),
                )
                SayiAlani(
                    deger = yanlisMetni,
                    onDegisim = { yanlisMetni = it },
                    etiket = stringResource(R.string.rutin_etiket_yanlis),
                    hata = if (yanlis == null) stringResource(R.string.rutin_sayi_hata) else null,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = {
                    // `kaydedilebilir` her zaman doğru buraya gelindiğinde;
                    // yine de savunma olarak yeniden bakılır — düğme
                    // "kapalı" hâlde basılırsa kayıt yazılmaz.
                    if (!kaydedilebilir) return@Button
                    onKaydet(
                        RutinKaydi(
                            id = mevcut?.id ?: 0L,
                            tarih = tarih.gunBaslangici(),
                            ders = seciliDers,
                            soru = soru!!,
                            dogru = dogru!!,
                            yanlis = yanlis!!,
                            // Başlık kırpılır: "Matematik 2 " ile
                            // "Matematik 2" aynı kayıttır. Boş kalırsa
                            // kartta ders adı görünür (bkz. sınıf notu).
                            baslik = baslikMetni.trim(),
                        ),
                    )
                },
                enabled = kaydedilebilir,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.rutin_kaydet))
            }

            if (mevcut != null) {
                OutlinedButton(
                    onClick = { onSil(mevcut.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.rutin_sil),
                        color = renkler.error,
                    )
                }
            }
        }
    }

    // Ders listesi, panelin **üstünde** açılır: form kapanmaz, doldurulan
    // sayılar ve başlık seçim sırasında taşınmaya devam eder. Panel bir
    // `Dialog` olduğundan üstüne gelen ikinci panel de üstte durur ve geri
    // tuşu yalnız onu kapatır.
    if (dersPanelGoster) {
        DersSecimPaneli(
            baslik = stringResource(R.string.ders_sec),
            dersler = dersler,
            secili = seciliDers.takeIf { it.isNotBlank() },
            onSecim = { seciliDers = it.orEmpty() },
            onKapat = { dersPanelGoster = false },
        )
    }
}

/** Boş alanda `""`, dolu alanda sayının metni. */
private fun metin(deger: Int?): String = deger?.toString().orEmpty()

/**
 * Dersin seçileceği kart — ödev formundaki seçicinin **aynısı**.
 *
 * Bir metin kutusu gibi değil, seçilebilir bir satır gibi çizilir:
 * etiketi ve değeri içinde durur, dokunma kartın tamamındadır ve tek
 * işi [DersSecimPaneli]yi açmaktır. Değer seçilmemişse soluk görünür:
 * alan hâlâ boştur, "seçildi" sanılmasın.
 */
@Composable
private fun DersSecimKart(
    ders: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = renkler.surface),
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
                tint = renkler.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.rutin_etiket_ders),
                    style = MaterialTheme.typography.labelSmall,
                    color = renkler.onSurfaceVariant,
                )
                Text(
                    text = ders.ifBlank { stringResource(R.string.ders_secilmedi) },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (ders.isBlank()) {
                        renkler.onSurfaceVariant
                    } else {
                        renkler.onSurface
                    },
                )
            }
        }
    }
}

/**
 * Panelin sayı alanı: yalnız sayı yazılabilen, hatasını altında söyleyen
 * tek satırlık alan.
 *
 * [com.odevtakip.app.ui.deneme.DenemeFormEkrani]'ndaki alanla aynı
 * görünür; burada **kopyalanmıştır**, çünkü oradaki `private`'dır ve iki
 * ekranın ihtiyacı birbirinden ayrı gelişti — paylaşmak için ortak bir
 * yere taşımak, bu panelin tek işini de o dosyaya bağlardı.
 */
@Composable
private fun SayiAlani(
    deger: String,
    onDegisim: (String) -> Unit,
    etiket: String,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    hata: String? = null,
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
        placeholder = { Text("0") },
        singleLine = true,
        isError = hata != null,
        supportingText = if (hata != null) {
            {
                Text(
                    text = hata,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
        colors = alanRenkleri,
        modifier = modifier,
    )
}
