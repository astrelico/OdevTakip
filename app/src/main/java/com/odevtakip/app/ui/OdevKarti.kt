package com.odevtakip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum

/**
 * Listelerde kullanılan tek ödev kartı.
 *
 * Liste ve takvim ekranları aynı bileşeni paylaşır; iki ekran aynı ödevi
 * farklı görürse kullanıcı yanlış kartta işlem yapabilir.
 *
 * Görünüm mantığı referans tasarımla uyumludur: zemin daima [MaterialTheme]'ın
 * `surface`'idir (açık temada beyaz), ayrım çerçevelerle değil **gölgeyle**
 * kurulur. Durum; onay kutusu, üstü çizili başlık ve rozet ile bildirilir.
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

    Card(
        onClick = onSec,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = tamamlandi,
                onCheckedChange = onDegistir,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 8.dp),
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
