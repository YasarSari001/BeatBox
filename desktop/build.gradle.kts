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

// Sürüm tek kaynaktan (version.properties) gelir.
val beatboxVersionCode = rootProject.extra["beatboxVersionCode"] as Int
val beatboxVersionName = rootProject.extra["beatboxVersionName"] as String

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

/*
 * Güncelleme denetleyicisinin karşılaştırma yaptığı sürüm sabitleri elle
 * tutulmaz: build sırasında version.properties'ten üretilir. Böylece sürüm
 * artırmak tek dosyada kalır.
 */
val generatedVersionDir = layout.buildDirectory.dir("generated/version")
val generateVersionSource by tasks.registering {
    val outputDir = generatedVersionDir
    val code = beatboxVersionCode
    val name = beatboxVersionName
    inputs.property("versionCode", code)
    inputs.property("versionName", name)
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get()
            .file("com/blackstack/beatbox/desktop/BeatBoxVersion.kt")
            .asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.blackstack.beatbox.desktop
            |
            |/**
            | * Uygulamanın sürümü.
            | *
            | * `version.properties` dosyasından build sırasında üretilir; bu dosyayı
            | * elle düzenlemeyin, sürümü version.properties içinde artırın.
            | */
            |object BeatBoxVersion {
            |    const val CODE = $code
            |    const val NAME = "$name"
            |}
            |
            """.trimMargin(),
            Charsets.UTF_8,
        )
    }
}

kotlin.sourceSets.getByName("main").kotlin.srcDir(generateVersionSource)

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

    // Etiket (ID3/Vorbis/MP4) okuma ve yazma.
    implementation("net.jthink:jaudiotagger:3.0.1")

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.blackstack.beatbox.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "BeatBox"
            packageVersion = beatboxVersionName
            // WiX, kurulum paketinde ASCII dışı karakter kabul etmiyor (LGHT0311).
            description = "BeatBox music player for Windows"
            vendor = "BlackStack"

            /*
             * Windows paket bilgileri. Simge olmadan kurulan uygulama
             * "Uygulamalar" listesinde genel Java simgesiyle ve düşük
             * çözünürlüklü görünüyordu; upgradeUuid ise yeni sürümün aynı
             * kaydı güncellemesini sağlar.
             */
            windows {
                iconFile.set(project.file("icons/beatbox.ico"))
                menuGroup = "BeatBox"
                shortcut = true
                // Kullanıcı kurulumu: uygulama içi güncelleme yönetici izni istemez.
                perUserInstall = true
                dirChooser = false
                // Sabit kalması gerekir; değişirse kurulu kopyalar güncellenemez.
                upgradeUuid = "b110e892-7c54-4730-b180-a5d1cba1617d"
            }
        }
    }
}
