package com.odevtakip.app

import com.odevtakip.app.data.HatirlatmaAraligi
import com.odevtakip.app.util.tarihSaatiniDonustur
import com.odevtakip.app.work.hatirlatmaZamani
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Teslim öncesi hatırlatma zamanının birim testleri.
 *
 * Saf fonksiyon olduğu için cihaz gerekmez; [HatirlatmaZamanlayici.eslestir]
 * içindeki WorkManager çağrıları ise cihaz gerektirdiğinden enstrümant test
 * alanındadır. Buradaki asıl görev iki kuralı kilitlemektir:
 *
 *  1. Bildirim **asla teslimden sonra gitmez.**
 *  2. Plan, gece yarısını aşan bir saatte **uyandırmaz.**
 */
class HatirlatmaTest {

    /** Yerel saat diliminde belirli bir an üretir. */
    private fun an(yil: Int, ay: Int, gun: Int, saat: Int, dakika: Int = 0): Long =
        tarihSaatiniDonustur(LocalDate.of(yil, ay, gun), LocalTime.of(saat, dakika))

    // ---- hatirlatmaZamani ----

    @Test
    fun `kapali secenek hicbir zaman hatirlatma uretmez`() {
        assertNull(hatirlatmaZamani(an(2026, 10, 5, 23, 59), HatirlatmaAraligi.KAPALI))
        assertNull(hatirlatmaZamani(an(2026, 10, 5, 9, 0), HatirlatmaAraligi.KAPALI))
    }

    @Test
    fun `uc saat once teslimden tam uc saat onceki ani verir`() {
        assertEquals(
            an(2026, 10, 5, 20, 59),
            hatirlatmaZamani(an(2026, 10, 5, 23, 59), HatirlatmaAraligi.UC_SAAT),
        )
    }

    @Test
    fun `bir gun once tam gun onceki ani verir`() {
        assertEquals(
            an(2026, 10, 5, 12, 0),
            hatirlatmaZamani(an(2026, 10, 6, 12, 0), HatirlatmaAraligi.BIR_GUN),
        )
    }

    @Test
    fun `sabah secenegi teslim gununun sabahini verir`() {
        assertEquals(
            an(2026, 10, 5, 8, 0),
            hatirlatmaZamani(an(2026, 10, 5, 17, 0), HatirlatmaAraligi.SABAH),
        )
    }

    @Test
    fun `gece yarisi altina dusen plan sabah saatine cekilir`() {
        // 3 saat önce 07.00'ye denk geliyor; uygulama bunu sabah 08.00'e çeker
        // ki bildirim kullanıcıyı erken bir saatte uyandırmasın.
        assertEquals(
            an(2026, 10, 5, 8, 0),
            hatirlatmaZamani(an(2026, 10, 5, 10, 0), HatirlatmaAraligi.UC_SAAT),
        )
    }

    @Test
    fun `sabah cekilmesi teslimi gecerse planlanmaz`() {
        // 3 saat önce 05.00'e düşüyor; 08.00'e çekildiğinde teslim saati
        // doluyor olurdu. Bu durumda bildirim yerine "gecikti" bildirimi
        // devreye girer.
        assertNull(hatirlatmaZamani(an(2026, 10, 5, 8, 0), HatirlatmaAraligi.UC_SAAT))
    }

    @Test
    fun `sabah teslimde sabah hatirlatmasi olmaz`() {
        // Teslim 07.00 ise 08.00'deki bildirim teslimden sonra kalmış olurdu.
        assertNull(hatirlatmaZamani(an(2026, 10, 5, 7, 0), HatirlatmaAraligi.SABAH))
        assertNull(hatirlatmaZamani(an(2026, 10, 5, 8, 0), HatirlatmaAraligi.SABAH))
    }

    @Test
    fun `uretilen plan her zaman teslimden once kalir`() {
        // Tek tek örnekler yetmez: sabah saati bir çeyrek geciktirdiği için
        // bazı saatlerde sınır ihlal edilebilirdi. Saat aralığı taranarak
        // hiçbir kombinasyonda teslimi aşmayan tek bir an bile üretilmemesi
        // garanti altına alınır.
        val araliklar = HatirlatmaAraligi.entries.filter { it != HatirlatmaAraligi.KAPALI }
        var uretilen = 0

        for (gun in 1..7) {
            for (saat in listOf(0, 1, 6, 7, 8, 9, 12, 17, 20, 23)) {
                for (dakika in listOf(0, 45)) {
                    val sonTarih = an(2026, 10, gun, saat, dakika)
                    for (aralik in araliklar) {
                        val plan = hatirlatmaZamani(sonTarih, aralik) ?: continue
                        uretilen++
                        assertTrue(
                            "$aralik seçeneği teslimden sonra planlandı: " +
                                "teslim=$sonTarih plan=$plan",
                            plan < sonTarih,
                        )
                    }
                }
            }
        }

        assertTrue("Hiçbir plan üretilmedi; test boşu boşuna geçti", uretilen > 0)
    }

    // ---- HatirlatmaAraligi.guvenliDeger ----

    @Test
    fun `bilinen aralik metni dogru cozumlenir`() {
        HatirlatmaAraligi.entries.forEach { secenek ->
            assertEquals(secenek, HatirlatmaAraligi.guvenliDeger(secenek.name))
        }
    }

    @Test
    fun `bilinmeyen veya bos aralik metni varsayilana doner`() {
        // Güncelleme metni değişse bile uygulama hatırlatmasız kalmaz;
        // sessizce varsayılan aralığa geçer.
        assertEquals(HatirlatmaAraligi.UC_SAAT, HatirlatmaAraligi.guvenliDeger(null))
        assertEquals(HatirlatmaAraligi.UC_SAAT, HatirlatmaAraligi.guvenliDeger(""))
        assertEquals(HatirlatmaAraligi.UC_SAAT, HatirlatmaAraligi.guvenliDeger("HER_GUN"))
    }
}
