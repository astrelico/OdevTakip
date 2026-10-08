package com.odevtakip.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ek dosyalarının tutulduğu **uygulama özel** dizini: `filesDir/ekler/`.
 *
 * Neden uygulama özel? Çünkü burada ne izin gerekir ne de dosya paylaşımı:
 * ek, ödevin bir parçasıdır ve başka uygulamaların ilgisini çekmez. Dosya
 * kendi elimizde olduğu için silme yetkisi de bizde kalır — ödev silinince
 * ek de gider.
 *
 * Veritabanında yalnızca dosyanın **adı** durur ([Odev.ek]); yol her okumada
 * buradan türetilir. Böylece yedek/geri yükleme taşıması gereken tek şey
 * küçük bir metin alanıdır.
 *
 * Bu sınıf kendi `Dispatcher`'ını kurar; çağıran kod bloklanacak işi
 * düşünmek zorunda değildir.
 */
class EkDeposu(context: Context) {

    private val klasor = File(context.filesDir, KLASOR)
    private val cozucu = context.contentResolver

    /** Ek dosyasının tam yolu. Dosyanın var olduğu garanti edilmez. */
    fun dosya(ad: String): File = File(klasor, ad)

    /**
     * İçeriği kopyalar ve **saklanan dosya adını** döndürür; başarısızsa
     * `null`.
     *
     * Neden kopya? Seçici [Uri]'si bize yalnızca geçici okuma izni verir ve
     * o izin ekran kapanınca düşebilir. Dosya diske geçince ek, seçici
     * yaşamadan da her zaman okunabilir.
     *
     * Ad [ekBenzersizAd] ile çakışmayacak biçimde seçilir: iki ödevin de
     * "IMG_0001.jpg"i olabilir, biri diğerini ezmaz. Kopya yarım kalırsa
     * (iptal, dolu disk) oluşturulan dosya silinir — yarım bir ek, hiç
     * olmamasından kötüdür.
     */
    suspend fun kopyala(uri: Uri): String? =
        withContext(Dispatchers.IO) { kopyalaYerel(uri) }

    /**
     * Depodaki mevcut bir ek dosyasının **bağımsız kopyasını** üretir;
     * başarısızsa `null`.
     *
     * Ödev kopyalarken gereklidir: iki kayıt da aynı dosya adını taşırsa
     * birini silmek ötekinin ekini de yok ederdi — [sil] yalnızca adı okuyup
     * dosyayı siler, diğer kaydın haberi olmaz. Bu yüzden kopya, yeni bir
     * ad alır: kaynak "IMG.jpg" ise kopya "IMG (1).jpg" olur.
     *
     * Kaynak dosya yoksa `null` döner. Bu bir hata değildir: arada silinmiş
     * bir ek, kopyanın **eksize** kalmasından başka bir sonuç doğurmaz.
     *
     * @param ad Depodaki dosyanın adı — yani [Odev.ek] değeri.
     */
    suspend fun kopyalaMevcut(ad: String): String? =
        withContext(Dispatchers.IO) { kopyalaMevcutYerel(ad) }

    /** Ek dosyasını siler; dosya zaten yoksa sessizce biter. */
    suspend fun sil(ad: String) = withContext(Dispatchers.IO) { silYerel(ad) }

    /**
     * Seçicinin verdiği [uri]'nin kullanıcıya görünen dosya adını okur.
     *
     * Form, kopyalamadan **önce** bu adı ister: kullanıcı seçtiği dosyayı
     * önizlemede görmeli. Okunamazsa `null` döner, arayüz jenerik bir
     * metinle devam eder.
     */
    suspend fun adiniOku(uri: Uri): String? =
        withContext(Dispatchers.IO) { adiniOkuYerel(uri) }

    private fun kopyalaYerel(uri: Uri): String? {
        val aday = ekAdiniTemizle(adiniOkuYerel(uri) ?: EK_VARSAYILAN_ADI)
        val ad = ekBenzersizAd(aday) { dosya(it).exists() }
        val hedef = dosya(ad)

        return try {
            klasor.mkdirs()
            val girdi = cozucu.openInputStream(uri) ?: return null
            girdi.use { g -> hedef.outputStream().use { c -> g.copyTo(c) } }
            ad
        } catch (e: Exception) {
            hedef.delete()
            null
        }
    }

    private fun kopyalaMevcutYerel(ad: String): String? {
        // Ad veritabanından geliyor; "…/../" ile klasörün dışına çıkmasın.
        val kaynak = dosya(ad)
        if (kaynak.parentFile != klasor || !kaynak.isFile) return null

        val yeniAd = ekBenzersizAd(kaynak.name) { dosya(it).exists() }
        val hedef = dosya(yeniAd)

        return try {
            klasor.mkdirs()
            kaynak.inputStream().use { g -> hedef.outputStream().use { c -> g.copyTo(c) } }
            yeniAd
        } catch (e: Exception) {
            hedef.delete()
            null
        }
    }

    private fun adiniOkuYerel(uri: Uri): String? = try {
        cozucu.query(uri, null, null, null, null)?.use { imlec ->
            if (!imlec.moveToFirst()) return@use null
            val sira = imlec.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (sira < 0) null else imlec.getString(sira)
        }
    } catch (e: Exception) {
        null
    }

    private fun silYerel(ad: String) {
        // Ad veritabanından geliyor; "../" gibi bir değer yalnızca bu
        // klasörün **içindeki** bir dosyaya ulaşabilsin.
        val hedef = dosya(ad)
        if (hedef.parentFile == klasor) hedef.delete()
    }

    private companion object {
        const val KLASOR = "ekler"
    }
}
