package com.odevtakip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Ders programı tablosu için veri erişim işlemleri.
 *
 * Program **eklenmez, yazılır**: bir saat ikinci kez doldurulduğunda eski
 * değer gitmelidir. Bunu tek bir `INSERT` yaparız — birleşik anahtar
 * `(gun, sira)` çakışınca Room satırı değiştirir ([OnConflictStrategy.REPLACE]).
 * Bu yüzden ayrı bir "güncelleme" metodu yoktur.
 *
 * Boşaltma da aynı yoldan: ders adı `""` olarak yazılır, satır silinmez
 * (bkz. [ProgramSatiri]).
 */
@Dao
interface ProgramDao {

    /** Programı gün ve sıra sırasıyla yayınlar. */
    @Query("SELECT * FROM program ORDER BY gun ASC, sira ASC")
    fun programiIzle(): Flow<List<ProgramSatiri>>

    /** Bir ders saatinin dersini yazar; aynı saat doluysa değiştirir. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kaydet(satir: ProgramSatiri)
}
