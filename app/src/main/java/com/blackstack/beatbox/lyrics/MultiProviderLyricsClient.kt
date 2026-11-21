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

import com.blackstack.beatbox.BuildConfig
import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.Url
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/*
 * Every JSON field below is annotated with @SerializedName on purpose.
 * Gson resolves fields by their *name* via reflection, and R8 renames fields
 * unless they are kept. Without the annotations the release build silently
 * deserializes every response to nulls (verified in the built APK: the field
 * names were absent from classes.dex). Keep the annotation and the value in
 * sync with the upstream JSON key.
 */

/**
 * ============================================
 * LRCLIB API (Primary - Free, No API Key)
 * ============================================
 */
data class LRCLIBResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("albumName") val albumName: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
    @SerializedName("plainLyrics") val plainLyrics: String? = null
)

interface LRCLIBApi {
    @GET("get")
    suspend fun getExactLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): LRCLIBResponse

    @GET("search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LRCLIBResponse>

    @GET("search")
    suspend fun searchLyricsByFields(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): List<LRCLIBResponse>
}

/**
 * ============================================
 * Lyrics.ovh API (Backup - Free, No API Key)
 * ============================================
 */
data class LyricsOvhResponse(
    @SerializedName("lyrics") val lyrics: String?
)

interface LyricsOvhApi {
    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): LyricsOvhResponse
}

/**
 * ============================================
 * Netease Cloud Music API (Chinese - Free)
 * ============================================
 */
data class NeteaseSearchResponse(
    @SerializedName("result") val result: NeteaseResult?
)

data class NeteaseResult(
    @SerializedName("songs") val songs: List<NeteaseSong>?
)

data class NeteaseSong(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("artists") val artists: List<NeteaseArtist>?
)

data class NeteaseArtist(
    @SerializedName("name") val name: String?
)

data class NeteaseLyricsResponse(
    @SerializedName("lrc") val lrc: NeteaseLrc?
)

data class NeteaseLrc(
    @SerializedName("lyric") val lyric: String?
)

interface NeteaseApi {
    @GET("search")
    suspend fun search(
        @Query("keywords") keywords: String,
        @Query("type") type: Int = 1
    ): NeteaseSearchResponse
    
    @GET("lyric")
    suspend fun getLyrics(
        @Query("id") id: Long
    ): NeteaseLyricsResponse
}

/**
 * ============================================
 * Genius API (Free - No API Key for search)
 * ============================================
 */
data class GeniusSearchResponse(
    @SerializedName("response") val response: GeniusResponseData?
)

data class GeniusResponseData(
    @SerializedName("hits") val hits: List<GeniusHit>?
)

data class GeniusHit(
    @SerializedName("result") val result: GeniusResult?
)

data class GeniusResult(
    @SerializedName("url") val url: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("primary_artist") val primary_artist: GeniusArtist?
)

data class GeniusArtist(
    @SerializedName("name") val name: String?
)

interface GeniusApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String
    ): GeniusSearchResponse
}

/**
 * ============================================
 * Multi-Provider Lyrics Client
 * ============================================
 * Attempts to fetch lyrics from multiple sources with fallback strategy
 */
object MultiProviderLyricsClient {
    private const val CONNECT_TIMEOUT_SECONDS = 8L
    private const val READ_TIMEOUT_SECONDS = 12L
    private const val CALL_TIMEOUT_SECONDS = 15L

