package com.blackstack.beatbox.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.blackstack.beatbox.AppLanguage
import com.blackstack.beatbox.EQPreset
import com.blackstack.beatbox.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.Calendar
import java.util.prefs.Preferences
import javax.swing.JFileChooser

private enum class Page { HOME, LIBRARY, ALBUMS, QUEUE, LYRICS, SETTINGS }

/** Tekrar modu: kapalı, tüm listeyi döngüye al, şarkıyı tekrarla. */
private enum class RepeatMode {
    OFF, ALL, ONE;

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]
}

/** Sıradaki parçayı motora önceden bildirir (crossfade/gapless için). */
private fun prepareFollowing(
    engine: AudioEngine,
    track: Track,
    list: List<Track>,
    shuffled: Boolean,
    repeat: RepeatMode,
) {
    val next = if (repeat == RepeatMode.ONE) {
        null
    } else {
        pickTrack(track, list, shuffled, repeat, forward = true, automatic = true)
    }
    engine.prepareNext(next?.file, next?.durationSeconds ?: 0.0)
}

/** Shuffle/repeat kurallarına göre sıradaki (veya önceki) parçayı seçer. */
private fun pickTrack(
    current: Track?,
    list: List<Track>,
    shuffled: Boolean,
    repeat: RepeatMode,
    forward: Boolean,
    automatic: Boolean,
): Track? {
    if (list.isEmpty()) return null
    val index = list.indexOf(current)

    if (shuffled && list.size > 1) {
        return list.filter { it != current }.randomOrNull()
    }
    if (automatic && repeat == RepeatMode.ONE && current != null) return current

    if (forward) {
        if (index >= 0 && index < list.lastIndex) return list[index + 1]
        if (repeat == RepeatMode.ALL || !automatic) return list.firstOrNull()
        return null
    }

    if (index > 0) return list[index - 1]
    if (repeat == RepeatMode.ALL || !automatic) return list.lastOrNull()
    return list.firstOrNull()
}

/** Açılışta sessiz güncelleme denetiminin aralığı: günde bir kez. */
private const val UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

fun main() = application {
    val prefs = remember { Preferences.userRoot().node("com/blackstack/beatbox/desktop") }
    val engine = remember { AudioEngine() }
    var theme by remember { mutableStateOf(loadTheme(prefs)) }

    var nowPlayingVisible by remember { mutableStateOf(prefs.getBoolean("now_playing_visible", true)) }
    var closeToTray by remember { mutableStateOf(prefs.getBoolean("close_to_tray", true)) }
    var windowVisible by remember { mutableStateOf(true) }
    val trayController = remember { TrayController() }

    val savedWidth = prefs.getInt("window_width", 1320)
    val savedHeight = prefs.getInt("window_height", 840)
    val windowState = rememberWindowState(width = savedWidth.dp, height = savedHeight.dp)

    fun saveWindowSize() {
        runCatching {
            prefs.putInt("window_width", windowState.size.width.value.toInt())
            prefs.putInt("window_height", windowState.size.height.value.toInt())
        }
    }

    fun quit() {
        saveWindowSize()
        prefs.putBoolean("now_playing_visible", nowPlayingVisible)
        prefs.putBoolean("close_to_tray", closeToTray)
        engine.stop()
        exitApplication()
    }

    BeatBoxTray(
        controller = trayController,
        onShow = { windowVisible = true },
        onExit = { quit() },
    )

    Window(
        onPreviewKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else when (event.key) {
                Key.MediaPlayPause -> { trayController.onTogglePlay(); true }
                Key.MediaNext, Key.MediaSkipForward -> { trayController.onNext(); true }
                Key.MediaPrevious, Key.MediaSkipBackward -> { trayController.onPrevious(); true }
                Key.Spacebar, Key.DirectionRight, Key.DirectionLeft -> {
                    if (trayController.shortcutsBlocked || event.isCtrlPressed) {
                        false
                    } else {
                        when (event.key) {
                            Key.Spacebar -> trayController.onTogglePlay()
                            Key.DirectionRight -> trayController.onNext()
                            else -> trayController.onPrevious()
                        }
                        true
                    }
                }
                else -> false
            }
        },
        onCloseRequest = {
            saveWindowSize()
            if (closeToTray) {
                prefs.putBoolean("close_to_tray", true)
                windowVisible = false
            } else {
                quit()
            }
        },
        visible = windowVisible,
        title = "BeatBox",
        state = windowState
    ) {
        MaterialTheme(colorScheme = DesktopColorScheme(theme)) {
            BeatBoxDesktop(
                engine = engine,
                prefs = prefs,
                theme = theme,
                onThemeChange = { updated ->
                    theme = updated
                    saveTheme(prefs, updated)
                },
                trayController = trayController,
                closeToTray = closeToTray,
                onCloseToTrayChange = { enabled ->
                    closeToTray = enabled
                    prefs.putBoolean("close_to_tray", enabled)
                },
                onExit = { quit() },
            )
        }
    }
}

