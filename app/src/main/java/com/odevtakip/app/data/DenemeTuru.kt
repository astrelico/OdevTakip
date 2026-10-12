package com.odevtakip.app.data

/**
 * Bir deneme sınavının **türü** — yani hangi formatta çözüldüğü.
 *
 * YKS'nin oturumlarından yalnızca sayısal adayın çözdüğü iki format
 * desteklenir:
 *
 *  - [TYT] — 120 soru: Türkçe 40, Sosyal 20, Matematik 40, Fen 20.
 *  - [AYT] — 80 soru: sayısal adayın AYT testi; Matematik 40, Fizik 14,
 *    Kimya 13, Biyoloji 13.
 *
 * Soru sayıları **kilitlidir** ([dersleri]): kullanıcı bunları
 * değiştiremez, yalnızca o sayılar üzerinden doğru/yanlış girer. Bu kilidin
 * iki getirisi vardır: "bu denemede kaç soru vardı" sorusu kayıt altına
 * alınır ve formun hata kontrolü ([DersKotasi.asildiMi]) tek bir kaynaktan
 * beslenir — sayılar kodda bir kez, ekranda bir kez yazılır.
 *
 * Sıra **bildirimdir**: [dersleri] listesinin sırası, formdaki satırların
 * ve detaydaki dağılımın sırasıdır.
 *
 * Eşit ağırlık ve sözel adayların AYT testleri (Edebiyat-Sosyal 1,
 * Sosyal 2) şimdilik yoktur; eklendiğinde yalnızca bu enum'a yeni bir
 * kayıt yeterlidir — tablo şeması değişmez, çünkü her tür kendi
 * derslerini kendisi taşır.
 *
 * Veritabanında `name`'iyle saklanır (bkz. [Converters]).
 */
enum class DenemeTuru(
    /** Bu türün dersleri ve soru sayıları; **sıra önemlidir**. */
    val dersleri: List<DersKotasi>,
) {
    TYT(
        listOf(
            DersKotasi(ders = "Türkçe", soru = 40),
            DersKotasi(
                ders = "Sosyal",
                soru = 20,
                altDagilim = "Tarih 5 · Coğrafya 5 · Felsefe 5 · Din Kültürü 5",
            ),
            DersKotasi(ders = "Matematik", soru = 40),
            DersKotasi(
                ders = "Fen",
                soru = 20,
                altDagilim = "Fizik 7 · Kimya 7 · Biyoloji 6",
            ),
        ),
    ),
    AYT(
        listOf(
            DersKotasi(ders = "Matematik", soru = 40),
            DersKotasi(ders = "Fizik", soru = 14),
            DersKotasi(ders = "Kimya", soru = 13),
            DersKotasi(ders = "Biyoloji", soru = 13),
        ),
    );

    /** Sınavdaki toplam soru sayısı — ders kotalarının toplamı. */
    val toplamSoru: Int
        get() = dersleri.sumOf { it.soru }

    /**
     * Kısa özet, tür seçimindeki satırın ilk cümlesi:
     * `"Türkçe 40, Sosyal 20, Matematik 40, Fen 20"`.
     *
     * Sayılar [dersleri]'nden üretilir; ekranda elle yazılmış ikinci bir
     * kopya yoktur.
     */
    val dersOzeti: String
        get() = dersleri.joinToString(", ") { "${it.ders} ${it.soru}" }

    /**
     * Alt test dağılımları; yalnızca tanımlı olanlar.
     *
     * TYT'de Sosyal ve Fen bölünmüştür (bkz. [DersKotasi.altDagilim]);
     * Türkçe ya da Ayt derslerinin bölünmesi yoktur.
     */
    val altDagilimlar: List<String>
        get() = dersleri.mapNotNull { it.altDagilim }

    /** Verilen dersin **bu türdeki** kotası; ders bu türde yoksa `null`. */
    fun kotasi(ders: String): Int? = dersleri.firstOrNull { it.ders == ders }?.soru

    companion object {
        /** Bilinmeyen/boş bir değer gelirse varsayılanı döndürür. */
        fun guvenliDeger(metin: String?): DenemeTuru =
            entries.firstOrNull { it.name == metin } ?: TYT
    }
}

/**
 * Bir deneme türündeki tek dersin **soru sayısı kotası**.
 *
 * @property ders Dersin adı; kayıtta [DersSonucu.ders] ile **aynı**
 *   yazımda olmalıdır — eşleşme ad üzerinden yapılır, kimlik üzerinden
 *   değil.
 * @property soru Bu derste kaç soru vardır — formda kilitli olan sayı.
 * @property altDagilim Resmî alt test dağılımı, yalnızca gösterim için:
 *   `"Tarih 5 · Coğrafya 5 · Felsefe 5 · Din Kültürü 5"`. `null` ise o
 *   ders bölünmez (ör. Türkçe).
 */
data class DersKotasi(
    val ders: String,
    val soru: Int,
    val altDagilim: String? = null,
) {
    /**
     * Doğru+yanlış toplamı kota aşıyor mu?
     *
     * Sınır **dâhilidir**: 40 soruluk derste 40 doğru + 0 yanlış
     * geçerlidir, 41 ise değildir. Kural saf işlevtir ki ekrandan
     * bağımsız sınanabilsin (bkz. `DenemeTuruTest`).
     */
    fun asildiMi(dogru: Int, yanlis: Int): Boolean = dogru + yanlis > soru
}
