package com.odevtakip.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir deneme sınavı kaydı.
 *
 * Ödevden farklı olarak denemenin **durumu yoktur**: sınav ya olmuştur ya
 * olmamıştır, tarihi geçince "gecikti" gibi bir kavram doğmaz. Bu yüzden
 * [Odev.durum], [Odev.tamamlanmaTarihi] gibi alanlar burada yoktur.
 *
 * Net ayrıca **saklanmaz** — [net] özelliği her okumada doğru/yanlış
 * hesabından türetilir. Ayrı bir kolon olsaydı iki alan arasında tutarsızlık
 * (kaydedilmiş net ≠ doğru-yanlış/4) mümkün olurdu ve bu, istatistikte
 * yanlış ortalamaya yol açardı.
 *
 * @property ad Sınavın adı; zorunludur. "3. Deneme", "TYT prova 2" gibi.
 * @property tarih Sınavın yapıldığı gün, epoch millis. Saat **bilinçli olarak
 *   tutulmaz**: deneme "12 Ekim'de yapıldı" diye hatırlanır, kaçta
 *   başladığı değil. Gün başına yazılır ki sıralama ve filtre bozulmasın.
 * @property ders Sınavın ait olduğu dersin **adı** — [Odev.ders] ile aynı
 *   kural: tabloya bağlı değil, boş string "seçilmedi" demektir. Boş
 *   bırakılabilir (genel denemeler tüm dersleri kapsar).
 * @property dogru Doğru cevap sayısı.
 * @property yanlis Yanlış cevap sayısı.
 * @property puan Öğrencinin girdiği puan; girilmediyse `null`. Okul
 *   puanlarıyla standart puan farklı olduğundan **hesaplanmaz**, kullanıcının
 *   yazdığı değer olduğu gibi korunur.
 * @property aciklama İsteğe bağlı not: "matematik kötüydü" gibi.
 */
@Entity(tableName = "denemeler")
data class Deneme(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ad: String,
    val tarih: Long,
    val ders: String = "",
    val dogru: Int = 0,
    val yanlis: Int = 0,
    val puan: Double? = null,
    val aciklama: String = "",
    val olusturmaTarihi: Long = System.currentTimeMillis(),
) {
    /**
     * Net: `doğru − (yanlış ÷ 4)`.
     *
     * Dört seçenekli standart varsayım; bu yüzden kolon değil **özellik**
     * olarak tutulur (bkz. sınıf açıklaması). Kayıtta cevap girilmemişse
     * `0.0` döner — `0 - 0/4` zaten sıfırdır.
     */
    val net: Double
        get() = dogru - yanlis / 4.0
}
