package com.odevtakip.app.ui

import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Widget'ın ekranda çizebileceği azami ödev satırı.
 *
 * Sınır bilinçli: ana ekran widget'ı bir liste değil, **bakışta özet**
 * olduğu içindir. Üç satır dört hücrelik bir alanda okunur; daha fazlası
 * kullanıcıyı kartlara geri gönderir.
 */
const val WIDGET_SATIR_SAYISI = 3

/**
 * Büyük ("Günün listesi") widget'ının satır sınırı.
 *
 * Aynı mantık, daha geniş bir alan: dört hücrelik kartta altı satır
 * rahatça okunur ve günün tamamı — gecikenler dahil — ekrana sığar.
 * [WIDGET_SATIR_SAYISI] ise özet widget'ı için kalır; ikisini tek
 * değerde birleştirmek, birini değiştirirken diğerini unutmayı kolaylaştırır.
 */
const val LISTE_SATIR_SAYISI = 6

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

/**
 * Sayaç widget'ının üç rakamı.
 *
 * [toplam] bilerek **tüm** ödevleri sayar, bekleyenleri değil. Küçük
 * widget'ın işi özet değil, listeyi açmadan ölçek görmektir: "8 ödevin
 * 4'ü gecikmiş, 2'si bugün" cümlesi ancak üç sayı bir arada okununca
 * kurulur. Tamamlananlar geciken ve bugün sayaçlarına girmez, yalnızca
 * toplamı büyütür.
 */
data class WidgetSayaclari(
    val geciken: Int,
    val bugun: Int,
    val toplam: Int,
)

/**
 * Sayaçları hesaplar.
 *
 * Kovalar [widgetIcerigi] ile **birebir aynı** kurallara tabidir, böylece
 * küçük widget "4 geciken" derken büyük widget da aynı dörder satırı
 * gösterir; iki widget birbirini yalanlamaz.
 */
fun widgetSayaclari(odevler: List<Odev>, simdi: Long): WidgetSayaclari {
    val geciken = odevler.count { it.gercekDurum(simdi) == Durum.GECEKTI }
    val bugun = odevler.count {
        it.gercekDurum(simdi) != Durum.GECEKTI &&
            it.gercekDurum(simdi) != Durum.TAMAMLANDI &&
            it.sonTarih.yerelTarih() == simdi.yerelTarih()
    }
    return WidgetSayaclari(geciken = geciken, bugun = bugun, toplam = odevler.size)
}

/**
 * Sıradaki teslim — bekleyenlerin **en eski** teslim tarihine sahip olanı.
 *
 * Geçmiş tarihli iş en başta gelir; widget'ın görevi "sıradaki"yi
 * göstermek değil, "çözülmesi gereken ilk"i göstermektir. Hiç bekleyen
 * yoksa `null` — çağıran taraf gerçek cevabı ("Bekleyen ödev yok")
 * verir, uydurma bir başlık koymaz.
 */
fun sonrakiTeslim(odevler: List<Odev>, simdi: Long): Odev? =
    odevler
        .filter { it.gercekDurum(simdi) != Durum.TAMAMLANDI }
        .minByOrNull { it.sonTarih }

/**
 * Teslime kalan takvim günü.
 *
 * Saat farkı değil **gün farkı** verir: bugün 23:59'a kalan süre ile
 * yarın 09:00'a kalan süre kullanıcı için aynı "yarın"dır. Negatif değer
 * teslimin geçtiğini söyler; yine de tek başına yorumlanmamalıdır —
 * bugünün saati geçmiş bir iş için gün farkı 0'dır. Bu yüzden arayüz
 * önce [Odev.gercekDurum] kontrol eder, geçtiyse "Gecikti" yazar.
 */
fun widgetKalanGun(odev: Odev, simdi: Long): Long =
    ChronoUnit.DAYS.between(simdi.yerelTarih(), odev.sonTarih.yerelTarih())

// ---- Tamamla kutusunun niyet adresi ----

/**
 * Tamamla kutusunun **yuva** tabanları: her widget'ın kendi aralığı vardır.
 *
 * Yuva, kutunun tuttuğu ödevden **bağımsızdır**: ödev değişse de yuva
 * aynı kalır. Bunun nedeni [widgetTamamlamaAdresi] yorumunda.
 */
const val WIDGET_YUVA_OZET = 0

/** [WIDGET_YUVA_OZET] üç satır kaplar; aralık ona göre açılır. */
const val WIDGET_YUVA_LISTE = 10

/** Sonraki teslim widget'ında yalnızca tek satır, dolayısıyla tek yuva. */
const val WIDGET_YUVA_SONRAKI = 20

/**
 * Tamamla kutusunun gönderdiği yayının **adresi**.
 *
 * ### Neden extra değil adres?
 *
 * `PendingIntent` eşitliği `Intent.filterEquals` **extra'ları içermez**: aynı
 * bileşene ve aynı adrese sahip iki niyet, istek kodu da aynıysa sistemde
 * **tek kayıt**tır ve en son yazılan extra hepsine uygulanır. Üç widget'taki
 * on bir kutunun hepsi "aynı niyet" çıkardı, bir kutuya basıldığında listedeki
 * **son** ödev tamamlanırdı — sessiz ve ciddi bir hata.
 *
 * Adres bu yüzden kutunun **kendi yuvasını** taşır, ödevin kimliğini değil.
 * Yuva sabit kaldığı için `FLAG_UPDATE_CURRENT` aynı kaydı yerinde günceller:
 * kutu yeni bir ödev gösterdiğinde eski niyet çöpe gitmez, extra tazelenir.
 * Aksi hâlde her çizimde sistemde bir niyet daha birikirdi.
 *
 * Bildirim eyleminin niyeti bu adrese **sahip değildir**; o yüzden iki eylem
 * aynı alıcıya varsa bile asla birbirinin extra'larını ezemez.
 */
fun widgetTamamlamaAdresi(yuva: Int): String = "odevtakip://widget/tamamla/$yuva"
