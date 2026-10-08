package com.odevtakip.app.ui

import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate

/**
 * Widget'ın ekranda çizebileceği azami ödev satırı.
 *
 * Sınır bilinçli: ana ekran widget'ı bir liste değil, **bakışta özet**
 * olduğu içindir. Üç satır dört hücrelik bir alanda okunur; daha fazlası
 * kullanıcıyı kartlara geri gönderir.
 */
const val WIDGET_SATIR_SAYISI = 3

/**
 * Ana ekran widget'ının göstereceği içerik.
 *
 * [satirlar] her zaman **sıralı** ve en fazla [WIDGET_SATIR_SAYISI]
 * uzunluktadır; sayaçlar ise gösterilmeyen ödevleri de kapsar, böylece
 * widget üç satırdan fazlası varsa "kalanı" söyleyebilir.
 *
 * @property geciken Teslim tarihi geçmiş ve tamamlanmamış ödev sayısı.
 * @property bugun Bugün teslim edilecek (henüz geçmemiş) tamamlanmamış ödev sayısı.
 * @property satirlar Ekrana çıkacak ödevler, öncelik sırasına göre.
 * @property bekleyenToplam Tamamlanmamış tüm ödev sayısı.
 */
data class WidgetIcerigi(
    val geciken: Int,
    val bugun: Int,
    val satirlar: List<Odev>,
    val bekleyenToplam: Int,
) {
    /** Bekleyen hiçbir ödev yok — widget "ödev yok" der. */
    val bos: Boolean get() = bekleyenToplam == 0
}

/**
 * Widget'ın ne göstereceğini ve hangi sırayla göstereceğini hesaplar.
 *
 * Saftır: [simdi] ve [bugun] dışarıdan verildiği için testlerde zaman
 * dondurulabilir. Varsayılanları gerçeği yansıtır (`bugun` zaten
 * [simdi]'nin yeri günüdür) — çağıran kodun ikisini de yazması gerekmez.
 *
 * ### Kova ayrımı
 *
 * Üç kova **kesişmez**; bir ödev aynı anda iki kez sayılmaz:
 *
 *  - **geciken**: `gercekDurum == GECEKTI` — teslim saati geçmiş. Bugün
 *    teslimli ama saati geçmiş bir iş de buradadır; "bugün" demek onu
 *    hem geciken hem bugün sayıp özet şişirmek olurdu.
 *  - **bugün**: bugün teslim edilecek, henüz saati geçmemiş.
 *  - **diğer**: ileri tarihli.
 *
 * Sıra bu yüzden "en yakından uzağa": kullanıcı widget'a önce yapması
 * gereken şeyle karşılaşır. Her kova kendi içinde teslim tarihine göre
 * artan sıralanır.
 *
 * Tamamlanmış ödevler baştan elenir — widget bir "neler yaptım" ekranı
 * değil, "neler kaldı" ekranıdır.
 */
fun widgetIcerigi(
    odevler: List<Odev>,
    simdi: Long,
    bugun: LocalDate = simdi.yerelTarih(),
    satirSayisi: Int = WIDGET_SATIR_SAYISI,
): WidgetIcerigi {
    val bekleyen = odevler.filter { it.gercekDurum(simdi) != Durum.TAMAMLANDI }

    val gecikenler = bekleyen
        .filter { it.gercekDurum(simdi) == Durum.GECEKTI }
        .sortedBy { it.sonTarih }
    val bugunler = bekleyen
        .filter { it.gercekDurum(simdi) != Durum.GECEKTI && it.sonTarih.yerelTarih() == bugun }
        .sortedBy { it.sonTarih }
    val digerleri = bekleyen
        .filter { it.gercekDurum(simdi) != Durum.GECEKTI && it.sonTarih.yerelTarih() != bugun }
        .sortedBy { it.sonTarih }

    return WidgetIcerigi(
        geciken = gecikenler.size,
        bugun = bugunler.size,
        satirlar = (gecikenler + bugunler + digerleri).take(satirSayisi),
        bekleyenToplam = bekleyen.size,
    )
}

/**
 * Tek satırın metni: `Ders — ödev`.
 *
 * Ders adı boşsa (kullanıcı ders seçmemişse) yalnızca ödev adı yazılır;
 * boş bir tire çizmek okunurluğu düşürür.
 */
fun widgetSatirMetni(odev: Odev): String =
    if (odev.ders.isBlank()) odev.baslik else "${odev.ders} — ${odev.baslik}"
