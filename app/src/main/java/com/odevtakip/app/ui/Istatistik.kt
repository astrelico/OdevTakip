package com.odevtakip.app.ui

import com.odevtakip.app.data.Durum
import com.odevtakip.app.data.Odev
import com.odevtakip.app.data.gercekDurum
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate

/**
 * İstatistik ekranının sayısal dökümü.
 *
 * Dört alan **bütünü böler**: `tamamlanan + geciken + bekleyen == toplam`.
 * Bu yüzden pasta grafiği üç dilimi de eksiğiz yazabilir; ara bir "diğer"
 * kategorisi gerekmez. [Odev.gercekDurum] ile aynı kural uygulandığı için
 * ekrandaki sayılar listeyle daima tutarlıdır — ayrı bir sayım tutulmaz.
 *
 * @property bugunTeslim Teslim tarihi **bugün** olan ve henüz tamamlanmamış
 *   ödevler. Bölüme dahil **değildir**: "bugün" bir teslim penceresidir, bir
 *   durum değil; yarın başka bir sayıya döner.
 * @property tamamlamaOrani Tamamlananların tüm kayıtlara oranı (0f..1f).
 *   Kayıt yoksa `0f` — arayüz bunu yüzdeye çevirir.
 */
data class Istatistik(
    val toplam: Int,
    val tamamlanan: Int,
    val geciken: Int,
    val bekleyen: Int,
    val bugunTeslim: Int,
) {
    val tamamlamaOrani: Float
        get() = if (toplam == 0) 0f else tamamlanan.toFloat() / toplam
}

/** Pastadaki tek bir dilim. */
data class Dilim(val durum: Durum, val adet: Int)

/**
 * Bir dersin ödev dağılımı.
 *
 * @property ders Dersin adı. Eski kayıtlarda ders boş bırakılmış olabilir;
 *   arayüz bunu "Ders seçilmedi" olarak gösterir.
 */
data class DersDagilimi(
    val ders: String,
    val toplam: Int,
    val tamamlanan: Int,
    val geciken: Int,
    val bekleyen: Int,
)

/**
 * Bir güne düşen tamamlanan ödev sayısı.
 *
 * @property gun ISO günü. Sütun grafiğinin etiketi de bu tarihten üretilir,
 *   böylece grafikteki her sütun hangi güne ait olduğunu bilir.
 */
data class GunlukSayim(val gun: LocalDate, val adet: Int)

/** Pastanın dilim sırası: en iyiden en kötüye, saat yönünde. */
private val DILIM_SIRASI = listOf(Durum.TAMAMLANDI, Durum.GECEKTI, Durum.BEKLIYOR)

/** Sütun grafiğinin varsayılan genişliği. */
const val HAFTA_GUN_SAYISI: Int = 7

/**
 * Bütün kayıtları tek bir özet sayıya indirger.
 *
 * @param simdi Karşılaştırılacak an; `LocalDate.now()` yerine parametre
 *   alınır ki testler sabit bir "şimdi" ile çalışsın.
 */
fun List<Odev>.istatistikHesapla(simdi: Long): Istatistik {
    var tamamlanan = 0
    var geciken = 0
    var bekleyen = 0

    forEach {
        when (it.gercekDurum(simdi)) {
            Durum.TAMAMLANDI -> tamamlanan++
            Durum.GECEKTI -> geciken++
            Durum.BEKLIYOR -> bekleyen++
        }
    }

    val bugun = simdi.yerelTarih()
    val bugunTeslim = count {
        it.sonTarih.yerelTarih() == bugun && it.gercekDurum(simdi) != Durum.TAMAMLANDI
    }

    return Istatistik(
        toplam = size,
        tamamlanan = tamamlanan,
        geciken = geciken,
        bekleyen = bekleyen,
        bugunTeslim = bugunTeslim,
    )
}

/**
 * Durum dağılımını [DILIM_SIRASI] sırasıyla verir.
 *
 * Adedi `0` olan dilim de döner: sıralamayı ve rengi arayüzün değil
 * fonksiyonun belirlemesi, grafiği çizen kodun üç durumu da ezberlemesini
 * engeller. Çizim sırasında elenir.
 */
fun List<Odev>.durumDilimleri(simdi: Long): List<Dilim> {
    val sayim = mutableMapOf<Durum, Int>()
    forEach { sayim.merge(it.gercekDurum(simdi), 1, Int::plus) }
    return DILIM_SIRASI.map { Dilim(it, sayim[it] ?: 0) }
}

/**
 * Derslere göre dağılım, en çok ödevden en az ödeve.
 *
 * Eşitlikte ders adı devreye girer; aynı liste iki açılışta hep aynı sırada
 * çizilir. Boş ders adları kendi grubunda toplanır (eskiden kalan kayıtlar),
 * "hiç ders seçilmemiş" ile "bir grup boş satır" birbirine karışmaz.
 */
fun List<Odev>.dersDagilimi(simdi: Long): List<DersDagilimi> =
    groupBy { it.ders.trim() }
        .map { (ders, kayitlar) ->
            DersDagilimi(
                ders = ders,
                toplam = kayitlar.size,
                tamamlanan = kayitlar.count { it.gercekDurum(simdi) == Durum.TAMAMLANDI },
                geciken = kayitlar.count { it.gercekDurum(simdi) == Durum.GECEKTI },
                bekleyen = kayitlar.count { it.gercekDurum(simdi) == Durum.BEKLIYOR },
            )
        }
        .sortedWith(
            compareByDescending<DersDagilimi> { it.toplam }.thenBy { it.ders }
        )

/**
 * Son [gunSayisi] günün tamamlanan ödev sayımları, **en eskiden bugüne**.
 *
 * Sütun grafiğinin soldan sağa okunması bu sıraya dayanır. Sayım yalnızca
 * [Odev.tamamlanmaTarihi]'ne bakar — ekleme tarihine değil — yani "o gün
 * biten iş" ölçülür. [gunSayisi] günden eski kayıtlar grafiğe girmez.
 *
 * @return Her gün için bir kayıt; liste uzunluğu her zaman [gunSayisi]
 *   olur (kayıt olmayan gün `0` döner), böylece eksen hiç atlanmaz.
 */
fun List<Odev>.sonGunlerinTamamlanmasi(
    simdi: Long,
    gunSayisi: Int = HAFTA_GUN_SAYISI,
): List<GunlukSayim> {
    val bugun = simdi.yerelTarih()
    val sayim = filter { it.tamamlanmaTarihi != null }
        .groupingBy { it.tamamlanmaTarihi!!.yerelTarih() }
        .eachCount()

    return (gunSayisi - 1 downTo 0).map { geri ->
        val gun = bugun.minusDays(geri.toLong())
        GunlukSayim(gun, sayim[gun] ?: 0)
    }
}
