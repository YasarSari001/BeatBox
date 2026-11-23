/*
 * BeatBox
 *
 * Bu dosya, GPL-3.0 lisansı altında yayımlanan Metrolist projesinden
 * (https://github.com/MetrolistGroup/Metrolist) uyarlanmıştır.
 * Uyarlanmış bu dosya da GPL-3.0 koşulları altında dağıtılır.
 *
 * This file is adapted from the Metrolist project, licensed under GPL-3.0.
 * This adapted file is distributed under the terms of GPL-3.0.
 */
package com.blackstack.beatbox.desktop.lyrics

import java.io.File

/**
 * Kelime bazlı zaman damgası (gelişmiş LRC / TTML).
 *
 * Metrolist'in `WordTimestamp` modelinden uyarlanmıştır; yalnızca BeatBox'ın
 * karaoke ekranı için gereken alanları taşır.
 *
 * @property text Kelimenin ham metni (ayırıcı boşluklar dahil)
 * @property startMs Kelimenin söylenmeye başladığı an (milisaniye)
 */
data class LyricsWord(
    val text: String,
    val startMs: Long,
)

/**
 * Senkronize şarkı sözünün tek satırı.
 *
 * @property timestampMs Satırın gösterilmeye başlanacağı an (milisaniye)
 * @property text Satırın metni
 * @property words Karaoke vurgusu için isteğe bağlı kelime zamanları
 */
data class SyncedLyricsLine(
    val timestampMs: Long,
    val text: String,
    val words: List<LyricsWord> = emptyList(),
)

/**
 * Bir şarkının tüm senkronize sözleri.
 *
 * @property lines Zamana göre sıralanmış satırlar
 * @property metadata LRC etiketlerinden çıkarılan künye
 */
data class SyncedLyrics(
    val lines: List<SyncedLyricsLine>,
    val metadata: LyricsMetadata = LyricsMetadata(),
    /** `false` ise sözler zaman damgasızdır (düz metin); vurgulama yapılmaz. */
    val synced: Boolean = true,
)

/**
 * LRC dosya etiketlerinden okunan künye.
 *
 * @property source Sözlerin kaynağı ("LRCLIB", "Lyrics.ovh", "local", "cache", ...)
 */
data class LyricsMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val by: String? = null,
    val offset: Long = 0L,
    val source: String? = null,
)

/**
 * Disk önbelleğindeki söz kaydı.
 */
data class CachedLyrics(
    val songKey: String,
    val lrcContent: String,
    val source: String,
    val fetchedAt: Long,
)

/**
 * Söz yükleme durumu.
 */
sealed class LyricsState {
    data object Idle : LyricsState()
    data object Loading : LyricsState()
    data class Success(val lyrics: SyncedLyrics) : LyricsState()
    data class Error(val message: String) : LyricsState()
}

/**
 * Dosya tabanlı söz önbelleği (masaüstünde Room'un yerini alır).
 *
 * Her kayıt iki dosyaya yazılır: `<key>.lrc` ham içerik, `<key>.src` ise
 * kaynak adı ve zaman damgasını `kaynak|epochMs` biçiminde tutar.
 */
object LyricsCache {
    private val cacheDir: File =
        File(System.getProperty("user.home"), ".beatbox/lyrics_cache").apply { mkdirs() }

    fun get(songKey: String): CachedLyrics? {
        val base = baseName(songKey)
        val lrcFile = File(cacheDir, "$base.lrc")
        if (!lrcFile.exists()) return null
        val sourceFile = File(cacheDir, "$base.src")
        val (source, fetchedAt) = readSource(sourceFile)
        return try {
            CachedLyrics(
                songKey = songKey,
                lrcContent = lrcFile.readText(),
                source = source,
                fetchedAt = fetchedAt,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun put(lyrics: CachedLyrics) {
        val base = baseName(lyrics.songKey)
        try {
            File(cacheDir, "$base.lrc").writeText(lyrics.lrcContent)
            File(cacheDir, "$base.src").writeText("${lyrics.source}|${lyrics.fetchedAt}")
        } catch (_: Exception) {
            // Önbellek yazımı başarısız olsa da oynatma devam eder.
        }
    }

    fun clear(songKey: String) {
        val base = baseName(songKey)
        File(cacheDir, "$base.lrc").delete()
        File(cacheDir, "$base.src").delete()
    }

    fun clearOlderThan(timestamp: Long) {
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".lrc")) {
                val src = File(cacheDir, file.name.removeSuffix(".lrc") + ".src")
                val (_, fetchedAt) = readSource(src)
                if (fetchedAt in 1 until timestamp) {
                    file.delete()
                    src.delete()
                }
            }
        }
    }

    private fun readSource(file: File): Pair<String, Long> {
        if (!file.exists()) return "" to 0L
        return try {
            val raw = file.readText()
            val split = raw.lastIndexOf('|')
            if (split <= 0) raw to 0L
            else raw.substring(0, split) to (raw.substring(split + 1).toLongOrNull() ?: 0L)
        } catch (_: Exception) {
            "" to 0L
        }
    }

    private fun baseName(songKey: String): String =
        songKey.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(200)
}
