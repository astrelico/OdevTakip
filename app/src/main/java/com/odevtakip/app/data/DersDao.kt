package com.odevtakip.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Ders tablosu için veri erişim işlemleri.
 *
 * Ders adları benzersiz **olmak zorunda değildir** (şemada kısıt yok);
 * yinelenen engeli arayüz katmanında kurulur. Şemaya kısıt koymak, Türkçe
 * büyük/küçük harf eşleşmesinin SQLite NOCASE ile sınırlı olması nedeniyle
 * yanlış "zaten var" sonuçları üretirdi.
 */
@Dao
interface DersDao {

    /** Tüm dersleri ekleme sırasına göre yayınlar. */
    @Query("SELECT * FROM dersler ORDER BY olusturmaTarihi ASC, id ASC")
    fun tumDersleriIzle(): Flow<List<Ders>>

    /** Ders sayısını bir kez okur (ilk açılışta varsayılanları doldurmak için). */
    @Query("SELECT COUNT(*) FROM dersler")
    suspend fun sayisi(): Int

    /** Tüm dersleri bir kez okur (yinelenen ad kontrolü için). */
    @Query("SELECT * FROM dersler ORDER BY olusturmaTarihi ASC, id ASC")
    suspend fun tumDersleriAl(): List<Ders>

    /** Yeni ders ekler. */
    @Insert
    suspend fun ekle(ders: Ders): Long

    /** Dersi siler, silinen satır sayısını döndürür. */
    @Query("DELETE FROM dersler WHERE id = :id")
    suspend fun sil(id: Long): Int
}
