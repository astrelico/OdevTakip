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

    /**
     * Bir ders saatini tablodan siler — gün ve sıra eşleşen satır(lar) gider.
     *
     * "Son dersi kaldır" düğmesi bunu çağırır: 8. satır alt sınır olduğu için
     * arayüz yalnızca ondan uzun günlerde kaldırma gösterir, dolayısıyla
     * `[GUNLUK_DERS_SAYISI]` altına inen bir kayıt oluşmaz.
     *
     * @return Silinen satır sayısı.
     */
    @Query("DELETE FROM program WHERE gun = :gun AND sira = :sira")
    suspend fun sil(gun: Int, sira: Int): Int

    /**
     * Bir günün en büyük sıra numarası; o güne hiç satır yazılmamışsa 0.
     *
     * "Ders ekle" bunu okuyup bir fazlasını yazar: sayaç ekrandaki
     * kompozisyon durumundan değil, her zaman tablodan türetilir. Böylece
     * kullanıcı aynı anda iki kez dokunsa da iki farklı satır açılır.
     */
    @Query("SELECT COALESCE(MAX(sira), 0) FROM program WHERE gun = :gun")
    suspend fun gununSonSirasi(gun: Int): Int
}
