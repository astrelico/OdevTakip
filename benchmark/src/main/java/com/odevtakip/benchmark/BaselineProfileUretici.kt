package com.odevtakip.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline profile üreteci.
 *
 * Uygulamayı **gerçek** bir senaryoyla gezer — liste kaydırır, sekmeler
 * arası geçer, bir ödevi açar, formu açar — ve bu sırada yürütülen sıcak
 * yolların (sınıf ve method çağrı listesi) profilini kaydeder. Android bu
 * profili kurulumda dexopt'a verir; uygulama ilk açılışta o yolları yorumlayarak
 * değil, önceden derlenmiş hâlde çalışır.
 *
 * ## UYARI: bu test cihazdaki uygulama verisini SİLER
 *
 * Eklenti her iterasyonda profilin eski kalıplardan arınması için paketi
 * **kalıcı olarak kaldırıp yeniden kurar** (`Uninstalling` / `Installing`,
 * logcat'te `PACKAGE_FULLY_REMOVED`). Room veritabanı, ek dosyalar ve
 * tercihler de dahil her şey gider; geri getirilemez.
 *
 * Bu bilinçli bir ödünleşmedir: stabil bir ölçüm için derlenmiş durumun ve
 * verinin her iterasyonda aynı olması gerekir, benchmark'ın kendi davranışı
 * budur. Yine de **çalıştırmadan önce cihazdaki verinin önemsiz olduğundan
 * emin olun** ya da ayrı bir test cihazı kullanın. Üretim verisi taşıyan
 * bir telefonda asla çalıştırılmamalıdır.
 *
 * Cihazda yalnızca release varyantı çalıştırılır. Debug varyantı profil
 * üretemez: `debuggable=true` olan paketlerde ART profili uygulamaz.
 *
 * Seçiciler bilinçli olarak **veriye bağlı değil**: "Ödev Ekle", "Kaydet",
 * "Geri" gibi arayüzün sabit metinleri kullanılır. Ödev adı ya da ders adı
 * kullanılsaydı, kullanıcı kendi verisini değiştirdiği anda test kırılırdı.
 *
 * Çalıştırma: `gradlew :app:generateReleaseBaselineProfile`
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileUretici {

    @get:Rule
    val kural = BaselineProfileRule()

    @Test
    fun anaSenaryo() = kural.collect(
        packageName = PAKET,
        maxIterations = 5,
        // Baseline profile'a ek olarak **startup profile** da üretilir.
        //
        // Fark: baseline profile uygulamanın genel sıcak yollarını,
        // startup profile ise yalnızca **açılış sırasında** dokunulan
        // yolları kapsar. Android bunu kurulumdexopt'una ayrı bir öncelikle
        // verir; cold start'i hızlandıran asıl parça budur.
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        // Liste ekranı: kartlar gelene kadar bekle, sonra kaydır.
        device.wait(Until.hasObject(By.text("Ödev Ekle")), ACILIS_ZAMAN_ASIMI)
        kaydir(yon = Direction.DOWN, tekrar = 3)
        kaydir(yon = Direction.UP, tekrar = 1)

        // Sekmeler: her geçişte ekran tamamen yeniden çizilir; en pahalı yol.
        sekmeAc("Takvim")
        kaydir(yon = Direction.RIGHT, tekrar = 2)

        sekmeAc("Ders Programı")
        device.waitForIdle()

        sekmeAc("İstatistik")
        device.waitForIdle()

        sekmeAc("Ödevler")

        // Detay ekranı: ilk tıklanabilir karta dokun, geri dön.
        // Seçici veriye bağlı değil — herhangi bir tıklanabilir öge yeterli,
        // çünkü kart gövdesinin tamamı tıklanabilir.
        if (device.findObject(By.clickable(true)) != null) {
            device.findObject(By.clickable(true))?.click()
            device.wait(Until.hasObject(By.desc("Geri")), ACILIS_ZAMAN_ASIMI)
            device.pressBack()
            device.waitForIdle()
        }

        // Form ekranı: FAB'i aç, geri dön.
        device.findObject(By.text("Ödev Ekle"))?.click()
        device.wait(Until.hasObject(By.text("Kaydet")), ACILIS_ZAMAN_ASIMI)
        device.pressBack()
        device.waitForIdle()
    }

    /** Alt menüden bir sekmeye dokunur; içerik gelene kadar bekler. */
    private fun MacrobenchmarkScope.sekmeAc(ad: String) {
        device.findObject(By.text(ad))?.click()
        device.waitForIdle()
    }

    /**
     * Verilen yönde kaydırma hareketi yapar.
     *
     * `UiObject2.scroll()` burada **kullanılmaz**: Compose `LazyColumn` her
     * karede öğeleri geri dönüşüm havuzuna verip yeniden bağladığı için,
     * bir kez bulunmuş düğüm kaydırmanın kendisi sırasında
     * `StaleObjectException` ile ölür. Ekran koordinatlarıyla ham `swipe`
     * yapıldığında hiçbir düğüme tutunulmaz; kaydırma girdiyi doğrudan
     * sistemeiletir, Compose da kendi geçerliliğini korur.
     *
     * Payımlar (0.6f) ekranın orta bandını kullanır: FAB sağ altta, alt menü
     * en altda, üst bar en tepede olduğu için bu bant içerik dışında hiçbir
     * düğmeye denk gelmez.
     */
    private fun MacrobenchmarkScope.kaydir(yon: Direction, tekrar: Int) {
        val genislik = device.displayWidth
        val yukseklik = device.displayHeight

        repeat(tekrar) {
            val (x1, y1, x2, y2) = when (yon) {
                Direction.DOWN -> listOf(
                    genislik / 2, (yukseklik * 0.70f).toInt(),
                    genislik / 2, (yukseklik * 0.28f).toInt(),
                )
                Direction.UP -> listOf(
                    genislik / 2, (yukseklik * 0.28f).toInt(),
                    genislik / 2, (yukseklik * 0.70f).toInt(),
                )
                Direction.RIGHT -> listOf(
                    (genislik * 0.75f).toInt(), (yukseklik * 0.45f).toInt(),
                    (genislik * 0.25f).toInt(), (yukseklik * 0.45f).toInt(),
                )
                else -> listOf(
                    (genislik * 0.25f).toInt(), (yukseklik * 0.45f).toInt(),
                    (genislik * 0.75f).toInt(), (yukseklik * 0.45f).toInt(),
                )
            }
            device.swipe(x1, y1, x2, y2, ADIM)
            device.waitForIdle()
        }
    }

    private companion object {
        const val PAKET = "com.odevtakip.app"

        /** Soğuk açılış Compose ve Room ilk kez ısındığı için cömert tutulur. */
        const val ACILIS_ZAMAN_ASIMI = 10_000L

        /**
         * Kaydırma adımları (koordinat arası kaç ara nokta).
         *
         * Az adım = kısa süren ama "flick" gibi ani bir hareket; çok adım =
         * uzun süren ama gerçek parmak kaydırmasına benzeyen bir hareket.
         * 25, ikisinin ortası: yeterince uzun ki Compose kendi çizim döngüsünü
         * işleyebilsin, kısa ki beş tekrar makul sürede bitsin.
         */
        const val ADIM = 25
    }
}
