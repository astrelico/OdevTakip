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
 * Deneme tek bir ders değil, bir **sınav formatıdır** ([tur]): TYT'de
 * dört test, AYT'de dört ders vardır ve her birinin soru sayısı kilitlidir.
 * Bu yüzden kaydın omurgası [sonuclar]dır — ders bazlı doğru/yanlış.
 *
 * Toplamlar ayrıca **saklanmaz** — [dogru], [yanlis] ve [net] her okumada
 * [sonuclar]'dan türetilir. Ayrı kolonlar olsaydı iki yer arasındaki
 * tutarsızlık (kayıtlı toplam ≠ derslerin toplamı) mümkün olurdu ve bu,
 * istatistikte yanlış ortalamaya yol açardı. Aynı gerekçe [net] için de
 * geçerlidir.
 *
 * @property ad Sınavın adı; zorunludur. "3. Deneme", "TYT prova 2" gibi.
 * @property tarih Sınavın yapıldığı gün, epoch millis. Saat **bilinçli
 *   olarak tutulmaz**: deneme "12 Ekim'de yapıldı" diye hatırlanır, kaçta
 *   başladığı değil. Gün başına yazılır ki sıralama bozulmasın.
 * @property tur Sınavın formatı (TYT/AYT); ders listesini ve soru
 *   kotalarını belirler. Ekleme anında seçilir ve **değiştirilemez** —
 *   120 soruluk bir formun kaydı, 80 soruya taşınamaz.
 * @property sonuclar Ders bazlı doğru/yanlış; sıra
 *   [DenemeTuru.yapraklar] ile aynıdır. Bölünmüş dersler (Sosyal, Fen)
 *   **alt testleriyle** saklanır — Sosyal'un toplamı dört alt testin
 *   toplamıdır, ayrıca yazılmaz (bkz. [kotaSonucu]). Veritabanında tek
 *   satırlık metin olarak saklanır (bkz. [sonuclariYaz]).
 * @property aciklama İsteğe bağlı not: "matematik kötüydü" gibi.
 */
@Entity(tableName = "denemeler")
data class Deneme(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ad: String,
    val tarih: Long,
    val tur: DenemeTuru = DenemeTuru.TYT,
    val sonuclar: List<DersSonucu> = emptyList(),
    val aciklama: String = "",
    val olusturmaTarihi: Long = System.currentTimeMillis(),
) {
    /**
     * Toplam doğru cevap sayısı — [sonuclar]'ın toplamı.
     *
     * Kayıtta hiç ders sonucu yoksa `0`; bkz. [yanlis].
     */
    val dogru: Int
        get() = sonuclar.sumOf { it.dogru }

    /** Toplam yanlış cevap sayısı — [sonuclar]'ın toplamı. */
    val yanlis: Int
        get() = sonuclar.sumOf { it.yanlis }

    /**
     * Net: `doğru − (yanlış ÷ 4)`.
     *
     * Dört seçenekli standart varsayım. Toplamlar da [sonuclar]'dan
     * türetildiği için bu özellik **hiçbir şey saklamaz** — okuma anında
     * her zaman güncel ve tutarlıdır.
     */
    val net: Double
        get() = dogru - yanlis / 4.0
}
