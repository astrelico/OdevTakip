package com.odevtakip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.odevtakip.app.R
import com.odevtakip.app.util.TarihYaklasimi
import com.odevtakip.app.util.formatliSaat
import com.odevtakip.app.util.formatliTarih
import com.odevtakip.app.util.tarihYaklasimi

/**
 * Bir tarihin gün olarak adını verir: "Bugün", "Yarın", "Dün" ya da "3 Eki 2026".
 *
 * Göreli adlar kaynaktan gelir; burada yerelleştirilmiş metne çevrilir.
 */
@Composable
fun gunMetni(millis: Long): String = when (tarihYaklasimi(millis)) {
    TarihYaklasimi.BUGUN -> stringResource(R.string.bugun)
    TarihYaklasimi.YARIN -> stringResource(R.string.yarin)
    TarihYaklasimi.DUN -> stringResource(R.string.dun)
    TarihYaklasimi.UZAK -> millis.formatliTarih()
}

/**
 * Teslim tarihini "Bugün 23:59" biçiminde gösterir.
 *
 * Liste ve detay ekranları aynı biçimi kullanır; farklı görünürlerse
 * kullanıcı aynı ödevi farklı zamanlı sanabilir.
 */
@Composable
fun teslimMetni(sonTarih: Long): String =
    "${gunMetni(sonTarih)} ${sonTarih.formatliSaat()}"
