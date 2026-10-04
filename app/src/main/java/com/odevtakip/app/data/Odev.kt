package com.odevtakip.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir ödev kaydı.
 *
 * @property sonTarih Teslim tarihi + saati, epoch millis olarak. Tarih ve saat
 *   tek bir alan olarak tutulur; ayrı alanlar yerine bu, "geçti mi?" sorusunu
 *   tek bir karşılaştırmayla yanıtlamamızı sağlar.
 * @property durum Veritabanında saklanan son bilinen durum.
 * @property tamamlanmaTarihi Kullanıcının tamamlandı olarak işaretlediği an.
 */
@Entity(tableName = "odevler")
data class Odev(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val baslik: String,
    val aciklama: String = "",
    val sonTarih: Long,
    val durum: Durum = Durum.BEKLIYOR,
    val olusturmaTarihi: Long = System.currentTimeMillis(),
    val tamamlanmaTarihi: Long? = null,
)