@Composable
private fun BeatBoxDesktop(
    engine: AudioEngine,
    prefs: Preferences,
    theme: DesktopTheme,
    onThemeChange: (DesktopTheme) -> Unit,
    trayController: TrayController,
    closeToTray: Boolean,
    onCloseToTrayChange: (Boolean) -> Unit,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val videoEngine = remember { VideoEngine() }

    var language by remember {
        mutableStateOf(
            AppLanguage.values().firstOrNull { it.name == prefs.get("language", AppLanguage.TR.name) }
                ?: AppLanguage.TR
        )
    }
    val strings = remember(language) { Strings(language) }

    var page by remember { mutableStateOf(Page.HOME) }
    var nowPlayingVisible by remember {
        mutableStateOf(prefs.getBoolean("now_playing_visible", true))
    }
    var shuffle by remember { mutableStateOf(prefs.getBoolean("shuffle", false)) }
    var repeatMode by remember {
        mutableStateOf(
            runCatching { RepeatMode.valueOf(prefs.get("repeat_mode", RepeatMode.OFF.name)) }
                .getOrDefault(RepeatMode.OFF)
        )
    }
    var searchFocused by remember { mutableStateOf(false) }
    var albumFilter by remember { mutableStateOf<String?>(null) }
    var queue by remember { mutableStateOf<List<Track>>(emptyList()) }
    var crossfadeSeconds by remember { mutableStateOf(prefs.getFloat("crossfade_seconds", 0f)) }
    var replayGain by remember { mutableStateOf(prefs.getBoolean("replay_gain", false)) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<MediaKind?>(null) }
    var sortByDate by remember { mutableStateOf(prefs.getBoolean("sort_by_date", false)) }
    var current by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0.0) }
    var volume by remember { mutableStateOf(prefs.getFloat("volume", 0.8f)) }
    var eqPreset by remember {
        mutableStateOf(
            runCatching { EQPreset.valueOf(prefs.get("eq_preset", EQPreset.NONE.name)) }
                .getOrDefault(EQPreset.NONE)
        )
    }
    var visualizerEnabled by remember { mutableStateOf(prefs.getBoolean("visualizer", true)) }
    var folder by remember {
        mutableStateOf(prefs.get("folder", null)?.let { File(it) }?.takeIf { it.isDirectory })
    }
    var error by remember { mutableStateOf<String?>(null) }
    var sleepMinutes by remember { mutableStateOf(prefs.getInt("sleep_minutes", 0)) }
    var sleepUntil by remember { mutableStateOf<Long?>(null) }
    var sleepRemaining by remember { mutableStateOf(0L) }
    var viewingPhoto by remember { mutableStateOf<Track?>(null) }
    var playingVideo by remember { mutableStateOf<Track?>(null) }
    var editingTrack by remember { mutableStateOf<Track?>(null) }
    var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }

    val albumName: (Track) -> String = { track ->
        track.album.ifBlank { track.folderName }.ifBlank { strings.get("unknown_album") }
    }
    val visible = remember(tracks, query, sortByDate, filter, albumFilter, strings) {
        val needle = query.trim()
        val filtered = tracks.filter {
            (filter == null || it.kind == filter) &&
                (albumFilter == null || albumName(it) == albumFilter) &&
                (needle.isEmpty() ||
                    it.title.contains(needle, ignoreCase = true) ||
                    it.artist.contains(needle, ignoreCase = true))
        }
        if (sortByDate) filtered.sortedByDescending { it.modifiedAt }
        else filtered.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
    }
    val audioTracks = remember(tracks) { tracks.filter { it.kind == MediaKind.AUDIO } }
    val albums = remember(audioTracks, strings) {
        audioTracks.groupBy(albumName).map { (key, group) ->
            AlbumGroup(
                key = key,
                title = key,
                artist = group.firstOrNull()?.artist?.takeIf { it.isNotBlank() }
                    ?: strings.get("artist"),
                tracks = group,
            )
        }.sortedBy { it.title.lowercase() }
    }
    val photoTracks = remember(tracks) { tracks.filter { it.kind == MediaKind.IMAGE } }
    val totalSeconds = remember(audioTracks) { audioTracks.sumOf { it.durationSeconds } }

    fun playTrack(track: Track, from: Double = 0.0) {
        error = null
        current = track
        position = from
        duration = track.durationSeconds
        engine.setVolume(volume)
        engine.setEqPreset(eqPreset)
        engine.play(track.file, from, track.durationSeconds)
        prepareFollowing(
            engine = engine,
            track = track,
            list = queue.ifEmpty { visible.filter { it.kind == MediaKind.AUDIO } },
            shuffled = shuffle,
            repeat = repeatMode,
        )
        isPlaying = true
    }

    fun openMedia(track: Track) {
        when (track.kind) {
            MediaKind.AUDIO -> playTrack(track)
            MediaKind.VIDEO -> {
                engine.pause()
                isPlaying = false
                playingVideo = track
            }
            MediaKind.IMAGE -> viewingPhoto = track
        }
    }

    fun step(delta: Int) {
        val list = queue.ifEmpty { visible.filter { it.kind == MediaKind.AUDIO } }
        val target = pickTrack(
            current = current,
            list = list,
            shuffled = shuffle,
            repeat = repeatMode,
            forward = delta > 0,
            automatic = false,
        )
        if (target != null) playTrack(target)
    }

    fun loadFolder(target: File) {
        scope.launch {
            scanning = true
            error = null
            val loaded = scanFolder(target, engine)
            tracks = loaded
            scanning = false
            engine.stop()
            current = null
            isPlaying = false
            position = 0.0
            duration = 0.0
        }
    }

    fun chooseFolder() {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = strings.get("folders")
            prefs.get("folder", null)?.let { saved ->
                File(saved).takeIf { it.isDirectory }?.let { currentDirectory = it }
            }
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return
        val picked = chooser.selectedFile ?: return
        prefs.put("folder", picked.absolutePath)
        folder = picked
        loadFolder(picked)
    }

    SideEffect {
        trayController.strings = strings
        trayController.isPlaying = isPlaying
        trayController.shortcutsBlocked = editingTrack != null || searchFocused
        trayController.title = current?.let { track ->
            listOf(track.title, track.artist)
                .filter { it.isNotBlank() }
                .joinToString(" — ")
                .ifBlank { track.file.name }
        }
        trayController.onTogglePlay = {
            if (current == null) {
                visible.firstOrNull { it.kind == MediaKind.AUDIO }?.let { playTrack(it) }
            } else if (isPlaying) {
                engine.pause()
                isPlaying = false
            } else {
                engine.resume()
                isPlaying = true
            }
        }
        trayController.onNext = { step(1) }
        trayController.onPrevious = { step(-1) }
        trayController.updateAvailable = updateState is UpdateUiState.Available
    }

    /*
     * Açılışta sessiz denetim: en fazla günde bir kez, kullanıcıyı bekletmeden.
     * Sonuç Hakkında satırında ve tepsi menüsünde görünür; ağ hatası sessizce
     * yutulur (elle denetimde hata mesajı gösterilmeye devam eder).
     */
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val last = prefs.getLong("update_last_check", 0L)
        if (now - last < UPDATE_CHECK_INTERVAL_MS) return@LaunchedEffect
        // Önceki oturumlardan kalan yarım indirmeler açılışta silinir.
        withContext(Dispatchers.IO) { DesktopUpdateChecker.cleanupTempInstallers() }
        updateState = UpdateUiState.Checking
        val result = DesktopUpdateChecker.check()
        updateState = when (result) {
            is DesktopUpdateChecker.Result.Available -> UpdateUiState.Available(result.update)
            DesktopUpdateChecker.Result.UpToDate -> UpdateUiState.UpToDate
            DesktopUpdateChecker.Result.Failed -> UpdateUiState.Idle
        }
        /*
         * Zaman damgası yalnızca sonuç alınan denetimden sonra yazılır; ağ
         * hatasında bir sonraki açılışta yeniden denenir.
         */
        if (result != DesktopUpdateChecker.Result.Failed) {
            prefs.putLong("update_last_check", System.currentTimeMillis())
        }
    }

    val visibleNow by rememberUpdatedState(visible)
    val currentNow by rememberUpdatedState(current)
    val queueNow by rememberUpdatedState(queue)
    val shuffleNow by rememberUpdatedState(shuffle)
    val repeatNow by rememberUpdatedState(repeatMode)
    val volumeNow by rememberUpdatedState(volume)
    val eqNow by rememberUpdatedState(eqPreset)

    DisposableEffect(engine) {
        engine.onCompleted = {
            val list = queueNow.ifEmpty { visibleNow.filter { it.kind == MediaKind.AUDIO } }
            val nextTrack = pickTrack(
                current = currentNow,
                list = list,
                shuffled = shuffleNow,
                repeat = repeatNow,
                forward = true,
                automatic = true,
            )
            if (nextTrack != null) {
                error = null
                current = nextTrack
                position = 0.0
                duration = nextTrack.durationSeconds
                engine.setVolume(volumeNow)
                engine.setEqPreset(eqNow)
                engine.play(nextTrack.file, 0.0, nextTrack.durationSeconds)
                prepareFollowing(engine, nextTrack, list, shuffleNow, repeatNow)
            } else {
                isPlaying = false
                position = 0.0
            }
        }
        engine.onTrackChange = { file, startedAt ->
            val list = queueNow.ifEmpty { visibleNow.filter { it.kind == MediaKind.AUDIO } }
            val track = list.firstOrNull { it.file == file }
            if (track != null) {
                error = null
                current = track
                position = startedAt
                duration = track.durationSeconds
                prepareFollowing(engine, track, list, shuffleNow, repeatNow)
            }
        }
        engine.onError = { message ->
            isPlaying = false
            error = message
        }
        onDispose {
            engine.onCompleted = null
            engine.onError = null
            engine.onTrackChange = null
            engine.stop()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (engine.isPlaying) position = engine.positionSeconds()
            delay(200)
        }
    }

    LaunchedEffect(crossfadeSeconds, replayGain) {
        engine.crossfadeSeconds = crossfadeSeconds
        engine.replayGainEnabled = replayGain
        prefs.putFloat("crossfade_seconds", crossfadeSeconds)
        prefs.putBoolean("replay_gain", replayGain)
    }

    LaunchedEffect(Unit) {
        engine.replayGainProvider = { file -> TagEditor.replayGainDb(file) }
        engine.setVolume(volume)
        engine.setEqPreset(eqPreset)
        folder?.let { loadFolder(it) }
    }

    LaunchedEffect(sleepUntil) {
        val until = sleepUntil
        if (until == null) {
            sleepRemaining = 0L
            return@LaunchedEffect
        }
        sleepRemaining = (until - System.currentTimeMillis()).coerceAtLeast(0L)
        while (true) {
            val left = until - System.currentTimeMillis()
            if (left <= 0) break
            sleepRemaining = left
            delay(500)
        }
        engine.pause()
        isPlaying = false
        sleepUntil = null
        sleepRemaining = 0L
        sleepMinutes = 0
        prefs.putInt("sleep_minutes", 0)
    }

    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(DesktopBackgroundGradient(theme)))
        ) {
            NavRail(
                strings = strings,
                page = page,
                onPageChange = { page = it },
                accent = MaterialTheme.colorScheme.primary,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                HeaderBar(
                    strings = strings,
                    page = page,
                    language = language,
                    folder = folder,
                    onLanguageChange = {
                        language = it
                        prefs.put("language", it.name)
                    },
                    onChooseFolder = { chooseFolder() },
                    onNowPlayingToggle = {
                        nowPlayingVisible = !nowPlayingVisible
                        prefs.putBoolean("now_playing_visible", nowPlayingVisible)
                    }
                )

                albumFilter?.let { selected ->
                    Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        FilterChip(
                            label = "${strings.dt("tag_album")}: $selected  ×",
                            selected = true,
                            onClick = { albumFilter = null },
                        )
                    }
                }

                if (scanning) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (page) {
                        Page.HOME -> HomePage(
                            strings = strings,
                            tracks = tracks,
                            visible = visible,
                            current = current,
                            isPlaying = isPlaying,
                            engine = engine,
                            visualizerEnabled = visualizerEnabled,
                            audioCount = audioTracks.size,
                            videoCount = tracks.count { it.kind == MediaKind.VIDEO },
                            photoCount = photoTracks.size,
                            totalSeconds = totalSeconds,
                            scanning = scanning,
                            onOpen = { openMedia(it) },
                            onTogglePlay = {
                                val track = audioTracks.firstOrNull()
                                when {
                                    current == null && track != null -> playTrack(track)
                                    current == null -> Unit
                                    isPlaying -> {
                                        engine.pause(); isPlaying = false
                                    }
                                    else -> {
                                        engine.resume(); isPlaying = true
                                    }
                                }
                            },
                            onChooseFolder = { chooseFolder() },
                        )

                        Page.LIBRARY -> LibraryPage(
                            strings = strings,
                            visible = visible,
                            tracks = tracks,
                            current = current,
                            isPlaying = isPlaying,
                            filter = filter,
                            onFilterChange = { filter = it },
                            scanning = scanning,
                            query = query,
                            onQueryChange = { query = it },
                            sortByDate = sortByDate,
                            onSortChange = {
                                sortByDate = it
                                prefs.putBoolean("sort_by_date", it)
                            },
                            onOpen = { openMedia(it) },
                            onEdit = { editingTrack = it },
                            onQueue = { track ->
                                if (track.kind == MediaKind.AUDIO && track !in queue) queue = queue + track
                            },
                            onSearchFocusChange = { searchFocused = it },
                            onChooseFolder = { chooseFolder() },
                        )

                        Page.ALBUMS -> AlbumsPage(
                            strings = strings,
                            albums = albums,
                            onOpen = { group ->
                                albumFilter = group.key
                                page = Page.LIBRARY
                            },
                        )

                        Page.QUEUE -> QueuePage(
                            strings = strings,
                            queue = queue,
                            current = current,
                            isPlaying = isPlaying,
                            onPlay = { playTrack(it) },
                            onRemove = { track -> queue = queue.filter { it != track } },
                            onClear = { queue = emptyList() },
                        )

                        Page.LYRICS -> LyricsPage(
                            strings = strings,
                            track = current,
                            positionSeconds = position,
                            onSeek = { seconds -> current?.let { playTrack(it, seconds) } },
                        )

                        Page.SETTINGS -> SettingsPage(
                            strings = strings,
                            language = language,
                            onLanguage = {
                                language = it
                                prefs.put("language", it.name)
                            },
                            theme = theme,
                            onThemeChange = onThemeChange,
                            visualizerEnabled = visualizerEnabled,
                            onVisualizerChange = {
                                visualizerEnabled = it
                                prefs.putBoolean("visualizer", it)
                            },
                            closeToTray = closeToTray,
                            onCloseToTrayChange = onCloseToTrayChange,
                            crossfadeSeconds = crossfadeSeconds,
                            onCrossfadeChange = { crossfadeSeconds = it },
                            replayGain = replayGain,
                            onReplayGainChange = { replayGain = it },
                            eqPreset = eqPreset,
                            onEqPresetChange = {
                                eqPreset = it
                                prefs.put("eq_preset", it.name)
                                engine.setEqPreset(it)
                            },
                            volume = volume,
                            onVolumeChange = {
                                volume = it
                                prefs.putFloat("volume", it)
                                engine.setVolume(it)
                                videoEngine.setVolume(it)
                            },
                            folder = folder,
                            trackCount = tracks.size,
                            totalSeconds = totalSeconds,
                            onChooseFolder = { chooseFolder() },
                            onRefresh = { folder?.let { loadFolder(it) } },
                            sleepMinutes = sleepMinutes,
                            onSleepMinutes = { minutes ->
                                sleepMinutes = minutes
                                prefs.putInt("sleep_minutes", minutes)
                                sleepUntil = if (minutes <= 0) null else System.currentTimeMillis() + minutes * 60_000L
                            },
                            sleepRemaining = sleepUntil?.let { sleepRemaining },
                            updateState = updateState,
                            onCheckUpdate = {
                                if (updateState !is UpdateUiState.Checking) {
                                    updateState = UpdateUiState.Checking
                                    scope.launch {
                                        updateState = when (val result = DesktopUpdateChecker.check()) {
                                            is DesktopUpdateChecker.Result.Available ->
                                                UpdateUiState.Available(result.update)
                                            DesktopUpdateChecker.Result.UpToDate -> UpdateUiState.UpToDate
                                            DesktopUpdateChecker.Result.Failed -> UpdateUiState.Failed
                                        }
                                    }
                                }
                            },
                            onDownloadUpdate = {
                                val available = (updateState as? UpdateUiState.Available)?.update
                                if (available != null) {
                                    updateState = UpdateUiState.Downloading(available, 0f)
                                    scope.launch {
                                        try {
                                            // İlerleme arka planda hesaplanır, ekran ana iş parçacığında güncellenir.
                                            val installer = DesktopUpdateChecker.downloadInstaller { progress ->
                                                withContext(Dispatchers.Main) {
                                                    updateState = UpdateUiState.Downloading(available, progress)
                                                }
                                            }
                                            updateState = UpdateUiState.ReadyToInstall(installer)
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            updateState = UpdateUiState.Failed
                                        }
                                    }
                                }
                            },
                            onInstallUpdate = {
                                val installer = (updateState as? UpdateUiState.ReadyToInstall)?.installer
                                if (installer != null) {
                                    updateState = UpdateUiState.Idle
                                    // Kurulum uygulama kapandıktan sonra başlar; önce dosyalar serbest bırakılır.
                                    if (DesktopUpdateChecker.installAfterExit(installer)) {
                                        onExit()
                                    } else {
                                        // Başlatılamayan indirme boşuna yer kaplamasın.
                                        installer.delete()
                                        updateState = UpdateUiState.Failed
                                    }
                                }
                            },
                        )
                    }

                    if (!nowPlayingVisible) {
                        current?.takeIf { it.kind == MediaKind.AUDIO }?.let { active ->
                            Spacer(Modifier.height(10.dp))
                            MiniPlayer(
                                strings = strings,
                                track = active,
                                isPlaying = isPlaying,
                                position = position,
                                duration = duration,
                                onTogglePlay = {
                                    if (isPlaying) {
                                        engine.pause()
                                        isPlaying = false
                                    } else {
                                        engine.resume()
                                        isPlaying = true
                                    }
                                },
                                onNext = { step(1) },
                                onShowPanel = {
                                    nowPlayingVisible = true
                                    prefs.putBoolean("now_playing_visible", true)
                                },
                            )
                        }
                    }
                }
            }

            if (nowPlayingVisible) { NowPlayingPanel(
                strings = strings,
                current = current,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                engine = engine,
                visualizerEnabled = visualizerEnabled,
                volume = volume,
                eqPreset = eqPreset,
                sleepRemaining = sleepUntil?.let { sleepRemaining },
                folder = folder,
                shuffle = shuffle,
                repeatMode = repeatMode,
                onShuffleChange = {
                    shuffle = it
                    prefs.putBoolean("shuffle", it)
                },
                onRepeatChange = {
                    repeatMode = it
                    prefs.put("repeat_mode", it.name)
                },
                onSeek = { target -> current?.let { playTrack(it, target) } },
                onTogglePlay = {
                    val track = current ?: audioTracks.firstOrNull()
                    when {
                        track == null -> Unit
                        current == null -> playTrack(track)
                        isPlaying -> {
                            engine.pause(); isPlaying = false
                        }
                        else -> {
                            engine.resume(); isPlaying = true
                        }
                    }
                },
                onVolume = {
                    volume = it
                    prefs.putFloat("volume", it)
                    engine.setVolume(it)
                },
                onPrev = {
                    if (position > 3.0 && current != null) playTrack(current!!) else step(-1)
                },
                onNext = { step(1) },
            )
        }
        }

        playingVideo?.let { track ->
            VideoPlayerOverlay(
                track = track,
                engine = videoEngine,
                onClose = {
                    videoEngine.stop()
                    playingVideo = null
                },
            )
        }

        viewingPhoto?.let { photo ->
            PhotoViewerOverlay(
                photo = photo,
                photos = photoTracks,
                onClose = { viewingPhoto = null },
                onNavigate = { viewingPhoto = it },
            )
        }

        editingTrack?.let { track ->
            TagEditorDialog(
                strings = strings,
                track = track,
                onDismiss = { editingTrack = null },
                onSaved = { updated ->
                    tracks = tracks.map { item ->
                        if (item.file == updated.file) updated else item
                    }
                    if (current?.file == updated.file) current = updated
                    editingTrack = null
                },
            )
        }
    }
}

