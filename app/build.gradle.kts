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
        versionCode = 7
        versionName = "1.5.5"

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
        // Hakkında ekranı sürüm numarasını `BuildConfig.VERSION_NAME`den
        // okur; bu sınıf varsayılan olarak üretilmez, açılmazsa derleme
        // `Unresolved reference 'BuildConfig'` ile patlar.
        buildConfig = true
    }
    // Baseline profile eklentisi release varyantının dışa aktarılmasını ister;
    // verilmazse profil APK'ya gömülemez.
    publishing {
        singleVariant("release")
    }

    // MigrationTestHelper, şema JSON'larını androidTest'in asset'lerinden okur.
    // Dosyalar `schemas/` altında üretilir; testin erişebilmesi için oraya
    // kaynak olarak eklenmeleri gerekir.
    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }
}

// Room şemasını sürüm kontrolü için kaydet (migration stratejisi Faz 1.5'te kullanılacak).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// kotlinx-serialization sürümlerini aynıda tut: 1.8.1.
//
// `androidx.savedstate:1.5.0` serialization **core 1.7.3** çekerken, Room
// 2.8.5 **json 1.8.1** istiyor. `json` 1.8.1 zaten sınıf yolunda duruyor,
// `core` ise 1.7.3'e düşürülüyordu — yani uygulama **karışık** bir çiftle
// çıkıyordu. Etkisi şimdilik gizli, çünkü üretimde şema JSON'u okunmaz;
// ama `MigrationTestHelper` tam da onu okur ve Room'un üretilmiş
// serializer'ları `core`'un eski `GeneratedSerializer` arayüzüyle
// uyuşmadığı için `AbstractMethodError` ile çöker.
//
// `core`'u yukarı çekmek `json`'un zaten istediği sürümdür; 1.7.3 → 1.8.1
// 1.x içinde uyumlu bir sürüm. Gradle'ın **tutarlı çözümü** ana yoldaki
// kararı test yoluna `strictly` olarak aktardığı için kısıt yalnızca test
// sınıfına konamaz — ana yol düzeldiğinde test de düzeldiği için gerek de
// kalmaz. Bölüm sonu (Faz 18+): bağımlılık sürüm düzeltmesi, davranış
// değişikliği değil.
dependencies {
    constraints {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    }

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
    // Migration testleri: şema JSON'larını okuyup eski sürüm bir veritabanı
    // açar ve migration'ın beklenen şemayla uyuştuğunu doğrular.
    androidTestImplementation(libs.room.testing)
}
