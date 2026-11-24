// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}

/*
 * Sürümün tek kaynağı: version.properties.
 *
 * app (versionCode/versionName), desktop (packageVersion + üretilen
 * BeatBoxVersion) ve update.json bu dosyadan türetilir; aynı numarayı birkaç
 * yerde elle güncellemek gerekmez.
 */
val beatboxVersion = java.util.Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val beatboxVersionCode = beatboxVersion.getProperty("version.code").trim().toInt()
val beatboxVersionName = beatboxVersion.getProperty("version.name").trim()
extra["beatboxVersionCode"] = beatboxVersionCode
extra["beatboxVersionName"] = beatboxVersionName

/** JSON metni için kaçış dizileri; update.json üretiminde kullanılır. */
fun jsonEscape(text: String): String = buildString {
    text.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
        }
    }
}

/**
 * `update.json` dosyasını sürüm dosyasından ve `release-notes.txt` içeriğinden
 * üretir. Çıktı depoda tutulur (raw.githubusercontent.com oradan okunur); CI
 * tazeliğini `git diff --exit-code update.json` ile doğrular.
 */
tasks.register("generateUpdateManifest") {
    group = "release"
    description = "update.json dosyasını version.properties ve release-notes.txt'ten üretir."

    val notesFile = layout.projectDirectory.file("release-notes.txt")
    val manifestFile = layout.projectDirectory.file("update.json")
    inputs.file(notesFile)
    inputs.property("versionCode", beatboxVersionCode)
    inputs.property("versionName", beatboxVersionName)
    outputs.file(manifestFile)

    doLast {
        /*
         * Satır sonları normalleştirilir: Windows'ta dosya CRLF olarak
         * çekilebildii için üretilen JSON'un içeriği checkout ayarına bağlı
         * kalmamalı, aksi halde CI tazelik denetimi boşuna düşer.
         */
        val notes = notesFile.asFile.readText(Charsets.UTF_8)
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
        manifestFile.asFile.writeText(
            """
            |{
            |  "versionCode": $beatboxVersionCode,
            |  "versionName": "${jsonEscape(beatboxVersionName)}",
            |  "apkUrl": "https://github.com/YasarSari001/BeatBox/releases/latest/download/BeatBox.apk",
            |  "changelog": "${jsonEscape(notes)}"
            |}
            |
            """.trimMargin(),
            Charsets.UTF_8,
        )
    }
}
