package com.blackstack.beatbox.ui.components

import com.blackstack.beatbox.*
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion

import com.blackstack.beatbox.ui.home.HomeScreen
import com.blackstack.beatbox.ui.library.LibraryScreen
import com.blackstack.beatbox.ui.playlists.PlaylistsScreen

/**
 * Sekme içerikleri (Ana Sayfa, Çalma Listeleri, Kütüphane) ve sekme geçişi.
 *
 * Ekranlar yalnızca sunum yapar; çalma durumu ve kalıcı yazma işlemleri
 * [onCurrentAudioChange], [onCurrentPlaylistChange] ve [onPlaylistsChange]
 * üzerinden uygulama kabuğuna bildirilir.
 */
@Composable
fun BeatBoxTabContent(
    selectedTab: Int,
    allAudioList: List<AudioFile>,
    favoriteIds: Set<String>,
    playlists: List<Playlist>,
    currentAudio: AudioFile?,
    currentPlaylist: List<AudioFile>,
    isPlaying: Boolean,
    searchQuery: String,
    sortOption: SortOption,
    visualizerState: VisualizerState,
    strings: Strings,
    ctx: Context,
    prefs: SharedPreferences,
    player: MainActivity,
    overlays: BeatBoxOverlayState,
    onCurrentPlaylistChange: (List<AudioFile>) -> Unit,
    onCurrentAudioChange: (AudioFile) -> Unit,
    onFavoriteIdsChange: (Set<String>) -> Unit,
    onPlaylistsChange: (List<Playlist>) -> Unit,
    /** Ana sayfa compact top bar görünürlüğü değiştiğinde çağrılır. */
    onHomeCompactBarVisibleChange: ((Boolean) -> Unit)? = null,
    /** Kitaplık yenilendiğinde çağrılır (aşağı çekerek yenile). */
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false
) {
    val reducedMotion = LocalReducedMotion.current
    AnimatedContent(
        targetState = selectedTab,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(tween(BeatBoxMotion.Instant)) togetherWith
                    fadeOut(tween(BeatBoxMotion.Instant))
            } else {
                // Paylaşımlı eksen: eski sayfa hafifçe büyüyüp geri çekilirken
                // yeni sayfa derinlikten öne gelir. Yatay sürükleme yerine
                // "yer değiştirme" hissi veren, modern bir geçiş.
                (fadeIn(tween(BeatBoxMotion.Standard, easing = BeatBoxMotion.ExpressiveEasing)) +
                    scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(360, easing = BeatBoxMotion.ExpressiveEasing)
                    ) +
                    slideInVertically(
                        animationSpec = tween(360, easing = BeatBoxMotion.ExpressiveEasing)
                    ) { height -> height / 24 }) togetherWith
                    (fadeOut(tween(BeatBoxMotion.Quick)) +
                        scaleOut(
                            targetScale = 1.04f,
                            animationSpec = tween(280, easing = BeatBoxMotion.StandardEasing)
                        ) +
                        slideOutVertically(
                            animationSpec = tween(280, easing = BeatBoxMotion.StandardEasing)
                        ) { height -> -height / 24 })
            }
        },
        label = "tab_transition"
    ) { tab ->
        when (tab) {
            // Tab 0 = Home (Ana Sayfa)
            0 -> {
                HomeScreen(
                    allAudioList = allAudioList,
                    favoriteIds = favoriteIds,
                    playlists = playlists,
                    currentAudioId = currentAudio?.id,
                    currentAudio = currentAudio,
                    isPlaying = isPlaying,
                    onPlaySong = { song ->
                        onCurrentPlaylistChange(allAudioList)
                        onCurrentAudioChange(song)
                        player.playAudio(song)
                        updateSongStats(prefs, song)
                    },
                    onShuffleAll = {
                        if (allAudioList.isNotEmpty()) {
                            val shuffled = allAudioList.shuffled()
                            onCurrentPlaylistChange(shuffled)
                            onCurrentAudioChange(shuffled.first())
                            player.playAudio(shuffled.first())
                            updateSongStats(prefs, shuffled.first())
                        }
                    },
                    onPlayArtist = { songs ->
                        if (songs.isNotEmpty()) {
                            onCurrentPlaylistChange(songs)
                            onCurrentAudioChange(songs.first())
                            player.playAudio(songs.first())
                            updateSongStats(prefs, songs.first())
                        }
                    },
                    onOpenPlayer = { overlays.showPlayer = true },
                    onPlayPause = {
                        currentAudio?.let { audio ->
                            if (isPlaying) player.pauseAudio(audio) else player.resumeAudio(audio)
                        }
                    },
                    progress = {
                        val duration = player.getDuration()
                        if (duration > 0) {
                            player.getCurrentPosition().toFloat() / duration
                        } else {
                            0f
                        }
                    },
                    strings = strings,
                    ctx = ctx,
                    visualizerState = visualizerState,
                    onCompactBarVisibleChange = onHomeCompactBarVisibleChange,
                    onRefresh = onRefresh,
                    isRefreshing = isRefreshing
                )
            }
            // Tab 1 = Playlists (Çalma Listeleri)
            1 -> {
                PlaylistsScreen(
                    allAudioList = allAudioList,
                    favoriteIds = favoriteIds,
                    playlists = playlists,
                    strings = strings,
                    ctx = ctx,
                    onPlaylistOpen = { overlays.playlistDetail = it },
                    onPlaylistDelete = { p -> onPlaylistsChange(playlists.filter { it.id != p.id }) },
                    onPlaylistEdit = { overlays.editPlaylist = it },
                    onPlaylistsChange = { onPlaylistsChange(it) },
                    onCreatePlaylist = { overlays.showCreatePlaylist = true },
                    onRefresh = onRefresh,
                    isRefreshing = isRefreshing
                )
            }
            // Tab 2 = Library (Kütüphane)
            2 -> {
                LibraryScreen(
                    allAudioList = allAudioList,
                    searchQuery = searchQuery,
                    sortOption = sortOption,
                    strings = strings,
                    currentAudioId = currentAudio?.id,
                    isPlaying = isPlaying,
                    favoriteIds = favoriteIds,
                    onFavClick = { a -> player.toggleFavorite(a.id); onFavoriteIdsChange(player.getFavorites()) },
                    onAddFavorites = { songs ->
                        val current = player.getFavorites()
                        songs
                            .filterNot { current.contains(it.id.toString()) }
                            .forEach { player.toggleFavorite(it.id) }
                        onFavoriteIdsChange(player.getFavorites())
                    },
                    onAddToQueue = { songs ->
                        if (songs.isNotEmpty()) {
                            onCurrentPlaylistChange(
                                (currentPlaylist + songs).distinctBy { it.id }
                            )
                        }
                    },
                    onDeleteSong = { overlays.songToDelete = it; overlays.showDelete = true },
                    onSongClick = { a, list ->
                        if (currentPlaylist != list) onCurrentPlaylistChange(list)
                        onCurrentAudioChange(a)
                        player.playAudio(a)
                        updateSongStats(prefs, a)
                    },
                    onRefresh = onRefresh,
                    isRefreshing = isRefreshing
                )
            }
        }
    }
}
