package com.odevtakip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Rutin kayıtları tablosu için veri erişim işlemleri.
 *
 * Sorgular **aralık** üzerinden çalışır: ekran haftanın ya da ayın
 * kayıtlarını ister, tüm geçmişi çekmek hem gereksiz hem de ileride
 * kayıt sayısı artınca yavaş olurdu. Aralık sınırı günün başlangıcı
 * millisidir; `tarih` de o biçimde saklandığı için karşılaştırma düz
 * bir sayı karşılaştırmasıdır.
 */
@Dao
interface RutinDao {

    /**
     * Aralıktaki kayıtları yayınlar; **gün ve ders adına** göre sıralı.
     *
     * Sıra bilinçlidir: ekran kayıtları güne göre gruplar, her günün
     * içinde dersler alfabetik dizilir. Kartta "Matematik" hep aynı
     * yerde durur — kayıt ekleme sırasına göre zıplamasın diye.
     */
    @Query(
        "SELECT * FROM rutin_kayitlari " +
            "WHERE tarih >= :baslangic AND tarih < :bitis " +
            "ORDER BY tarih ASC, ders ASC",
    )
    fun araligiIzle(baslangic: Long, bitis: Long): Flow<List<RutinKaydi>>

    /**
     * Aynı günün aynı dersini bir kez okur.
     *
     * Panel açıldığında "bu ders bugün zaten girilmiş mi" sorusunun
     * yanıtıdır; girilmişse panel **düzenleme** olarak açılır ve eski
     * değerler dolar.
     */
    @Query("SELECT * FROM rutin_kayitlari WHERE tarih = :tarih AND ders = :ders")
    suspend fun gununDersiniAl(tarih: Long, ders: String): RutinKaydi?

    /**
     * Kaydı ekler ya da var olanı günceller.
     *
     * Unique indeks `(tarih, ders)` olduğu için [OnConflictStrategy.REPLACE]
     * aynı günün aynı dersini **sessizce değiştirir** — kullanıcının
     * "Matematik"i ikinci kez girmesi ayrı bir satır değil, tek satırın
     * güncellenmesidir. Böylece gün toplamı iki katma çıkmaz.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kaydet(kaydi: RutinKaydi): Long

    /** Kaydı kimliğiyle siler. */
    @Query("DELETE FROM rutin_kayitlari WHERE id = :id")
    suspend fun kimlikleSil(id: Long): Int

    /** Günün bütün kayıtlarını siler (gün temizleme). */
    @Query("DELETE FROM rutin_kayitlari WHERE tarih = :tarih")
    suspend fun gunuSil(tarih: Long): Int
}
