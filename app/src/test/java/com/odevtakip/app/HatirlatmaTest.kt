package com.odevtakip.app

import com.odevtakip.app.data.HatirlatmaAraligi
import com.odevtakip.app.util.tarihSaatiniDonustur
import com.odevtakip.app.work.hatirlatmaZamani
import com.odevtakip.app.work.hatirlatmaZamanlari
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
 * alanındadır. Buradaki asıl görev üç kuralı kilitlemektir:
 *
 *  1. Bildirim **asla teslimden sonra gitmez.**
 *  2. Plan, gece yarısını aşan bir saatte **uyandırmaz.**
 *  3. Birden çok aralık seçilince her biri ayrı an üretir, **aynı ana
 *     düşenler tek planda birleşir** ve hiç seçilmediğinde plan boştur.
 */
class HatirlatmaTest {

    /** Yerel saat diliminde belirli bir an üretir. */
    private fun an(yil: Int, ay: Int, gun: Int, saat: Int, dakika: Int = 0): Long =
        tarihSaatiniDonustur(LocalDate.of(yil, ay, gun), LocalTime.of(saat, dakika))

    // ---- hatirlatmaZamani ----

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
        // garanti altına alınır. Seçim kümesi tamamı olduğu için çoklu
        // seçimin ürettiği birleşik plan da aynı geçerlilikte test edilir.
        val tumAraliklar = HatirlatmaAraligi.entries.toSet()
        var uretilen = 0

        for (gun in 1..7) {
            for (saat in listOf(0, 1, 6, 7, 8, 9, 12, 17, 20, 23)) {
                for (dakika in listOf(0, 45)) {
                    val sonTarih = an(2026, 10, gun, saat, dakika)
                    for (aralik in tumAraliklar) {
                        val plan = hatirlatmaZamani(sonTarih, aralik) ?: continue
                        uretilen++
                        assertTrue(
                            "$aralik seçeneği teslimden sonra planlandı: " +
                                "teslim=$sonTarih plan=$plan",
                            plan < sonTarih,
                        )
                    }
                    val birlesik = hatirlatmaZamanlari(sonTarih, tumAraliklar)
                    assertEquals(
                        "Birleşik plan, tek tek planlardan farklı olmamalı",
                        tumAraliklar.mapNotNull { hatirlatmaZamani(sonTarih, it) }
                            .distinct()
                            .sorted(),
                        birlesik,
                    )
                    assertTrue(
                        "Birleşik plan teslimden sonra üretildi: " +
                            "teslim=$sonTarih plan=$birlesik",
                        birlesik.all { it < sonTarih },
                    )
                }
            }
        }

