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
package com.blackstack.beatbox.lyrics
import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.text.Normalizer
import java.util.Base64
import java.util.Locale
import kotlin.math.abs

/*
 * Metrolist'in KuGou, lrc.red ve Unison sağlayıcılarından uyarlanan API
 * tanımları. Tüm JSON alanları @SerializedName ile işaretlidir; R8 alan
 * adlarını değiştirdiğinde yanıtların null'a düşmesini bu önler.
 */

data class KuGouSearchResponse(
    @SerializedName("candidates") val candidates: List<KuGouCandidate>? = null,
    @SerializedName("status") val status: Int? = null
)

data class KuGouCandidate(
    @SerializedName("id") val id: String? = null,
    @SerializedName("accesskey") val accesskey: String? = null,
    @SerializedName("song") val song: String? = null,
    @SerializedName("singer") val singer: String? = null,
    @SerializedName("duration") val duration: Long? = null
)

data class KuGouDownloadResponse(
    @SerializedName("content") val content: String? = null,
    @SerializedName("fmt") val fmt: String? = null,
    @SerializedName("status") val status: Int? = null
)

interface KuGouApi {
    @GET("search")
    suspend fun search(
        @Query("ver") ver: Int = 1,
        @Query("man") man: String = "yes",
        @Query("client") client: String = "pc",
        @Query("keyword") keyword: String,
        @Query("duration") durationMs: Long? = null
    ): KuGouSearchResponse

    @GET("download")
    suspend fun download(
        @Query("fmt") fmt: String = "lrc",
        @Query("charset") charset: String = "utf8",
        @Query("client") client: String = "pc",
        @Query("ver") ver: Int = 1,
        @Query("id") id: String,
        @Query("accesskey") accessKey: String
    ): KuGouDownloadResponse
}

data class LrcRedSearchResponse(
    @SerializedName("results") val results: List<LrcRedResult>? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("total") val total: Int? = null
)

data class LrcRedResult(
    @SerializedName("id") val id: String? = null,
    @SerializedName("isrc") val isrc: String? = null,
    @SerializedName("track_name") val trackName: String? = null,
    @SerializedName("artist_name") val artistName: String? = null,
    @SerializedName("album_name") val albumName: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("lyricsUrl") val lyricsUrl: String? = null,
    @SerializedName("timing_type") val timingType: String? = null
)

interface LrcRedApi {
    @GET("api/v1")
    suspend fun search(
        @Query("q") query: String
    ): LrcRedSearchResponse

    @GET
    suspend fun getDocument(
        @Url url: String
    ): okhttp3.ResponseBody
}

data class UnisonResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: UnisonData? = null
)

data class UnisonData(
    @SerializedName("lyrics") val lyrics: String? = null,
    @SerializedName("format") val format: String? = null
)

interface UnisonApi {
    @GET("lyrics")
    suspend fun getLyrics(
        @Query("song") song: String,
        @Query("artist") artist: String? = null,
        @Query("album") album: String? = null,
        @Query("duration") durationSec: Long? = null
    ): UnisonResponse
}

/**
 * Çoklu sağlayıcı istemcisinin eşleştirme ve getirme yardımcıları.
 *
 * Metrolist'in `LrcLibLyricsProvider`, `LrcredLyricsProvider`,
 * `KuGouLyricsProvider` ve `UnisonProvider` uygulamalarından uyarlanmıştır.
 * Ayrı dosyada tutulması, ana istemci dosyasının okunabilir kalmasını sağlar.
 */
internal object LyricsProviderSupport {

    private const val PROVIDER_TIMEOUT_MS = 10_000L
    private const val LRCLIB_DURATION_TOLERANCE_MS = 10_000.0
    private const val LRCRED_DURATION_TOLERANCE_SEC = 5.0
    private const val KUGOU_DURATION_TOLERANCE_MS = 8_000L

    /** İptal sinyalini yutmayı önler; diğer hatalar sağlayıcıya özgüdür. */
    fun rethrowIfCancelled(e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
    }

