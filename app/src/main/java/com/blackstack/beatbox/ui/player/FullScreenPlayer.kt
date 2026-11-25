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
package com.blackstack.beatbox.ui.player
import com.blackstack.beatbox.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import com.blackstack.beatbox.ui.components.AlbumArtImage
import com.blackstack.beatbox.lyrics.LyricsDatabase
import com.blackstack.beatbox.lyrics.LyricsProvider
import com.blackstack.beatbox.ui.lyrics.LyricsScreen
import com.blackstack.beatbox.lyrics.LyricsViewModel
import com.blackstack.beatbox.ui.components.MusicVisualizer
import com.blackstack.beatbox.ui.components.VisualizerState
import com.blackstack.beatbox.ui.components.SongItem
import com.blackstack.beatbox.ui.components.formatTime
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import com.blackstack.beatbox.ui.theme.rememberPerformanceMode

/**
 * Metrolist düzenindeki tam ekran oynatıcı.
 *
 * Görsel yapı Metrolist'in modern telefon oynatıcısından uyarlanmıştır:
 * bulanık kapak arka planı, büyük yuvarlak kapak, başlık/sanatçı satırı,
 * ilerleme çubuğu, geniş hap biçimli taşıma kontrolleri ve bağlı eylem
 * çubuğu. BeatBox'a özgü visualizer, hız seçimi, ses düzeyi, ekolayzer,
 * uyku zamanlayıcısı ve sıra çekmecesi korunmuştur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    player: MainActivity,
    a: AudioFile,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    animDir: Int,
    strings: Strings,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onAdd: () -> Unit,
    currentPlaylist: List<AudioFile> = emptyList(),
    onSelectFromQueue: ((AudioFile) -> Unit)? = null,
    onOpenEQ: (() -> Unit)? = null,
    onOpenSleepTimer: (() -> Unit)? = null,
    visualizerState: VisualizerState,
    /** Mini oynatıcı kapağının açılış anındaki konumu; hero morph başlangıcı. */
    heroSourceBounds: Rect? = null
) {
    val context = LocalContext.current
    var pos by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showVolumeControl by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // ── Hero morph ────────────────────────────────────────────────────────
    // Kapak, mini oynatıcıdaki küçük boyutundan tam ekran boyutuna büyür.
    // Gerçek kapak, morph tamamlanana kadar görünmez kalır; geçiş katmanı
    // aradaki kareyi çizer.
    val reducedMotion = LocalReducedMotion.current
    val performanceMode = rememberPerformanceMode()
    val scope = rememberCoroutineScope()
    // Ölçüsü alınamamış veya sıfır kalmış bir kaynak kullanılmaz; aksi halde
    // kapak ekranın sol üst köşesinden büyüyerek gelirdi.
    val validSource = heroSourceBounds?.takeIf { it.width > 1f && it.height > 1f }
    val morph = remember { Animatable(if (validSource == null) 1f else 0f) }
    var artBounds by remember { mutableStateOf<Rect?>(null) }
    var closeStarted by remember { mutableStateOf(false) }

    LaunchedEffect(artBounds) {
        val target = artBounds
        if (validSource != null && target != null && morph.value < 1f && !closeStarted) {
            if (reducedMotion) {
                morph.snapTo(1f)
            } else {
                morph.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = if (performanceMode) 260 else BeatBoxMotion.Cinematic,
                        easing = BeatBoxMotion.ExpressiveEasing
                    )
                )
            }
        }
    }

    // Kapatma tek seferliktir: üst çubuk, geri tuşu ve aşağı sürükleme aynı
    // yoldan geçer; yarım kalan morph ikinci kez tetiklenmez. Kapak önce mini
    // oynatıcıdaki yerine küçülür, sonra ekran kapanır.
    val closePlayer: () -> Unit = {
        if (!closeStarted) {
            closeStarted = true
            if (validSource != null && !reducedMotion && morph.value > 0.98f) {
                scope.launch {
                    // Animasyon yarıda kesilse de (ör. uygulama arka plana
                    // alınınca coroutine iptal edilir) ekran kapanmalı; aksi
                    // halde bayrak takılı kalır ve oynatıcı bir daha kapanmaz.
                    try {
                        morph.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = if (performanceMode) 200 else 300,
                                easing = FastOutLinearInEasing
                            )
                        )
                    } finally {
                        onCollapse()
                    }
                }
            } else {
                onCollapse()
            }
        }
    }

    // Geri tuşu da ekranın kendi kapanış animasyonunu kullanır.
    BackHandler { closePlayer() }

    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }
    var currentVolume by remember(audioManager) {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0)
    }

    val lyricsViewModel: LyricsViewModel = viewModel(
        key = "lyrics_viewmodel_${strings.lang.code}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return (try {
                    val ctx = player.applicationContext
                    val database = LyricsDatabase.getDatabase(ctx)
                    val lyricsProvider = LyricsProvider(
                        context = ctx,
                        lyricsCache = database.lyricsCacheDao(),
                        strings = strings
                    )
                    LyricsViewModel(lyricsProvider, strings)
                } catch (e: Exception) {
                    android.util.Log.e("FullScreenPlayer", "Error creating LyricsViewModel", e)
                    LyricsViewModel(null, strings)
                }) as T
            }
        }
    )

    // Konum yalnızca oynatma sırasında ve kullanıcı çubuğu sürüklemiyorken
    // okunur; sürükleme sırasında değer parmakla birlikte ilerler.
    LaunchedEffect(isPlaying, performanceMode) {
        while (isPlaying) {
            if (!drag) pos = player.getCurrentPosition().toFloat()
            delay(if (performanceMode) 300 else 120)
        }
    }

    LaunchedEffect(a.id) {
        pos = player.getCurrentPosition().toFloat()
        currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: currentVolume
    }

    // Donanım ses tuşlarıyla yapılan değişiklikler panel açıkken yansısın.
    LaunchedEffect(showVolumeControl) {
        while (showVolumeControl) {
            currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: currentVolume
            delay(250)
        }
    }

    LaunchedEffect(showLyrics) {
        if (showLyrics) {
            player.musicService?.setLyricsPositionCallback { position ->
                lyricsViewModel.updatePlaybackPosition(position)
            }
        } else {
            player.musicService?.removeLyricsPositionCallback()
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.musicService?.removeLyricsPositionCallback() }
    }

    val shareSong: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${a.title} - ${a.artist}")
            putExtra(
                Intent.EXTRA_TEXT,
                "${strings.get("share_message")}\n\"${a.title}\" - ${a.artist}\n\n#BeatBox #Music"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, strings.get("share_song")))
    }

    // Tam ekran katmanı alttaki ekrana dokunuşları geçirmemeli.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { }
    ) {
        PlayerBackdrop(a)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            PlayerTopBar(
                modifier = Modifier.chromeEntrance(0),
                strings = strings,
                onCollapse = closePlayer,
                onOpenEQ = onOpenEQ,
                moreMenu = {
                    Box {
                        PlayerScaleIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = strings.get("more_options"),
                            onClick = { showMoreMenu = true }
                        )
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            if (onOpenSleepTimer != null) {
                                DropdownMenuItem(
                                    text = { Text(strings.get("sleep_timer")) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Nightlight, contentDescription = null)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenSleepTimer()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(strings.get("add_to_playlist")) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onAdd()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.get("song_info")) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Info, contentDescription = null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showSongInfoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.get("volume")) },
                                leadingIcon = {
                                    Icon(
                                        if (currentVolume == 0) {
                                            Icons.AutoMirrored.Rounded.VolumeOff
                                        } else {
                                            Icons.AutoMirrored.Rounded.VolumeUp
                                        },
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showVolumeControl = !showVolumeControl
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.get("speed")) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Speed, contentDescription = null)
                                },
                                trailingIcon = {
                                    Text(
                                        text = "${currentSpeed}x",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showSpeedDialog = true
                                }
                            )
                        }
                    }
                }
            )

            // Kapak alanı: kalan yüksekliği alır, kare boyutu iki eksende
            // sınırlanır; küçük ekranlarda taşma olmaz.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val artSize = minOf(maxWidth * 0.86f, maxHeight * 0.94f)
                AnimatedContent(
                    targetState = a,
                    transitionSpec = {
                        val duration = 360
                        val enter = if (animDir > 0) {
                            slideInHorizontally(animationSpec = tween(duration)) { it } +
                                fadeIn(animationSpec = tween(duration / 2)) +
                                scaleIn(initialScale = 0.92f, animationSpec = tween(duration))
                        } else {
                            slideInHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeIn(animationSpec = tween(duration / 2)) +
                                scaleIn(initialScale = 1.08f, animationSpec = tween(duration))
                        }
                        val exit = if (animDir > 0) {
                            slideOutHorizontally(animationSpec = tween(duration)) { -it } +
                                fadeOut(animationSpec = tween(duration / 2)) +
                                scaleOut(targetScale = 1.04f, animationSpec = tween(duration))
                        } else {
                            slideOutHorizontally(animationSpec = tween(duration)) { it } +
                                fadeOut(animationSpec = tween(duration / 2)) +
                                scaleOut(targetScale = 0.96f, animationSpec = tween(duration))
                        }
                        (enter togetherWith exit).using(SizeTransform(clip = false))
                    },
                    label = "song_transition",
                    modifier = Modifier.fillMaxSize()
                ) { song ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PlayerArtwork(
                            audio = song,
                            size = artSize,
                            alphaProvider = { morph.value },
                            onBoundsChange = { rect -> artBounds = rect },
                            onNext = onNext,
                            onPrev = onPrev,
                            onCollapse = closePlayer
                        )
                    }
                }
            }

            MusicVisualizer(
                isPlaying = isPlaying,
                state = visualizerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .chromeEntrance(1)
            )
            Spacer(Modifier.height(4.dp))

            PlayerTrackInfo(
                modifier = Modifier.chromeEntrance(2),
                audio = a,
                isFav = isFav,
                strings = strings,
                onToggleFav = onToggleFav,
                onShare = shareSong
            )

            Spacer(Modifier.height(2.dp))

            // Konum yalnızca bu bölümde okunur; 120 ms'lik konum
            // güncellemeleri oynatıcının tamamını değil, yalnızca bu satırı
            // yeniden compose eder.
            PlayerSeekSection(
                modifier = Modifier.chromeEntrance(3),
                positionProvider = { pos },
                durationMs = a.duration,
                audioKey = a.id,
                isPlaying = isPlaying,
                visualizerState = visualizerState,
                contentDescription = strings.get("seek_position"),
                onScrubStart = { drag = true },
                onScrub = { fraction -> pos = fraction * trackDurationOf(a.duration) },
                onScrubFinished = { fraction ->
                    val target = (fraction * trackDurationOf(a.duration)).toInt()
                    pos = target.toFloat()
                    player.seekTo(target)
                    drag = false
                }
            )

            Spacer(Modifier.height(8.dp))

            PlayerPrimaryControls(
                modifier = Modifier.chromeEntrance(4),
                isPlaying = isPlaying,
                strings = strings,
                onPrev = onPrev,
                onPlayPause = onPlayPause,
                onNext = onNext
            )

            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(
                visible = showVolumeControl,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(140))
            ) {
                PlayerVolumePanel(
                    volume = currentVolume,
                    maxVolume = maxVolume,
                    strings = strings,
                    onVolumeChange = { volume ->
                        currentVolume = volume
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                    },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            PlayerConnectedBar(
                modifier = Modifier.chromeEntrance(5),
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                isLyricsActive = showLyrics,
                isSleepTimerAvailable = onOpenSleepTimer != null,
                strings = strings,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
                onLyrics = { showLyrics = true },
                onQueue = { showQueueSheet = true },
                onSleepTimer = { onOpenSleepTimer?.invoke() }
            )

            Spacer(Modifier.height(8.dp))
        }

        // Hero morph geçiş katmanı: mini oynatıcıdaki kapak büyüyerek
        // tam ekran kapağıın yerine oturur. Kaynak ve hedef ölçüler kök
        // penceredeki koordinatlardır.
        // Geçiş katmanı ayrı bir bileşen: morph her karede ilerlerken yalnızca
        // bu katman yeniden compose olur, oynatıcının geri kalanı değil.
        HeroMorphOverlay(
            progress = { morph.value },
            source = validSource,
            target = artBounds,
            audio = a
        )

        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(animationSpec = tween(350, easing = FastOutSlowInEasing)) { it } +
                fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(animationSpec = tween(250, easing = FastOutLinearInEasing)) { it } +
                fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            LyricsScreen(
                audio = a,
                currentPositionMs = pos.toLong(),
                isPlaying = isPlaying,
                viewModel = lyricsViewModel,
                strings = strings,
                onSeekTo = { ms -> player.seekTo(ms.toInt()) },
                onDismiss = { showLyrics = false },
                durationMs = a.duration,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrev = onPrev
            )
        }

        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF141622),
                dragHandle = {
                    BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.30f))
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = strings.get("play_order"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 420.dp)
                    ) {
                        items(currentPlaylist) { song ->
                            SongItem(
                                a = song,
                                strings = strings,
                                isCurrent = song.id == a.id,
                                isPlaying = isPlaying && song.id == a.id,
                                isFavorite = false,
                                onFavClick = {},
                                onDelete = {},
                                onClick = {
                                    onSelectFromQueue?.invoke(song)
                                    showQueueSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showSongInfoDialog) {
            AlertDialog(
                onDismissRequest = { showSongInfoDialog = false },
                title = { Text(text = a.title, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${strings.get("artist")}: ${a.artist}")
                        Text("${strings.get("duration")}: ${formatTime(a.duration)}")
                        Text(
                            text = "URI: ${a.uri}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSongInfoDialog = false }) {
                        Text(strings.get("ok"))
                    }
                }
            )
        }

        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text(strings.get("speed"), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PLAYBACK_SPEEDS.forEach { speed ->
                            val selected = speed == currentSpeed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentSpeed = speed
                                        player.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${speed}x",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (selected) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text(strings.get("close"))
                    }
                }
            )
        }
    }
}

