import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Signing credentials live in keystore.properties, which is gitignored. The build
// falls back to unsigned when the file is absent, so a fresh clone still builds.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

android {
    namespace = "com.samasar.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.samasar.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.getProperty("storeFile") != null) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // Same live feed as release. To develop against a local pipeline
            // instead, run `python -m http.server 8000 --bind 127.0.0.1` in
            // backend/public and swap this for "http://10.0.2.2:8000/" —
            // 10.0.2.2 is the emulator's alias for the host loopback.
            buildConfigField(
                "String",
                "FEED_BASE_URL",
                "\"https://raw.githubusercontent.com/mk-bairagi/News-Pro/feeds/\"",
            )
        }
        release {
            if (keystoreProperties.getProperty("storeFile") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField(
                "String",
                "FEED_BASE_URL",
                "\"https://raw.githubusercontent.com/mk-bairagi/News-Pro/feeds/\"",
            )
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
