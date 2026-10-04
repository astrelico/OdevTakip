package com.odevtakip.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Odev tablosu için veri erişim işlemleri.
 *
 * Okumaların tamamı [Flow] döndürür; böylece Room bir kayıt değiştiğinde
 * Compose ekranını kendiliğinden günceller.
 */
@Dao
interface OdevDao {

    // ---- Okuma ----

    /** Tüm ödevleri teslim tarihine göre sıralı olarak yayınlar. */
    @Query("SELECT * FROM odevler ORDER BY sonTarih ASC")
    fun tumOdevleriIzle(): Flow<List<Odev>>

    /** Verilen durumdaki ödevleri teslim tarihine göre sıralı yayınlar. */
    @Query("SELECT * FROM odevler WHERE durum = :durum ORDER BY sonTarih ASC")
    fun durumdakiOdevleriIzle(durum: Durum): Flow<List<Odev>>

    /** Tek bir ödevi yayınlar. */
    @Query("SELECT * FROM odevler WHERE id = :id")
    fun odeviIzle(id: Long): Flow<Odev?>

    /** Tek bir ödevi bir kez okur (detay/düzenleme ekranı için). */
    @Query("SELECT * FROM odevler WHERE id = :id")
    suspend fun odeviAl(id: Long): Odev?

    /** Verilen durumda kaç ödev olduğunu yayınlar (rozet/sayacı için). */
    @Query("SELECT COUNT(*) FROM odevler WHERE durum = :durum")
    fun sayiyiIzle(durum: Durum): Flow<Int>

    // ---- Yazma ----

    /**
     * Yeni ödev ekler veya `id > 0` ise var olanı günceller.
     * Kaydedilen kaydın kimliğini döndürür.
     */
    @Upsert
    suspend fun kaydet(odev: Odev): Long

    /** Ödevi siler. */
    @Delete
    suspend fun sil(odev: Odev)

    /** Kimliğe göre ödev siler, silinen satır sayısını döndürür. */
    @Query("DELETE FROM odevler WHERE id = :id")
    suspend fun kimlikleSil(id: Long): Int

    /** Kimliklere göre ödevleri okur (senkron sırasında yeni gecikenleri bulmak için). */
    @Query("SELECT * FROM odevler WHERE id IN (:idler)")
    suspend fun idleriGetir(idler: List<Long>): List<Odev>

    /**
     * Son tarihi geçtiği hâlde henüz işaretlenmemiş ödevlerin kimlikleri.
     *
     * [gecikmisleriIsaretle]'den **önce** çağrılır: böylece yalnızca bu
     * geçişte gecikenleri biliriz ve aynı ödevleri tekrar tekrar bildirmeyiz.
     */
    @Query("SELECT id FROM odevler WHERE durum = :durum AND sonTarih <= :simdi")
    suspend fun gecikmisOdevIdleri(durum: Durum, simdi: Long): List<Long>

    /**
     * Son tarihi geçmiş ama hâlâ bekleyen ödevleri [Durum.GECEKTI] olarak işaretler.
     *
     * @param simdi Karşılaştırılacak an (epoch millis).
     * @return Kaç satırın güncellendiği.
     */
    @Query(
        "UPDATE odevler SET durum = :yeni " +
            "WHERE durum = :eski AND sonTarih <= :simdi"
    )
    suspend fun gecikmisleriIsaretle(eski: Durum, yeni: Durum, simdi: Long): Int

    /**
     * Tek bir ödevin durumunu ve istenirse tamamlanma zamanını günceller.
     *
     * @param tamamlanmaZamani [Durum.TAMAMLANDI] için dolu, diğerleri için null.
     */
    @Query(
        "UPDATE odevler SET durum = :durum, tamamlanmaTarihi = :tamamlanmaZamani " +
            "WHERE id = :id"
    )
    suspend fun durumuGuncelle(id: Long, durum: Durum, tamamlanmaZamani: Long?): Int

    /**
     * Son tarihi güncellenen ödevi, tarihi geçmediyse yeniden [Durum.BEKLIYOR]
     * durumuna alır (kullanıcı süreyi uzattığında kullanılır).
     */
    @Query(
        "UPDATE odevler SET durum = :yeni " +
            "WHERE id = :id AND durum = :eski AND sonTarih > :simdi"
    )
    suspend fun ertelendigindeBeklemeyeAl(id: Long, eski: Durum, yeni: Durum, simdi: Long): Int

    // ---- Test yardımcıları ----

    /** Tüm kayıtları senkron olarak okur (yalnızca birim testleri için). */
    @Query("SELECT COUNT(*) FROM odevler")
    suspend fun toplamSayi(): Int
}
