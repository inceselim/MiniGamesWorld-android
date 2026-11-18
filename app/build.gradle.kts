import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Firebase (Remote Config): app/google-services.json varsa eklenir. Dosya yoksa uygulama
// yine derlenir; Remote Config calismaz ve reklam ayarlari varsayilan degerlerde kalir.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("app/google-services.json bulunamadi: Firebase Remote Config devre disi, varsayilan reklam ayarlari kullanilacak.")
}

// Imza bilgileri proje kokundeki keystore.properties dosyasindan okunur (git'e girmez).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun adId(name: String, default: String = ""): String =
    (project.findProperty(name) as String?)?.trim()?.takeIf { it.isNotEmpty() } ?: default

android {
    // Kotlin paket adi / R sinifi. Play Store kimligi asagidaki applicationId'dir.
    namespace = "com.eomer91.minigamesworld"
    compileSdk = 36

    defaultConfig {
        // DIKKAT: Play Store'daki mevcut uygulamayla birebir ayni olmali. Degistirme.
        applicationId = "com.eomer91.MiniGamesWorldincesoy"
        minSdk = 24
        targetSdk = 36
        // Play Store'daki son surum 3 idi. Her yuklemede artir.
        versionCode = 19
        versionName = "3.0.0"

        // Reklamlar Unity Ads ile gosterilir. AdMob uygulama kimligi yalnizca Google UMP
        // (GDPR onay formu) icin manifestte kalir; AdMob SDK'si artik yok.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-1017432203303316~7837238391"

        // Unity Ads kimlikleri gradle.properties dosyasindan okunur.
        buildConfigField("String", "UNITY_GAME_ID", "\"${adId("UNITY_GAME_ID")}\"")
        buildConfigField("String", "UNITY_BANNER_ID", "\"${adId("UNITY_BANNER_ID", "Banner_Android")}\"")
        buildConfigField("String", "UNITY_INTERSTITIAL_ID", "\"${adId("UNITY_INTERSTITIAL_ID", "Interstitial_Android")}\"")
        buildConfigField("String", "UNITY_REWARDED_ID", "\"${adId("UNITY_REWARDED_ID", "Rewarded_Android")}\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Debug derlemede Unity Ads test reklamlari gosterir (gercek gelir yok).
            buildConfigField("boolean", "UNITY_TEST_MODE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField("boolean", "UNITY_TEST_MODE", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Oyun durumlari (or. Kulup Patronu dunyasi) degisken nesnelerde tutuluyor; "strong skipping"
// ayni nesne gelince ekrani yeniden cizmedigi icin secimler/bakiye gec gorunuyordu. Kapali tut.
composeCompiler {
    @Suppress("DEPRECATION")
    featureFlags.add(org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag.StrongSkipping.disabled())
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.unity.ads)
    implementation(libs.ump)
    implementation(libs.play.review)
    implementation(libs.billing)
    implementation(libs.androidx.work.runtime)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    testImplementation(libs.junit)
}
