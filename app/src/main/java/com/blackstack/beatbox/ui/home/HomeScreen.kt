package com.blackstack.beatbox.ui.home

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Playlist
import com.blackstack.beatbox.ui.components.VisualizerState
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.groupSongsByArtist
import com.blackstack.beatbox.ui.components.PullToRefresh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/**
 * Ana sayfa: selamlama, devam eden çalma sahnesi ve kişisel raflar.
 *
 * Ekran yalnızca sunum yapar. Çalma durumu, ilerleme ve gezinme eylemleri
 * [com.blackstack.beatbox.ui.components.BeatBoxTabContent] tarafından beslenir.
 */
@Composable
fun HomeScreen(
    allAudioList: List<AudioFile>,
    favoriteIds: Set<String>,
    playlists: List<Playlist>,
    currentAudioId: Long?,
    currentAudio: AudioFile?,
    isPlaying: Boolean,
    onPlaySong: (AudioFile) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayArtist: (List<AudioFile>) -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    progress: () -> Float,
    strings: Strings,
    ctx: Context,
    visualizerState: VisualizerState,
    /** Compact top bar görünürlüğü değişikliklerinde çağrılır. */
    onCompactBarVisibleChange: ((Boolean) -> Unit)? = null,
    /** Aşağı çekerek kitaplığı yenile. */
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false
) {
    val prefs = remember(ctx) {
        ctx.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
    }
    val listState = rememberLazyListState()

    // Compact top bar görünürlüğü: LazyListState alanları snapshot ile
    // gözlemlenebilir. Selamlama 120 px'den fazla yukarı kaydığında veya
    // tamamen görünümden çıktığında çubuk görünür. Polling yok.
    LaunchedEffect(listState, onCompactBarVisibleChange) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 120
        }
            .distinctUntilChanged()
            .collect { visible -> onCompactBarVisibleChange?.invoke(visible) }
    }

    // Kitaplık etiket geçişi sırasında 25'lik parçalarla yenilenir. Rafları her
    // parçada ana iş parçacığında yeniden sıralamak kaydırmayı kilitler; bu
    // yüzden hesaplama arka planda yapılır ve parçalar kısa bir gecikmeyle
    // birleştirilir. Önceki sonuç, yeni sonuç hazır olana kadar korunur.
    val sections by produceState(
        initialValue = HomeSections.EMPTY,
        allAudioList,
        currentAudioId,
        strings,
        prefs
    ) {
        value = withContext(Dispatchers.Default) {
            delay(200)
            val stats = prefs.all
            val top = allAudioList
                .sortedWith(
                    compareByDescending<AudioFile> {
                        (stats["stats_count_${it.id}"] as? Number)?.toInt() ?: 0
                    }.thenByDescending { it.dateAdded }
                )
                .take(10)
            val recentlyAdded = allAudioList
                .sortedByDescending { it.dateAdded }
                .take(6)
            val recentlyPlayed = run {
                val byId = allAudioList.associateBy { it.id }
                prefs.getString("stats_history", "")
                    .orEmpty()
                    .split(',')
                    .asSequence()
                    .mapNotNull { it.trim().toLongOrNull() }
                    .mapNotNull { byId[it] }
                    .distinctBy { it.id }
                    .take(8)
                    .toList()
            }
            val artists = groupSongsByArtist(allAudioList, strings.get("unknown_artist"))
                .entries
                .sortedByDescending { it.value.size }
                .take(12)
            HomeSections(top, recentlyAdded, recentlyPlayed, artists)
        }
    }
    val topSongs = sections.topSongs
    val recentlyAdded = sections.recentlyAdded
    val recentlyPlayed = sections.recentlyPlayed
    val artists = sections.artists

    val totalMinutes = remember(prefs, currentAudioId) {
        prefs.getLong("stats_total_minutes", 0L)
    }

    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "greeting") {
                HomeGreeting(
                    strings = strings,
                    songCount = allAudioList.size,
                    playlistCount = playlists.size,
                    onShuffleAll = onShuffleAll,
                    listState = listState
                )
            }

            item(key = "hero") {
                HomeHero(
                    currentAudio = currentAudio,
                    isPlaying = isPlaying,
                    isFavorite = currentAudio?.let { favoriteIds.contains(it.id.toString()) } == true,
                    progress = progress,
                    onPlayPause = onPlayPause,
                    onOpenPlayer = onOpenPlayer,
                    onShuffleAll = onShuffleAll,
                    songCount = allAudioList.size,
                    strings = strings
                )
            }

            if (topSongs.isNotEmpty()) {
                item(key = "for_you") {
                    HomeForYouSection(
                        songs = topSongs,
                        currentAudioId = currentAudioId,
                        isPlaying = isPlaying,
                        visualizerState = visualizerState,
                        strings = strings,
                        onPlaySong = onPlaySong
                    )
                }
            }

            if (recentlyPlayed.size >= 2) {
                item(key = "recently_played") {
                    HomeRecentlyPlayedSection(
                        songs = recentlyPlayed,
                        currentAudioId = currentAudioId,
                        strings = strings,
                        onPlaySong = onPlaySong
                    )
                }
            }

            if (recentlyAdded.isNotEmpty()) {
                item(key = "recently_added") {
                    HomeRecentlyAddedSection(
                        songs = recentlyAdded,
                        currentAudioId = currentAudioId,
                        strings = strings,
                        onPlaySong = onPlaySong
                    )
                }
            }

            if (artists.isNotEmpty()) {
                item(key = "artists") {
                    HomeArtistsSection(
                        artists = artists,
                        strings = strings,
                        onPlayArtist = onPlayArtist
                    )
                }
            }

            if (totalMinutes > 0L) {
                item(key = "stats") {
                    HomeListeningStat(totalMinutes = totalMinutes, strings = strings)
                }
            }

            if (allAudioList.isEmpty()) {
                item(key = "empty") {
                    HomeEmptyLibrary(strings = strings)
                }
            }
        }
    }
}

/**
 * Saate göre selamlama anahtarını döndürür. Saf fonksiyon; birim testi vardır.
 */
internal fun greetingKeyForHour(hour: Int): String = when (hour) {
    in 5..11 -> "good_morning"
    in 12..17 -> "good_afternoon"
    else -> "good_evening"
}

/** Ana sayfa raflarının arka planda hesaplanmış hali. */
private data class HomeSections(
    val topSongs: List<AudioFile>,
    val recentlyAdded: List<AudioFile>,
    val recentlyPlayed: List<AudioFile>,
    val artists: List<Map.Entry<String, List<AudioFile>>>
) {
    companion object {
        val EMPTY = HomeSections(emptyList(), emptyList(), emptyList(), emptyList())
    }
}
