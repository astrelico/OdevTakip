package com.odevtakip.app.data

import androidx.room.Entity
import com.odevtakip.app.util.gunKisaAdi
import com.odevtakip.app.util.gunUzunAdi
import java.time.LocalDate

/**
 * Bir okul gününün **varsayılan** ders saati sayısı.
 *
 * Program **haftalık**dır: kullanıcı haftanın gününü seçip o günün
 * [GUNLUK_DERS_SAYISI] satırını doldurur; her hafta aynı düzen tekrarlanır.
 * Okul gününe göre satır ekranın altındaki "Ders ekle" ile 9'a, 10'a
 * çıkarılabilir, "Son dersi kaldır" ile geri indirilebilir ve sonunda
 * **0'a** boşaltılabilir — dersi olmayan bir gün (hafta sonu) böylece
 * tamamen boş bırakılır. Günün kaç satır çizeceği [gununDersSayisi]
 * ile okunur.
 */
const val GUNLUK_DERS_SAYISI: Int = 8

/**
 * Bir günün ekranda çizilecek ders saati sayısı.
 *
 * Tabloda yalnızca **dolu** satırlar değil, "Ders ekle" ile açılmış — belki
 * hâlâ boş — satırlar da durur; bu yüzden sayı o günün en büyük sıra
 * numarasından türer. İki durum ayrıştırılır:
 *
 * - **İşaret satırı varsa** ([ProgramSatiri.SIRA_ISARET]) gün kullanıma
 *   alınmış demektir ve sayı doğrudan en büyük sıradır. Kullanıcı 8'den
 *   aşağı indiyse sayı da o kadardır; günün tamamı boşaltılmışsa yalnızca
 *   işaret kalır ve sonuç **0** olur.
 * - **İşaret yoksa** gün ya hiç dokunulmamıştır (varsayılan
 *   [GUNLUK_DERS_SAYISI] satır), ya da yalnızca atanan saatlerin yazıldığı
 *   eski veridir. Her ikisinde de en az [GUNLUK_DERS_SAYISI] gösterilir;
 *   olmayan satırlar "Ders seç" olarak çizilir.
 *
 * @param gun ISO günü (0 = pazartesi).
 * @param program Program tablosunun tamamı; işlev yalnızca [gun] satırlarına bakar.
 */
fun gununDersSayisi(gun: Int, program: List<ProgramSatiri>): Int {
    val siralar = program.filter { it.gun == gun }.map { it.sira }

    return if (ProgramSatiri.SIRA_ISARET in siralar) {
        siralar.max()
    } else {
        maxOf(GUNLUK_DERS_SAYISI, siralar.maxOrNull() ?: 0)
    }
}

/**
 * Haftanın günü.
 *
 * Sıra, ISO-8601'in gün sırasıyla aynıdır: `0 = pazartesi … 6 = pazar`.
 * Hem veritabanındaki `gun` kolonu hem de arayüzdeki çip sırası bu değeri
 * kullanır; ayrı bir indeks tablosu tutulmaz.
 *
 * Gün adları [REFERANS] tarihinden üretilir — böylece Türkçe biçimlendirme
 * tek yerde (`util.Tarih`) kalır ve program ekranı takvimle aynı dili konuşur.
 */
enum class HaftaGunu(val sira: Int) {
    PAZARTESI(0),
    SALI(1),
    CARSAMBA(2),
    PERSEMBE(3),
    CUMA(4),
    CUMARTESI(5),
    PAZAR(6);

    companion object {
        /** 1 Ocak 2024 pazartesidir; gün adları bu referans günün üstünden okunur. */
        private val REFERANS = LocalDate.of(2024, 1, 1)

        /**
         * Sıra değerini güne çevirir.
         *
         * Aralık dışı bir değer (negatif ya da 7 ve sonrası) sessizce
         * pazartesye düşer. Dışarıdan girdi her zaman [den] üzerinden
         * geçirildiği için arayüzde bilinmeyen bir gün oluşmaz.
         */
        fun den(sira: Int): HaftaGunu = entries.getOrElse(sira) { PAZARTESI }

        /** Bugünün hafta içi sırası (0 = pazartesi). */
        fun bugun(): HaftaGunu = den(LocalDate.now().dayOfWeek.value - 1)
    }

    /** Adı biçimsel olarak okunacak tarih karşılığı. */
    private val tarih: LocalDate get() = REFERANS.plusDays(sira.toLong())

    /** "Pzt" gibi kısa gün adı — program çipleri. */
    fun kisaAdi(): String = tarih.gunKisaAdi()

    /** "Pazartesi" gibi uzun gün adı — seçili günün başlığı. */
    fun uzunAdi(): String = tarih.gunUzunAdi()
}

/**
 * Haftanın bir gününde bir ders saati.
 *
 * Tablo bir **ızgaradır**: `(gun, sira)` çifti tekildir; ikinci kez yazılan
 * kayıt mevcut satırı değiştirir ([com.odevtakip.app.data.ProgramDao]).
 *
 * [ders] o saatte okunan dersin **adını** tutar, kimlik değil —
 * [Odev.ders] ile aynı gerekçe: bir ders listeden silinirse programdaki
 * yazısı yerinde kalır, ekran boşlukla değil o yazıyla karşılaşır.
 * Saate hiç ders atanmadıysa satır silinmez, yalnızca `ders` boş bırakılır;
 * böylece ekran günün [gununDersSayisi] satırını aynı düzende çizebilir.
 * 8'i aşan satırlar da aynı yolla — boş `ders` ile — açılır: boş satır
 * burada bir **işarettir**, o saatin var olduğunu ama henüz ders
 * atanmadığını söyler.
 *
 * Bunların dışında günün **kendisine** ait tek bir satır daha vardır:
 * [SIRA_ISARET]. O bir ders saati değildir — bkz. companion.
 *
 * @property gun ISO günü: 0 = pazartesi … 6 = pazar.
 * @property sira Ders saatinin sırası, 1'den başlar; [SIRA_ISARET] bunun
 *   dışında tek değerdir.
 * @property ders Atanan ders adı; atanmadıysa `""`.
 */
@Entity(tableName = "program", primaryKeys = ["gun", "sira"])
data class ProgramSatiri(
    val gun: Int,
    val sira: Int,
    val ders: String = "",
) {
    /** Bu ders saatine henüz ders atanmadı mı? */
    val bosMu: Boolean get() = ders.isBlank()

    companion object {
        /**
         * Günün **kullanıma alındığını** söyleyen işaret satırı — bir ders
         * saati değil, `sira` alanının tek dışı değeri.
         *
         * Sayaç satırlardan türetildiği için "hiç satır yok" iki farklı şey
         * anlamına gelebilirdi: gün hiç dokunulmadı (sekiz satır çizilir) ya
         * da kullanıcı tamamen boşalttı (hiç satır çizilmez). İşaret satırı
         * bu ikisini ayırır — varsa sayı [gununDersSayisi] içinde doğrudan en
         * büyük sıradır ve tek başına kalan bir gün **0 ders** demektir.
         * Arayüz bu satırı asla çizmez, sayı daima 1'den başlar.
         */
        const val SIRA_ISARET: Int = 0
    }
}
