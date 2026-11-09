import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Imza bilgileri proje kokundeki keystore.properties dosyasindan okunur (git'e girmez).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun adId(name: String): String = (project.findProperty(name) as String?)?.trim().orEmpty()

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
        versionCode = 12
        versionName = "2.6.0"

        // Unity Ads: Game ID ve reklam birimleri gradle.properties'ten (gizli degil).
        buildConfigField("String", "UNITY_GAME_ID", "\"${adId("UNITY_GAME_ID")}\"")
        buildConfigField("String", "UNITY_INTERSTITIAL_ID", "\"${adId("UNITY_INTERSTITIAL_ID").ifEmpty { "Interstitial_Android" }}\"")
        buildConfigField("String", "UNITY_REWARDED_ID", "\"${adId("UNITY_REWARDED_ID").ifEmpty { "Rewarded_Android" }}\"")
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
            // Debug derlemede Unity yalnizca test reklami gosterir
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
    implementation(libs.play.review)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
}