private fun trackDurationOf(durationMs: Long): Float =
    durationMs.toFloat().takeIf { it > 0f } ?: 1f

/**
 * Waveform ve süre satırı.
 *
 * Oynatma konumu yalnızca burada okunur; saniyede birkaç kez güncellenen
 * konum, oynatıcının tüm ağacını değil yalnızca bu bölümü geçersiz kılar.
 */
@Composable
private fun PlayerSeekSection(
    positionProvider: () -> Float,
    durationMs: Long,
    audioKey: Long,
    isPlaying: Boolean,
    visualizerState: VisualizerState,
    contentDescription: String,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pos = positionProvider()
    val trackDuration = trackDurationOf(durationMs)
    Column(modifier.fillMaxWidth()) {
        WaveformSeekBar(
            progress = (pos / trackDuration).coerceIn(0f, 1f),
            durationMs = durationMs,
            audioKey = audioKey,
            isPlaying = isPlaying,
            visualizerState = visualizerState,
            contentDescription = contentDescription,
            onScrub = { fraction ->
                onScrubStart()
                onScrub(fraction)
            },
            onScrubFinished = onScrubFinished,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(pos.toLong()),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = formatTime(durationMs),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

/**
 * Oynatıcı açılırken çevre birimlerin sırayla belirmesi için kademeli giriş.
 *
 * [index] her grubun gecikmesini belirler: üst çubuk önce iner, ardından
 * görselleştirici, şarkı bilgisi, ilerleme ve kontroller alttan yükselir.
 * Kapak kendi hero morph animasyonunu kullandığı için burada animasyonlanmaz.
 * Sistemde hareket kapalıysa öğe doğrudan görünür.
 */
@Composable
private fun Modifier.chromeEntrance(index: Int, distance: Dp = 16.dp): Modifier {
    if (LocalReducedMotion.current) return this
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60L + index * 38L)
        started = true
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = BeatBoxMotion.Standard,
            easing = BeatBoxMotion.ExpressiveEasing
        ),
        label = "player_chrome_$index"
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * distance.toPx()
    }
}

