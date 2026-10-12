package com.odevtakip.app.ui.hakkinda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odevtakip.app.BuildConfig
import com.odevtakip.app.R
import com.odevtakip.app.ui.UstBar
import com.odevtakip.app.ui.UygulamaSimgesi

/**
 * Uygulama hakkında ekranı — yan menüdeki "Hakkında" düğmesinin açtığı rota.
 *
 * İçerik dört karta bölünür:
 *
 *  - **Başlık kartı:** el çizimi uygulama simgesi, ad ve sürüm.
 *  - **Ne yapar:** dört madde; uygulamanın kapsamını tek bakışta söyler.
 *  - **Tasarım atıfı:** Figma referansının künyesi. **CC BY 4.0 lisansı
 *    atfı şart koşar** — bu yüzden ekran içine de, README'ye de yazılır.
 *  - **Künye:** paket adı, minSdk, dil ve teknoloji; hata bildirirken
 *    istenecek bilgiler buradadır.
 *
 * Ekran salt okunurdur; hiçbir düğme başka yere gitmez. Geri düğmesi
 * [onGeri] ile yığına döner.
 *
 * @param onGeri Üst bardaki geri düğmesi.
 */
@Composable
fun HakkindaEkrani(
    onGeri: () -> Unit,
) {
    val baglam = LocalContext.current

    Scaffold(
        topBar = {
            UstBar(
                baslik = { Text(stringResource(R.string.hakkinda_baslik)) },
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BaslikKarti()

            UcKart(
                baslik = stringResource(R.string.hakkinda_ozellikler_baslik),
                maddeler = listOf(
                    stringResource(R.string.hakkinda_ozellik_1),
                    stringResource(R.string.hakkinda_ozellik_2),
                    stringResource(R.string.hakkinda_ozellik_3),
                    stringResource(R.string.hakkinda_ozellik_4),
                ),
            )

            TasarimKarti()

            KunyeKarti(paketAdi = baglam.packageName)

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Üstteki kimlik kartı: simge + uygulama adı + sürüm. */
@Composable
private fun BaslikKarti() {
    val renkler = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = renkler.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Uygulama simgesi dairenin içinde durur; köşe yarıçapı tema
            // şekilleriyle aynı ölçekte seçilir.
            UygulamaSimgesi(
                boyut = 72.dp,
                modifier = Modifier.clip(CircleShape),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = stringResource(R.string.hakkinda_surum, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = renkler.onSurfaceVariant,
            )
        }
    }
}

/**
 * Başlıklı kart: bir açıklama satırı + işaretli maddeler.
 *
 * [baslik] kartın kendi başlığıdır — üst barın başlığıyla aynı olmak
 * zorunda değildir.
 */
@Composable
private fun UcKart(
    baslik: String,
    maddeler: List<String>,
) {
    KartYuzeyi(baslik = baslik) {
        maddeler.forEach { madde ->
            Madde(metin = madde)
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Tasarım atıfı kartı — CC BY 4.0 şartının ekran içindeki karşılığı. */
@Composable
private fun TasarimKarti() {
    KartYuzeyi(baslik = stringResource(R.string.hakkinda_tasarim_baslik)) {
        Text(
            text = stringResource(R.string.hakkinda_tasarim_metin),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.hakkinda_tasarim_eser),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = stringResource(R.string.hakkinda_tasarim_sahip),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(R.string.hakkinda_tasarim_lisans),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.hakkinda_tasarim_not),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Künye kartı: hata bildirirken istenecek sabit bilgiler. */
@Composable
private fun KunyeKarti(paketAdi: String) {
    val satirlar = listOf(
        stringResource(R.string.hakkinda_kunye_uygulama) to stringResource(R.string.app_name),
        stringResource(R.string.hakkinda_kunye_paket) to paketAdi,
        stringResource(R.string.hakkinda_kunye_min) to stringResource(R.string.hakkinda_kunye_min_deger),
        stringResource(R.string.hakkinda_kunye_dil) to stringResource(R.string.hakkinda_kunye_dil_deger),
        stringResource(R.string.hakkinda_kunye_teknoloji) to stringResource(R.string.hakkinda_kunye_teknoloji_deger),
    )

    KartYuzeyi(baslik = stringResource(R.string.hakkinda_kunye_baslik)) {
        satirlar.forEach { (etiket, deger) ->
            Satir(etiket = etiket, deger = deger)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Ortak kart yüzeyi: başlık + içerik sütunu.
 *
 * Üç kart da aynı zemin, aynı köşe ve aynı iç boşluğu paylaşır; ayrı
 * ayrı kurulsaydı aralarında 1 dp'lik sapmalar birikirdi.
 */
@Composable
private fun KartYuzeyi(
    baslik: String,
    icerik: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = baslik,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            icerik()
        }
    }
}

/** İşaretli madde: tik ikonu + metin. */
@Composable
private fun Madde(metin: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = metin,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Etiket–değer satırı; etiket solda, değer sağa yaslı. */
@Composable
private fun Satir(
    etiket: String,
    deger: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiket,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = deger,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
