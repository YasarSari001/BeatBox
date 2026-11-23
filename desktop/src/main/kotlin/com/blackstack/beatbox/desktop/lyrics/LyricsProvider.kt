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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.time.Duration

/**
 * Söz isteği: oynatılan dosyanın kimliği ve çevrimiçi arama bilgileri.
 *
 * @property file Yerel `.lrc` dosyasını bulmak için kullanılan ses dosyası
 */
data class LyricsRequest(
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Double,
    val file: File,
)

/**
 * Şarkı sözü sağlayıcısı.
 *
 * Android sürümündeki `LyricsProvider` ile aynı öncelik sırasını izler:
 * 1. Ses dosyasının yanındaki yerel `.lrc` dosyası
 * 2. Disk önbelleği (`~/.beatbox/lyrics_cache`)
 * 3. LRCLIB (senkronize LRC)
 * 4. Lyrics.ovh (düz metin)
 *
 * Ağ erişimi JDK'nın yerleşik [HttpClient]'ı ile yapılır; ek bağımlılık yoktur.
 */
object LyricsProvider {

    private const val USER_AGENT = "BeatBox/3.9.1 (Windows desktop)"

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * Sözleri yükler. [refresh] `true` ise önbellek atlanır.
     * Ağ hatalarında bile yerel dosya ve önbellek sonuçları kullanılır.
     */
    suspend fun load(request: LyricsRequest, refresh: Boolean = false): LyricsState =
        withContext(Dispatchers.IO) {
            val songKey = request.songKey()

            // 1) Ses dosyasının yanındaki .lrc
            findLocalLrc(request.file)?.let { lrc ->
                val text = readTextSmart(lrc)
                if (text.isNotBlank()) {
                    fromText(text, "LRC")?.let { return@withContext LyricsState.Success(it) }
                }
            }

            // 2) Disk önbelleği
            if (!refresh) {
                LyricsCache.get(songKey)?.let { cached ->
                    fromText(cached.lrcContent, cached.source)?.let {
                        return@withContext LyricsState.Success(it)
                    }
                }
            }

            // 3) Çevrimiçi sağlayıcılar
            var attempts = 0
            var failures = 0
            attempts++
            try {
                fetchLrclib(request)?.let { result ->
                    LyricsCache.put(CachedLyrics(songKey, result.first, result.second, System.currentTimeMillis()))
                    fromText(result.first, result.second)?.let { return@withContext LyricsState.Success(it) }
                }
            } catch (_: Exception) {
                failures++
            }

            attempts++
            try {
                fetchLyricsOvh(request)?.let { text ->
                    LyricsCache.put(CachedLyrics(songKey, text, "Lyrics.ovh", System.currentTimeMillis()))
                    fromText(text, "Lyrics.ovh")?.let { return@withContext LyricsState.Success(it) }
                }
            } catch (_: Exception) {
                failures++
            }

            if (attempts > 0 && failures == attempts) LyricsState.Error("lyrics_network_error")
            else LyricsState.Error("lyrics_not_found")
        }

    /**
     * Ham metni senkronize ya da düz söze çevirir.
     */
    fun fromText(raw: String, source: String): SyncedLyrics? =
        LRCParser.parse(raw, source) ?: plainToLyrics(raw, source)

    /** Zaman damgası olmayan sözleri tek "senkronize olmayan" blok olarak sarar. */
    private fun plainToLyrics(raw: String, source: String): SyncedLyrics? {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        return SyncedLyrics(
            lines = lines.map { SyncedLyricsLine(timestampMs = 0L, text = it) },
            metadata = LyricsMetadata(source = source),
            synced = false,
        )
    }

    private fun LyricsRequest.songKey(): String =
        listOf(artist, title, album, durationSeconds.toInt().toString()).joinToString("|")

    private fun findLocalLrc(audio: File): File? {
        val dir = audio.parentFile ?: return null
        if (!dir.isDirectory) return null
        val base = audio.nameWithoutExtension.lowercase()
        val full = audio.name.lowercase()
        return dir.listFiles()?.firstOrNull { candidate ->
            candidate.isFile && candidate.extension.equals("lrc", ignoreCase = true) &&
                (candidate.nameWithoutExtension.lowercase() == base ||
                    candidate.nameWithoutExtension.lowercase() == full)
        }
    }

