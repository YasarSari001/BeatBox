package com.blackstack.beatbox.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.SortOption
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.groupSongsByArtist
import com.blackstack.beatbox.ui.components.BeatBoxSongRow
import com.blackstack.beatbox.ui.library.sortedByLibraryKey
import com.blackstack.beatbox.ui.components.EmptyArtKind
import com.blackstack.beatbox.ui.components.PullToRefresh
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import kotlinx.coroutines.launch

/**
 * Kütüphane filtreleri. "Hepsi" alfabetik bölümlere ayrılır, "Şarkılar"
 * düz liste gösterir; diğerleri albüm, sanatçı ve klasör gruplarını açar.
 */
internal enum class LibraryFilter { ALL, SONGS, FAVORITES, ALBUMS, ARTISTS, FOLDERS }

internal data class LibraryAlbum(
    val key: String,
    val title: String,
    val artist: String,
    val songs: List<AudioFile>
)

internal data class LibraryFolder(
    val name: String,
    val songs: List<AudioFile>
)

private const val LIST_HEADER_ITEMS = 2

/**
 * Kütüphane sekmesi.
 *
 * Arama ve sıralama dışarıdan gelir; ekran filtreleme, A-Z gezinme, seçim
 * ve sunumdan sorumludur. Kalıcı yazma işlemleri geri ça output.ırlarıyla
 * kabuğa bildirilir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    allAudioList: List<AudioFile>,
    searchQuery: String,
    sortOption: SortOption,
    strings: Strings,
    currentAudioId: Long?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onFavClick: (AudioFile) -> Unit,
    onAddFavorites: (List<AudioFile>) -> Unit,
    onAddToQueue: (List<AudioFile>) -> Unit,
    onDeleteSong: (AudioFile) -> Unit,
    onSongClick: (AudioFile, List<AudioFile>) -> Unit,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val stickyOffsetPx = with(LocalDensity.current) { (-58).dp.roundToPx() }

    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var compact by rememberSaveable { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Satır yer değiştirmesi ve ekleme/çıkma geçişleri sistem hareketi
    // kapalıyken oynamaz; liste anında güncellenir.
    val reducedMotion = LocalReducedMotion.current

    val search = searchQuery.trim()
    val searched = remember(allAudioList, search) {
        if (search.isBlank()) {
            allAudioList
        } else {
            allAudioList.filter { song ->
                song.title.contains(search, ignoreCase = true) ||
                    song.artist.contains(search, ignoreCase = true) ||
                    song.folderName.contains(search, ignoreCase = true)
            }
        }
    }

    val songsSorted = remember(searched, sortOption) {
        when (sortOption) {
            SortOption.NAME -> sortedByTitle(searched)
            SortOption.DATE -> searched.sortedByDescending { it.dateAdded }
        }
    }

    // Albüm/sanatçı/klasör grupları yalnızca o görünüm açıkken hesaplanır.
    // Kitaplık etiket geçişi listeyi sık yenilediğinden, varsayılan "Hepsi"
    // görünümünde her partide üç ayrı gruplama + sıralama yapılması zayıf
    // cihazda kaydırmayı kilitliyordu.
    val favoriteSongs = if (filter == LibraryFilter.FAVORITES) {
        remember(songsSorted, favoriteIds) {
            songsSorted.filter { favoriteIds.contains(it.id.toString()) }
        }
    } else {
        emptyList()
    }

    // "Hepsi" görünümü her zaman A-Z sıralıdır; harf başlıkları buna dayanır.
    val alphabetical = if (filter == LibraryFilter.ALL || filter == LibraryFilter.SONGS) {
        remember(songsSorted) { sortedByTitle(songsSorted) }
    } else {
        songsSorted
    }
    val songGroups = if (filter == LibraryFilter.ALL) {
        remember(alphabetical) { alphabetical.groupBy { libraryLetter(it.title) } }
    } else {
        emptyMap()
    }

    val albums = if (filter == LibraryFilter.ALBUMS) {
        remember(songsSorted, strings) {
            songsSorted
                .groupBy { song ->
                    if (song.albumId > 0) "album-${song.albumId}" else "folder-${song.folderName}"
                }
                .map { (key, songs) ->
                    val first = songs.first()
                    LibraryAlbum(
                        key = key,
                        title = first.albumName
                            ?.takeIf { it.isNotBlank() }
                            ?: first.folderName.ifBlank { strings.get("unknown_album") },
                        artist = first.albumArtist
                            ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                            ?: first.artist.takeIf { !it.equals("<unknown>", true) }
                            ?: strings.get("unknown_artist"),
                        songs = sortedByTitle(songs)
                    )
                }
                .let { sortedByLibraryKey(it) { album -> album.title } }
        }
    } else {
        emptyList()
    }

    val artists = if (filter == LibraryFilter.ARTISTS) {
        remember(songsSorted, strings) {
            groupSongsByArtist(songsSorted, strings.get("unknown_artist"))
                .entries
                .toList()
                .let { sortedByLibraryKey(it) { entry -> entry.key } }
        }
    } else {
        emptyList()
    }

    val folders = if (filter == LibraryFilter.FOLDERS) {
        remember(songsSorted) {
            songsSorted
                .groupBy { it.folderName.ifBlank { "/" } }
                .map { (name, songs) -> LibraryFolder(name, sortedByTitle(songs)) }
                .let { sortedByLibraryKey(it) { folder -> folder.name } }
        }
    } else {
        emptyList()
    }

    val totalMinutes = remember(allAudioList) {
        allAudioList.sumOf { it.duration } / 60_000L
    }

    LaunchedEffect(search, filter, sortOption) {
        selection = emptySet()
    }

    val selectionMode = selection.isNotEmpty()
    val selectedSongs = remember(selection, songsSorted) {
        songsSorted.filter { selection.contains(it.id) }
    }

    val displaySongs = when (filter) {
        LibraryFilter.ALL -> alphabetical
        LibraryFilter.SONGS -> songsSorted
        LibraryFilter.FAVORITES -> favoriteSongs
        LibraryFilter.ALBUMS,
        LibraryFilter.ARTISTS,
        LibraryFilter.FOLDERS -> emptyList()
    }

    // A-Z kaydırıcısının hedef öğe indeksleri. İlk iki öğe hero ve sticky
    // filtre çubuğudur; harf gruplarında araya başlık öğesi girer.
    val letterAnchors = remember(filter, alphabetical, songsSorted, favoriteSongs) {
        val anchors = linkedMapOf<Char, Int>()
        when (filter) {
            LibraryFilter.ALL -> {
                var index = LIST_HEADER_ITEMS
                songGroups.forEach { (letter, songs) ->
                    anchors[letter] = index
                    index += 1 + songs.size
                }
            }

            LibraryFilter.SONGS -> songsSorted.forEachIndexed { index, song ->
                anchors.putIfAbsent(libraryLetter(song.title), index + LIST_HEADER_ITEMS)
            }

            LibraryFilter.FAVORITES -> favoriteSongs.forEachIndexed { index, song ->
                anchors.putIfAbsent(libraryLetter(song.title), index + LIST_HEADER_ITEMS)
            }

            else -> Unit
        }
        anchors
    }

    fun toggleSelection(song: AudioFile) {
        selection = if (selection.contains(song.id)) {
            selection - song.id
        } else {
            selection + song.id
        }
    }

    fun playSelection() {
        if (selectedSongs.isNotEmpty()) {
            onSongClick(selectedSongs.first(), selectedSongs)
            selection = emptySet()
        }
    }

    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 132.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item(key = "library_hero") {
                    LibraryHero(
                        songCount = allAudioList.size,
                        favoriteCount = favoriteIds.size,
                        totalMinutes = totalMinutes,
                        strings = strings,
                        onShuffle = {
                            if (songsSorted.isNotEmpty()) {
                                val shuffled = songsSorted.shuffled()
                                onSongClick(shuffled.first(), shuffled)
                            }
                        }
                    )
                }

                stickyHeader(key = "library_filters") {
                    LibraryFilterBar(
                        filter = filter,
                        onFilterChange = { filter = it },
                        compact = compact,
                        onToggleDensity = { compact = !compact },
                        strings = strings,
                        selectionCount = selection.size,
                        onClearSelection = { selection = emptySet() },
                        onPlaySelected = { playSelection() },
                        onFavoriteSelected = { onAddFavorites(selectedSongs) },
                        onQueueSelected = { onAddToQueue(selectedSongs) }
                    )
                }

                when (filter) {
                    LibraryFilter.ALL -> {
                        if (alphabetical.isEmpty()) {
                            item(key = "empty") {
                                LibraryEmpty(
                                    title = emptyTitle(allAudioList, filter, strings),
                                    kind = emptyArtKind(allAudioList, filter, search)
                                )
                            }
                        } else {
                            songGroups.forEach { (letter, songs) ->
                                item(key = "letter-$letter") {
                                    LibraryLetterHeader(letter)
                                }
                                itemsIndexed(songs, key = { _, song -> "song-${song.id}" }) { index, song ->
                                    LibrarySongRow(
                                        modifier = if (reducedMotion) Modifier else Modifier.animateItem(),
                                        song = song,
                                        index = index,
                                        list = displaySongs,
                                        strings = strings,
                                        currentAudioId = currentAudioId,
                                        isPlaying = isPlaying,
                                        favoriteIds = favoriteIds,
                                        selection = selection,
                                        selectionMode = selectionMode,
                                        compact = compact,
                                        onFavClick = onFavClick,
                                        onDeleteSong = onDeleteSong,
                                        onSongClick = onSongClick,
                                        onToggleSelection = ::toggleSelection,
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (selection.isEmpty()) {
                                                selection = setOf(song.id)
                                            } else {
                                                toggleSelection(song)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LibraryFilter.SONGS,
                    LibraryFilter.FAVORITES -> {
                        if (displaySongs.isEmpty()) {
                            item(key = "empty") {
                                LibraryEmpty(
                                    title = emptyTitle(allAudioList, filter, strings),
                                    kind = emptyArtKind(allAudioList, filter, search)
                                )
                            }
                        } else {
                            itemsIndexed(displaySongs, key = { _, song -> "song-${song.id}" }) { index, song ->
                                LibrarySongRow(
                                    modifier = if (reducedMotion) Modifier else Modifier.animateItem(),
                                    song = song,
                                    index = index,
                                    list = displaySongs,
                                    strings = strings,
                                    currentAudioId = currentAudioId,
                                    isPlaying = isPlaying,
                                    favoriteIds = favoriteIds,
                                    selection = selection,
                                    selectionMode = selectionMode,
                                    compact = compact,
                                    onFavClick = onFavClick,
                                    onDeleteSong = onDeleteSong,
                                    onSongClick = onSongClick,
                                    onToggleSelection = ::toggleSelection,
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (selection.isEmpty()) {
                                            selection = setOf(song.id)
                                        } else {
                                            toggleSelection(song)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    LibraryFilter.ALBUMS -> {
                        if (albums.isEmpty()) {
                            item(key = "empty") {
                                LibraryEmpty(
                                    title = emptyTitle(allAudioList, filter, strings),
                                    kind = emptyArtKind(allAudioList, filter, search)
                                )
                            }
                        } else {
                            items(albums, key = { it.key }) { album ->
                                LibraryAlbumRow(
                                    album = album,
                                    strings = strings,
                                    onClick = {
                                        onSongClick(album.songs.first(), album.songs)
                                    }
                                )
                            }
                        }
                    }

                    LibraryFilter.ARTISTS -> {
                        if (artists.isEmpty()) {
                            item(key = "empty") {
                                LibraryEmpty(
                                    title = emptyTitle(allAudioList, filter, strings),
                                    kind = emptyArtKind(allAudioList, filter, search)
                                )
                            }
                        } else {
                            items(artists, key = { "artist-${it.key}" }) { entry ->
                                LibraryArtistRow(
                                    name = entry.key,
                                    songs = entry.value,
                                    strings = strings,
                                    onClick = {
                                        onSongClick(entry.value.first(), entry.value)
                                    }
                                )
                            }
                        }
                    }

                    LibraryFilter.FOLDERS -> {
                        if (folders.isEmpty()) {
                            item(key = "empty") {
                                LibraryEmpty(
                                    title = emptyTitle(allAudioList, filter, strings),
                                    kind = emptyArtKind(allAudioList, filter, search)
                                )
                            }
                        } else {
                            items(folders, key = { "folder-row-${it.name}" }) { folder ->
                                LibraryFolderRow(
                                    folder = folder,
                                    strings = strings,
                                    onClick = {
                                        onSongClick(folder.songs.first(), folder.songs)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (letterAnchors.isNotEmpty() && !selectionMode) {
                AlphabetScrubber(
                    letters = letterAnchors.keys.toList(),
                    onLetterSelected = { letter ->
                        letterAnchors[letter]?.let { index ->
                            scope.launch {
                                listState.scrollToItem(index, stickyOffsetPx)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun LibrarySongRow(
    song: AudioFile,
    index: Int,
    list: List<AudioFile>,
    strings: Strings,
    currentAudioId: Long?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    selection: Set<Long>,
    selectionMode: Boolean,
    compact: Boolean,
    onFavClick: (AudioFile) -> Unit,
    onDeleteSong: (AudioFile) -> Unit,
    onSongClick: (AudioFile, List<AudioFile>) -> Unit,
    onToggleSelection: (AudioFile) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    BeatBoxSongRow(
        audio = song,
        strings = strings,
        isCurrent = currentAudioId == song.id,
        isPlaying = isPlaying,
        isFavorite = favoriteIds.contains(song.id.toString()),
        isSelected = selection.contains(song.id),
        selectionMode = selectionMode,
        compact = compact,
        index = index,
        onFavClick = { onFavClick(song) },
        onDelete = { onDeleteSong(song) },
        onRowClick = {
            if (selectionMode) onToggleSelection(song) else onSongClick(song, list)
        },
        onRowLongClick = onLongPress,
        modifier = modifier
    )
}

private fun emptyTitle(
    allAudioList: List<AudioFile>,
    filter: LibraryFilter,
    strings: Strings
): String = when {
    allAudioList.isEmpty() -> strings.get("empty_library")
    filter == LibraryFilter.FAVORITES -> strings.get("no_favorites")
    else -> strings.get("no_results")
}

private fun emptyArtKind(
    allAudioList: List<AudioFile>,
    filter: LibraryFilter,
    searchQuery: String
): EmptyArtKind = when {
    allAudioList.isEmpty() -> EmptyArtKind.MUSIC
    filter == LibraryFilter.FOLDERS -> EmptyArtKind.FOLDER
    searchQuery.isNotBlank() -> EmptyArtKind.SEARCH
    filter == LibraryFilter.FAVORITES -> EmptyArtKind.MUSIC
    else -> EmptyArtKind.SEARCH
}