        assertTrue("Hiçbir plan üretilmedi; test boşu boşuna geçti", uretilen > 0)
    }

    // ---- hatirlatmaZamanlari (çoklu seçim) ----

    @Test
    fun `hic secim yapilmamissa hicbir zaman uretilmez`() {
        // "Kapalı" adlı bir seçenek yoktur: hatırlatmanın kapanması, hiçbir
        // aralığın işaretli olmamasıdır. Planın boş olması bunun tek kanıtıdır.
        assertTrue(hatirlatmaZamanlari(an(2026, 10, 5, 23, 59), emptySet()).isEmpty())
        assertTrue(hatirlatmaZamanlari(an(2026, 10, 5, 9, 0), emptySet()).isEmpty())
    }

    @Test
    fun `birden fazla secim sirali ve ayri zamanlar uretir`() {
        // Teslim 6 Eki 17.00:
        //   BIR_GUN -> 5 Eki 17.00
        //   SABAH   -> 6 Eki 08.00
        //   UC_SAAT -> 6 Eki 14.00
        assertEquals(
            listOf(
                an(2026, 10, 5, 17, 0),
                an(2026, 10, 6, 8, 0),
                an(2026, 10, 6, 14, 0),
            ),
            hatirlatmaZamanlari(
                an(2026, 10, 6, 17, 0),
                setOf(
                    HatirlatmaAraligi.UC_SAAT,
                    HatirlatmaAraligi.SABAH,
                    HatirlatmaAraligi.BIR_GUN,
                ),
            ),
        )
    }

    @Test
    fun `ayni ana denk gelen secimler tek planda birlesir`() {
        // Teslim 10.00: "3 saat önce" 07.00'ye düşüp 08.00'e çekilir, "teslim
        // günü sabahı" da zaten 08.00'dedir. İki ayrı iş kurulsaydı aynı anda
        // iki kez tetiklenir, ikisi de aynı bildirim kimliğine yazardı.
        assertEquals(
            listOf(an(2026, 10, 5, 8, 0)),
            hatirlatmaZamanlari(
                an(2026, 10, 5, 10, 0),
                setOf(HatirlatmaAraligi.UC_SAAT, HatirlatmaAraligi.SABAH),
            ),
        )
    }

    // ---- HatirlatmaAraligi secim kümesi ----

    @Test
    fun `secimler metne cevrilip geri ayni kume olur`() {
        val ornekler = listOf(
            emptySet<HatirlatmaAraligi>(),
            setOf(HatirlatmaAraligi.UC_SAAT),
            setOf(HatirlatmaAraligi.SABAH, HatirlatmaAraligi.BIR_GUN),
            HatirlatmaAraligi.entries.toSet(),
        )
        ornekler.forEach { kume ->
            assertEquals(
                kume,
                HatirlatmaAraligi.secimleriCozumle(HatirlatmaAraligi.secimleriYaz(kume)),
            )
        }
    }

    @Test
    fun `secim sirasi fark etmez ayni kume uretir`() {
        // Yer imi sırası tıklama sırasını izleseydi aynı seçim iki farklı
        // metin üretir, her açılışta gereksiz yeniden planlama tetiklenirdi.
        assertEquals(
            setOf(HatirlatmaAraligi.BIR_GUN, HatirlatmaAraligi.UC_SAAT),
            HatirlatmaAraligi.secimleriCozumle(
                HatirlatmaAraligi.secimleriYaz(
                    setOf(HatirlatmaAraligi.UC_SAAT, HatirlatmaAraligi.BIR_GUN),
                ),
            ),
        )
    }

    @Test
    fun `tek secimli eski deger dogru kume olur`() {
        // Tek aralık seçebilen eski sürüm aynı anahtara tek ad yazıyordu;
        // değer olduğu gibi okunur, kullanıcı yanlışlıkla ikinci bir
        // hatırlatma almaz.
        assertEquals(
            setOf(HatirlatmaAraligi.UC_SAAT),
            HatirlatmaAraligi.secimleriCozumle("UC_SAAT"),
        )
        // Eski "Kapalı" artık listede yok; o kullanıcının zaten kapatmak
        // istemesiyle aynı sonucu verir: boş küme.
        assertEquals(
            emptySet<HatirlatmaAraligi>(),
            HatirlatmaAraligi.secimleriCozumle("KAPALI"),
        )
    }

    @Test
    fun `anahtar yoksa varsayilan secim gelir bos metin kapali olur`() {
        // Hiç kayıt yok (yeni kurulum) -> varsayılan tek aralık işaretlidir.
        assertEquals(
            HatirlatmaAraligi.varsayilanSecimler(),
            HatirlatmaAraligi.secimleriCozumle(null),
        )
        // Kullanıcı her şeyin işaretini kaldırmış -> boş küme, yani kapalı.
        assertEquals(
            emptySet<HatirlatmaAraligi>(),
            HatirlatmaAraligi.secimleriCozumle(HatirlatmaAraligi.secimleriYaz(emptySet())),
        )
        // Gelecekte adı değişen bir seçenek sessizce atlanır, uygulama
        // açılamaz hâle gelmez.
        assertEquals(
            emptySet<HatirlatmaAraligi>(),
            HatirlatmaAraligi.secimleriCozumle("HIKAYE"),
        )
    }
}