    private fun readTextSmart(file: File): String {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return ""
        if (bytes.isEmpty()) return ""
        val offset = if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) 3 else 0
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.size - offset)).toString()
        } catch (_: CharacterCodingException) {
            // Eski LRC dosyaları çoğunlukla Windows-1254 / ISO-8859-9 ile yazılmıştır.
            runCatching { String(bytes, offset, bytes.size - offset, Charset.forName("windows-1254")) }
                .getOrElse { String(bytes, offset, bytes.size - offset, Charsets.ISO_8859_1) }
        }
    }

    // ---------------------------------------------------------------- LRCLIB

    private fun fetchLrclib(request: LyricsRequest): Pair<String, String>? {
        val exact = buildString {
            append("https://lrclib.net/api/get")
            append("?artist_name=").append(encode(request.artist))
            append("&track_name=").append(encode(request.title))
            if (request.album.isNotBlank()) append("&album_name=").append(encode(request.album))
            append("&duration=").append(request.durationSeconds.toInt())
        }
        get(exact)?.let { body ->
            pickLyrics(body)?.let { return it to "LRCLIB" }
        }

        val search = "https://lrclib.net/api/search?q=" +
            encode(listOf(request.artist, request.title).filter { it.isNotBlank() }.joinToString(" "))
        val body = get(search) ?: return null
        pickLyrics(body)?.let { return it to "LRCLIB" }
        return null
    }

    /**
     * LRCLIB yanıtından (nesne ya da dizi) ilk senkronize sözü, yoksa ilk
     * düz sözü seçer.
     */
    private fun pickLyrics(json: String): String? {
        val synced = jsonStrings(json, "syncedLyrics").firstOrNull { it.isNotBlank() }
        if (synced != null) return synced
        return jsonStrings(json, "plainLyrics").firstOrNull { it.isNotBlank() }
    }

    // ------------------------------------------------------------ Lyrics.ovh

    private fun fetchLyricsOvh(request: LyricsRequest): String? {
        if (request.artist.isBlank() || request.title.isBlank()) return null
        val url = "https://api.lyrics.ovh/v1/${encode(request.artist)}/${encode(request.title)}"
        val body = get(url) ?: return null
        return jsonStrings(body, "lyrics").firstOrNull { it.isNotBlank() }
    }

    // ---------------------------------------------------------------- HTTP

    private fun get(url: String): String? {
        val sanitized = url.replace(" ", "%20")
        val request = HttpRequest.newBuilder(URI.create(sanitized))
            .timeout(Duration.ofSeconds(12))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        if (response.statusCode() !in 200..299) return null
        return response.body()
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value.trim(), Charsets.UTF_8).replace("+", "%20")

    // ------------------------------------------------------------ JSON

    /**
     * Basit ve bağımlılıksız JSON dize alanı okuyucu.
     *
     * Yalnızca sağlayıcı yanıtlarındaki düz alanları okumak için kullanılır;
     * iç içe nesneleri çözmez. Aynı alan dizi yanıtlarında birden çok kez
     * geçebilir; hepsi sırayla döner.
     *
     * Regex yerine elle taranır: uzun söz metinlerinde geri izlemeli
     * (backtracking) desenler yığın taşmasına yol açıyordu.
     */
    private fun jsonStrings(json: String, field: String): List<String> {
        val key = "\"$field\""
        val results = mutableListOf<String>()
        var searchFrom = 0

        while (searchFrom < json.length) {
            val keyIndex = json.indexOf(key, searchFrom)
            if (keyIndex < 0) break

            var index = keyIndex + key.length
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] != ':') {
                searchFrom = keyIndex + key.length
                continue
            }
            index++
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] != '"') {
                searchFrom = index
                continue
            }

            index++
            val builder = StringBuilder()
            var closed = false
            while (index < json.length) {
                val ch = json[index]
                when {
                    ch == '\\' -> {
                        if (index + 1 >= json.length) break
                        when (val escape = json[index + 1]) {
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'b' -> builder.append('\b')
                            'f' -> builder.append('\u000C')
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            'u' -> {
                                if (index + 5 >= json.length) break
                                val code = json.substring(index + 2, index + 6).toIntOrNull(16)
                                if (code == null) break
                                builder.append(code.toChar())
                                index += 4
                            }
                            else -> builder.append(escape)
                        }
                        index += 2
                    }
                    ch == '"' -> {
                        closed = true
                        index++
                        break
                    }
                    else -> {
                        builder.append(ch)
                        index++
                    }
                }
            }

            if (closed) results += builder.toString()
            searchFrom = index.coerceAtLeast(searchFrom + 1)
        }
        return results
    }
}