    /**
     * Her sağlayıcıya kendi bütçesini verir. Yavaş bir servis, sıradaki
     * sağlayıcıların hiç denenmemesine yol açmaz.
     */
    suspend fun <T> withProviderTimeout(block: suspend () -> T?): T? =
        try {
            withTimeoutOrNull(PROVIDER_TIMEOUT_MS) { block() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d("MultiProvider", "Provider failed: ${e.message}")
            null
        }

    /** LRCLIB yanıtından önce senkron, sonra düz şarkı sözünü seçer. */
    fun pickLrcLibLyrics(response: LRCLIBResponse): String? =
        response.syncedLyrics?.takeIf { it.isNotBlank() }
            ?: response.plainLyrics?.takeIf { it.isNotBlank() }

    /**
     * LRCLIB arama sonuçlarını Metrolist'teki normalizasyon mantığıyla
     * filtreler: normalize başlık, feat temizleme, sanatçı tokenları ve
     * ±10 saniye süre toleransı.
     */
    fun selectBestLrcLibCandidate(
        results: List<LRCLIBResponse>,
        trackName: String,
        artistName: String,
        durationMs: Long
    ): LRCLIBResponse? {
        if (results.isEmpty()) return null

        val normalizedTitle = normalizeMatchText(trackName)
        val coreTitle = normalizeMatchText(stripFeatAndDecorators(trackName))
        val normalizedArtist = normalizeMatchText(artistName)
        val artistTokens = splitArtistTokens(artistName)

        return results.asSequence()
            .filter { !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank() }
            .filter { candidate ->
                val candidateTitle = candidate.trackName.orEmpty()
                val normalizedCandidate = normalizeMatchText(candidateTitle)
                val candidateCore = normalizeMatchText(stripFeatAndDecorators(candidateTitle))
                val titleMatches = normalizedCandidate.isNotEmpty() &&
                    (normalizedCandidate == normalizedTitle ||
                        (coreTitle.isNotEmpty() && candidateCore == coreTitle))
                val artistMatches = normalizedArtist.isEmpty() ||
                    artistMatchesCandidate(normalizedArtist, artistTokens, candidate.artistName.orEmpty())
                val durationMatches = durationMs <= 0L || candidate.duration == null ||
                    abs(candidate.duration * 1000.0 - durationMs) <= LRCLIB_DURATION_TOLERANCE_MS
                titleMatches && artistMatches && durationMatches
            }
            .sortedWith(
                compareByDescending<LRCLIBResponse> {
                    normalizeMatchText(it.trackName.orEmpty()) == normalizedTitle
                }
                    .thenByDescending {
                        normalizedArtist.isNotEmpty() &&
                            normalizeMatchText(it.artistName.orEmpty()) == normalizedArtist
                    }
                    .thenByDescending { !it.syncedLyrics.isNullOrBlank() }
                    .thenBy { candidate ->
                        if (durationMs > 0L && candidate.duration != null) {
                            abs(candidate.duration * 1000.0 - durationMs)
                        } else {
                            0.0
                        }
                    }
            )
            .firstOrNull()
    }

    private fun artistMatchesCandidate(
        normalizedArtist: String,
        artistTokens: List<String>,
        candidateArtist: String
    ): Boolean {
        if (normalizedArtist.isEmpty()) return true
        val normalizedCandidate = normalizeMatchText(candidateArtist)
        if (normalizedCandidate == normalizedArtist) return true

        val candidateTokens = splitArtistTokens(candidateArtist)
        if (artistTokens.isEmpty() || candidateTokens.isEmpty()) return false

        return artistTokens.any { target ->
            candidateTokens.any { candidate ->
                target == candidate ||
                    (target.length >= 4 && candidate.length >= 4 &&
                        (target.contains(candidate) || candidate.contains(target)))
            }
        }
    }

    private fun splitArtistTokens(raw: String): List<String> =
        raw.split(ARTIST_DELIMITER)
            .map { normalizeMatchText(it) }
            .filter { it.isNotEmpty() && it !in GENERIC_ARTIST_TOKENS }

    /** Başlıktaki `(feat. ...)`, `[Remix]` gibi ekleri temizler. */
    fun stripFeatAndDecorators(raw: String): String =
        raw.replace(BRACKETED_FEAT, " ")
            .replace(TRAILING_FEAT, " ")
            .trim()
            .ifEmpty { raw }

    /**
     * Karşılaştırma için metni sadeleştirir: aksanları kaldırır,
     * küçük harfe çevirir ve harf/rakam dışı karakterleri boşluğa çevirir.
     */
    fun normalizeMatchText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD)
        return decomposed
            .replace(ACCENT_MARKS, "")
            .replace('ı', 'i')
            .replace('İ', 'i')
            .lowercase(Locale.ROOT)
            .replace(NON_ALPHANUMERIC_OR_SPACE, " ")
            .replace(MULTIPLE_SPACES, " ")
            .trim()
    }

    /** lrc.red araması yapar ve en iyi eşleşmenin TTML/LRC içeriğini getirir. */
    suspend fun fetchLrcRedLyrics(
        api: LrcRedApi,
        trackName: String,
        artistName: String,
        durationMs: Long
    ): String? {
        val query = "$trackName $artistName".trim()
        if (query.isEmpty()) return null

        val response = api.search(query)
        val results = response.results.orEmpty().filter { !it.lyricsUrl.isNullOrBlank() }
        if (results.isEmpty()) return null

        val best = selectLrcRedResult(results, trackName, artistName, durationMs) ?: return null
        val url = best.lyricsUrl.orEmpty().let {
            if (it.startsWith("/")) "https://lrc.red$it" else it
        }
        val payload = fetchText(api, url) ?: return null
        return convertLyricsPayload(payload)
    }

    internal fun selectLrcRedResult(
        results: List<LrcRedResult>,
        trackName: String,
        artistName: String,
        durationMs: Long
    ): LrcRedResult? {
        val normalizedTitle = normalizeMatchText(trackName)
        val coreTitle = normalizeMatchText(stripFeatAndDecorators(trackName))
        val normalizedArtist = normalizeMatchText(artistName)
        val artistTokens = splitArtistTokens(artistName)
        val targetDurationSec = durationMs / 1000.0

        return results.asSequence()
            .filter { result ->
                targetDurationSec <= 0.0 || result.duration == null || result.duration <= 0.0 ||
                    abs(result.duration - targetDurationSec) <= LRCRED_DURATION_TOLERANCE_SEC
            }
            .mapNotNull { result ->
                val resultTitle = normalizeMatchText(result.trackName)
                val resultCore = normalizeMatchText(stripFeatAndDecorators(result.trackName.orEmpty()))
                val titleMatches = (normalizedTitle.isNotEmpty() && resultTitle == normalizedTitle) ||
                    (coreTitle.isNotEmpty() && resultCore == coreTitle)
                if (!titleMatches) return@mapNotNull null

                var score = if (resultTitle == normalizedTitle) 100.0 else 60.0
                if (normalizedArtist.isNotEmpty() &&
                    artistMatchesCandidate(normalizedArtist, artistTokens, result.artistName.orEmpty())
                ) {
                    score += 50.0
                }
                if (result.duration != null && result.duration > 0.0 && targetDurationSec > 0.0) {
                    score += (LRCRED_DURATION_TOLERANCE_SEC -
                        abs(result.duration - targetDurationSec)).coerceAtLeast(0.0)
                }
                result to score
            }
            .maxByOrNull { it.second }
            ?.first
    }

    /** Unison araması; TTML yanıtını gelişmiş LRC'ye çevirir. */
    suspend fun fetchUnisonLyrics(
        api: UnisonApi,
        trackName: String,
        artistName: String,
        durationMs: Long
    ): String? {
        if (trackName.isBlank()) return null

        val response = api.getLyrics(
            song = trackName,
            artist = artistName.takeIf { it.isNotBlank() },
            durationSec = (durationMs / 1000L).takeIf { it > 0L }
        )
        if (response.success != true) return null

        val lyrics = response.data?.lyrics?.takeIf { it.isNotBlank() } ?: return null
        return when (response.data?.format?.lowercase(Locale.ROOT)) {
            "ttml" -> convertTtmlToLrc(lyrics)
            else -> lyrics
        }
    }

    /** KuGou araması + Base64 kodlu LRC indirmesi. */
    suspend fun fetchKuGouLyrics(
        api: KuGouApi,
        trackName: String,
        artistName: String,
        durationMs: Long
    ): String? {
        val keyword = buildString {
            append(kugouNormalizeTitle(trackName))
            if (artistName.isNotBlank()) {
                append(" - ")
                append(kugouNormalizeArtist(artistName))
            }
        }.trim()
        if (keyword.isEmpty()) return null

        val search = api.search(
            keyword = keyword,
            durationMs = durationMs.takeIf { it > 0L }
        )
        val candidates = search.candidates.orEmpty()
            .filter { !it.id.isNullOrBlank() && !it.accesskey.isNullOrBlank() }
        if (candidates.isEmpty()) return null

        val best = if (durationMs > 0L) {
            candidates
                .minByOrNull { abs((it.duration ?: 0L) - durationMs) }
                ?.takeIf { candidate ->
                    val candidateDuration = candidate.duration ?: 0L
                    candidateDuration <= 0L ||
                        abs(candidateDuration - durationMs) <= KUGOU_DURATION_TOLERANCE_MS
                }
        } else {
            candidates.firstOrNull()
        } ?: return null

        val download = api.download(
            id = best.id.orEmpty(),
            accessKey = best.accesskey.orEmpty()
        )
        val decoded = decodeBase64(download.content.orEmpty()) ?: return null
        return decoded.takeIf { it.isNotBlank() }
    }

    private fun decodeBase64(value: String): String? {
        if (value.isBlank()) return null
        return try {
            Base64.getMimeDecoder().decode(value).toString(Charsets.UTF_8)
                .takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            null
        }
    }

    /** TTML ise gelişmiş LRC'ye çevirir; LRC/plain metni olduğu gibi bırakır. */
    private fun convertLyricsPayload(payload: String): String? {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("<")) convertTtmlToLrc(trimmed) else trimmed
    }

    private fun convertTtmlToLrc(ttml: String): String? {
        val nodes = TTMLParser.parseTTML(ttml)
        if (nodes.isEmpty()) return null
        return TTMLParser.toLRC(nodes).takeIf { it.isNotBlank() }
    }

    /** İkincil sağlayıcıların metin yanıtlarını Retrofit üzerinden indirir. */
    private suspend fun fetchText(api: LrcRedApi, url: String): String? =
        try {
            api.getDocument(url).use { it.string() }.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            Log.d("MultiProvider", "Text fetch failed for $url: ${e.message}")
            null
        }

    private fun kugouNormalizeTitle(title: String): String =
        title
            .replace(Regex("""\(.*\)"""), "")
            .replace(Regex("""（.*）"""), "")
            .replace(Regex("""「.*」"""), "")
            .replace(Regex("""『.*』"""), "")
            .replace(Regex("""<.*>"""), "")
            .replace(Regex("""《.*》"""), "")
            .replace(Regex("""〈.*〉"""), "")
            .replace(Regex("""＜.*＞"""), "")
            .trim()

    private fun kugouNormalizeArtist(artist: String): String =
        artist
            .replace(", ", "、")
            .replace(" & ", "、")
            .replace(".", "")
            .replace(Regex("""\(.*\)"""), "")
            .replace(Regex("""（.*）"""), "")
            .trim()

    private val ACCENT_MARKS = Regex("""\p{Mn}+""")
    private val NON_ALPHANUMERIC_OR_SPACE = Regex("""[^\p{L}\p{N}\s]""")
    private val MULTIPLE_SPACES = Regex("""\s+""")
    private val ARTIST_DELIMITER =
        Regex("""(?i)[,&/+]|\b(?:feat\.?|ft\.?|featuring|with|and|x)\b""")
    private val GENERIC_ARTIST_TOKENS = setOf(
        "artist", "band", "music", "official", "vevo", "topic", "the"
    )
    private val BRACKETED_FEAT =
        Regex("""[\(\[](?:feat\.?|ft\.?|featuring|with)\b[^\)\]]*[\)\]]""", RegexOption.IGNORE_CASE)
    private val TRAILING_FEAT =
        Regex("""\s+\b(?:feat\.?|ft\.?|featuring)\b.*$""", RegexOption.IGNORE_CASE)
}
