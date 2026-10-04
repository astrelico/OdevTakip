package com.odevtakip.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.odevtakip.app.R
import com.odevtakip.app.data.Durum

/**
 * Durumun arayüzdeki rengi.
 *
 * Material renk rollerinden türetilir; böylece Faz 5'te Figma teması
 * geldiğinde bu yardımcılar otomatik olarak yeni palete uyum sağlar.
 *
 *  - Gecikti → `error` (kırmızı)
 *  - Tamamlandı → `primary`
 *  - Bekliyor → `secondary`
 */
@Composable
fun durumRengi(durum: Durum): Color = when (durum) {
    Durum.GECEKTI -> MaterialTheme.colorScheme.error
    Durum.TAMAMLANDI -> MaterialTheme.colorScheme.primary
    Durum.BEKLIYOR -> MaterialTheme.colorScheme.secondary
}

/** Durumun kullanıcıya gösterilecek Türkçe metni. */
@Composable
fun durumMetni(durum: Durum): String = when (durum) {
    Durum.GECEKTI -> stringResource(R.string.durum_gecikti)
    Durum.TAMAMLANDI -> stringResource(R.string.durum_tamamlandi)
    Durum.BEKLIYOR -> stringResource(R.string.durum_bekliyor)
}
