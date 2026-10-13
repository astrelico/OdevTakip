package com.odevtakip.app.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.odevtakip.app.R
import java.time.LocalDate

/**
 * Bir gün için bir dersten çözülen sorular.
 *
 * Rutin, denemeden farklı olarak **sınav değildir**: her günün kendi
 * dökümü vardır ve günler birbirini dize getirmez. Bu yüzden kaydın
 * omurgası [tarih] + [ders]dir — bir günde bir dersten **tek** kayıt
 * durur.
 *
 * @property tarih Kaydın ait olduğu günün **başlangıcı**, epoch millis.
 *   Saat bilinçli olarak tutulmaz: "12 Ekim'de Matematik çalıştım" bir
 *   gündür, bir an değil. İndirgeme
 *   [com.odevtakip.app.data.OdevRepository.rutinKaydiniYaz]'da yapılır.
 * @property ders Çalışılan dersin adı; [Ders] tablosuna kimlikle bağlı
 *   değildir (bkz. [Ders] — ödevdekiyle aynı gerekçe).
 * @property soru O derste o gün çözülen **toplam** soru.
 * @property dogru Çözülenlerin doğruları.
 * @property yanlis Çözülenlerin yanlışları.
 *
 * [dogru] + [yanlis] ≤ [soru] olmak zorunda değildir: kullanıcı "40
 * soru çözdüm, 35'i doğru, 5'i yanlış" der; kalanı **boştur** ve ayrıca
 * girilmez. Boşları toplamdan çıkarıp doğru/yanlış toplamını ayrıca
 * göstermek, kullanıcının zaten bildiği bir bilgiyi ikinci kez girmesini
 * isterdi.
 */
@Entity(
    tableName = "rutin_kayitlari",
    indices = [
        Index(value = ["tarih", "ders"], unique = true),
    ],
)
data class RutinKaydi(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tarih: Long,
    val ders: String,
    val soru: Int,
    val dogru: Int,
    val yanlis: Int,
)

/**
 * Bir dersten çözülenlerin dökümü — kartın satırı.
 *
 * Boş soru, toplam ile doğru/yanlış arasındaki farktır; ayrıca
 * saklanmaz.
 */
data class RutinSatiri(
    val ders: String,
    val soru: Int,
    val dogru: Int,
    val yanlis: Int,
) {
    /** Çözülmüş ama boş bırakılmış soru sayısı. */
    val bos: Int get() = soru - dogru - yanlis
}

/**
 * Bir günün özeti: o günün bütün derslerinin toplamı.
 *
 * Baloncuğun büyüklüğü ve "N soru" rakamı bundan gelir.
 */
data class RutinGunOzeti(
    val soru: Int,
    val dogru: Int,
    val yanlis: Int,
    val dersSayisi: Int,
) {
    val bos: Int get() = soru - dogru - yanlis

    companion object {
        /** Kayıt yoksa görülecek değer: her şey sıfır. */
        val BOS = RutinGunOzeti(soru = 0, dogru = 0, yanlis = 0, dersSayisi = 0)
    }
}

/**
 * Bir dönemin (hafta ya da ay) günlük dökümü.
 *
 * [gunOzetleri] **yalnızca kayıt olan günleri** içerir; boş gün
 * haritada hiç yoktur. Çizim tarafı "bu gün için kayıt var mı"
 * sorusunu `containsKey` ile yanıtlar — boş bir özet nesnesi taşımak
 * hem gereksiz hem de "kayıt var ama toplamı sıfır" ile "hiç kayıt
 * yok" arasındaki farkı bulanıklaştırırdı.
 */
data class RutinDonemOzeti(
    val gunOzetleri: Map<LocalDate, RutinGunOzeti>,
    val toplamSoru: Int,
    val toplamDogru: Int,
    val toplamYanlis: Int,
    val calisilanGun: Int,
) {
    companion object {
        /** Hiç kayıt gelmeden önce görülecek değer. */
        val BOS = RutinDonemOzeti(
            gunOzetleri = emptyMap(),
            toplamSoru = 0,
            toplamDogru = 0,
            toplamYanlis = 0,
            calisilanGun = 0,
        )
    }
}

/**
 * Rutin ekranının biçimi.
 *
 * Takvimdeki [TakvimGorunumu] ile aynı yapı ve aynı **düğme kuralı**:
 * **hafta** tek bakışta yedi günün dökümünü verir, **ay** ise bütün ayı
 * baloncuk haritası olarak gösterir. İkisinde de o günün toplam sorusu
 * görünür — yalnızca detay derinliği değişir.
 *
 * Düğme etiketi **hedefe** ad verir (bkz. [dugmeEtiketRes]): hafta
 * açıkken "Aylık" der, ay açıkken "Haftalık". Aksi hâlde kullanıcı açık
 * olan görünümün adını okuyup kapatmaya çalışır.
 *
 * Değer saklanmaz; yalnızca oturum boyunca taşınır.
 */
enum class RutinGorunumu(
    /** Bu görünüm açıkken düğmede duran etiket — yani **hedefin** adı. */
    @StringRes val dugmeEtiketRes: Int,
) {
    /** Seçili haftanın yedi gün kartı — varsayılan; düğme "Aylık" der. */
    HAFTA(R.string.rutin_gorunum_ay),

    /** Ay ızgarası + seçili günün dökümü; düğme "Haftalık" der. */
    AY(R.string.rutin_gorunum_hafta);

    /** Bir sonraki tıklamada geçilecek görünüm. */
    fun hedefi(): RutinGorunumu = when (this) {
        HAFTA -> AY
        AY -> HAFTA
    }
}

/**
 * Ekranın izlediği dönemin ham kayıtları **ve** özeti.
 *
 * Tek akışta taşınır ki kartların satırları ([kayitlar]) ile baloncukların
 * büyüklüğü ([ozet]) **her zaman aynı sorgudan** gelsin: iki ayrı akış olsaydı
 * kayıt eklendiği anda biri yenilenip diğeri güncellenmediği için kısa bir
 * süre boyunca kartta iki "Matematik" satırı, baloncukta ise bir tane
 * görünebilirdi.
 */
data class RutinDonemDurumu(
    val kayitlar: List<RutinKaydi>,
    val ozet: RutinDonemOzeti,
) {
    companion object {
        /** Akış ilk akmadan önce görülecek değer. */
        val BOS = RutinDonemDurumu(kayitlar = emptyList(), ozet = RutinDonemOzeti.BOS)
    }
}
