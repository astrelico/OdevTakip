package com.odevtakip.app.data

/**
 * Bir ödevin durumu.
 *
 * Durum iki kaynaktan gelir:
 *  - [TAMAMLANDI] yalnızca kullanıcının işaretlemsiyle oluşur, otomatik değişmez.
 *  - [GECEKTI] tarih geçtiğinde otomatik olarak atanır.
 *  - [BEKLIYOR] teslim tarihine zaman varsa ya da kullanıcı tarihi ileri alırsa.
 *
 * Veritabanında saklanır (bildirim/WorkManager sorguları için) ancak arayüz her
 * zaman [gercekDurum] ile hesaplanmış değeri gösterir; böylece uygulama
 * bir günden fazla açık kalsa bile liste tutarlı kalır.
 */
enum class Durum {
    BEKLIYOR,
    TAMAMLANDI,
    GECEKTI;

    companion object {
        /** Bilinmeyen/boş bir değer gelirse varsayılanı döndürür. */
        fun guvenliDeger(value: String?): Durum =
            entries.firstOrNull { it.name == value } ?: BEKLIYOR
    }
}

/**
 * Ödevin o anki gerçek durumunu hesaplar.
 *
 * Kural:
 *  1. Kullanıcı tamamladıysa → [Durum.TAMAMLANDI] (son tarih geçse bile)
 *  2. Son tarih geçtiyse      → [Durum.GECEKTI]
 *  3. Değilse                 → [Durum.BEKLIYOR]
 */
fun Odev.gercekDurum(simdi: Long): Durum = when {
    durum == Durum.TAMAMLANDI -> Durum.TAMAMLANDI
    // `<=` olmalı: DAO'daki "gecikmişleri işaretle" sorgusu da aynı karşılaştırmayı
    // kullanır. Farklı yazarsak sınır değerde (teslim anı tam "şimdi")
    // arayüz ile veritabanı farklı sonuç verir.
    sonTarih <= simdi -> Durum.GECEKTI
    else -> Durum.BEKLIYOR
}