    /**
     * Shared OkHttp client for all providers
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "BeatBox/3.0 (Android Music Player)")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .apply {
                // BASIC level logs istek satırlarını yazar; yalnızca debug
                // derlemelerinde kalsın.
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            .build()
    }

    /**
     * LRCLIB API - Primary provider (synced lyrics)
     */
    val lrclibApi: LRCLIBApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LRCLIBApi::class.java)
    }

    /**
     * Lyrics.ovh API - Secondary provider (plain lyrics)
     */
    val lyricsOvhApi: LyricsOvhApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.lyrics.ovh/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LyricsOvhApi::class.java)
    }

    /**
     * Netease Cloud Music API - Fourth provider (synced lyrics, great for Chinese/Asian music)
     */
    val neteaseApi: NeteaseApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://music.163.com/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NeteaseApi::class.java)
    }
    
    /**
     * Genius API - Fourth provider (web scraping for lyrics, good coverage)
     */
    val geniusApi: GeniusApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.genius.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeniusApi::class.java)
    }

    /**
     * KuGou Lyrics API - line-synced LRC, geniş Asya/Türkçe katalog
     */
    val kuGouApi: KuGouApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lyrics.kugou.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KuGouApi::class.java)
    }

    /**
     * lrc.red API - TTML/kelime zamanlı şarkı sözleri
     */
    val lrcRedApi: LrcRedApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrc.red/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrcRedApi::class.java)
    }

    /**
     * Unison API - TTML veya LRC, kelime zamanlı olabilir
     */
    val unisonApi: UnisonApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://unison.boidu.dev/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UnisonApi::class.java)
    }

    // Cache failed searches to avoid spamming APIs
    private val failedSearchCache = ConcurrentHashMap<String, Long>()
    private val failureCacheTimeMs = 60000L // 1 minute

    /** `null` tüm sağlayıcıların açık olduğu anlamına gelir. */
    private fun Set<String>?.allowsProvider(name: String): Boolean =
        this == null || name in this

    
    /**
     * Try all providers in order until one succeeds
     * @return Pair of (lyrics text, source name)
     */
    suspend fun fetchLyricsFromAllProviders(
        trackName: String,
        artistName: String,
        duration: Long,
        skipCache: Boolean = false,
        enabledProviders: Set<String>? = null
    ): Pair<String?, String>? {
        val cacheKey = "$trackName-$artistName".lowercase().trim()
        if (!skipCache) {
            val lastFailure = failedSearchCache[cacheKey]
            if (lastFailure != null && System.currentTimeMillis() - lastFailure < failureCacheTimeMs) {
                Log.d("MultiProvider", "Skipping search - failed recently (${System.currentTimeMillis() - lastFailure}ms ago)")
                return null
            }
        }
        
        android.util.Log.d("MultiProvider", "=== Trying all providers for: $trackName - $artistName ===")
        
        // Provider 1: LRCLIB (Best - has synced lyrics)
        if (enabledProviders.allowsProvider("LRCLIB")) try {
            Log.d("MultiProvider", "Trying LRCLIB...")
            val durationSec = (duration / 1000).toInt().takeIf { it > 0 }

            // 2a. Exact match yalnızca süre biliniyorsa kullanılır; süre
            // olmadan yapılan istek yanlış sürümü getirebilir.
            if (durationSec != null && artistName.isNotBlank()) {
                val exactResponse = try {
                    lrclibApi.getExactLyrics(
                        trackName = trackName,
                        artistName = artistName,
                        duration = durationSec
                    )
                } catch (e: Exception) {
                    LyricsProviderSupport.rethrowIfCancelled(e)
                    null
                }

                exactResponse?.let { LyricsProviderSupport.pickLrcLibLyrics(it) }?.let {
                    Log.d("MultiProvider", "✓ Found lyrics from LRCLIB (exact match)")
                    failedSearchCache.remove(cacheKey)
                    return Pair(it, "LRCLIB")
                }
            }

            // 2b. Alan araması + genel sorgu. Sonuçlar normalize başlık,
            // sanatçı tokenları ve ±10 sn süre toleransıyla filtrelenir.
            val candidates = mutableListOf<LRCLIBResponse>()
            if (artistName.isNotBlank()) {
                Log.d("MultiProvider", "Trying LRCLIB field search...")
                candidates += try {
                    lrclibApi.searchLyricsByFields(trackName = trackName, artistName = artistName)
                } catch (e: Exception) {
                    LyricsProviderSupport.rethrowIfCancelled(e)
                    emptyList()
                }
            }

            val queryStr = if (artistName.isNotBlank()) "$trackName $artistName".trim() else trackName.trim()
            Log.d("MultiProvider", "Trying LRCLIB query search: '$queryStr'")
            candidates += try {
                lrclibApi.searchLyrics(query = queryStr)
            } catch (e: Exception) {
                LyricsProviderSupport.rethrowIfCancelled(e)
                emptyList()
            }

            val best = LyricsProviderSupport.selectBestLrcLibCandidate(
                results = candidates.distinctBy { it.id },
                trackName = trackName,
                artistName = artistName,
                durationMs = duration
            )
            best?.let { LyricsProviderSupport.pickLrcLibLyrics(it) }?.let {
                Log.d("MultiProvider", "✓ Found lyrics from LRCLIB (search)")
                failedSearchCache.remove(cacheKey)
                return Pair(it, "LRCLIB")
            }
            Log.d("MultiProvider", "✗ LRCLIB returned no lyrics")
        } catch (e: Exception) {
            LyricsProviderSupport.rethrowIfCancelled(e)
            Log.e("MultiProvider", "Error in LRCLIB search", e)
        }

        // Provider 2: lrc.red (TTML - kelime zamanlı)
        if (enabledProviders.allowsProvider("lrc.red")) try {
            Log.d("MultiProvider", "Trying lrc.red...")
            val lrcRed = LyricsProviderSupport.withProviderTimeout {
                LyricsProviderSupport.fetchLrcRedLyrics(lrcRedApi, trackName, artistName, duration)
            }
            if (lrcRed != null) {
                Log.d("MultiProvider", "✓ Found lyrics from lrc.red")
                failedSearchCache.remove(cacheKey)
                return Pair(lrcRed, "lrc.red")
            }
            Log.d("MultiProvider", "✗ lrc.red returned no lyrics")
        } catch (e: Exception) {
            LyricsProviderSupport.rethrowIfCancelled(e)
            Log.d("MultiProvider", "lrc.red failed: ${e.message}")
        }

        // Provider 3: Unison (TTML/LRC - kelime zamanlı olabilir)
        if (enabledProviders.allowsProvider("Unison")) try {
            Log.d("MultiProvider", "Trying Unison...")
            val unison = LyricsProviderSupport.withProviderTimeout {
                LyricsProviderSupport.fetchUnisonLyrics(unisonApi, trackName, artistName, duration)
            }
            if (unison != null) {
                Log.d("MultiProvider", "✓ Found lyrics from Unison")
                failedSearchCache.remove(cacheKey)
                return Pair(unison, "Unison")
            }
            Log.d("MultiProvider", "✗ Unison returned no lyrics")
        } catch (e: Exception) {
            LyricsProviderSupport.rethrowIfCancelled(e)
            Log.d("MultiProvider", "Unison failed: ${e.message}")
        }

        // Provider 4: KuGou (line-synced LRC, geniş Asya kataloğu)
        if (enabledProviders.allowsProvider("KuGou")) try {
            Log.d("MultiProvider", "Trying KuGou...")
            val kuGou = LyricsProviderSupport.withProviderTimeout {
                LyricsProviderSupport.fetchKuGouLyrics(kuGouApi, trackName, artistName, duration)
            }
            if (kuGou != null) {
                Log.d("MultiProvider", "✓ Found lyrics from KuGou")
                failedSearchCache.remove(cacheKey)
                return Pair(kuGou, "KuGou")
            }
            Log.d("MultiProvider", "✗ KuGou returned no lyrics")
        } catch (e: Exception) {
            LyricsProviderSupport.rethrowIfCancelled(e)
            Log.d("MultiProvider", "KuGou failed: ${e.message}")
        }

        // Provider 5: Lyrics.ovh
        if (enabledProviders.allowsProvider("Lyrics.ovh")) try {
            android.util.Log.d("MultiProvider", "Trying Lyrics.ovh...")
            val cleanTitleForOvh = cleanTitle(trackName)
            val cleanArtistForOvh = cleanArtist(artistName)
            val response = lyricsOvhApi.getLyrics(
                artist = cleanArtistForOvh,
                title = cleanTitleForOvh
            )
            
            response.lyrics?.takeIf { it.isNotBlank() }?.let {
                Log.d("MultiProvider", "✓ Found lyrics from Lyrics.ovh")
                failedSearchCache.remove(cacheKey)
                return Pair(it, "Lyrics.ovh")
            }
            android.util.Log.d("MultiProvider", "✗ Lyrics.ovh returned no lyrics")
        } catch (e: Exception) {
            LyricsProviderSupport.rethrowIfCancelled(e)
            android.util.Log.e("MultiProvider", "✗ Lyrics.ovh failed: ${e.message}")
        }
        
        // Provider 6: Netease Cloud Music (Great for Asian music)
        if (enabledProviders.allowsProvider("Netease Cloud Music")) try {
            android.util.Log.d("MultiProvider", "Trying Netease Cloud Music...")
            val searchResponse = neteaseApi.search("$trackName $artistName")
            
            val songId = searchResponse.result?.songs?.firstOrNull()?.id
            if (songId != null) {
                val lyricsResponse = neteaseApi.getLyrics(songId)
                lyricsResponse.lrc?.lyric?.takeIf { it.isNotBlank() }?.let {
                    Log.d("MultiProvider", "✓ Found synced lyrics from Netease")
                    failedSearchCache.remove(cacheKey)
                    return Pair(it, "Netease Cloud Music")
                }
            }
            android.util.Log.d("MultiProvider", "✗ Netease returned no lyrics")
        } catch (e: Exception) {
            LyricsProviderSupport.rethrowIfCancelled(e)
            android.util.Log.e("MultiProvider", "✗ Netease failed: ${e.message}")
        }
        
        // All providers failed - cache failure only if not skipping cache
        if (!skipCache) {
            failedSearchCache[cacheKey] = System.currentTimeMillis()
        }
        Log.e("MultiProvider", "✗ All providers failed for: $trackName - $artistName")
        return null
    }
    
    /**
     * Try fetching with different name variations
     */
    suspend fun fetchWithVariations(
        originalTrack: String,
        originalArtist: String,
        duration: Long,
        enabledProviders: Set<String>? = null
    ): Pair<String?, String>? {
        // Try 1: Original names
        fetchLyricsFromAllProviders(
            originalTrack, originalArtist, duration,
            skipCache = true, enabledProviders = enabledProviders
        )?.let {
            return it
        }
        
        // Try 2: Cleaned names
        val cleanedTrack = cleanTitle(originalTrack)
        val cleanedArtist = cleanArtist(originalArtist)
        if (cleanedTrack != originalTrack || cleanedArtist != originalArtist) {
            fetchLyricsFromAllProviders(
                cleanedTrack, cleanedArtist, duration,
                skipCache = true, enabledProviders = enabledProviders
            )?.let {
                return it
            }
        }
        
        // Try 3: Smart parsed
        val (smartTrack, smartArtist) = smartParseTitleAndArtist(originalTrack, originalArtist)
        if (smartTrack != cleanedTrack || smartArtist != cleanedArtist) {
            fetchLyricsFromAllProviders(
                smartTrack, smartArtist, duration,
                skipCache = true, enabledProviders = enabledProviders
            )?.let {
                return it
            }
        }
        
        // Try 4: Without duration
        fetchLyricsFromAllProviders(
            smartTrack, smartArtist, 0,
            skipCache = true, enabledProviders = enabledProviders
        )?.let {
            return it
        }
        
        return null
    }

    /**
     * Clean title - remove video tags, file extensions, and track numbers
     */
    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        
        // Remove file extensions (.mp3, .flac, .wav, .m4a, .aac, .ogg)
        cleaned = cleaned.replace(Regex("""\.(mp3|flac|wav|m4a|aac|ogg|wma|opus)$""", RegexOption.IGNORE_CASE), "")

        // Remove leading track numbers (e.g. "01. ", "01 - ", "1 - ")
        cleaned = cleaned.replace(Regex("""^\d+[\s.-]+"""), "")

        val patternsToRemove = listOf(
            Regex("""\s*[\(\[](Official\s+)?(Video|Audio|Music\s+Video|Lyric\s+Video|MV|Remix|Cover)[\)\]]\s*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*[\(\[](HD|HQ|4K|1080p|720p)[\)\]]\s*$""", RegexOption.IGNORE_CASE)
        )
        
        patternsToRemove.forEach { pattern ->
            cleaned = cleaned.replace(pattern, "")
        }
        
        cleaned = cleaned.replace(Regex("""\s*[-|]\s*$"""), "")
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        
        return cleaned
    }
    
    /**
     * Clean artist name
     */
    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        
        // Clear unknown artist placeholders
        val unknownPlaceholders = listOf("<unknown>", "unknown", "<bilinmeyen sanatçı>", "bilinmeyen sanatçı", "unknown artist", "<unknown artist>")
        if (unknownPlaceholders.any { cleaned.lowercase() == it }) {
            return ""
        }

        val suffixPatterns = listOf(
            Regex("""\s*-\s*(VEVO|Topic)\s*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*[\(\[](VEVO|Topic)[\)\]]\s*$""", RegexOption.IGNORE_CASE)
        )
        
        suffixPatterns.forEach { pattern ->
            cleaned = cleaned.replace(pattern, "")
        }
        
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        
        return cleaned
    }
    
    /**
     * Smart parse title and artist from metadata
     */
    private fun smartParseTitleAndArtist(title: String, artist: String): Pair<String, String> {
        val cleanedArt = cleanArtist(artist)
        val cleanedTit = cleanTitle(title)

        val isUnknownArtist = cleanedArt.isEmpty() || 
                           cleanedArt.contains("VEVO", ignoreCase = true) || 
                           cleanedArt.contains("Topic", ignoreCase = true) ||
                           cleanedArt.contains("Official", ignoreCase = true)
        
        if ((isUnknownArtist || cleanedArt.isEmpty()) && cleanedTit.contains(" - ")) {
            val parts = cleanedTit.split(" - ").map { it.trim() }
            if (parts.size >= 2) {
                return Pair(cleanTitle(parts[1]), cleanArtist(parts[0]))
            }
        }
        
        return Pair(cleanedTit, cleanedArt)
    }
}
