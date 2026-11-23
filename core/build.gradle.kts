/*
 * Android ve Windows uygulamalarının paylaştığı saf Kotlin kodu:
 * 12 dilli metin tablosu ve dosya adından başlık/sanatçı çıkarma.
 *
 * Bu modülde Android veya Compose bağımlılığı bulunmaz; bu yüzden hem
 * uygulama (:app) hem de masaüstü sürümü (:desktop) tarafından kullanılır.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
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
