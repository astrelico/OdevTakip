plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    // Baseline profile desteğini uygulamaya ekler: :benchmark modülünün
    // ürettiği profili release APK'ya gömer.
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.odevtakip.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.odevtakip.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // APK'nın kurulabilmesi için imza zorunlu; uygulama Google Play'e
            // konmayacağı için imzalama anahtarı olarak **debug anahtarı**
            // kullanılıyor. Bu, SDK kurulumunda otomatik üretilen anahtardır
            // (~/.android/debug.keystore) ve parolası herkeste aynıdır.
            //
            // Bilinçli bir ödün: Play'e çıkacak olsaydık buraya özel bir
            // keystore gerekirdi ve o anahtar **asla değişmemeliydi** —
            // değişirse kullanıcıların elindeki sürümün üstüne güncelleme
            // kurulamaz. Şimdilik GitHub dağıtımı için yeterli.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (LocalDate, ZoneId) minSdk 24'te hazır değil; desugaring ile
        // eski cihazlarda da kullanılabilir kılıyoruz.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    // Baseline profile eklentisi release varyantının dışa aktarılmasını ister;
    // verilmazse profil APK'ya gömülemez.
    publishing {
        singleVariant("release")
    }
}

// Room şemasını sürüm kontrolü için kaydet (migration stratejisi Faz 1.5'te kullanılacak).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines (Flow + suspend DAO çağrıları)
    implementation(libs.kotlinx.coroutines.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Arka plan işleri: gecikmiş ödev senkronu (Faz 3)
    implementation(libs.androidx.work.runtime)

    // Baseline profile: benchmark modülünün ürettiği profil buradan APK'ya
    // girer; profileinstaller profili ilk açılışta cihazdexopt'una kurar.
    baselineProfile(project(":benchmark"))
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
}
