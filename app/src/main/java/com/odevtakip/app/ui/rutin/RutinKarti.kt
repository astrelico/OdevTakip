package com.odevtakip.app.ui.rutin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R
import com.odevtakip.app.data.RutinKaydi
import com.odevtakip.app.data.baloncukOrani
import com.odevtakip.app.data.gunOzetiniHesapla
import com.odevtakip.app.ui.kartCizgisi
import com.odevtakip.app.ui.kartElevasyonu
import com.odevtakip.app.util.gunUzunAdi
import com.odevtakip.app.util.tarihMetni
import java.time.LocalDate

/**
 * Haftalık görünümde bir **gün kartı**.
 *
 * Kart üç parçadır: başlıkta günün adı ve tarihi, sağında o günün toplamını
 * taşıyan **baloncuk**, altında ise derslerin dökümü ve "+": o güne ders
 * ekleme satırı.
 *
 * Baloncuk [baloncukOrani] ile **karekök** ölçeklenir: 10 ile 20 soru
 * arasındaki fark okunur kalırken 200 ile 210 arasındaki fark ayırt
 * edilemez. Rakam daima gerçek toplamdır — baloncuk yalnızca **görsel
 * kısayoldur**, okumak için sayıya bakmak gerekmez.
 *
 * @param gun Kartın günü.
 * @param kayitlar **Bu güne** ait kayıtlar; baştan süzülmüş ve sıralanmış
 *   olmalıdır (bkz. [RutinKaydi]).
 * @param enBuyukGunSoru Baloncukların birbirine oranlanacağı taban —
 *   dönemin en yoğun günü. Dönem bomboşsa `0` verilir, baloncuk çizilmez.
 * @param onDersEkle Kartın altındaki "+" satırı; paneli bu güne açar.
 * @param onDersDuzenle Bir ders satırına dokununca; paneli o kaydı
 *   doldurarak açar.
 */
@Composable
fun RutinKarti(
    gun: LocalDate,
    kayitlar: List<RutinKaydi>,
    enBuyukGunSoru: Int,
    onDersEkle: () -> Unit,
    onDersDuzenle: (RutinKaydi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme
    val bugun = LocalDate.now()
    val ozet = gunOzetiniHesapla(kayitlar)
    val oran = baloncukOrani(ozet.soru, enBuyukGunSoru)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = renkler.surface),
        border = kartCizgisi(),
        elevation = kartElevasyonu(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // Günün adı, bugünün rengiyle vurgulanır: yedi kartın
                    // içinde kullanıcının nerede olduğunu tek bakışta
                    // gösteren işaret budur.
                    text = gun.gunUzunAdi(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (gun == bugun) renkler.primary else renkler.onSurface,
                )
                Text(
                    text = gun.tarihMetni(),
                    style = MaterialTheme.typography.bodySmall,
                    color = renkler.onSurfaceVariant,
                )
            }

            if (ozet.soru > 0) {
                Baloncuk(soru = ozet.soru, oran = oran)
            }
        }

        if (kayitlar.isEmpty()) {
            // Boş gün: kart yok olmaz, "kayıt yok" der ve "+"yı gösterir.
            // Kayıt olan kartlarla aynı yükseklikte kalır ki liste
            // kaydırırken zıplamasın.
            Text(
                text = stringResource(R.string.rutin_gun_bos),
                style = MaterialTheme.typography.bodySmall,
                color = renkler.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                kayitlar.forEach { kayit ->
                    DersSatiri(kayit = kayit, onDuzenle = { onDersDuzenle(kayit) })
                }
            }
        }

        DersEkleSatiri(onClick = onDersEkle)
    }
}

/**
 * Kartın bir ders satırı: ad, çözülenlerin dökümü ve düzenleme oku.
 *
 * Satırın **tamamı** dokunulabilir — düzenlemenin yolu satırdır; ok yalnızca
 * bunu gösteren işarettir, ayrıca bir hedef değildir.
 */
@Composable
private fun DersSatiri(
    kayit: RutinKaydi,
    onDuzenle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme

    Surface(
        onClick = onDuzenle,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = kayit.ders,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(
                        R.string.rutin_dogru_yanlis,
                        kayit.dogru,
                        kayit.yanlis,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = renkler.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.rutin_soru, kayit.soru),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = renkler.primary,
            )

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = renkler.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Kartın alt satırı: o güne ders ekleme.
 *
 * Doldurulmamış bir satır gibi değil, **davet** gibi çizilir: artı işareti
 * ve metni birincil renktedir, zemin şeffaftır. Kartta kaydı olmayan
 * kullanıcı tek bakışta nereye basacağını görür.
 */
@Composable
private fun DersEkleSatiri(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = renkler.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.rutin_kayit_ekle),
                style = MaterialTheme.typography.labelLarge,
                color = renkler.primary,
            )
        }
    }
}

/**
 * O günün toplamını taşıyan **baloncuk**.
 *
 * Dairenin büyüklüğü **ve** doluluğu [oran]la birlikte artar: az çözülmüş
 * gün soluk ve küçük, çok çözülmüş gün koyu ve büyük kalır. İki işaretin de
 * aynı yönde olması, renk körü kullanıcıda dahi sıralamayı okunur kılar.
 *
 * Rakam gerçek toplamdır; baloncuk onun **görsel özetidir**, yerine geçmez.
 */
@Composable
private fun Baloncuk(
    soru: Int,
    oran: Float,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme
    val boyut = BALONCUK_EN_KUCUK + (BALONCUK_EN_BUYUK - BALONCUK_EN_KUCUK) * oran

    Box(
        modifier = modifier
            .size(boyut)
            .clip(CircleShape)
            .background(renkler.primary.copy(alpha = 0.12f + 0.18f * oran)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = soru.toString(),
            style = if (oran > 0.55f) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.titleSmall
            },
            fontWeight = FontWeight.Bold,
            color = renkler.primary,
        )
    }
}

// Baloncuk uçları: en küçük gün 40 dp'de kalır (okunur), en büyük gün 64 dp'ye
// kadar büyür. Kartın satır yüksekliğini zorlamadan farkı görünür kılar.
private val BALONCUK_EN_KUCUK = 40.dp
private val BALONCUK_EN_BUYUK = 64.dp
