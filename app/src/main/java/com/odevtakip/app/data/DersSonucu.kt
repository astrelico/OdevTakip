package com.odevtakip.app.data

/**
 * Bir deneme sınavında **tek dersin** sonucu.
 *
 * Kaydın toplam doğru/yanlış sayısı bunların toplamıdır ([Deneme.dogru]);
 * ayrı kolon olarak **saklanmaz**, tıpkı [Deneme.net] gibi türetilir.
 * İki yerde tutulsaydı biri diğerini ezebilirdi ve istatistik hangisinin
 * doğru olduğunu bilemezdi.
 *
 * Veritabanında [sonuclariYaz] ile tek satırlık bir metne, okunurken
 * [sonuclariCoz] ile geriye çevrilir (bkz. [Converters]).
 *
 * @property ders Dersin adı; [DenemeTuru.yapraklar]daki adla **aynı**
 *   yazımda olmalıdır — eşleşme ad üzerinden yapılır.
 */
data class DersSonucu(
    val ders: String,
    val dogru: Int = 0,
    val yanlis: Int = 0,
)

/**
 * Sonuçları tek metne yazar:
 * `"Türkçe=35/4;Tarih=3/1;Coğrafya=4/1;…"` (sıra [DenemeTuru.yapraklar]).
 *
 * Biçim bilinçli olarak yalındır. Ders adları [DenemeTuru]daki kendi
 * sabitlerimizdir; hiçbirinde `;`, `=` ya da `/` bulunmaz — bu yüzden
 * kaçışlamaya, ayraç seçmeye ya da üçüncü bir kütüphaneye gerek yoktur.
 * Ayracın seçimi de okunabilir: kaydı ham metin hâlinde gören biri
 * (`sqlite3` ile bakarken bile) ne yazıldığını anında çözebilir.
 *
 * Biçim ayrıca **eski kayıtlarla uyumludur**: ana ders düzeyinde yazılmış
 * eski satırlar (`Sosyal=12/3`) aynen çözülür; okuma çözümü ana dal
 * düzeyinde de yapabilir (bkz. [kotaSonucu]).
 *
 * Sıra korunur: yazılan sıra, okunan sıradır ve bu, formdaki satır
 * sırasıyla kayıtların eşleşmesini sağlar.
 */
fun sonuclariYaz(sonuclar: List<DersSonucu>): String =
    sonuclar.joinToString(";") { "${it.ders}=${it.dogru}/${it.yanlis}" }

/**
 * Metni sonuçlara çözer.
 *
 *  - **Bozuk satırlar sessizce atlanır**: tek bir yazım hatası bütün
 *    kaydı okunamaz yapmamalıdır; geriye kalan satırlar yine okunur.
 *    Boş ders adı da bozuktur: hiçbir kota ile eşleşmeyeceği için
 *    okunsa bile yalnızca çöp üretirdi.
 *  - **Negatif sayı kabul edilmez**: `0` sayılır. Girdi yolu zaten yok
 *    (form negatifi reddeder, bkz. `tamSayiCoz`) — buradaki koruma,
 *    elle düzenlenmiş bir kaydın toplamı **eksiye** düşürmesini engeller.
 *  - Boş metin boş listedir: hiç ders sonucu yazılmamış kayıt.
 */
fun sonuclariCoz(metin: String): List<DersSonucu> =
    metin.split(';').mapNotNull { parca ->
        val anahtar = parca.split('=', limit = 2)
        if (anahtar.size != 2 || anahtar[0].isBlank()) return@mapNotNull null

        val degerler = anahtar[1].split('/', limit = 2)
        if (degerler.size != 2) return@mapNotNull null

        val dogru = degerler[0].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
        val yanlis = degerler[1].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null

        DersSonucu(ders = anahtar[0], dogru = dogru, yanlis = yanlis)
    }
