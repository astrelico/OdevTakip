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
     * Satır **yoksa** ekler, varsa hiçbir şey yapmaz.
     *
     * [com.odevtakip.app.data.OdevRepository.gunuHazirla] eski verideki eksik
     * saatleri bu yolla tamamlar: satır zaten varsa — üzerinde atanmış bir
     * ders bile olsa — ezilmez. Yazma yollarının geri kalanı [kaydet]ı
     * kullanır, çünkü orada eski değeri değiştirmek istenir.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ekle(satir: ProgramSatiri)

    /** Belirtilen sıradaki satır var mı? — işareti okumak için kullanılır. */
    @Query("SELECT EXISTS(SELECT 1 FROM program WHERE gun = :gun AND sira = :sira)")
    suspend fun satirVarMi(gun: Int, sira: Int): Boolean

    /**
     * Bir ders saatini — ve o saate atanmış dersi — tablodan siler;
     * gün ve sıra eşleşen satır gider.
     *
     * "Son dersi kaldır" bunu çağırır. Sıra 1'e ve sonunda 0'a kadar
     * inebilir; ancak [ProgramSatiri.SIRA_ISARET] satırı bir saat
     * olmadığından kaldırma yolunun dışında tutulur — sayaç ancak orada
     * durabilir.
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
     *
     * İşaret satırının sırası 0 olduğundan sonuç ondan etkilenmez — gün
     * tamamen boşaltılmışsa dönen değer 0 olur ve bir sonraki satır 1'e
     * yazılır.
     */
    @Query("SELECT COALESCE(MAX(sira), 0) FROM program WHERE gun = :gun")
    suspend fun gununSonSirasi(gun: Int): Int
}
