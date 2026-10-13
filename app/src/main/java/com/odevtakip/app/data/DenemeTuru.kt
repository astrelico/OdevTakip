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
                altlar = listOf(
                    DersKotasi(ders = "Tarih", soru = 5),
                    DersKotasi(ders = "Coğrafya", soru = 5),
                    DersKotasi(ders = "Felsefe", soru = 5),
                    DersKotasi(ders = "Din Kültürü", soru = 5),
                ),
            ),
            DersKotasi(ders = "Matematik", soru = 40),
            DersKotasi(
                ders = "Fen",
                soru = 20,
                altlar = listOf(
                    DersKotasi(ders = "Fizik", soru = 7),
                    DersKotasi(ders = "Kimya", soru = 7),
                    DersKotasi(ders = "Biyoloji", soru = 6),
                ),
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
     * Formun **girdi satırları**: bölünmemiş dersler ile bölünmüş
     * derslerin alt testleri, ekranda göründükleri sırayla.
     *
     * TYT'de dokuz satır doğar: Türkçe; Sosyal'un altında Tarih,
     * Coğrafya, Felsefe, Din Kültürü; Matematik; Fen'un altında Fizik,
     * Kimya, Biyoloji. Kullanıcı yalnızca bu satırlara değer girer;
     * Sosyal ve Fen'un toplamları bunlardan hesaplanır — **iki yerde
     * aynı sayı tutulmaz** (bkz. [com.odevtakip.app.data.Deneme]).
     *
     * Yaprakların soru sayıları ana kotalara toplanır; bu eşitlik
     * `DenemeTuruTest`'te kilitlenmiştir.
     */
    val yapraklar: List<DersKotasi>
        get() = dersleri.flatMap { kota ->
            if (kota.altlar.isEmpty()) listOf(kota) else kota.altlar
        }

    /**
     * Her ana dersin yaprakları [yapraklar] listesindeki yeri.
     *
     * Dersler sıralı olduğundan aralıklar da sıralı ve bitişiktir: TYT'de
     * Türkçe `0..0`, Sosyal `1..4`, Matematik `5..5`, Fen `6..8`. Form
     * "Sosyal başlığı bu dört satırın toplamını gösterir" derken bu
     * aralığı kullanır; böylece satır sayısı artsa bile eşleşme bozulmaz.
     */
    val kotaAraliklari: List<IntRange>
        get() = buildList {
            var baslangic = 0
            dersleri.forEach { kota ->
                val adet = kota.altlar.size.coerceAtLeast(1)
                add(baslangic until (baslangic + adet))
                baslangic += adet
            }
        }

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
 * @property altlar Derse ait **alt testler** (bkz. [altDagilim]).
 *   TYT'de Sosyal dört test, Fen üç test olarak çözülür; bu altlar
 *   yalnızca gösterim değil, **girdi satırıdır**: kullanıcı Tarih'in
 *   doğru/yanlışını ayrıca girer, Sosyal'un toplamı onlardan hesaplanır.
 *   Bölünmeyen derslerde (Türkçe, AYT dersleri) liste boştur.
 */
data class DersKotasi(
    val ders: String,
    val soru: Int,
    val altlar: List<DersKotasi> = emptyList(),
) {
    /**
     * Resmî alt test dağılımı, yalnızca **görüntüleme** için:
     * `"Tarih 5 · Coğrafya 5 · Felsefe 5 · Din Kültürü 5"`.
     *
     * Metin elle yazılmaz — [altlar]'an üretilir ki paneldeki sayı ile
     * formdaki satır kotası asla ayrışmasın. Alt test yoksa `null`.
     */
    val altDagilim: String?
        get() = altlar.takeIf { it.isNotEmpty() }
            ?.joinToString(" · ") { "${it.ders} ${it.soru}" }

    /**
     * Doğru+yanlış toplamı kota aşıyor mu?
     *
     * Sınır **dâhilidir**: 40 soruluk derste 40 doğru + 0 yanlış
     * geçerlidir, 41 ise değildir. Kural saf işlevtir ki ekrandan
     * bağımsız sınanabilsin (bkz. `DenemeTuruTest`).
     */
    fun asildiMi(dogru: Int, yanlis: Int): Boolean = dogru + yanlis > soru
}

/**
 * Bir bölünmüş dersin **alt test sonuçları**.
 *
 * Kayıt yalnızca yaprak satırları taşır (bkz. [DenemeTuru.yapraklar]);
 * bu işlev onları kota sırasıyla, kaydı olmayan alt test için `0/0`
 * doldurarak toplar. Böylece detay kartı her alt test satırını çizebilir.
 *
 * `null` iki durumu bildirir ve ikisi de **satır çizmemek** anlamına
 * gelir:
 *
 *  - Ders bölünmemişse (Türkçe, AYT dersleri).
 *  - Kayıt **eski biçimdeyse**: alt test hiç girilmemiş ama ana ders
 *    (Sosyal=12/3) doğrudan kayıtlıdır; o zaman alt satırları sıfırla
 *    göstermek kaydın eski bütünlüğünü ezerdi.
 *
 * Saf işlevtir (bkz. `KotaSonucuTest`).
 */
fun kotaAltSonuclari(
    kota: DersKotasi,
    sonuclar: List<DersSonucu>,
): List<DersSonucu>? {
    if (kota.altlar.isEmpty()) return null
    if (kota.altlar.none { alt -> sonuclar.any { it.ders == alt.ders } }) return null

    return kota.altlar.map { alt ->
        sonuclar.firstOrNull { it.ders == alt.ders } ?: DersSonucu(alt.ders)
    }
}

/**
 * Bir ana dersin kayıttaki sonucu.
 *
 * Kayıt yalnızca **yaprak** satırları taşır (bkz. [DenemeTuru.yapraklar]):
 * Sosyal'un sonucu dört alt testin toplamıdır, ayrıca yazılmaz. Bu
 * çözümleme iki biçimle de çalışır:
 *
 *  - **Yaprak biçiminde** (bugün): alt test kayıtları varsa toplanır
 *    ([kotaAltSonuclari]).
 *  - **Ana ders biçiminde** (eski): alt test kaydı yok ama Sosyal'ın
 *    kendisi kayıtlıysa o değer kullanılır — biçimi değişmiş tek bir
 *    kayıt bile ekranda yanlış görünmesin.
 *
 * Saf işlevtir; ekran ve depo aynı sözleşmeyi paylaşır
 * (bkz. `KotaSonucuTest`).
 */
fun kotaSonucu(kota: DersKotasi, sonuclar: List<DersSonucu>): DersSonucu {
    val altlar = kotaAltSonuclari(kota, sonuclar)
    if (altlar != null) {
        return DersSonucu(
            ders = kota.ders,
            dogru = altlar.sumOf { it.dogru },
            yanlis = altlar.sumOf { it.yanlis },
        )
    }

    return sonuclar.firstOrNull { it.ders == kota.ders } ?: DersSonucu(kota.ders)
}