/**
 * Kapak morph geçiş katmanı.
 *
 * İlerleme yalnızca burada okunduğu için animasyon sürerken oynatıcının
 * tamamı değil, sadece bu katman her karede yeniden compose edilir.
 */
@Composable
private fun HeroMorphOverlay(
    progress: () -> Float,
    source: Rect?,
    target: Rect?,
    audio: AudioFile
) {
    val fraction = progress()
    if (source == null || target == null || fraction >= 0.999f) return
    val density = LocalDensity.current
    val left = source.left + (target.left - source.left) * fraction
    val top = source.top + (target.top - source.top) * fraction
    val widthPx = source.width + (target.width - source.width) * fraction
    val heightPx = source.height + (target.height - source.height) * fraction
    val radius = (15f + (26f - 15f) * fraction).dp
    val morphShape = RoundedCornerShape(radius)
    Box(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(
                with(density) { widthPx.toDp() },
                with(density) { heightPx.toDp() }
            )
            .shadow(26.dp, morphShape, spotColor = Color.Black)
            .clip(morphShape)
    ) {
        AlbumArtImage(audio, Modifier.fillMaxSize(), true)
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.12f), morphShape)
        )
    }
}

/**
 * Büyük yuvarlak albüm kapağı; yatay sürükleme şarkı değiştirir,
 * aşağı sürükleme oynatıcıyı kapatır.
 */
