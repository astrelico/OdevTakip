package com.odevtakip.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir ödev kaydı.
 *
 * @property sonTarih Teslim tarihi + saati, epoch millis olarak. Tarih ve saat
 *   tek bir alan olarak tutulur; ayrı alanlar yerine bu, "geçti mi?" sorusunu
 *   tek bir karşılaştırmayla yanıtlamamızı sağlar.
 * @property ders Ödevin ait olduğu dersin **adı**. [Ders] tablosuna kimlikle
 *   bağlı değildir — bkz. [Ders] sınıfı. Boş string "ders seçilmedi" demektir
 *   (Faz 12'den önceki kayıtlar taşma olmadan güncellensin diye migration
 *   bu kolonu `DEFAULT ''` ile ekler).
 * @property durum Veritabanında saklanan son bilinen durum.
 * @property tamamlanmaTarihi Kullanıcının tamamlandı olarak işaretlediği an.
 * @property ek Eklenen dosyanın uygulama deposundaki **adı**; `null` = ek yok.
 *   Dosyanın kendisi `filesDir/ekler/` altında durur (bkz. [EkDeposu]) —
 *   tabloda yalnızca adı tutulur, hem satır hafif kalır hem yedek taşıması
 *   tek bir metin alanına iner. `ders`'ten farklı olarak migration **DEFAULT**
 *   vermez: eski kayıtlar `NULL` kalır ve "ek yok" anlamına gelir.
 */
@Entity(tableName = "odevler")
data class Odev(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val baslik: String,
    val aciklama: String = "",
    @ColumnInfo(defaultValue = "''")
    val ders: String = "",
    val sonTarih: Long,
    val durum: Durum = Durum.BEKLIYOR,
    val olusturmaTarihi: Long = System.currentTimeMillis(),
    val tamamlanmaTarihi: Long? = null,
    val ek: String? = null,
)
