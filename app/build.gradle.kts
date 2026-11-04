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
        versionCode = 9
        versionName = "2.3.0"

        // AdMob artik LevelPlay icinden (adaptor ile) calisiyor; adaptor yine de App ID ister.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-1017432203303316~7837238391"

        // Unity LevelPlay anahtarlari gradle.properties'ten okunur. Bos birakilirsa reklam gosterilmez.
        buildConfigField("String", "LEVELPLAY_APP_KEY", "\"${adId("LEVELPLAY_APP_KEY")}\"")
        buildConfigField("String", "LEVELPLAY_BANNER_ID", "\"${adId("LEVELPLAY_BANNER_ID")}\"")
        buildConfigField("String", "LEVELPLAY_INTERSTITIAL_ID", "\"${adId("LEVELPLAY_INTERSTITIAL_ID")}\"")
        buildConfigField("String", "LEVELPLAY_REWARDED_ID", "\"${adId("LEVELPLAY_REWARDED_ID")}\"")
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
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

// Reklam anahtarlari olmadan yanlislikla reklamsiz surum yayinlanmasin.
val levelPlayAppKey = adId("LEVELPLAY_APP_KEY")
val checkAdKeys by tasks.registering {
    doLast {
        if (levelPlayAppKey.isBlank()) {
            throw GradleException("LEVELPLAY_APP_KEY bos: gradle.properties dosyasina LevelPlay anahtarlarini gir.")
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkAdKeys) }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
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

    // Unity LevelPlay (reklam aracisi) + ag adaptorleri. Adaptorler kendi SDK'larini icerir.
    implementation(libs.levelplay)
    implementation(libs.levelplay.unityads.adapter)
    implementation(libs.levelplay.admob.adapter)
    implementation(libs.play.services.appset)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.ump)
    implementation(libs.play.review)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
}
