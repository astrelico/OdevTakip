package com.odevtakip.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odevtakip.app.R
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum
import kotlinx.coroutines.launch

/**
 * Listelerde kullanılan tek ödev kartı.
 *
 * Liste ve takvim ekranları aynı bileşeni paylaşır; iki ekran aynı ödevi
 * farklı görürse kullanıcı yanlış kartta işlem yapabilir.
 *
 * Görünüm mantığı referans tasarımla uyumludur: zemin daima [MaterialTheme]'ın
 * `surface`'idir (açık temada beyaz), ayrım çerçevelerle değil **gölgeyle**
 * kurulur. Durum; üstü çizili başlık ve renkli rozet ile bildirilir.
 *
 * ### Tamamlama: kaydırma
 *
 * Kare onay kutusu kaldırıldı; kart **sağa kaydırınca tamamlanır**, kart
 * tamamlanmışken **sola kaydırınca geri alınır**. Yön duruma göre tek olur —
 * böylece arkada görünen zemin her zaman tek ve doğru bir eylemi anlatır.
 *
 * Kaydırma bir "dismiss" değildir: kart listeden silinmez, eylemden hemen
 * sonra yerine döner ([SwipeToDismissBoxState.snapTo]). "Neden kaybolmadı?"
 * sorusunun önüne geçmek için zeminde eylemin adı da yazılıdır.
 */
@Composable
fun OdevKarti(
    odev: Odev,
    onSec: () -> Unit,
    onDegistir: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durum = odev.gercekDurum(System.currentTimeMillis())
    val tamamlandi = durum == Durum.TAMAMLANDI
    val renk = durumRengi(durum)

    val swipeDurumu = rememberSwipeToDismissBoxState()
    val hiz = rememberCoroutineScope()

    // Eylem her zaman güncel `onDegistir` ile çağrılır ama
    // `onDismiss`'in kimliği sabit tutulur. Kimlik her recomposition'da
    // değişseydi SwipeToDismissBox kendi effect'ini yeniden başlatır ve ödev
    // iki kez işaretlenirdi (bkz. SwipeToDismissBox'ın onDismiss effect'i).
    val eylem = rememberUpdatedState<(SwipeToDismissBoxValue) -> Unit> { yon ->
        when (yon) {
            SwipeToDismissBoxValue.StartToEnd -> onDegistir(true)
            SwipeToDismissBoxValue.EndToStart -> onDegistir(false)
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    val onDismiss = remember<(SwipeToDismissBoxValue) -> Unit> {
        { yon ->
            eylem.value(yon)
            // Geri dönüş, kartın kompozisyonundan bağımsız bir scope'ta
            // başlatılır: liste yeniden çizilse de yarı kaymış kart kalmaz.
            hiz.launch { swipeDurumu.snapTo(SwipeToDismissBoxValue.Settled) }
        }
    }

    val eylemEtiketi = stringResource(
        if (tamamlandi) R.string.tamamlamayi_geri_al else R.string.tamamla
    )

    SwipeToDismissBox(
        state = swipeDurumu,
        onDismiss = onDismiss,
        enableDismissFromStartToEnd = !tamamlandi,
        enableDismissFromEndToStart = tamamlandi,
        backgroundContent = {
            SwipeArkaPlani(
                tamamlandi = tamamlandi,
                modifier = Modifier.fillMaxSize(),
            )
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Card(
            onClick = onSec,
            modifier = Modifier
                .fillMaxWidth()
                // Kaydırma jesti TalkBack'te kullanılamaz; aynı eylem
                // kartın erişilebilirlik menüsüne elle eklenir.
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(eylemEtiketi) {
                            onDegistir(!tamamlandi)
                            true
                        },
                    )
                },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = odev.baslik,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (tamamlandi) TextDecoration.LineThrough else null,
                        color = if (tamamlandi) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )

                    if (odev.aciklama.isNotBlank()) {
                        Text(
                            text = odev.aciklama,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = renk,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = teslimMetni(odev.sonTarih),
                            style = MaterialTheme.typography.labelMedium,
                            color = renk,
                        )
                    }
                }

                DurumRozeti(durum = durum, renk = renk)
            }
        }
    }
}

/**
 * Kaydırırken arkada görünen zemin ve eylemin adı.
 *
 * Zemin kartla aynı köşe yarıçapını taşır; kart tam kaydığında köşeler
 * hizalanır, yarım kaydırmada da kenar yumuşak görünür.
 *
 * Yön durumdan türetilir: bekleyen kart sağa kaydırılır (sağ tarafa
 * yığılır, içerik **sol** kenarda görünür), tamamlanmış kart sola
 * (içerik **sağ** kenarda). Bu yüzden hizalama da durumla birlikte döner.
 */
@Composable
private fun SwipeArkaPlani(
    tamamlandi: Boolean,
    modifier: Modifier = Modifier,
) {
    val renkler = MaterialTheme.colorScheme
    val zemin = if (tamamlandi) renkler.secondaryContainer else renkler.primary
    val icerikRengi = if (tamamlandi) renkler.onSecondaryContainer else renkler.onPrimary

    Box(
        modifier = modifier.background(color = zemin, shape = MaterialTheme.shapes.large),
        contentAlignment = if (tamamlandi) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tamamlandi) {
                Etiket(stringResource(R.string.geri_al), icerikRengi)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = null,
                    tint = icerikRengi,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = icerikRengi,
                )
                Spacer(Modifier.width(8.dp))
                Etiket(stringResource(R.string.durum_tamamlandi), icerikRengi)
            }
        }
    }
}

@Composable
private fun Etiket(metin: String, renk: Color) {
    Text(
        text = metin,
        style = MaterialTheme.typography.labelLarge,
        color = renk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Durum rozeti: %14 tonlanmış zemin + tam renkli metin.
 *
 * Zemin her zaman kartın `surface`'i üzerine biner; açık temada beyaz, koyu
 * temada koyu yüzey — her ikisinde de ~8:1 kontrast kalır.
 */
@Composable
private fun DurumRozeti(durum: Durum, renk: Color) {
    Surface(
        color = renk.copy(alpha = 0.14f),
        contentColor = renk,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text = durumMetni(durum),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