@Composable
private fun PlayerArtwork(
    audio: AudioFile,
    size: Dp,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    /** Hero morph ilerlemesi; 1 iken kapak tam görünür. */
    alphaProvider: () -> Float = { 1f },
    onBoundsChange: (Rect) -> Unit = {}
) {
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    // Aşağı sürükleme: kapak parmağın yarısı kadar direnir (lastik bant),
    // hafifçe küçülür ve soluklaşır; bırakınca geri yaylanır.
    var dismissDrag by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dismissThreshold = with(LocalDensity.current) { size.toPx() } * 0.22f
    val cancelDismiss: () -> Unit = {
        scope.launch {
            animate(dismissDrag, 0f) { value, _ -> dismissDrag = value }
        }
    }
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .size(size)
            .onGloballyPositioned { onBoundsChange(it.boundsInRoot()) }
            .graphicsLayer {
                // Durum katman aşamasında okunur; sürükleme recomposition tetiklemez.
                val progress = (dismissDrag / dismissThreshold).coerceIn(0f, 1f)
                alpha = alphaProvider() * (1f - 0.18f * progress)
                translationY = dismissDrag * 0.5f
                val dragScale = 1f - 0.06f * progress
                scaleX = dragScale
                scaleY = dragScale
            }
            .pointerInput(audio.id) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                        dismissDrag = 0f
                    },
                    onDragEnd = {
                        val horizontal = abs(totalDragX) > abs(totalDragY)
                        if (horizontal && abs(totalDragX) > 90f) {
                            if (totalDragX < 0) onNext() else onPrev()
                        } else if (!horizontal && totalDragY > dismissThreshold) {
                            // Eşik geçildi: kısa haptik, kapak yerine döner ve
                            // oynatıcı morph ile kapanır.
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dismissDrag = 0f
                            onCollapse()
                        } else {
                            cancelDismiss()
                        }
                    },
                    onDragCancel = { cancelDismiss() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        dismissDrag = if (abs(totalDragY) >= abs(totalDragX)) {
                            totalDragY.coerceAtLeast(0f)
                        } else {
                            0f
                        }
                    }
                )
            }
            .shadow(24.dp, shape, spotColor = Color.Black)
            .clip(shape)
    ) {
        AlbumArtImage(audio, Modifier.fillMaxSize(), true)
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
        )
    }
}

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
