package com.odevtakip.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Ders adını kaydetmeden önce temizler: baştaki/sondaki boşluklar atılır. */
internal fun String.temizDersAdi(): String = trim()

/**
 * Liste bu ders adını içeriyor mu? (büyük/küçük harf duyarsız)
 *
 * Yinelenen engeli şemada değil **burada** kurulur; bkz. [DersDao].
 */
internal fun List<Ders>.dersAdiVarMi(ad: String): Boolean {
    val temiz = ad.temizDersAdi()
    if (temiz.isEmpty()) return false
    return any { it.ad.equals(temiz, ignoreCase = true) }
}

/**
 * Bir ders (ders adı).
 *
 * Ders listesi kullanıcı tarafından ayarlar ekranından düzenlenir. Ödev bu
 * tabloya **bağlanmaz**: [Odev.ders] adı olduğu gibi string olarak taşınır.
 * Böylece bir ders silindiğinde onu kullanan ödevler kaybolmaz — yalnızca
 * listeden kalkar, kartlardaki yazısı yerinde durur.
 *
 * @property olusturmaTarihi Ekleme sırasını korumak için kullanılır; sabit
 *   değerlerle yazılan varsayılan dersler her açılışta aynı sırada görünür.
 */
@Entity(tableName = "dersler")
data class Ders(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ad: String,
    val olusturmaTarihi: Long = System.currentTimeMillis(),
)
