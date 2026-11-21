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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Strings

/**
 * ViewModel for managing lyrics state and playback synchronization
 *
 * The provider is nullable because the database and the DAO are created lazily:
 * if that fails the screen still has to render an error instead of crashing.
 */
class LyricsViewModel(
    private val lyricsProvider: LyricsProvider?,
    private val strings: Strings
) : ViewModel() {
    
    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()
    
    private val _currentLineIndex = MutableStateFlow(0)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()
    private var loadJob: Job? = null
    
    /**
     * Loads lyrics for the specified audio file
     * 
     * @param audio The audio file to load lyrics for
     */
    fun loadLyrics(audio: AudioFile) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val provider = lyricsProvider
            if (provider == null) {
                _lyricsState.value = LyricsState.Error(strings.get("lyrics_service_unavailable"))
                return@launch
            }
            _lyricsState.value = LyricsState.Loading
            provider.getLyrics(audio).fold(
                onSuccess = { lyrics ->
                    // Add intro line if first line starts after 1 second (to show something during intro)
                    val modifiedLyrics = if (lyrics.lines.isNotEmpty() && lyrics.lines.first().timestampMs > 1000) {
                        val introLine = SyncedLyricsLine(0, "♪ ♪ ♪")
                        lyrics.copy(lines = listOf(introLine) + lyrics.lines)
                    } else {
                        lyrics
                    }
                    _lyricsState.value = LyricsState.Success(modifiedLyrics)
                },
                onFailure = { error ->
                    _lyricsState.value = LyricsState.Error(error.message ?: strings.get("unknown_error"))
                }
            )
        }
    }
    
    /**
     * Allows searching for lyrics manually with custom track/artist search query
     */
    fun searchLyricsManually(trackName: String, artistName: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _lyricsState.value = LyricsState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    MultiProviderLyricsClient.fetchLyricsFromAllProviders(
                        trackName = trackName,
                        artistName = artistName,
                        duration = 0,
                        skipCache = true
                    )
                }
                if (result != null && !result.first.isNullOrBlank()) {
                    val lyricsText = result.first!!
                    val source = result.second
                    val parseResult = LRCParser.parse(lyricsText)
                    val parsed = parseResult.getOrNull()
                    val lyrics = if (parsed != null) {
                        parsed.copy(metadata = parsed.metadata.copy(source = source))
                    } else {
                        val rawLines = lyricsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                        val lines = if (rawLines.isNotEmpty()) {
                            rawLines.mapIndexed { index, lineText -> SyncedLyricsLine(index * 3000L, lineText) }
                        } else {
                            listOf(SyncedLyricsLine(0, lyricsText))
                        }
                        SyncedLyrics(
                            lines = lines,
                            metadata = LyricsMetadata(title = trackName, artist = artistName, source = source)
                        )
                    }
                    _lyricsState.value = LyricsState.Success(lyrics)
                } else {
                    _lyricsState.value = LyricsState.Error(strings.get("lyrics_not_found"))
                }
            } catch (e: Exception) {
                _lyricsState.value = LyricsState.Error(e.message ?: strings.get("lyrics_search_error"))
            }
        }
    }

    /**
     * Updates the current playback position and finds the corresponding lyrics line
     * 
     * @param positionMs Current playback position in milliseconds
     */
    fun updatePlaybackPosition(positionMs: Long) {
        val state = _lyricsState.value
        if (state is LyricsState.Success) {
            val newIndex = findCurrentLineIndex(state.lyrics.lines, positionMs)
            if (newIndex != _currentLineIndex.value) {
                _currentLineIndex.value = newIndex
            }
        }
    }
    
    /**
     * Retries loading lyrics after an error
     * 
     * @param audio The audio file to retry loading lyrics for
     */
    fun retry(audio: AudioFile) {
        loadLyrics(audio)
    }

    /**
     * Önbelleği atlayarak sözleri yeniden getirir.
     *
     * Farklı bir sağlayıcı seçildikten veya kaynak değiştirildikten sonra
     * kullanıcının "önbelleği yok say" eylemidir.
     */
    fun refresh(audio: AudioFile) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            lyricsProvider?.clearCache(audio.id)
            _lyricsState.value = LyricsState.Loading
            val provider = lyricsProvider
            if (provider == null) {
                _lyricsState.value = LyricsState.Error(strings.get("lyrics_service_unavailable"))
                return@launch
            }
            provider.getLyrics(audio).fold(
                onSuccess = { lyrics ->
                    val modifiedLyrics = if (lyrics.lines.isNotEmpty() && lyrics.lines.first().timestampMs > 1000) {
                        val introLine = SyncedLyricsLine(0, "♪ ♪ ♪")
                        lyrics.copy(lines = listOf(introLine) + lyrics.lines)
                    } else {
                        lyrics
                    }
                    _lyricsState.value = LyricsState.Success(modifiedLyrics)
                },
                onFailure = { error ->
                    _lyricsState.value = LyricsState.Error(error.message ?: strings.get("unknown_error"))
                }
            )
        }
    }
    
    /**
     * Finds the index of the current lyrics line using binary search
     * Returns the highest index where the line's timestamp is <= playback position
     * 
     * @param lines List of synced lyrics lines
     * @param positionMs Current playback position in milliseconds
     * @return Index of the current line
     */
    private fun findCurrentLineIndex(lines: List<SyncedLyricsLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return 0
        
        var left = 0
        var right = lines.size - 1
        var result = 0
        
        while (left <= right) {
            val mid = (left + right) / 2
            if (lines[mid].timestampMs <= positionMs) {
                result = mid
                left = mid + 1
            } else {
                right = mid - 1
            }
        }
        
        return result
    }
}

/**
 * Sealed class representing the state of lyrics loading
 */
sealed class LyricsState {
    object Idle : LyricsState()
    object Loading : LyricsState()
    data class Success(val lyrics: SyncedLyrics) : LyricsState()
    data class Error(val message: String) : LyricsState()
}
