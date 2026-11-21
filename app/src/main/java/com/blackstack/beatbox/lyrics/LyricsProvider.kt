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
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.library.LocalFileReader
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.playback.hasUsableNetwork

/**
 * Provider for retrieving synchronized lyrics from multiple sources
 * 
 * Checks sources in priority order:
 * 1. Local LRC file (same directory as audio file)
 * 2. Cache (Room database)
 * 3. Multiple Online APIs (LRCLIB, Lyrics.ovh, and more)
 * 
 * All operations are executed on Dispatchers.IO for background processing.
 */
class LyricsProvider(
    private val context: Context,
    private val lyricsCache: LyricsCacheDao,
    private val strings: Strings
) {
    /**
     * Retrieves lyrics for a song, checking sources in priority order
     * 
     * @param audio The audio file to get lyrics for
     * @return Result containing SyncedLyrics on success, or error on failure
     */
    suspend fun getLyrics(audio: AudioFile): Result<SyncedLyrics> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("LyricsProvider", "=== Starting lyrics search for: ${audio.title} - ${audio.artist} ===")
            
            // Priority 1: Check for local LRC file
            android.util.Log.d("LyricsProvider", "Checking local LRC file...")
            checkLocalFile(audio)?.let { lyrics ->
                android.util.Log.d("LyricsProvider", "✓ Found lyrics in local file")
                return@withContext Result.success(lyrics)
            }
            android.util.Log.d("LyricsProvider", "✗ No local LRC file found")
            
            // Priority 2: Check cache
            android.util.Log.d("LyricsProvider", "Checking cache...")
            checkCache(audio.id)?.let { lyrics ->
                android.util.Log.d("LyricsProvider", "✓ Found lyrics in cache")
                return@withContext Result.success(lyrics)
            }
            android.util.Log.d("LyricsProvider", "✗ No cached lyrics found")

            if (!context.hasUsableNetwork()) {
                android.util.Log.w("LyricsProvider", "Lyrics API skipped because the device is offline")
                return@withContext Result.failure(
                    IllegalStateException(strings.get("lyrics_offline"))
                )
            }
            
            // Priority 3: Fetch from API
            android.util.Log.d("LyricsProvider", "Fetching from LRCLIB API...")
            fetchFromApi(audio)?.let { lyrics ->
                android.util.Log.d("LyricsProvider", "✓ Found lyrics from API")
                // Cache the fetched lyrics with the provider that supplied them.
                cacheLyrics(audio.id, lyrics, lyrics.metadata.source ?: "api")
                return@withContext Result.success(lyrics)
            }
            
            // All sources failed
            android.util.Log.e("LyricsProvider", "✗ No lyrics found from any source")
            Result.failure(Exception(strings.get("lyrics_not_found")))
        } catch (e: Exception) {
            android.util.Log.e("LyricsProvider", "Error in getLyrics", e)
            Result.failure(
                Exception(
                    strings.get("lyrics_load_error")
                        .replace("%s", e.message ?: strings.get("unknown_error"))
                )
            )
        }
    }
    
    /**
     * Checks for a local LRC file alongside the audio file
     * 
     * @param audio The audio file to check for
     * @return SyncedLyrics if local file exists and is valid, null otherwise
     */
    private suspend fun checkLocalFile(audio: AudioFile): SyncedLyrics? {
        return try {
            val lrcContent = LocalFileReader.readLrcFile(context, audio.uri) ?: return null
            val parseResult = LRCParser.parse(lrcContent)
            parseResult.getOrNull()?.let { lyrics ->
                lyrics.copy(
                    metadata = lyrics.metadata.copy(source = "local")
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks the cache for previously fetched lyrics
     * 
     * @param songId The unique identifier of the song
     * @return SyncedLyrics if cached lyrics exist and are valid, null otherwise
     */
    private suspend fun checkCache(songId: Long): SyncedLyrics? {
        return try {
            val cachedLyrics = lyricsCache.getLyrics(songId) ?: return null
            val parseResult = LRCParser.parse(cachedLyrics.lrcContent)
            parseResult.getOrNull()?.let { lyrics ->
                val source = cachedLyrics.source
                    .takeIf { it.isNotBlank() && it != "api" }
                    ?: "cache"
                lyrics.copy(
                    metadata = lyrics.metadata.copy(source = source)
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Fetches lyrics from multiple API providers with fallback
     * Tries LRCLIB, Lyrics.ovh, and other providers
     * 
     * @param audio The audio file to fetch lyrics for
     * @return SyncedLyrics if any API fetch succeeds, null otherwise
     */
    private suspend fun fetchFromApi(audio: AudioFile): SyncedLyrics? {
        return try {
            android.util.Log.d("LyricsProvider", "Fetching lyrics from multiple providers...")
            
            // Kullanıcının ayarlarda açık bıraktığı sağlayıcılar kullanılır.
            val settings = LyricsPreferences.load(context)
            val result = withTimeoutOrNull(30_000L) {
                MultiProviderLyricsClient.fetchWithVariations(
                    originalTrack = audio.title,
                    originalArtist = audio.artist,
                    duration = audio.duration,
                    enabledProviders = settings.enabledProviders
                )
            }

            if (result == null) {
                android.util.Log.w("LyricsProvider", "Lyrics search timed out or returned no result")
                return null
            }
            
            val (lyricsText, source) = result
            android.util.Log.d("LyricsProvider", "✓ Found lyrics from $source")
            
            if (!lyricsText.isNullOrBlank()) {
                // Try to parse as synced LRC format first
                val parseResult = LRCParser.parse(lyricsText)
                parseResult.getOrNull()?.let { parsed ->
                    android.util.Log.d("LyricsProvider", "✓ Parsed as synced lyrics from $source")
                    return parsed.copy(
                        metadata = parsed.metadata.copy(source = source)
                    )
                }
                
                // If not LRC format, split plain text into lines for clean display
                Log.d("LyricsProvider", "Using as plain text lyrics (unsynced)")
                val rawLines = lyricsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                val lines = if (rawLines.isNotEmpty()) {
                    rawLines.mapIndexed { index, lineText ->
                        SyncedLyricsLine(index * 3000L, lineText)
                    }
                } else {
                    listOf(SyncedLyricsLine(0, lyricsText))
                }

                val lyrics = SyncedLyrics(
                    lines = lines,
                    metadata = LyricsMetadata(
                        title = audio.title,
                        artist = audio.artist,
                        source = source
                    )
                )
                return lyrics
            }
            
            android.util.Log.d("LyricsProvider", "✗ No lyrics found from any provider")
            null
        } catch (e: Exception) {
            android.util.Log.e("LyricsProvider", "Error fetching from API", e)
            null
        }
    }
    
    /**
     * Önbellekteki sözleri siler; "önbelleği yok sayarak yeniden dene" için.
     */
    suspend fun clearCache(songId: Long) {
        withContext(Dispatchers.IO) {
            runCatching { lyricsCache.deleteLyrics(songId) }
        }
    }

    /**
     * Caches lyrics in the local database
     * 
     * @param songId The unique identifier of the song
     * @param lyrics The lyrics to cache
     * @param source The source of the lyrics ("local" or "api")
     */
    private suspend fun cacheLyrics(songId: Long, lyrics: SyncedLyrics, source: String) {
        try {
            val lrcContent = LRCParser.format(lyrics)
            val cachedLyrics = CachedLyrics(
                songId = songId,
                lrcContent = lrcContent,
                source = source,
                fetchedAt = System.currentTimeMillis()
            )
            lyricsCache.insertLyrics(cachedLyrics)
        } catch (e: Exception) {
            // Log error but don't fail the operation
            // Caching is not critical to the lyrics retrieval flow
        }
    }
}
