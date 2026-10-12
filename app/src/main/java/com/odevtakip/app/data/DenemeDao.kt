package com.odevtakip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Deneme sınavları tablosu için veri erişim işlemleri.
 *
 * Sıralama her sorguda **tarih azalan**dır: en son yapılan deneme en üstte
 * durur. Ödev listesindeki gibi "önerilen" bir karma yok — denemelerin
 * tek sıralanma mantığı zaman olduğu için karar sorguya gömülüdür.
 */
@Dao
interface DenemeDao {

    /** Tüm denemeleri, en yeni tarih önce gelecek şekilde yayınlar. */
    @Query("SELECT * FROM denemeler ORDER BY tarih DESC, id DESC")
    fun tumunuIzle(): Flow<List<Deneme>>

    /** Tek denemeyi yayınlar; kayıt silinince `null` akar. */
    @Query("SELECT * FROM denemeler WHERE id = :id")
    fun izle(id: Long): Flow<Deneme?>

    /**
     * Tek denemeyi bir kez okur.
     *
     * [OdevRepository.denemeSil] bunu, silmeden önce kaydı almak için
     * kullanır; ayrıca detay ekranının ilk çiziminde de çağrılır.
     */
    @Query("SELECT * FROM denemeler WHERE id = :id")
    suspend fun al(id: Long): Deneme?

    /**
     * Denemeyi ekler veya var olanı günceller.
     *
     * [OnConflictStrategy.REPLACE] sayesinde "güncelleme" için ayrı bir
     * sorgu gerekmez: aynı `id` ile yazılan kayıt satırı değiştirir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kaydet(deneme: Deneme): Long

    /** Denemeyi kimliğiyle siler. */
    @Query("DELETE FROM denemeler WHERE id = :id")
    suspend fun kimlikleSil(id: Long): Int

    /** Deneme sayısını bir kez okur (boş durum ekranı için). */
    @Query("SELECT COUNT(*) FROM denemeler")
    suspend fun sayisi(): Int
}
