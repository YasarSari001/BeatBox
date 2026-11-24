plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * google-services.json yalnızca Firebase yapılandırıldığında uygulanır.
 * Dosya yokken proje derlenmeye devam eder; giriş ekranı yapılandırma
 * eksikliğini gösterir ve misafir girişi çalışır.
 */
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

ksp {
    arg("room.generateKotlin", "true")
}

android {
    namespace = "com.blackstack.beatbox"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.blackstack.beatbox"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "3.9.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    lint {
        // Projede Fragment bağımlılığı yok; Activity Result API kullanımı
        // bu kontrolü yanlış pozitif olarak tetikliyor.
        disable += "InvalidFragmentVersionForActivityResult"
        // Kotlin FIR tabanlı lint analizi bu iki kontrolde
        // InvalidFirElementTypeException ile çöküyor (AGP/Kotlin araç hatası).
        disable += "UnsafeOptInUsageError"
        disable += "UnsafeOptInUsageWarning"
        // Sürüm güncelleme önerileri hata değil, bilgi düzeyinde kalsın.
        informational += listOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion"
        )
        abortOnError = true
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Ortak (Android'siz) metin ve dosya adı mantığı
    implementation(project(":core"))

    // Core Library Desugaring (required when isCoreLibraryDesugaringEnabled = true)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.foundation)

    // Media
    implementation(libs.androidx.media)

    // Palette (for dynamic colors from album art)
    implementation(libs.androidx.palette)

    // Material Color Utilities (Material You palette generation)
    implementation(libs.material.color.utilities)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit for API calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Firebase Authentication (e-posta/şifre + Google)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.kotest.assertions.core)
    androidTestImplementation(libs.kotest.property)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
