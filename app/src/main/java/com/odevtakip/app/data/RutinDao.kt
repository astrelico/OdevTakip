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
     * Aralıktaki kayıtları yayınlar; **gün, ders adına ve kimliğe** göre sıralı.
     *
     * Sıra bilinçlidir: ekran kayıtları güne göre gruplar, her günün
     * içinde dersler alfabetik dizilir. Aynı dersten birden fazla kayıt
     * varsa ([RutinKaydi.baslik]) **ekleme sırasına** göre durur — son
     * `id` bağlayıcıdır ki kullanıcı kayda sonradan bir başlık yazdığında
     * satır listede zıplamasın.
     */
    @Query(
        "SELECT * FROM rutin_kayitlari " +
            "WHERE tarih >= :baslangic AND tarih < :bitis " +
            "ORDER BY tarih ASC, ders ASC, id ASC",
    )
    fun araligiIzle(baslangic: Long, bitis: Long): Flow<List<RutinKaydi>>

    /**
     * Kaydı ekler; kimlik çakışırsa var olanı günceller.
     *
     * Artık `(tarih, ders)` unique indeksi **yoktur** (9. sürümde
     * kaldırıldı), dolayısıyla [OnConflictStrategy.REPLACE] yalnızca
     * `id` çakışmasına bakar:
     *
     *  - **Ekleme** modunda `id = 0` gelir → autoincrement yeni bir
     *    kimlik verir, her Kaydet **yeni satır** açar. Aynı günün aynı
     *    dersi ikinci kez girilse bile eski satıra dokunulmaz.
     *  - **Düzenleme** modunda satırın kendi `id`si gelir → yalnız o
     *    satır güncellenir.
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
