package com.odevtakip.app.ui.deneme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odevtakip.app.data.DenemeTuru

/**
 * Deneme ekranlarının **ortak görsel dili**: tür rozeti ve doğru/yanlış
 * oran şeridi.
 *
 * Liste kartı ile detay ekranı aynı sayıyı iki kez gösterir (bkz.
 * [DenemeKarti], [DenemeDetayEkrani]). Sayının **kendisi** farklı
 * okunabilir — kartta özet, detayda ayrıntı durur — ama **işaretleri**
 * aynı olmalıdır: "mavi = doğru, kırmızı = yanlış" öğretisi bir kez
 * verilir; kullanıcı ikinci ekranda yeniden öğrenmez.
 *
 * Bu dosyadaki iki bileşen bu yüzden tek yerdedir:
 *
 *  - [TurRozeti]: denemenin türü ("TYT" / "AYT"). Aynı mavi ailesinin iki
 *    farklı doygunluğudur — kırmızı-yeşil körlüğünde de ayırt edilir.
 *  - [OranSeridi]: doğru ile yanlışın **oransal** dökümü. Yanlış sayısı
 *    sabit 5 iken 100 soruda ve 20 soruda şeridin yüksekliği aynıdır; bu,
 *    sayıyı okumadan "bu deneme ne kadar temiz" hissini verir.
 */

// ---- Tür rozeti ----

/**
 * Denemenin türünü taşıyan küçük **rozet**: yuvarlak köşeli, tonlu zeminli
 * etiket.
 *
 * Düz metin yerine rozet kullanılmasının iki nedeni vardır:
 *
 *  1. **Kategoridir, sayı değildir.** Listenin en üstünde duran satır
 *     "bu kayıt hangi formatta" der; okunur bir blok olması, tarihin ve
 *     adın arasına sıkışıp kaybolmasını engeller.
 *  2. **Tema rollerinden türer.** Zemin `primaryContainer` /
 *     `secondaryContainer`, metin `onPrimaryContainer` /
 *     `onSecondaryContainer`dır; yeni bir palet gelse rozet kendiliğinden
 *     uyum sağlar. Sabit renk yazılsaydı koyu temada ya okunmaz ya da
 *     göz kamaştırıcı olurdu.
 *
 * TYT `primary`, AYT `secondary` üzerinden gider: ikisi de aynı mavi
 * ailesinde kalır, yalnızca doygunluk değişir.
 */
@Composable
fun TurRozeti(tur: DenemeTuru, modifier: Modifier = Modifier) {
    val renkler = MaterialTheme.colorScheme
    val zemin = when (tur) {
        DenemeTuru.TYT -> renkler.primaryContainer
        DenemeTuru.AYT -> renkler.secondaryContainer
    }
    val metin = when (tur) {
        DenemeTuru.TYT -> renkler.onPrimaryContainer
        DenemeTuru.AYT -> renkler.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = zemin,
    ) {
        Text(
            text = turEtiketi(tur),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = metin,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

// ---- Oran şeridi ----

/**
 * Doğru ile yanlışın **oransal** dökümü: solda mavi (doğru), sağda kırmızı
 * (yanlış), en sonda istenirse boş sorular.
 *
 * Şerit sayının **değil**, **orantının** taşıyıcısıdır. Kullanıcı "5 yanlış"
 * sayısını tek başına okuyunca iyi mi kötü mü karar veremez; şeridin kırmızı
 * tarafının dörtte birini görünce judgment'ı kendisi verir. Bu yüzden şerit
 * toplam üzerinden ölçeklenir.
 *
 * Boşluk **yoktur**: parçalar yan yana durur. İki renk arasına çizgi
 * koymak, ayrı nesne oldukları izlenimini verir — ama ikisi de aynı
 * cevap kümesinin parçasıdır, şeridin devamlılığı bunu doğru anlatır.
 *
 * @param dogru Doğru cevap sayısı; şeridin sol parçası.
 * @param yanlis Yanlış cevap sayısı; şeridin sağ parçası.
 * @param bos Boş cevap sayısı. Verilirse şeridin **tamamı** üç parçaya
 *   bölünür — detay ekranında "kaç soru hiç dokunulmadı" sorusunun
 *   görsel yanıtıdır. Verilmezse yalnız doğru/yanlış üzerinden
 *   ölçeklenir; liste kartında bu yeterlidir, çünkü kartın derdi
 *   "temiz miydi" sorusudur, "kaçı boştu" değil.
 * @param enKucukAgirlik Bir parçanın en az kaplayacağı pay. Sayısal
 *   olarak küçük ama **görsel olarak görünmesi gereken** bir parçayı
 *   tamamen kaybolmaktan kurtarır: 3 yanlışın 120 soruda payı %2,5tur
 *   ve 6 dp'lik şeritte yuvarlanıp çizgi olurdu.
 */
@Composable
fun OranSeridi(
    dogru: Int,
    yanlis: Int,
    modifier: Modifier = Modifier,
    bos: Int = 0,
    enKucukAgirlik: Float = 0.02f,
) {
    val renkler = MaterialTheme.colorScheme
    val toplam = (dogru + yanlis + bos).coerceAtLeast(1)

    fun pay(sayi: Int): Float =
        if (sayi <= 0) 0f
        else (sayi.toFloat() / toplam).coerceAtLeast(enKucukAgirlik)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(renkler.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        // Parçaların ağırlığı = gerçek sayı. `weight` Compose'un kendi
        // ölçümüne bırakılır: elle hesaplanan dp, farklı ekranlarda
        // yuvarlama hatası biriktirirdi.
        if (dogru > 0) SeritParcasi(renkler.primary, pay(dogru))
        if (yanlis > 0) SeritParcasi(renkler.error, pay(yanlis))
        if (bos > 0) SeritParcasi(renkler.outlineVariant.copy(alpha = 0.5f), pay(bos))
    }
}

/** Şeridin tek parçası; [agirlik] toplam içindeki payıdır. */
@Composable
private fun RowScope.SeritParcasi(renk: Color, agirlik: Float) {
    Box(
        modifier = Modifier
            .weight(agirlik)
            .fillMaxHeight()
            .background(renk),
    )
}
