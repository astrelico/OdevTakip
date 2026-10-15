package com.odevtakip.app.data

import com.odevtakip.app.util.gunBaslangici
import com.odevtakip.app.util.yerelTarih
import java.time.LocalDate

/**
 * Rutin kayıtlarından dönem özetleri üreten **saf işlevler**.
 *
 * Burada hiçbir taraf etkileşime girmez: girdi ham kayıt listesi,
 * çıktı özet. Ekran bunları yalnızca akış geldiğinde çağırır; bu yüzden
 * testler de girdi→çıktı çiftlerini sınar (bkz. `RutinHesapTest`).
 *
 * İki dönem biçimi vardır ve ikisi de [donemOzetiniHesapla]'yı çağırır:
 * hafta (Pazartesi başlangıçlı yedi gün) ve ay (ayın biri ile son günü
 * arası). Aralığı veren tek yer [java.time.temporal.ChronoUnit] ile
 * sayılır; "hafta kaç gün" sorusunun cevabı başka bir yerde saklanmaz.
 */

/** Bir dönemin ilk günü dâhil aralığındaki kayıtlardan dönem özeti. */
fun donemOzetiniHesapla(
    kayitlar: List<RutinKaydi>,
    ilkGun: LocalDate,
    sonGun: LocalDate,
): RutinDonemOzeti {
    // Aralığın sınırı: sonGun da dâhildir — "ayın 30'u" kaydı ay
    // özetinde görünmelidir.
    val baslangic = ilkGun.gunBaslangici()
    val bitis = sonGun.plusDays(1).gunBaslangici()

    val aralikta = kayitlar.filter { it.tarih >= baslangic && it.tarih < bitis }

    val gunlereGore = aralikta.groupBy { it.tarih.yerelTarih() }

    val gunOzetleri = buildMap {
        gunlereGore.forEach { (gun, gununKayitlari) ->
            put(gun, gunOzetiniHesapla(gununKayitlari))
        }
    }

    return RutinDonemOzeti(
        gunOzetleri = gunOzetleri,
        toplamSoru = aralikta.sumOf { it.soru },
        toplamDogru = aralikta.sumOf { it.dogru },
        toplamYanlis = aralikta.sumOf { it.yanlis },
        calisilanGun = gunOzetleri.size,
    )
}

/**
 * Bir günün bütün kayıtlarından gün özeti.
 *
 * Aynı dersten birden fazla kayıt **gelir** (9. sürümünden beri aynı
 * günün aynı dersi serbesttir — bkz. [RutinKaydi]); bu yüzden toplamlar
 * `sumOf` ile **bütün** satırlardan alınır. [RutinGunOzeti.dersSayisi]
 * ise farklı ders **adlarını** sayar: aynı dersten iki oturum tek ders
 * sayılır, toplam ise iki kaydı da içerir.
 */
fun gunOzetiniHesapla(kayitlar: List<RutinKaydi>): RutinGunOzeti =
    if (kayitlar.isEmpty()) {
        RutinGunOzeti.BOS
    } else {
        RutinGunOzeti(
            soru = kayitlar.sumOf { it.soru },
            dogru = kayitlar.sumOf { it.dogru },
            yanlis = kayitlar.sumOf { it.yanlis },
            dersSayisi = kayitlar.map { it.ders }.distinct().size,
        )
    }

/** Seçili haftanın **Pazartesi** günü. */
fun haftaninIlkGunu(hafta: LocalDate): LocalDate =
    hafta.minusDays((hafta.dayOfWeek.value - 1).toLong())

/** Seçili haftanın **Pazar** günü. */
fun haftaninSonGunu(hafta: LocalDate): LocalDate =
    haftaninIlkGunu(hafta).plusDays(6)

/** Bir haftanın yedi günü, Pazartesi'den Pazar'a. */
fun haftaninGunleri(hafta: LocalDate): List<LocalDate> =
    haftaninIlkGunu(hafta).let { ilk -> (0L..6L).map { ilk.plusDays(it) } }

/**
 * Baloncuğun **0..1 arası** büyüklüğü.
 *
 * Baloncuk, günün toplam sorusuyla orantılı büyür ama **oranla** değil:
 * 10 soru ile 20 soru arasındaki fark göze çarperken, 200 ile 210
 * arasındaki fark ayırt edilemez. Bu yüzden karekök alınıp en büyük
 * güne göre bölünür — küçük günler okunur kalır, büyük günler ekranı
 * ele geçirmez.
 *
 * [enBuyukGunSoru] `0` ya da negatifse (dönem bomboşsa) `0` döner;
 * bölmeye girilmez.
 */
fun baloncukOrani(soru: Int, enBuyukGunSoru: Int): Float {
    if (soru <= 0 || enBuyukGunSoru <= 0) return 0f
    val oran = Math.sqrt(soru.toDouble() / enBuyukGunSoru)
    return oran.toFloat().coerceIn(0f, 1f)
}
