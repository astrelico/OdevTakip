plugins {
    // com.android.application değil, com.android.test: bu modül kendisi
    // dağıtılmaz, yalnızca başka bir projeyi (app) test eden bir araçtır.
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.odevtakip.benchmark"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Ölçülecek paket. Bu satır olmazsa modül hangi uygulamayı hedeflediğini
    // bilemez ve derleme hatası verir.
    targetProjectPath = ":app"
    // Kendi kendine enstrümante: test APK'sı kendi izinlerini de kendi
    // bildirim manifestine taşır; hedef uygulamanın manifest'iyle çakışmaz.
    experimentalProperties["android.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Eklentinin release varyantını derleyip cihaza kurması için gereken tek
// yapılandırma; başka bir ayar gerekmez. `benchmark {}` bloğu yalnızca
// yönetilen cihaz ayarlarını taşır, burada gerekmedi.

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
