package com.odevtakip.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Uygulamanın dili. Türkçe-tekil bir uygulama; `strings.xml` varsayılanı da
 * Türkçe'dir.
 */
val UYGULAMA_DILI: Locale = Locale("tr")

/**
 * Verilen bağlamı Türkçe yapılandırmasıyla döndürür.
 *
 * [MainActivity.attachBaseContext] içinde çağrılır.
 *
 * **Neden gerekli?** Bizim `strings.xml` varsayılanı Türkçe olduğu için
 * metinlerimiz zaten her koşulda Türkçe görünür. Sorun *kütüphane kaynaklı*
 * metinlerdedir: Material3 seçicilerinin başlığı, ay adı ve gün kısaltmaları
 * `values/values.xml`'in varsayılan dilinden (İngilizce) gelir ve **sistem
 * dilini izler**. İngilizce sistemli bir cihazda uygulama "Ödev Ekle / Kaydet"
 * derken tarih seçicisi "Select date" ve "October" gösterirdi — görülebilir
 * bir uyumsuzluk.
 *
 * Bağlamı burada Türkçe'ye çevirmek hem bizim kaynakları hem Material3'ün
 * `values-tr` karşılıklarını çözüme sokar; ayrı ayrı iki yerde uğraşmak gerekmez.
 *
 * Yalnızca **kaynak çözümlemesini** etkiler; `Locale.getDefault()` ile yapılan
 * biçimlendirmeleri değiştirmez. O taraf [com.odevtakip.app.util.Tarih]
 * içinde zaten `Locale("tr")` ile sabitlenmiştir.
 *
 * Not: Android'in çizdiği **izin dialogu** sistem uygulamasıdır ve sistem dilini
 * izler; onu değiştirmek uygulamanın elinde değildir.
 */
fun Context.turkceyeSabitle(): Context {
    val yapilandirma = Configuration(resources.configuration).apply {
        setLocale(UYGULAMA_DILI)
        setLayoutDirection(UYGULAMA_DILI)
    }
    return createConfigurationContext(yapilandirma)
}
