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
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