@Composable
private fun NavRail(
    strings: Strings,
    page: Page,
    onPageChange: (Page) -> Unit,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .width(92.dp)
            .fillMaxHeight()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(accent, Color(0xFF22D3EE)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(10.dp))

        NavItem(strings.get("home"), Icons.Rounded.Home, page == Page.HOME) { onPageChange(Page.HOME) }
        NavItem(strings.get("library"), Icons.Rounded.LibraryMusic, page == Page.LIBRARY) { onPageChange(Page.LIBRARY) }
        NavItem(strings.get("albums"), Icons.Rounded.Album, page == Page.ALBUMS) { onPageChange(Page.ALBUMS) }
        NavItem(strings.dt("queue"), Icons.Rounded.QueueMusic, page == Page.QUEUE) { onPageChange(Page.QUEUE) }
        NavItem(strings.dt("lyrics"), Icons.Rounded.Lyrics, page == Page.LYRICS) { onPageChange(Page.LYRICS) }
        Spacer(Modifier.weight(1f))
        NavItem(strings.get("settings"), Icons.Rounded.Settings, page == Page.SETTINGS) { onPageChange(Page.SETTINGS) }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(23.dp),
            )
        }
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp).width(84.dp),
        )
    }
}

@Composable
private fun HeaderBar(
    strings: Strings,
    page: Page,
    language: AppLanguage,
    folder: File?,
    onLanguageChange: (AppLanguage) -> Unit,
    onChooseFolder: () -> Unit,
    onNowPlayingToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = when (page) {
                    Page.HOME -> strings.get("app_name")
                    Page.LIBRARY -> strings.get("library")
                    Page.ALBUMS -> strings.get("albums")
                    Page.QUEUE -> strings.dt("queue")
                    Page.LYRICS -> strings.dt("lyrics")
                    Page.SETTINGS -> strings.get("settings")
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = folder?.name?.takeIf { it.isNotBlank() } ?: strings.dt("no_folder"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        LanguagePicker(language, onLanguageChange)

        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onChooseFolder) {
            Icon(
                Icons.Rounded.FolderOpen,
                contentDescription = strings.get("folders"),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onNowPlayingToggle) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = strings.get("play_pause"),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun LanguagePicker(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Text(
                text = language.code.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { open = true }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            AppLanguage.values().forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onLanguageChange(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HomePage(
    strings: Strings,
    tracks: List<Track>,
    visible: List<Track>,
    current: Track?,
    isPlaying: Boolean,
    engine: AudioEngine,
    visualizerEnabled: Boolean,
    audioCount: Int,
    videoCount: Int,
    photoCount: Int,
    totalSeconds: Double,
    scanning: Boolean,
    onOpen: (Track) -> Unit,
    onTogglePlay: () -> Unit,
    onChooseFolder: () -> Unit,
) {
    if (tracks.isEmpty()) {
        EmptyLibrary(strings, scanning, onChooseFolder)
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text(
            text = greeting(strings),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(strings.dt("music"), "$audioCount", Modifier.weight(1f))
            StatCard(strings.dt("videos"), "$videoCount", Modifier.weight(1f))
            StatCard(strings.dt("photos"), "$photoCount", Modifier.weight(1f))
            StatCard(strings.dt("total_duration"), formatLongTime(totalSeconds), Modifier.weight(1.2f))
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = strings.dt("now_playing"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = current?.title ?: strings.dt("nothing_playing"),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = current?.artist?.takeIf { it.isNotBlank() } ?: strings.get("artist"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (visualizerEnabled) {
                    Visualizer(
                        engine = engine,
                        isPlaying = isPlaying,
                        accent = MaterialTheme.colorScheme.primary,
                        secondary = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.width(220.dp).height(64.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = strings.get("play"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.dt("recent_tracks"),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))

        val recent = remember(visible) { visible.sortedByDescending { it.modifiedAt }.take(14) }
        LazyColumn(Modifier.fillMaxSize()) {
            items(recent, key = { it.file.absolutePath }) { track ->
                TrackRow(
                    strings = strings,
                    track = track,
                    isCurrent = track == current,
                    isPlaying = isPlaying && track == current,
                    onClick = { onOpen(track) },
                )
            }
        }
    }
}

@Composable
private fun LibraryPage(
    strings: Strings,
    visible: List<Track>,
    tracks: List<Track>,
    current: Track?,
    isPlaying: Boolean,
    filter: MediaKind?,
    onFilterChange: (MediaKind?) -> Unit,
    scanning: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    sortByDate: Boolean,
    onSortChange: (Boolean) -> Unit,
    onOpen: (Track) -> Unit,
    onEdit: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onChooseFolder: () -> Unit,
) {
    if (tracks.isEmpty()) {
        EmptyLibrary(strings, scanning, onChooseFolder)
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text(strings.get("search"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.weight(1f).onFocusChanged { onSearchFocusChange(it.isFocused) },
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            var sortOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { sortOpen = true }) {
                    Icon(
                        Icons.Rounded.FilterList,
                        contentDescription = strings.dt("sort_by"),
                        tint = if (sortByDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(strings.dt("sort_title")) },
                        onClick = { onSortChange(false); sortOpen = false },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(strings.dt("sort_recent")) },
                        onClick = { onSortChange(true); sortOpen = false },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(strings.dt("all_media"), filter == null) { onFilterChange(null) }
            FilterChip(strings.dt("music"), filter == MediaKind.AUDIO) { onFilterChange(MediaKind.AUDIO) }
            FilterChip(strings.dt("videos"), filter == MediaKind.VIDEO) { onFilterChange(MediaKind.VIDEO) }
            FilterChip(strings.dt("photos"), filter == MediaKind.IMAGE) { onFilterChange(MediaKind.IMAGE) }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${visible.size}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (filter == MediaKind.IMAGE) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gridItems(visible, key = { it.file.absolutePath }) { photo ->
                    PhotoTile(photo, onClick = { onOpen(photo) })
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(visible, key = { it.file.absolutePath }) { track ->
                    TrackRow(
                        strings = strings,
                        track = track,
                        isCurrent = track == current,
                        isPlaying = isPlaying && track == current,
                        onClick = { onOpen(track) },
                        onEdit = if (track.kind == MediaKind.AUDIO) { { onEdit(track) } } else null,
                        onQueue = if (track.kind == MediaKind.AUDIO) { { onQueue(track) } } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhotoTile(photo: Track, onClick: () -> Unit) {
    var thumb by remember(photo.file) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photo.file) {
        thumb = withContext(Dispatchers.IO) { ThumbnailCache.get(photo.file) }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() },
    ) {
        val bitmap = thumb
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = photo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).size(28.dp),
            )
        }
        Text(
            text = photo.file.name,
            fontSize = 10.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun TrackRow(
    strings: Strings,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
) {
    val icon: ImageVector = when (track.kind) {
        MediaKind.AUDIO -> if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
        MediaKind.VIDEO -> Icons.Rounded.Movie
        MediaKind.IMAGE -> Icons.Rounded.Image
    }
    val trailing = when {
        track.kind == MediaKind.AUDIO -> formatTime(track.durationSeconds)
        else -> formatSize(track.sizeBytes)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 13.5.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    track.artist.takeIf { it.isNotBlank() },
                    track.folderName.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { strings.get("artist") },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onQueue != null && track.kind == MediaKind.AUDIO) {
            IconButton(onClick = onQueue, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.QueueMusic,
                    contentDescription = strings.dt("queue_add"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        if (onEdit != null && track.kind == MediaKind.AUDIO) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = strings.get("edit"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = trailing,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyLibrary(strings: Strings, scanning: Boolean, onChooseFolder: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = strings.get("empty_library"),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
        )
        Text(
            text = strings.dt("library_hint"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            modifier = Modifier.clickable { onChooseFolder() },
        ) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.get("folders"), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun NowPlayingPanel(
    strings: Strings,
    current: Track?,
    isPlaying: Boolean,
    position: Double,
    duration: Double,
    engine: AudioEngine,
    visualizerEnabled: Boolean,
    volume: Float,
    eqPreset: EQPreset,
    sleepRemaining: Long?,
    folder: File?,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    onShuffleChange: (Boolean) -> Unit,
    onRepeatChange: (RepeatMode) -> Unit,
    onSeek: (Double) -> Unit,
    onTogglePlay: () -> Unit,
    onVolume: (Float) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .width(392.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .border(width = 1.dp, color = Color.White.copy(alpha = GlassAlphaLow))
            .padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                            Color(0xFF1B1530),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(64.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = current?.title ?: strings.dt("nothing_playing"),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = current?.artist?.takeIf { it.isNotBlank() } ?: strings.get("artist"),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(10.dp))
        if (visualizerEnabled) {
            Visualizer(
                engine = engine,
                isPlaying = isPlaying,
                accent = accent,
                secondary = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth().height(86.dp),
            )
        } else {
            Spacer(Modifier.height(86.dp))
        }

        Spacer(Modifier.height(12.dp))

        var scrubbing by remember { mutableStateOf(false) }
        var scrubValue by remember { mutableStateOf(0f) }
        val maxSeconds = duration.toFloat().coerceAtLeast(0.01f)
        val shown = if (scrubbing) scrubValue else position.toFloat()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(position), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = shown.coerceIn(0f, maxSeconds),
                onValueChange = {
                    scrubbing = true
                    scrubValue = it
                },
                onValueChangeFinished = {
                    scrubbing = false
                    onSeek(scrubValue.toDouble())
                },
                valueRange = 0f..maxSeconds,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text(formatTime(duration), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = { onShuffleChange(!shuffle) }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = strings.get("shuffle"),
                    tint = if (shuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onPrev) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = strings.get("previous"), tint = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent, MaterialTheme.colorScheme.secondary)))
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = strings.get("play"),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = strings.get("next"), tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { onRepeatChange(repeatMode.next()) }) {
                Icon(
                    imageVector = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    },
                    contentDescription = when (repeatMode) {
                        RepeatMode.OFF -> strings.dt("repeat_off")
                        RepeatMode.ALL -> strings.dt("repeat_all")
                        RepeatMode.ONE -> strings.dt("repeat_one")
                    },
                    tint = if (repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.VolumeUp, contentDescription = strings.dt("volume"), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Slider(
                value = volume,
                onValueChange = onVolume,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PanelChip(strings.getEQPresetLabel(eqPreset))
            PanelChip(
                if (sleepRemaining != null) formatLongTime(sleepRemaining / 1000.0)
                else strings.get("sleep_timer")
            )
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = folder?.absolutePath ?: strings.dt("no_folder"),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PanelChip(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

private fun greeting(strings: Strings): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> strings.get("good_morning")
        hour < 18 -> strings.get("good_afternoon")
        else -> strings.get("good_evening")
    }
}

private fun loadTheme(prefs: Preferences): DesktopTheme = DesktopTheme(
    mode = runCatching {
        DesktopThemeMode.valueOf(prefs.get("theme_mode", DesktopThemeMode.DARK.name))
    }.getOrDefault(DesktopThemeMode.DARK),
    accentIndex = prefs.getInt("accent_index", 0).coerceIn(0, AccentPalette.lastIndex),
)

private fun saveTheme(prefs: Preferences, theme: DesktopTheme) {
    prefs.put("theme_mode", theme.mode.name)
    prefs.putInt("accent_index", theme.accentIndex)
}
