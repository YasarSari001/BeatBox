import org.jetbrains.compose.desktop.application.dsl.TargetFormat

/*
 * BeatBox'ın Windows sürümü.
 *
 * UI Android uygulamasındaki Compose ekranlarının birebir kopyası değil;
 * masaüstüne uygun tek pencereli bir oynatıcı. Ortak metin tablosu ve dosya
 * adı mantığı :core modülünden gelir.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Java Sound'a MP3 ve OGG desteği ekler; ayrı bir dekoder gerekmez.
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7-2")
    implementation("com.googlecode.soundlibs:vorbisspi:1.0.3.3")

    // Video çözme: JavaCV'nin FFmpeg sarmalayıcısı. Yalnızca FFmpeg bağları
    // alınır (OpenCV vb. yok); native kütüphane Windows x64 paketinden gelir.
    implementation("org.bytedeco:javacv:1.5.11") { isTransitive = false }
    implementation("org.bytedeco:javacpp:1.5.11")
    implementation("org.bytedeco:ffmpeg:7.1-1.5.11")
    runtimeOnly("org.bytedeco:ffmpeg:7.1-1.5.11:windows-x86_64")

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.blackstack.beatbox.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "BeatBox"
            packageVersion = "3.9.1"
            // WiX, kurulum paketinde ASCII dışı karakter kabul etmiyor (LGHT0311).
            description = "BeatBox music player for Windows"
            vendor = "BlackStack"
        }
    }
}
